package com.salvereminder;
import com.google.inject.Provides;
import com.salvereminder.core.ReminderManager;
import com.salvereminder.overlay.SalveReminderOverlay;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
import javax.inject.Provider;
@PluginDescriptor(
		name = "Salve Reminder"
)
public class SalveReminderPlugin extends Plugin {
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ReminderManager reminderManager;
	@Inject
	private Provider<SalveReminderOverlay> salveReminderOverlayProvider;
	private volatile SalveReminderOverlay salveReminderOverlay = null;
	private boolean overlayAdded = false;
	@Override
	protected synchronized void startUp() {
		reminderManager.start();
		updateOverlay();
	}
	@Override
	protected synchronized void shutDown() {
		reminderManager.reset();
		if (overlayAdded) {
			overlayManager.remove(salveReminderOverlay);
			overlayAdded = false;
		}
		salveReminderOverlay = null;
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) {
		if (reminderManager.isTrackingDisabled()) return;
		reminderManager.onGameStateChanged(event);
		updateOverlay();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onInteractingChanged(InteractingChanged event) {
		if (reminderManager.isTrackingDisabled()) return;
		reminderManager.onInteractingChanged(event);
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onNpcChanged(NpcChanged event) {
		if (reminderManager.isTrackingDisabled()) return;
		reminderManager.onNpcChanged(event);
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onNpcDespawned(NpcDespawned event) {
		if (reminderManager.isTrackingDisabled()) return;
		reminderManager.onNpcDespawned(event);
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick tick) {
		if (!reminderManager.hasEnabledAlerts()) return;
		if (reminderManager.onGameTick()) updateOverlay();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (reminderManager.isTrackingDisabled()) return;
		if (reminderManager.onItemContainerChanged(event)) updateOverlay();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onConfigChanged(ConfigChanged event) {
		String group = event.getGroup();
		boolean ownConfig = SalveReminderConfig.GROUP.equals(group);
		boolean taskConfig = "slayer".equals(group) && "taskName".equals(event.getKey());
		if (!ownConfig && !taskConfig) return;
		if (taskConfig && reminderManager.isTrackingDisabled()) return;
		boolean appearanceConfig = ownConfig && isAppearanceConfig(event.getKey());
		if (!appearanceConfig) reminderManager.onConfigChanged(event);
		SalveReminderOverlay overlay = salveReminderOverlay;
		if (overlay != null && appearanceConfig) overlay.invalidateConfig();
		updateOverlay();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onProfileChanged(ProfileChanged event) {
		reminderManager.onProfileChanged();
		SalveReminderOverlay overlay = salveReminderOverlay;
		if (overlay != null) overlay.invalidateConfig();
		updateOverlay();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event) {
		if (reminderManager.isTrackingDisabled()) return;
		reminderManager.onRuneScapeProfileChanged();
		updateOverlay();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (reminderManager.isTrackingDisabled()) return;
		if (reminderManager.onMenuOptionClicked(event)) updateOverlay();
	}
	private synchronized void updateOverlay() {
		boolean show = reminderManager.isShowAlert();
		if (show == overlayAdded) {
			if (!show && !reminderManager.hasEnabledAlerts()) salveReminderOverlay = null;
			return;
		}
		if (show) {
			SalveReminderOverlay overlay = salveReminderOverlay;
			if (overlay == null) salveReminderOverlay = overlay = salveReminderOverlayProvider.get();
			overlayManager.add(overlay);
			overlay.resetSize();
		} else {
			overlayManager.remove(salveReminderOverlay);
			if (!reminderManager.hasEnabledAlerts()) salveReminderOverlay = null;
		}
		overlayAdded = show;
	}
	private static boolean isAppearanceConfig(String key) {
		return "flashBackground".equals(key) || "backgroundColor".equals(key) || "flashBackgroundColor".equals(key);
	}
	@Provides
	@SuppressWarnings("unused")
	SalveReminderConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SalveReminderConfig.class);
	}
}
