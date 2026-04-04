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
	private final PrivateMessageFadePlugin plugin;
	private final PrivateMessageFadeConfig config;
	private final RuneLiteConfig runeLiteConfig;

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
		final Font baseFont = runeLiteConfig.infoboxFont().getFont();
		final int style = config.widgetBoldText() ? Font.BOLD : baseFont.getStyle();
		final float scaledFontSize = Math.max(10f, baseFont.getSize2D() * boxSize / Math.max(1f, runeLiteConfig.infoBoxSize()));
		final Font widgetFont = baseFont.deriveFont(style, scaledFontSize);

		graphics.setFont(widgetFont);
		panelComponent.setBackgroundColor(config.widgetBackgroundColor());
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(text)
			.color(config.widgetTextColor())
			.preferredSize(new Dimension(
				Math.max(boxSize, graphics.getFontMetrics().stringWidth(text) + 10),
				boxSize))
			.build());

		panelComponent.setPreferredSize(new Dimension(
			Math.max(boxSize, graphics.getFontMetrics().stringWidth(text) + 10),
			boxSize));

		try
		{
			return super.render(graphics);
		}
		finally
		{
			graphics.setFont(originalFont);
		}
	}
}
