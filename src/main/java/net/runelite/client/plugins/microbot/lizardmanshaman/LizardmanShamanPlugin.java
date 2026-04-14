package net.runelite.client.plugins.microbot.lizardmanshaman;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Lizardman Shamans",
        description = "Basic ranged Lizardman Shaman bot",
        tags = {"microbot", "combat", "shaman", "lizardman"},
        enabledByDefault = false
)
@Slf4j
public class LizardmanShamanPlugin extends Plugin {

    @Inject
    private LizardmanShamanConfig config;

    @Inject
    private LizardmanShamanScript script;

    @Provides
    LizardmanShamanConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LizardmanShamanConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
    }
}