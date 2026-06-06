package com.salvereminder.overlay;
import com.salvereminder.SalveReminderConfig;
import com.salvereminder.SalveReminderPlugin;
import com.salvereminder.core.ReminderManager;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
public class SalveReminderOverlay extends OverlayPanel {
	private final Client client;
	private final SalveReminderConfig config;
	private final ReminderManager reminderManager;
	private final ItemManager itemManager;
	private final TooltipManager tooltipManager;
	private int lastItemId = -1;
	private ImageComponent iconComponent = null;
	@Inject
	public SalveReminderOverlay(Client client, SalveReminderConfig config, ReminderManager reminderManager, ItemManager itemManager, TooltipManager tooltipManager, SalveReminderPlugin plugin) {
		super(plugin);
		this.client = client;
		this.config = config;
		this.reminderManager = reminderManager;
		this.itemManager = itemManager;
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setResizable(false);
		setResettable(true);
		addMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Salve Reminder");
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!reminderManager.isShowAlert()) return null;
		final boolean useFlash = config.flashBackground() && reminderManager.isFlash();
		Color bgColor = useFlash ? config.flashBackgroundColor() : config.backgroundColor();
		panelComponent.setBackgroundColor(bgColor);
		int itemIDToDisplay = config.displayIcon().getItemID();
		if (reminderManager.isStackingWarningActive()) {
			int conflictingId = reminderManager.getConflictingHeadgearId();
			if (conflictingId != -1) itemIDToDisplay = conflictingId;
		}
		if (itemIDToDisplay != lastItemId) {
			final BufferedImage iconImage = itemManager.getImage(itemIDToDisplay);
			iconComponent = iconImage == null ? null : new ImageComponent(iconImage);
			lastItemId = itemIDToDisplay;
		}
		if (iconComponent != null) panelComponent.getChildren().add(iconComponent);
		Rectangle bounds = getBounds();
		Point mouse = client.getMouseCanvasPosition();
		if (bounds != null && bounds.contains(mouse.getX(), mouse.getY())) {
			final String tooltip = reminderManager.getTooltipReason();
			if (tooltip != null) tooltipManager.add(new Tooltip(tooltip));
		}
		return super.render(graphics);
	}
}
