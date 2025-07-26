package com.salvereminder;

import com.google.inject.Provides;
import com.salvereminder.core.ReminderManager;
import com.salvereminder.overlay.SalveReminderOverlay;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
		name = "Salve Reminder"
)
public class SalveReminderPlugin extends Plugin {
	@Inject
	@SuppressWarnings("unused")
	private OverlayManager overlayManager;
	@Inject
	@SuppressWarnings("unused")
	private ReminderManager reminderManager;
	@Inject
	@SuppressWarnings("unused")
	private SalveReminderOverlay salveReminderOverlay;
	@Override
	protected void startUp() {
		reminderManager.start();
		overlayManager.add(salveReminderOverlay);
	}
	@Override
	protected void shutDown() {
		reminderManager.stop();
		overlayManager.remove(salveReminderOverlay);
	}
	@Provides
	@SuppressWarnings("unused")
	SalveReminderConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SalveReminderConfig.class);
	}
}