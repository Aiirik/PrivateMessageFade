package com.privatefade;

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

public class PrivateFadeOverlay extends Overlay
{
	private static final int LEFT_PADDING = 6;

	private final Client client;
	private final PrivateFadePlugin plugin;
	private final PrivateFadeConfig config;
	private final TextComponent textComponent = new TextComponent();

	@Inject
	private PrivateFadeOverlay(Client client, PrivateFadePlugin plugin, PrivateFadeConfig config)
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
		if (!plugin.shouldShowUnreadIndicator())
		{
			return null;
		}

		final Widget privateChatWidget = client.getWidget(InterfaceID.PM_CHAT, 0);
		if (privateChatWidget == null)
		{
			return null;
		}

		final Rectangle anchorBounds = getUsableBounds(privateChatWidget);
		final Rectangle textBounds = getIndicatorBounds(privateChatWidget);
		if (anchorBounds == null || anchorBounds.isEmpty() || textBounds == null || textBounds.isEmpty())
		{
			return null;
		}

		final Font bangFont = new Font("SansSerif", Font.BOLD, config.indicatorBangSize());
		final Font countFont = new Font("SansSerif", Font.BOLD, config.indicatorCountSize());
		final Font originalFont = graphics.getFont();
		graphics.setFont(bangFont);

		final FontMetrics bangMetrics = graphics.getFontMetrics();
		final int x = anchorBounds.x + LEFT_PADDING + config.indicatorOffsetX();
		final int y = textBounds.y
			+ Math.max(bangMetrics.getAscent(), (textBounds.height + bangMetrics.getAscent() - bangMetrics.getDescent()) / 2)
			+ config.indicatorOffsetY();

		graphics.setFont(originalFont);

		textComponent.setPosition(new Point(x, y));

		textComponent.setColor(config.indicatorColor());
		textComponent.setFont(bangFont);
		textComponent.setText("!");
		Dimension dimension = textComponent.render(graphics);

		if (plugin.shouldRenderUnreadCount())
		{
			final int countX = x + dimension.width + 2;
			textComponent.setPosition(new Point(countX, y));
			textComponent.setFont(countFont);
			textComponent.setText(String.valueOf(plugin.getUnreadCount()));
			final Dimension countDimension = textComponent.render(graphics);
			dimension = new Dimension(dimension.width + 2 + countDimension.width, Math.max(dimension.height, countDimension.height));
		}

		return dimension;
	}

	private Rectangle getIndicatorBounds(Widget rootWidget)
	{
		Rectangle bestBounds = getUsableBounds(rootWidget);
		bestBounds = findBestBounds(rootWidget.getChildren(), bestBounds);
		bestBounds = findBestBounds(rootWidget.getDynamicChildren(), bestBounds);
		bestBounds = findBestBounds(rootWidget.getStaticChildren(), bestBounds);
		bestBounds = findBestBounds(rootWidget.getNestedChildren(), bestBounds);
		return bestBounds;
	}

	private Rectangle findBestBounds(Widget[] widgets, Rectangle currentBest)
	{
		if (widgets == null)
		{
			return currentBest;
		}

		Rectangle bestBounds = currentBest;
		for (Widget widget : widgets)
		{
			if (widget == null)
			{
				continue;
			}

			final Rectangle candidateBounds = getIndicatorBounds(widget);
			if (isBetterBounds(candidateBounds, bestBounds))
			{
				bestBounds = candidateBounds;
			}
		}

		return bestBounds;
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

	private static boolean isBetterBounds(Rectangle candidate, Rectangle currentBest)
	{
		if (candidate == null || candidate.isEmpty())
		{
			return false;
		}

		if (currentBest == null || currentBest.isEmpty())
		{
			return true;
		}

		return candidate.width * candidate.height < currentBest.width * currentBest.height;
	}
}
