package com.salvereminder.overlay;
import net.runelite.api.Constants;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
class CenteredImageComponent implements LayoutableRenderableEntity {
	private final BufferedImage image;
	private final Rectangle bounds = new Rectangle();
	private final Rectangle imageBounds;
	private final Dimension size = new Dimension(Constants.ITEM_SPRITE_WIDTH, Constants.ITEM_SPRITE_WIDTH);
	private Point location = new Point();
	CenteredImageComponent(BufferedImage image) {
		this.image = image;
		imageBounds = getImageBounds(image);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		int x = location.x + (size.width - imageBounds.width) / 2;
		int y = location.y + (size.height - imageBounds.height) / 2;
		graphics.drawImage(image, x, y, x + imageBounds.width, y + imageBounds.height, imageBounds.x, imageBounds.y, imageBounds.x + imageBounds.width, imageBounds.y + imageBounds.height, null);
		bounds.setLocation(location);
		bounds.setSize(size);
		return size;
	}
	@Override
	public Rectangle getBounds() {
		return bounds;
	}
	@Override
	public void setPreferredLocation(Point position) {
		location = position;
	}
	@Override
	public void setPreferredSize(Dimension dimension) {
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
