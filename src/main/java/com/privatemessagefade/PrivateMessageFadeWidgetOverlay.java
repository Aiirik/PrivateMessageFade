package com.privatemessagefade;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.inject.Inject;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.client.config.RuneLiteConfig;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class PrivateMessageFadeWidgetOverlay extends OverlayPanel
{
	private static final int TEXT_PADDING = 4;

	private final PrivateMessageFadePlugin plugin;
	private final PrivateMessageFadeConfig config;
	private final RuneLiteConfig runeLiteConfig;
	private Font widgetFont;
	private int cachedBoxSize = -1;
	private boolean cachedBoldText;
	private float cachedBaseFontSize = -1f;
	private int cachedBaseFontStyle = Integer.MIN_VALUE;

	@Inject
	private PrivateMessageFadeWidgetOverlay(
		PrivateMessageFadePlugin plugin,
		PrivateMessageFadeConfig config,
		RuneLiteConfig runeLiteConfig)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		this.runeLiteConfig = runeLiteConfig;

		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Private message fade overlay");
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.shouldShowMovableWidgetIndicator())
		{
			return null;
		}

		final String text = plugin.shouldRenderMovableWidgetCount()
			? "! " + plugin.getUnreadCount()
			: "!";

		final int boxSize = config.widgetSize() > 0
			? config.widgetSize()
			: runeLiteConfig.infoBoxSize();
		final Font originalFont = graphics.getFont();
		final Font currentWidgetFont = getWidgetFont(boxSize);
		final int textWidth = graphics.getFontMetrics(currentWidgetFont).stringWidth(text);
		final int preferredWidth = textWidth + TEXT_PADDING;

		graphics.setFont(currentWidgetFont);
		panelComponent.setBackgroundColor(config.widgetBackgroundColor());
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(text)
			.color(config.widgetTextColor())
			.preferredSize(new Dimension(preferredWidth, boxSize))
			.build());

		panelComponent.setPreferredSize(new Dimension(preferredWidth, boxSize));

		try
		{
			return super.render(graphics);
		}
		finally
		{
			graphics.setFont(originalFont);
		}
	}

	private Font getWidgetFont(int boxSize)
	{
		final Font baseFont = runeLiteConfig.infoboxFont().getFont();
		final boolean boldText = config.widgetBoldText();
		final int style = boldText ? Font.BOLD : baseFont.getStyle();
		final float baseFontSize = baseFont.getSize2D();
		
		if (widgetFont == null
			|| cachedBoxSize != boxSize
			|| cachedBoldText != boldText
			|| cachedBaseFontSize != baseFontSize
			|| cachedBaseFontStyle != baseFont.getStyle())
		{
			final float scaledFontSize = Math.max(10f, baseFontSize * boxSize / Math.max(1f, runeLiteConfig.infoBoxSize()));
			widgetFont = baseFont.deriveFont(style, scaledFontSize);
			cachedBoxSize = boxSize;
			cachedBoldText = boldText;
			cachedBaseFontSize = baseFontSize;
			cachedBaseFontStyle = baseFont.getStyle();
		}

		return widgetFont;
	}
}
