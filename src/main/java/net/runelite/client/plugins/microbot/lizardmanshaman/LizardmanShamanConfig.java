package net.runelite.client.plugins.microbot.lizardmanshaman;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.plugins.microbot.inventorysetups.InventorySetup;

@ConfigGroup("lizardmanshaman")
public interface LizardmanShamanConfig extends Config {

    @ConfigItem(
            keyName = "inventorySetup",
            name = "Inventory setup",
            description = "Inventory setup to use for banking and gearing",
            position = 0
    )
    default InventorySetup inventorySetup() {
        return null;
    }

    @ConfigItem(
            keyName = "usePrayer",
            name = "Use prayers",
            description = "Use Protect from Missiles and best ranged prayer",
            position = 1
    )
    default boolean usePrayer() {
        return true;
    }

    @ConfigItem(
            keyName = "eatAt",
            name = "Eat at HP",
            description = "Eat when HP falls to this value",
            position = 2
    )
    default int eatAt() {
        return 45;
    }

    @ConfigItem(
            keyName = "minFoodToContinue",
            name = "Minimum food left",
            description = "Bank when food is at or below this amount",
            position = 3
    )
    default int minFoodToContinue() {
        return 2;
    }

    @ConfigItem(
            keyName = "lootRange",
            name = "Loot range",
            description = "How far to loot from the player",
            position = 4
    )
    default int lootRange() {
        return 8;
    }

    @ConfigItem(
            keyName = "lootItems",
            name = "Loot items",
            description = "Comma separated list of item names to loot",
            position = 5
    )
    default String lootItems() {
        return "dragon warhammer,rune med helm,rune full helm,grimy ranarr weed,coins";
    }
}