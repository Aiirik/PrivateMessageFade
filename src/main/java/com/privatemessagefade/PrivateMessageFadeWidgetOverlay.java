package com.privatemessagefade;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
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
	private static final long FADE_IN_DURATION_MS = 300; // Fixed duration for smooth fade-in

	private final PrivateMessageFadePlugin plugin;
	private final PrivateMessageFadeConfig config;
	private final RuneLiteConfig runeLiteConfig;
	private Font widgetFont;
	private int cachedBoxSize = -1;
	private boolean cachedBoldText;
	private float cachedBaseFontSize = -1f;
	private long fadeInStartTime = -1;
	private int previousUnreadCount = 0;

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
			fadeInStartTime = -1;
			previousUnreadCount = 0;
			return null;
		}

		final int currentUnreadCount = plugin.getUnreadCount();
		
		// Track when unread count first appears or changes - always fade in
		if (currentUnreadCount != previousUnreadCount)
		{
			fadeInStartTime = System.currentTimeMillis();
			previousUnreadCount = currentUnreadCount;
		}

		// Calculate fade-in opacity (0.0 to 1.0)
		float opacity = 1.0f;
		if (fadeInStartTime >= 0)
		{
			final long elapsed = System.currentTimeMillis() - fadeInStartTime;
			if (elapsed < FADE_IN_DURATION_MS)
			{
				opacity = (float) elapsed / FADE_IN_DURATION_MS;
			}
			else
			{
				fadeInStartTime = -1; // Animation complete
				opacity = 1.0f;
			}
		}

		final String text = plugin.shouldRenderMovableWidgetCount()
			? "! " + currentUnreadCount
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
			// Apply opacity via graphics composite during fade-in
			final Composite originalComposite = graphics.getComposite();
			if (opacity < 1.0f)
			{
				graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
			}
			
			final Dimension result = super.render(graphics);
			
			if (opacity < 1.0f)
			{
				graphics.setComposite(originalComposite);
			}
			
			return result;
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
		final int style = boldText ? Font.BOLD : Font.PLAIN;
		final float baseFontSize = baseFont.getSize2D();
		
		if (widgetFont == null
			|| cachedBoxSize != boxSize
			|| cachedBoldText != boldText
			|| cachedBaseFontSize != baseFontSize)
		{
			final float scaledFontSize = Math.max(10f, baseFontSize * boxSize / Math.max(1f, runeLiteConfig.infoBoxSize()));
			widgetFont = baseFont.deriveFont(style, scaledFontSize);
			cachedBoxSize = boxSize;
			cachedBoldText = boldText;
			cachedBaseFontSize = baseFontSize;
		}

		return widgetFont;
	}
}
