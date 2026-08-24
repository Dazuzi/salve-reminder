package com.salvereminder.overlay;
import com.salvereminder.SalveReminderConfig;
import com.salvereminder.SalveReminderPlugin;
import com.salvereminder.core.ReminderManager;
import net.runelite.api.Constants;
import net.runelite.api.MenuAction;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
public final class SalveReminderOverlay extends OverlayPanel {
	private static final int BORDER = ComponentConstants.STANDARD_BORDER;
	private static final int ICON_SIZE = Constants.ITEM_SPRITE_WIDTH;
	private static final int OVERLAY_SIZE = ICON_SIZE + BORDER * 2;
	private static final Dimension SIZE = new Dimension(OVERLAY_SIZE, OVERLAY_SIZE);
	private static final Color CROSS_COLOR = new Color(190, 0, 0, 230);
	private static final Stroke CROSS_STROKE = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private final SalveReminderConfig config;
	private final ReminderManager reminderManager;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	private final TooltipManager tooltipManager;
	private RenderedIcon icon = null;
	private RenderedIcon alternateIcon = null;
	private BackgroundColors backgroundColors = null;
	private BackgroundColors alternateBackgroundColors = null;
	private Tooltip tooltip = null;
	private volatile AppearanceSettings appearance = null;
	@Inject
	public SalveReminderOverlay(SalveReminderConfig config, ReminderManager reminderManager, ItemManager itemManager, SpriteManager spriteManager, TooltipManager tooltipManager, SalveReminderPlugin plugin) {
		super(plugin);
		this.config = config;
		this.reminderManager = reminderManager;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setResizable(false);
		setPreferredSize(new Dimension(SIZE));
		setResettable(true);
		addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Ignore NPC", "Salve Reminder", e -> reminderManager.ignoreCurrentTarget());
		addMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Salve Reminder");
	}
	public void resetSize() {
		Dimension size = getPreferredSize();
		if (!SIZE.equals(size)) setPreferredSize(size = new Dimension(SIZE));
		getBounds().setSize(size);
	}
	public synchronized void invalidateConfig() {
		appearance = null;
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!reminderManager.isShowAlert()) return null;
		AppearanceSettings settings = getAppearanceSettings();
		Color bgColor = settings.flashBackground && reminderManager.isFlash() ? settings.flashColor : settings.backgroundColor;
		int itemIDToDisplay = reminderManager.getAlertItemId();
		int spriteIDToDisplay = reminderManager.getAlertSpriteId();
		boolean crossed = reminderManager.isAlertIconCrossed();
		RenderedIcon renderedIcon = getIcon(itemIDToDisplay, spriteIDToDisplay, crossed);
		renderBackground(graphics, bgColor);
		if (renderedIcon.image != null) renderIcon(graphics, renderedIcon.image, renderedIcon.bounds);
		return SIZE;
	}
	@Override
	public void onMouseOver() {
		String text = reminderManager.getTooltipReason();
		if (text == null) return;
		if (tooltip == null || !text.equals(tooltip.getText())) tooltip = new Tooltip(text);
		tooltipManager.add(tooltip);
	}
	private AppearanceSettings getAppearanceSettings() {
		AppearanceSettings settings = appearance;
		if (settings == null) synchronized (this) {
			if ((settings = appearance) == null) {
				boolean flashBackground = config.flashBackground();
				appearance = settings = new AppearanceSettings(flashBackground, config.backgroundColor(), flashBackground ? config.flashBackgroundColor() : null);
			}
		}
		return settings;
	}
	private void renderBackground(Graphics2D graphics, Color color) {
		BackgroundColors colors = getBackgroundColors(color);
		graphics.setColor(colors.background);
		graphics.fillRect(0, 0, OVERLAY_SIZE, OVERLAY_SIZE);
		graphics.setColor(colors.outside);
		graphics.drawRect(0, 0, OVERLAY_SIZE - 1, OVERLAY_SIZE - 1);
		graphics.setColor(colors.inside);
		graphics.drawRect(1, 1, OVERLAY_SIZE - 3, OVERLAY_SIZE - 3);
	}
	private BackgroundColors getBackgroundColors(Color color) {
		if (backgroundColors != null && backgroundColors.background.equals(color)) return backgroundColors;
		if (alternateBackgroundColors != null && alternateBackgroundColors.background.equals(color)) {
			BackgroundColors previousColors = backgroundColors;
			backgroundColors = alternateBackgroundColors;
			alternateBackgroundColors = previousColors;
			return backgroundColors;
		}
		alternateBackgroundColors = backgroundColors;
		backgroundColors = new BackgroundColors(color);
		return backgroundColors;
	}
	private RenderedIcon getIcon(int itemId, int spriteId, boolean crossed) {
		if (icon != null && icon.matches(itemId, spriteId, crossed)) return icon;
		if (alternateIcon != null && alternateIcon.matches(itemId, spriteId, crossed)) {
			RenderedIcon previousIcon = icon;
			icon = alternateIcon;
			alternateIcon = previousIcon;
			return icon;
		}
		alternateIcon = icon;
		BufferedImage image = spriteId == -1 ? null : spriteManager.getSprite(spriteId, 0);
		if (image == null) image = itemManager.getImage(itemId);
		Rectangle bounds = image == null ? null : getImageBounds(image);
		if (image != null && crossed) {
			image = addCross(image, bounds);
			bounds = getImageBounds(image);
		}
		icon = new RenderedIcon(itemId, spriteId, crossed, image, bounds);
		return icon;
	}
	private static BufferedImage addCross(BufferedImage image, Rectangle bounds) {
		int inset = Math.max(4, Math.min(bounds.width, bounds.height) / 8);
		BufferedImage crossed = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = crossed.createGraphics();
		graphics.drawImage(image, 0, 0, null);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(CROSS_COLOR);
		graphics.setStroke(CROSS_STROKE);
		graphics.drawLine(bounds.x + inset, bounds.y + inset, bounds.x + bounds.width - inset - 1, bounds.y + bounds.height - inset - 1);
		graphics.drawLine(bounds.x + bounds.width - inset - 1, bounds.y + inset, bounds.x + inset, bounds.y + bounds.height - inset - 1);
		graphics.dispose();
		return crossed;
	}
	private static void renderIcon(Graphics2D graphics, BufferedImage image, Rectangle bounds) {
		int x = BORDER + (ICON_SIZE - bounds.width) / 2;
		int y = BORDER + (ICON_SIZE - bounds.height) / 2;
		graphics.drawImage(image, x, y, x + bounds.width, y + bounds.height, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, null);
	}
	private static Rectangle getImageBounds(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int minX = width;
		int minY = height;
		int maxX = -1;
		int maxY = -1;
		int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				if ((pixels[offset + x] >>> 24) == 0) continue;
				if (x < minX) minX = x;
				if (y < minY) minY = y;
				if (x > maxX) maxX = x;
				if (y > maxY) maxY = y;
			}
		}
		if (maxX == -1) return new Rectangle(0, 0, width, height);
		return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}
	private static final class RenderedIcon {
		private final int itemId;
		private final int spriteId;
		private final boolean crossed;
		private final BufferedImage image;
		private final Rectangle bounds;
		private RenderedIcon(int itemId, int spriteId, boolean crossed, BufferedImage image, Rectangle bounds) {
			this.itemId = itemId;
			this.spriteId = spriteId;
			this.crossed = crossed;
			this.image = image;
			this.bounds = bounds;
		}
		private boolean matches(int itemId, int spriteId, boolean crossed) {
			return this.itemId == itemId && this.spriteId == spriteId && this.crossed == crossed;
		}
	}
	private static final class BackgroundColors {
		private final Color background;
		private final Color outside;
		private final Color inside;
		private BackgroundColors(Color background) {
			this.background = background;
			outside = new Color((int) (background.getRed() * .8f), (int) (background.getGreen() * .8f), (int) (background.getBlue() * .8f), Math.min(255, (int) (background.getAlpha() * 1.4f)));
			inside = new Color(Math.min(255, (int) (background.getRed() * 1.2f)), Math.min(255, (int) (background.getGreen() * 1.2f)), Math.min(255, (int) (background.getBlue() * 1.2f)), Math.min(255, (int) (background.getAlpha() * 1.4f)));
		}
	}
	private static final class AppearanceSettings {
		private final boolean flashBackground;
		private final Color backgroundColor;
		private final Color flashColor;
		private AppearanceSettings(boolean flashBackground, Color backgroundColor, Color flashColor) {
			this.flashBackground = flashBackground;
			this.backgroundColor = backgroundColor;
			this.flashColor = flashColor;
		}
	}
}
