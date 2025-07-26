package com.salvereminder.overlay;

import com.salvereminder.SalveReminderConfig;
import com.salvereminder.SalveReminderPlugin;
import com.salvereminder.core.ReminderManager;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
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
        if (!reminderManager.isShowAlert()) {
            return null;
        }
        panelComponent.getChildren().clear();
        final boolean useFlash = config.flashBackground() && reminderManager.isFlash();
        Color bgColor = useFlash ? config.flashBackgroundColor() : config.backgroundColor();
        panelComponent.setBackgroundColor(bgColor);
        int itemIDToDisplay = config.displayIcon().getItemID();
        if (reminderManager.isStackingWarningActive() && useFlash) {
            int conflictingId = reminderManager.getConflictingHeadgearId();
            if (conflictingId != -1) {
                itemIDToDisplay = conflictingId;
            }
        }
        final BufferedImage iconImage = itemManager.getImage(itemIDToDisplay);
        if (iconImage != null) {
            panelComponent.getChildren().add(new ImageComponent(iconImage));
        }
        Rectangle bounds = getBounds();
        if (bounds != null && bounds.contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY())) {
            final String tooltip = reminderManager.getTooltipReason();
            if (tooltip != null) {
                tooltipManager.add(new Tooltip(tooltip));
            }
        }
        return super.render(graphics);
    }
}