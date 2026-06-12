package com.salvereminder.overlay;
import com.salvereminder.SalveReminderConfig;
import com.salvereminder.SalveReminderPlugin;
import com.salvereminder.core.ReminderManager;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.MenuAction;
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
public class SalveReminderOverlay extends OverlayPanel {
	private static final int BORDER = ComponentConstants.STANDARD_BORDER;
	private static final int ICON_SIZE = Constants.ITEM_SPRITE_WIDTH;
	private static final int OVERLAY_SIZE = ICON_SIZE + BORDER * 2;
	private static final int DEBUG_FRAMES = 20;
	private final Client client;
	private final SalveReminderConfig config;
	private final ReminderManager reminderManager;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	private final TooltipManager tooltipManager;
	private int lastItemId = -1;
	private int lastSpriteId = -1;
	private boolean lastCrossed = false;
	private BufferedImage iconImage = null;
	private SalveReminderConfig.DebugAlert lastDebugAlert = SalveReminderConfig.DebugAlert.OFF;
	private int debugFrames = 0;
	private String debugLastLocationState = null;
	@Inject
	public SalveReminderOverlay(Client client, SalveReminderConfig config, ReminderManager reminderManager, ItemManager itemManager, SpriteManager spriteManager, TooltipManager tooltipManager, SalveReminderPlugin plugin) {
		super(plugin);
		this.client = client;
		this.config = config;
		this.reminderManager = reminderManager;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setResizable(false);
		setPreferredSize(new Dimension(OVERLAY_SIZE, OVERLAY_SIZE));
		setResettable(true);
		addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Ignore NPC", "Salve Reminder", e -> reminderManager.ignoreCurrentTarget());
		addMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Salve Reminder");
	}
	public void resetSize() {
		Dimension size = size();
		setPreferredSize(size);
		getBounds().setSize(size);
	}
	public void debugStartUp() {
		SalveReminderConfig.DebugAlert alert = getDebugAlert();
		if (alert == SalveReminderConfig.DebugAlert.OFF) return;
		lastDebugAlert = alert;
		debugFrames = 0;
		debugLastLocationState = null;
		System.out.println("[salve-reminder overlay] startUp alert=" + alert + " " + debugLocationState());
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		SalveReminderConfig.DebugAlert debugAlert = getDebugAlert();
		updateDebug(debugAlert);
		if (!reminderManager.isShowAlert()) {
			if (getPreferredLocation() == null) return size();
			return null;
		}
		final boolean useFlash = config.flashBackground() && reminderManager.isFlash();
		Color bgColor = useFlash ? config.flashBackgroundColor() : config.backgroundColor();
		panelComponent.setBackgroundColor(bgColor);
		int itemIDToDisplay = reminderManager.getAlertItemId();
		int spriteIDToDisplay = reminderManager.getAlertSpriteId();
		boolean crossed = reminderManager.isAlertIconCrossed();
		if (itemIDToDisplay == -1) itemIDToDisplay = config.displayIcon().getItemID();
		if (itemIDToDisplay != lastItemId || spriteIDToDisplay != lastSpriteId || crossed != lastCrossed) {
			iconImage = null;
			if (spriteIDToDisplay != -1) iconImage = spriteManager.getSprite(spriteIDToDisplay, 0);
			if (iconImage == null) iconImage = itemManager.getImage(itemIDToDisplay);
			if (iconImage != null && crossed) iconImage = addCross(iconImage);
			lastItemId = itemIDToDisplay;
			lastSpriteId = spriteIDToDisplay;
			lastCrossed = crossed;
		}
		debugRender(debugAlert, itemIDToDisplay, spriteIDToDisplay, crossed);
		BackgroundComponent background = new BackgroundComponent();
		background.setBackgroundColor(bgColor);
		background.setRectangle(new Rectangle(0, 0, OVERLAY_SIZE, OVERLAY_SIZE));
		background.render(graphics);
		if (iconImage != null) renderIcon(graphics, iconImage);
		Rectangle bounds = new Rectangle(getBounds().x, getBounds().y, OVERLAY_SIZE, OVERLAY_SIZE);
		Point mouse = client.getMouseCanvasPosition();
		if (bounds.contains(mouse.getX(), mouse.getY())) {
			final String tooltip = reminderManager.getTooltipReason();
			if (tooltip != null) tooltipManager.add(new Tooltip(tooltip));
		}
		return size();
	}
	private void updateDebug(SalveReminderConfig.DebugAlert alert) {
		if (alert == lastDebugAlert) return;
		debugFrames = 0;
		debugLastLocationState = null;
		if (alert == SalveReminderConfig.DebugAlert.OFF) System.out.println("[salve-reminder overlay] debugOff " + debugLocationState());
		else System.out.println("[salve-reminder overlay] debugOn alert=" + alert + " " + debugLocationState());
		lastDebugAlert = alert;
	}
	private void debugRender(SalveReminderConfig.DebugAlert alert, int itemId, int spriteId, boolean crossed) {
		if (alert == SalveReminderConfig.DebugAlert.OFF) return;
		String locationState = debugLocationState();
		if (debugFrames < DEBUG_FRAMES || !locationState.equals(debugLastLocationState)) System.out.println("[salve-reminder overlay] render frame=" + debugFrames + " alert=" + alert + " " + locationState + " return=" + OVERLAY_SIZE + "x" + OVERLAY_SIZE + " item=" + itemId + " sprite=" + spriteId + " crossed=" + crossed);
		debugFrames++;
		debugLastLocationState = locationState;
	}
	private String debugLocationState() {
		return "pos=" + getPosition() + " prefPos=" + getPreferredPosition() + " prefLoc=" + point(getPreferredLocation()) + " prefSize=" + dim(getPreferredSize()) + " bounds=" + rect(getBounds()) + " canvas=" + client.getCanvasWidth() + "x" + client.getCanvasHeight() + " real=" + dim(client.getRealDimensions()) + " resized=" + client.isResized();
	}
	private SalveReminderConfig.DebugAlert getDebugAlert() {
		SalveReminderConfig.DebugAlert alert = config.debugAlert();
		if (alert == null) return SalveReminderConfig.DebugAlert.OFF;
		return alert;
	}
	private static String point(java.awt.Point point) {
		if (point == null) return "null";
		return point.x + "," + point.y;
	}
	private static String dim(Dimension dimension) {
		if (dimension == null) return "null";
		return dimension.width + "x" + dimension.height;
	}
	private static String rect(Rectangle rectangle) {
		if (rectangle == null) return "null";
		return rectangle.x + "," + rectangle.y + " " + rectangle.width + "x" + rectangle.height;
	}
	private static Dimension size() {
		return new Dimension(OVERLAY_SIZE, OVERLAY_SIZE);
	}
	private static BufferedImage addCross(BufferedImage image) {
		Rectangle bounds = getImageBounds(image);
		int inset = Math.max(4, Math.min(bounds.width, bounds.height) / 8);
		BufferedImage crossed = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = crossed.createGraphics();
		graphics.drawImage(image, 0, 0, null);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(190, 0, 0, 230));
		graphics.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(bounds.x + inset, bounds.y + inset, bounds.x + bounds.width - inset - 1, bounds.y + bounds.height - inset - 1);
		graphics.drawLine(bounds.x + bounds.width - inset - 1, bounds.y + inset, bounds.x + inset, bounds.y + bounds.height - inset - 1);
		graphics.dispose();
		return crossed;
	}
	private static void renderIcon(Graphics2D graphics, BufferedImage image) {
		Rectangle bounds = getImageBounds(image);
		int x = BORDER + (ICON_SIZE - bounds.width) / 2;
		int y = BORDER + (ICON_SIZE - bounds.height) / 2;
		graphics.drawImage(image, x, y, x + bounds.width, y + bounds.height, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, null);
	}
	private static Rectangle getImageBounds(BufferedImage image) {
		int minX = image.getWidth();
		int minY = image.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if ((image.getRGB(x, y) >>> 24) == 0) continue;
				if (x < minX) minX = x;
				if (y < minY) minY = y;
				if (x > maxX) maxX = x;
				if (y > maxY) maxY = y;
			}
		}
		if (maxX == -1) return new Rectangle(0, 0, image.getWidth(), image.getHeight());
		return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}
}
