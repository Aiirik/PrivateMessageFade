package com.aiirik;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.vars.InputType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Private Fade",
	description = "Hides split private chat after a configurable idle delay",
	tags = {"private", "pm", "chat", "split"}
)
public class PrivateFadePlugin extends Plugin
{
	private static final ChatMessageType[] PRIVATE_MESSAGE_TYPES =
	{
		ChatMessageType.PRIVATECHAT,
		ChatMessageType.PRIVATECHATOUT,
		ChatMessageType.MODPRIVATECHAT
	};

	@Inject
	private Client client;

	@Inject
	private PrivateFadeConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PrivateFadeOverlay overlay;

	private long lastActivityMillis;
	private boolean privateReplyInputOpen;
	private int unreadMessageCount;

	@Override
	protected void startUp()
	{
		privateReplyInputOpen = isPrivateReplyInputOpen();
		unreadMessageCount = 0;
		overlayManager.add(overlay);
		resetActivity();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		unreadMessageCount = 0;
		restoreWidget();
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		final ChatMessageType messageType = chatMessage.getType();
		if (!isPrivateMessage(messageType))
		{
			return;
		}

		if (isIncomingPrivateMessage(messageType))
		{
			if (privateReplyInputOpen)
			{
				clearUnreadMessages();
			}
			else
			{
				unreadMessageCount++;
			}
		}
		else
		{
			clearUnreadMessages();
		}

		resetActivity();
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (privateReplyInputOpen)
		{
			restoreWidget();
			return;
		}

		applyPrivateMessageState();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			privateReplyInputOpen = isPrivateReplyInputOpen();
			clearUnreadMessages();
			resetActivity();
		}
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged varClientIntChanged)
	{
		if (varClientIntChanged.getIndex() != VarClientID.MESLAYERMODE)
		{
			return;
		}

		final boolean replyInputNowOpen = isPrivateReplyInputOpen();
		if (replyInputNowOpen != privateReplyInputOpen)
		{
			privateReplyInputOpen = replyInputNowOpen;
			if (replyInputNowOpen)
			{
				clearUnreadMessages();
			}
			resetActivity();
		}
	}

	private void resetActivity()
	{
		lastActivityMillis = System.currentTimeMillis();
		restoreWidget();
	}

	boolean shouldShowUnreadIndicator()
	{
		return config.newMessageDisplay() && unreadMessageCount > 0 && !privateReplyInputOpen && isPrivateMessageFullyHidden(System.currentTimeMillis());
	}

	String getUnreadIndicatorText()
	{
		if (!config.showMessageCount() || unreadMessageCount <= 1)
		{
			return "!";
		}

		return "! " + unreadMessageCount;
	}

	boolean shouldRenderUnreadCount()
	{
		return config.showMessageCount() && unreadMessageCount > 1;
	}

	int getUnreadCount()
	{
		return unreadMessageCount;
	}

	private void applyPrivateMessageState()
	{
		final Widget privateChatWidget = client.getWidget(WidgetInfo.PRIVATE_CHAT_MESSAGE);
		if (privateChatWidget == null)
		{
			return;
		}

		final int fadeDelaySeconds = config.fadeDelaySeconds();
		if (fadeDelaySeconds <= 0)
		{
			restoreWidget(privateChatWidget);
			return;
		}

		final long now = System.currentTimeMillis();
		final long hideAtMillis = getHideAtMillis();
		if (now < hideAtMillis)
		{
			restoreWidget(privateChatWidget);
			return;
		}

		if (!config.enableFadeEffect())
		{
			setWidgetTreeOpacity(privateChatWidget, 0);
			privateChatWidget.setHidden(true);
			return;
		}

		privateChatWidget.setHidden(false);

		final long fadeDurationMillis = Math.max(1L, config.fadeDurationSeconds() * 1000L);
		final long fadeElapsedMillis = now - hideAtMillis;
		if (fadeElapsedMillis >= fadeDurationMillis)
		{
			setWidgetTreeOpacity(privateChatWidget, 255);
			privateChatWidget.setHidden(true);
			return;
		}

		final int opacity = (int) Math.min(255L, fadeElapsedMillis * 255L / fadeDurationMillis);
		setWidgetTreeOpacity(privateChatWidget, opacity);
	}

	private boolean isPrivateMessageFullyHidden(long now)
	{
		if (config.fadeDelaySeconds() <= 0)
		{
			return false;
		}

		final long hideAtMillis = getHideAtMillis();
		if (now < hideAtMillis)
		{
			return false;
		}

		if (!config.enableFadeEffect())
		{
			return true;
		}

		final long fadeDurationMillis = Math.max(1L, config.fadeDurationSeconds() * 1000L);
		return now - hideAtMillis >= fadeDurationMillis;
	}

	private long getHideAtMillis()
	{
		return lastActivityMillis + config.fadeDelaySeconds() * 1000L;
	}

	private void restoreWidget()
	{
		final Widget privateChatWidget = client.getWidget(WidgetInfo.PRIVATE_CHAT_MESSAGE);
		if (privateChatWidget != null)
		{
			restoreWidget(privateChatWidget);
		}
	}

	private static void restoreWidget(Widget privateChatWidget)
	{
		privateChatWidget.setHidden(false);
		setWidgetTreeOpacity(privateChatWidget, 0);
	}

	private boolean isPrivateReplyInputOpen()
	{
		return client.getVarcIntValue(VarClientID.MESLAYERMODE) == InputType.PRIVATE_MESSAGE.getType();
	}

	private void clearUnreadMessages()
	{
		unreadMessageCount = 0;
	}

	private static void setWidgetTreeOpacity(Widget rootWidget, int opacity)
	{
		rootWidget.setOpacity(opacity);
		applyOpacity(rootWidget.getChildren(), opacity);
		applyOpacity(rootWidget.getDynamicChildren(), opacity);
		applyOpacity(rootWidget.getStaticChildren(), opacity);
		applyOpacity(rootWidget.getNestedChildren(), opacity);
	}

	private static void applyOpacity(Widget[] widgets, int opacity)
	{
		if (widgets == null)
		{
			return;
		}

		for (Widget widget : widgets)
		{
			if (widget == null)
			{
				continue;
			}

			setWidgetTreeOpacity(widget, opacity);
		}
	}

	private static boolean isPrivateMessage(ChatMessageType messageType)
	{
		for (ChatMessageType privateMessageType : PRIVATE_MESSAGE_TYPES)
		{
			if (privateMessageType == messageType)
			{
				return true;
			}
		}

		return false;
	}

	private static boolean isIncomingPrivateMessage(ChatMessageType messageType)
	{
		return messageType == ChatMessageType.PRIVATECHAT || messageType == ChatMessageType.MODPRIVATECHAT;
	}

	@Provides
	PrivateFadeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PrivateFadeConfig.class);
	}
}
