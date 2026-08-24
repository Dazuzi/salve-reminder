package com.salvereminder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.salvereminder.core.ReminderManager;
import com.salvereminder.overlay.SalveReminderOverlay;
import net.runelite.api.events.GameTick;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.junit.Test;
import javax.inject.Provider;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
public class SalveReminderPluginLifecycleTest {
	@Test
	@SuppressWarnings("unchecked")
	public void overlayExistsOnlyWhileObservable() {
		OverlayManager overlayManager = mock(OverlayManager.class);
		ReminderManager reminderManager = mock(ReminderManager.class);
		SalveReminderPlugin plugin = new SalveReminderPlugin();
		SalveReminderOverlay overlay = new SalveReminderOverlay(mock(SalveReminderConfig.class), reminderManager, mock(ItemManager.class), mock(SpriteManager.class), mock(TooltipManager.class), plugin);
		Provider<SalveReminderOverlay> overlayProvider = mock(Provider.class);
		when(overlayProvider.get()).thenReturn(overlay);
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(OverlayManager.class).toInstance(overlayManager);
				bind(ReminderManager.class).toInstance(reminderManager);
				bind(SalveReminderOverlay.class).toProvider(overlayProvider);
			}
		}).injectMembers(plugin);
		when(reminderManager.isShowAlert()).thenReturn(false);
		plugin.startUp();
		verify(reminderManager).start();
		verify(overlayProvider, never()).get();
		verify(overlayManager, never()).add(overlay);
		when(reminderManager.isShowAlert()).thenReturn(true);
		when(reminderManager.hasEnabledAlerts()).thenReturn(true);
		when(reminderManager.onGameTick()).thenReturn(true, false, true);
		plugin.onGameTick(new GameTick());
		verify(overlayProvider, times(1)).get();
		plugin.onGameTick(new GameTick());
		verify(overlayManager, times(1)).add(overlay);
		when(reminderManager.isShowAlert()).thenReturn(false);
		plugin.onGameTick(new GameTick());
		verify(overlayManager, times(1)).remove(overlay);
		plugin.shutDown();
		verify(reminderManager).reset();
		verify(overlayManager, times(1)).remove(overlay);
	}
}
