package net.runelite.client.plugins.microbot.lizardmanshaman;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.Rs2NpcCache;
import net.runelite.client.plugins.microbot.api.tileitem.Rs2TileItemCache;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LizardmanShamanScript extends Script {

    public static double version = 1.0;

    private final Rs2NpcCache rs2NpcCache = Microbot.getRs2NpcCache();
    private final Rs2TileItemCache rs2TileItemCache = Microbot.getRs2TileItemCache();
    private final Rs2TileObjectCache rs2TileObjectCache = Microbot.getRs2TileObjectCache();

    private enum State {
        BANKING,
        WALKING_TO_TEMPLE,
        ENTERING_TEMPLE,
        MOVING_TO_SPOT,
        LOOTING,
        FIGHTING
    }

    private static final int SHAMAN_ID = 8565;
    private static final int TEMPLE_ENTRANCE_OBJECT = 34405;

    private static final WorldPoint TEMPLE_ENTRANCE = new WorldPoint(1312, 3686, 0);
    private static final WorldPoint PRIMARY_TILE = new WorldPoint(1294, 10092, 0);
    private static final WorldPoint FALLBACK_TILE = new WorldPoint(1298, 10092, 0);

    private State state = State.WALKING_TO_TEMPLE;

    public boolean run(LizardmanShamanConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                if (config.inventorySetup() == null) {
                    Microbot.status = "Select an inventory setup first";
                    return;
                }

                state = getState(config);
                Microbot.status = "Lizardman Shamans - " + state;

                switch (state) {
                    case BANKING:
                        handleBanking(config);
                        break;
                    case WALKING_TO_TEMPLE:
                        handleWalkToTemple();
                        break;
                    case ENTERING_TEMPLE:
                        handleEnterTemple();
                        break;
                    case MOVING_TO_SPOT:
                        handleMoveToSpot();
                        break;
                    case LOOTING:
                        handleLoot(config);
                        break;
                    case FIGHTING:
                        handleFight(config);
                        break;
                }

            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
                Microbot.log("LizardmanShamanScript: " + ex.getMessage());
            }
        }, 0, 400, TimeUnit.MILLISECONDS);

        return true;
    }

    private State getState(LizardmanShamanConfig config) {
        if (needBanking(config)) {
            return State.BANKING;
        }

        if (!isInTemple()) {
            if (isAtTempleEntrance()) {
                return State.ENTERING_TEMPLE;
            }
            return State.WALKING_TO_TEMPLE;
        }

        if (shouldLoot(config)) {
            return State.LOOTING;
        }

        if (Rs2Player.getWorldLocation().distanceTo(PRIMARY_TILE) > 2 && !Rs2Combat.inCombat()) {
            return State.MOVING_TO_SPOT;
        }

        return State.FIGHTING;
    }

    private void handleBanking(LizardmanShamanConfig config) {
        if (!Rs2Bank.walkToBankAndUseBank()) {
            return;
        }

        Rs2InventorySetup setup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);

        if (!setup.doesEquipmentMatch()) {
            setup.loadEquipment();
            return;
        }

        if (!setup.doesInventoryMatch()) {
            setup.loadInventory();
            return;
        }

        Rs2Bank.closeBank();
    }

    private void handleWalkToTemple() {
        if (!Rs2Player.isMoving()) {
            Rs2Walker.walkTo(TEMPLE_ENTRANCE, 5);
        }
    }

    private void handleEnterTemple() {
        Rs2TileObjectModel entrance = rs2TileObjectCache.query()
                .withId(TEMPLE_ENTRANCE_OBJECT)
                .nearest();

        if (entrance == null) {
            return;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation != null && playerLocation.distanceTo(entrance.getWorldLocation()) > 51) {
            Rs2Walker.walkTo(entrance.getWorldLocation());
            return;
        }

        if (entrance.click("Enter")) {
            sleepUntil(this::isInTemple, 5000);
        }
    }

    private void handleMoveToSpot() {
        if (!Rs2Player.getWorldLocation().equals(PRIMARY_TILE)) {
            Rs2Walker.walkFastCanvas(PRIMARY_TILE);
        }
    }

    private void handleFight(LizardmanShamanConfig config) {
        Rs2Player.eatAt(config.eatAt());
        handlePrayers(config);

        if (Rs2Player.isInteracting() || Rs2Combat.inCombat()) {
            return;
        }

        var localPlayer = Microbot.getClient().getLocalPlayer();
        var shaman = rs2NpcCache.query()
                .withId(SHAMAN_ID)
                .where(npc -> !npc.isDead())
                .where(npc -> !npc.isInteracting() || Objects.equals(npc.getInteracting(), localPlayer))
                .toList()
                .stream()
                .min(Comparator.comparingInt(npc ->
                        npc.getWorldLocation().distanceTo(Rs2Player.getWorldLocation())))
                .orElse(null);

        if (shaman == null) {
            return;
        }

        int distance = shaman.getWorldLocation().distanceTo(Rs2Player.getWorldLocation());
        if (distance <= 1 && !Rs2Player.isMoving()) {
            Rs2Walker.walkFastCanvas(FALLBACK_TILE);
            return;
        }

        shaman.click("Attack");
    }

    private void handleLoot(LizardmanShamanConfig config) {
        String[] names = Arrays.stream(config.lootItems().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("coins"))
                .toArray(String[]::new);

        for (String name : names) {
            if (rs2TileItemCache.query().withName(name).within(config.lootRange()).interact("Take")) {
                return;
            }
        }

        rs2TileItemCache.query().withName("Coins").within(config.lootRange()).interact("Take");
    }

    private void handlePrayers(LizardmanShamanConfig config) {
        if (!config.usePrayer()) {
            return;
        }

        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, true);

        Rs2PrayerEnum bestRangePrayer = Rs2Prayer.getBestRangePrayer();
        if (bestRangePrayer != null) {
            Rs2Prayer.toggle(bestRangePrayer, true);
        }
    }

    private boolean needBanking(LizardmanShamanConfig config) {
        if (Rs2Inventory.isFull()) {
            return true;
        }

        return Rs2Inventory.getInventoryFood().size() <= config.minFoodToContinue();
    }

    private boolean shouldLoot(LizardmanShamanConfig config) {
        if (Rs2Combat.inCombat()) {
            return false;
        }

        if (rs2TileItemCache.query().withName("Coins").within(config.lootRange()).nearest() != null) {
            return true;
        }

        return Arrays.stream(config.lootItems().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(name -> rs2TileItemCache.query().withName(name).within(config.lootRange()).nearest() != null);
    }

    private boolean isAtTempleEntrance() {
        return Rs2Player.getWorldLocation().distanceTo(TEMPLE_ENTRANCE) <= 8;
    }

    private boolean isInTemple() {
        WorldPoint wp = Rs2Player.getWorldLocation();
        return wp.getX() >= 1289 && wp.getX() <= 1333
                && wp.getY() >= 10067 && wp.getY() <= 10100
                && wp.getPlane() == 0;
    }

    @Override
    public void shutdown() {
        Rs2Prayer.disableAllPrayers();
        super.shutdown();
    }
}