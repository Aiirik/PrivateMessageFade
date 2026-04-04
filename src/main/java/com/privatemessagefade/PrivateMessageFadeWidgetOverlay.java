package com.privatemessagefade;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class PrivateMessageFadeWidgetOverlay extends OverlayPanel
{
	private final PrivateMessageFadePlugin plugin;
	private final PrivateMessageFadeConfig config;

	@Inject
	private PrivateMessageFadeWidgetOverlay(PrivateMessageFadePlugin plugin, PrivateMessageFadeConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;

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

		final String text = plugin.shouldRenderUnreadCount()
			? "! " + plugin.getUnreadCount()
			: "!";

		panelComponent.setBackgroundColor(config.widgetBackgroundColor());
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(text)
			.color(config.indicatorColor())
			.build());

		panelComponent.setPreferredSize(new Dimension(
			graphics.getFontMetrics().stringWidth(text) + 10,
			0));

		return super.render(graphics);
	}
}
