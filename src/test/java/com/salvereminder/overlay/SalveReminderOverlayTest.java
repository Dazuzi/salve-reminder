package com.salvereminder.overlay;
import com.salvereminder.SalveReminderConfig;
import com.salvereminder.SalveReminderPlugin;
import com.salvereminder.core.ReminderManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.mockito.ArgumentCaptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
public class SalveReminderOverlayTest {
	private SalveReminderConfig config;
	private ReminderManager manager;
	private ItemManager itemManager;
	private SpriteManager spriteManager;
	private TooltipManager tooltipManager;
	private SalveReminderOverlay overlay;
	private Graphics2D graphics;
	@Before
	public void setUp() {
		config = mock(SalveReminderConfig.class);
		manager = mock(ReminderManager.class);
		itemManager = mock(ItemManager.class);
		spriteManager = mock(SpriteManager.class);
		tooltipManager = mock(TooltipManager.class);
		overlay = new SalveReminderOverlay(config, manager, itemManager, spriteManager, tooltipManager, mock(SalveReminderPlugin.class));
		overlay.getBounds().setBounds(0, 0, 48, 48);
		graphics = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
	}
	@After
	public void tearDown() {
		graphics.dispose();
	}
	@Test
	public void hiddenRenderDoesNoWork() {
		when(manager.isShowAlert()).thenReturn(false);
		clearInvocations(config, manager, itemManager, spriteManager, tooltipManager);
		assertNull(overlay.render(graphics));
		verify(manager).isShowAlert();
		verifyNoInteractions(config, itemManager, spriteManager, tooltipManager);
	}
	@Test
	public void alternatingIconsAreScannedAndLoadedOnce() {
		CountingImage firstImage = new CountingImage();
		CountingImage secondImage = new CountingImage();
		when(manager.isShowAlert()).thenReturn(true);
		when(manager.getAlertItemId()).thenReturn(-1);
		when(manager.getAlertSpriteId()).thenReturn(1, 2, 1, 2);
		when(manager.isAlertIconCrossed()).thenReturn(false);
		when(config.flashBackground()).thenReturn(false);
		when(config.backgroundColor()).thenReturn(new Color(36, 33, 30, 150));
		when(spriteManager.getSprite(1, 0)).thenReturn(firstImage);
		when(spriteManager.getSprite(2, 0)).thenReturn(secondImage);
		Dimension firstSize = overlay.render(graphics);
		Dimension secondSize = overlay.render(graphics);
		Dimension thirdSize = overlay.render(graphics);
		Dimension fourthSize = overlay.render(graphics);
		assertEquals(1, firstImage.scans);
		assertEquals(1, secondImage.scans);
		assertSame(firstSize, secondSize);
		assertSame(firstSize, thirdSize);
		assertSame(firstSize, fourthSize);
		verify(spriteManager).getSprite(1, 0);
		verify(spriteManager).getSprite(2, 0);
		verify(config).flashBackground();
		verify(config).backgroundColor();
		verify(config, never()).flashBackgroundColor();
		verify(manager, never()).isFlash();
	}
	@Test
	public void backgroundMatchesRuneLiteComponent() {
		Color color = new Color(36, 33, 30, 150);
		when(manager.isShowAlert()).thenReturn(true);
		when(manager.getAlertItemId()).thenReturn(-1);
		when(manager.getAlertSpriteId()).thenReturn(-1);
		when(config.flashBackground()).thenReturn(false);
		when(config.backgroundColor()).thenReturn(color);
		BufferedImage actual = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D actualGraphics = actual.createGraphics();
		Dimension size = overlay.render(actualGraphics);
		actualGraphics.dispose();
		assertNotNull(size);
		BufferedImage expected = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D expectedGraphics = expected.createGraphics();
		BackgroundComponent background = new BackgroundComponent();
		background.setRectangle(new Rectangle(0, 0, size.width, size.height));
		background.setBackgroundColor(color);
		background.render(expectedGraphics);
		expectedGraphics.dispose();
		assertArrayEquals(expected.getRGB(0, 0, size.width, size.height, null, 0, size.width), actual.getRGB(0, 0, size.width, size.height, null, 0, size.width));
	}
	@Test
	public void narrowCrossIsNotCropped() {
		CountingImage image = new CountingImage();
		when(manager.isShowAlert()).thenReturn(true);
		when(manager.getAlertItemId()).thenReturn(-1);
		when(manager.getAlertSpriteId()).thenReturn(1);
		when(manager.isAlertIconCrossed()).thenReturn(true);
		when(config.flashBackground()).thenReturn(false);
		when(config.backgroundColor()).thenReturn(new Color(0, 0, 0, 0));
		when(spriteManager.getSprite(1, 0)).thenReturn(image);
		BufferedImage canvas = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D canvasGraphics = canvas.createGraphics();
		overlay.render(canvasGraphics);
		canvasGraphics.dispose();
		int crossedPixels = 0;
		for (int pixel : canvas.getRGB(0, 0, canvas.getWidth(), canvas.getHeight(), null, 0, canvas.getWidth())) {
			int red = pixel >>> 16 & 255;
			int green = pixel >>> 8 & 255;
			if ((pixel >>> 24) != 0 && red > green * 2) crossedPixels++;
		}
		assertTrue(crossedPixels > 1);
	}
	@Test
	public void hoveredTooltipIsReused() {
		when(manager.isShowAlert()).thenReturn(true);
		when(manager.getAlertItemId()).thenReturn(-1);
		when(manager.getAlertSpriteId()).thenReturn(-1);
		when(manager.getTooltipReason()).thenReturn("Reason");
		when(config.flashBackground()).thenReturn(false);
		when(config.backgroundColor()).thenReturn(Color.BLACK);
		overlay.render(graphics);
		overlay.render(graphics);
		overlay.onMouseOver();
		overlay.onMouseOver();
		ArgumentCaptor<Tooltip> captor = ArgumentCaptor.forClass(Tooltip.class);
		verify(tooltipManager, times(2)).add(captor.capture());
		assertSame(captor.getAllValues().get(0), captor.getAllValues().get(1));
	}
	private static final class CountingImage extends BufferedImage {
		private int scans = 0;
		private CountingImage() {
			super(36, 32, BufferedImage.TYPE_INT_ARGB);
			setRGB(10, 10, Color.WHITE.getRGB());
		}
		@Override
		public int[] getRGB(int startX, int startY, int width, int height, int[] rgbArray, int offset, int scansize) {
			scans++;
			return super.getRGB(startX, startY, width, height, rgbArray, offset, scansize);
		}
	}
}
