package com.salvereminder;
import com.google.inject.Provides;
import com.salvereminder.core.ReminderManager;
import com.salvereminder.overlay.SalveReminderOverlay;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
@PluginDescriptor(
		name = "Salve Reminder"
)
public class SalveReminderPlugin extends Plugin {
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ReminderManager reminderManager;
	@Inject
	private SalveReminderOverlay salveReminderOverlay;
	@Override
	protected void startUp() {
		overlayManager.add(salveReminderOverlay);
	}
	@Override
	protected void shutDown() {
		reminderManager.reset();
		overlayManager.remove(salveReminderOverlay);
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) {
		reminderManager.onGameStateChanged(event);
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onInteractingChanged(InteractingChanged event) {
		reminderManager.onInteractingChanged(event);
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick tick) {
		reminderManager.onGameTick();
	}
	@Provides
	@SuppressWarnings("unused")
	SalveReminderConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SalveReminderConfig.class);
	}
}
