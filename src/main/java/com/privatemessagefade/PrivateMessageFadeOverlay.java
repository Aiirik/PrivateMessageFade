package com.privatemessagefade;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;

public class PrivateMessageFadeOverlay extends Overlay
{
	private static final int RIGHT_PADDING = 3;
	private static final int TOP_PADDING = 3;

	private final Client client;
	private final PrivateMessageFadePlugin plugin;
	private final PrivateMessageFadeConfig config;
	private final TextComponent textComponent = new TextComponent();
	private Font bangFont;
	private Font countFont;
	private int bangFontSize = -1;
	private int countFontSize = -1;

	@Inject
	private PrivateMessageFadeOverlay(Client client, PrivateMessageFadePlugin plugin, PrivateMessageFadeConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.DYNAMIC);
		textComponent.setOutline(true);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.shouldShowPrivateTabIndicator())
		{
			return null;
		}

		final Rectangle privateTabBounds = getUsableBounds(client.getWidget(InterfaceID.Chatbox.CHAT_PRIVATE));
		if (privateTabBounds == null || privateTabBounds.isEmpty())
		{
			return null;
		}

		updateFonts();
		final Font originalFont = graphics.getFont();
		graphics.setFont(bangFont);

		final FontMetrics bangMetrics = graphics.getFontMetrics();
		final int x = privateTabBounds.x + privateTabBounds.width - RIGHT_PADDING + config.indicatorOffsetX();
		final int y = privateTabBounds.y - TOP_PADDING + config.indicatorOffsetY();
		final int baselineY = y + bangMetrics.getAscent();

		graphics.setFont(originalFont);

		textComponent.setColor(config.privateTabTextColor());
		textComponent.setFont(bangFont);
		textComponent.setText("!");
		final int bangX = x - bangMetrics.stringWidth("!");
		textComponent.setPosition(new Point(bangX, baselineY));
		Dimension dimension = textComponent.render(graphics);

		if (plugin.shouldRenderPrivateTabCount())
		{
			graphics.setFont(countFont);
			final FontMetrics countMetrics = graphics.getFontMetrics();
			final String unreadCount = String.valueOf(plugin.getUnreadCount());
			final int countX = x + 2;
			textComponent.setPosition(new Point(countX, baselineY));
			textComponent.setColor(config.privateTabTextColor());
			textComponent.setFont(countFont);
			textComponent.setText(unreadCount);
			final Dimension countDimension = textComponent.render(graphics);
			dimension = new Dimension(dimension.width + 2 + countDimension.width, Math.max(dimension.height, countDimension.height));
		}

		graphics.setFont(originalFont);
		return dimension;
	}

	private void updateFonts()
	{
		final int nextBangFontSize = config.indicatorBangSize();
		if (bangFont == null || bangFontSize != nextBangFontSize)
		{
			bangFont = new Font("SansSerif", Font.BOLD, nextBangFontSize);
			bangFontSize = nextBangFontSize;
		}

		final int nextCountFontSize = config.indicatorCountSize();
		if (countFont == null || countFontSize != nextCountFontSize)
		{
			countFont = new Font("SansSerif", Font.BOLD, nextCountFontSize);
			countFontSize = nextCountFontSize;
		}
	}

	private static Rectangle getUsableBounds(Widget widget)
	{
		if (widget == null)
		{
			return null;
		}

		final Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.isEmpty())
		{
			return null;
		}

		return bounds;
	}
}
