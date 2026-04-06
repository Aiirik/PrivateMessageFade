package com.privatemessagefade;

import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InterfaceID.PmChat;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.vars.InputType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Private Message Fade",
	description = "Hides split private chat after a configurable idle delay",
	tags = {"private", "pm", "chat", "split"}
)
public class PrivateMessageFadePlugin extends Plugin
{
	private static final int[] CHAT_TAB_COMPONENT_IDS =
	{
		InterfaceID.Chatbox.CHAT_ALL,
		InterfaceID.Chatbox.CHAT_ALL_GRAPHIC,
		InterfaceID.Chatbox.CHAT_ALL_TEXT1,
		InterfaceID.Chatbox.CHAT_GAME,
		InterfaceID.Chatbox.CHAT_GAME_GRAPHIC,
		InterfaceID.Chatbox.CHAT_GAME_TEXT1,
		InterfaceID.Chatbox.CHAT_GAME_FILTER,
		InterfaceID.Chatbox.CHAT_PUBLIC,
		InterfaceID.Chatbox.CHAT_PUBLIC_GRAPHIC,
		InterfaceID.Chatbox.CHAT_PUBLIC_TEXT1,
		InterfaceID.Chatbox.CHAT_PUBLIC_FILTER,
		InterfaceID.Chatbox.CHAT_PRIVATE,
		InterfaceID.Chatbox.CHAT_PRIVATE_GRAPHIC,
		InterfaceID.Chatbox.CHAT_PRIVATE_TEXT1,
		InterfaceID.Chatbox.CHAT_PRIVATE_FILTER,
		InterfaceID.Chatbox.CHAT_FRIENDSCHAT,
		InterfaceID.Chatbox.CHAT_FRIENDSCHAT_GRAPHIC,
		InterfaceID.Chatbox.CHAT_FRIENDSCHAT_TEXT1,
		InterfaceID.Chatbox.CHAT_FRIENDSCHAT_FILTER,
		InterfaceID.Chatbox.CHAT_CLAN,
		InterfaceID.Chatbox.CHAT_CLAN_GRAPHIC,
		InterfaceID.Chatbox.CHAT_CLAN_TEXT1,
		InterfaceID.Chatbox.CHAT_CLAN_FILTER,
		InterfaceID.Chatbox.CHAT_TRADE
		,
		InterfaceID.Chatbox.CHAT_TRADE_GRAPHIC,
		InterfaceID.Chatbox.CHAT_TRADE_TEXT,
		InterfaceID.Chatbox.CHAT_TRADE_FILTER
	};

	private static final int[] PRIVATE_TAB_COMPONENT_IDS =
	{
		InterfaceID.Chatbox.CHAT_PRIVATE,
		InterfaceID.Chatbox.CHAT_PRIVATE_GRAPHIC,
		InterfaceID.Chatbox.CHAT_PRIVATE_TEXT1,
		InterfaceID.Chatbox.CHAT_PRIVATE_FILTER
	};

	private static final int[] PRIVATE_MESSAGE_CHILD_IDS =
	{
		PmChat.PM1 & 0xFFFF,
		PmChat.PM2 & 0xFFFF,
		PmChat.PM3 & 0xFFFF,
		PmChat.PM4 & 0xFFFF,
		PmChat.PM5 & 0xFFFF
	};

	private static final ChatMessageType[] PRIVATE_MESSAGE_TYPES =
	{
		ChatMessageType.PRIVATECHAT,
		ChatMessageType.PRIVATECHATOUT,
		ChatMessageType.MODPRIVATECHAT
	};

	@Inject
	private Client client;

	@Inject
	private PrivateMessageFadeConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PrivateMessageFadeOverlay overlay;

	@Inject
	private PrivateMessageFadeWidgetOverlay widgetOverlay;

	private long lastActivityMillis;
	private boolean privateReplyInputOpen;
	private int unreadMessageCount;
	private boolean pendingInitialization;
	private boolean privateTabSelected;

	private final KeyListener keyListener = new KeyListener()
	{
		@Override
		public void keyTyped(KeyEvent e)
		{
		}

		@Override
		public void keyPressed(KeyEvent e)
		{
			if (!config.escClosesPrivateMessage()
				|| e.getKeyCode() != KeyEvent.VK_ESCAPE
				|| !privateReplyInputOpen)
			{
				return;
			}

			e.consume();
			clientThread.invoke(() -> client.runScript(ScriptID.MESSAGE_LAYER_CLOSE, 1, 1, 0));
		}

		@Override
		public void keyReleased(KeyEvent e)
		{
		}
	};

	@Override
	protected void startUp()
	{
		unreadMessageCount = 0;
		pendingInitialization = true;
		overlayManager.add(overlay);
		overlayManager.add(widgetOverlay);
		keyManager.registerKeyListener(keyListener);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(widgetOverlay);
		keyManager.unregisterKeyListener(keyListener);
		unreadMessageCount = 0;
		pendingInitialization = false;
		clientThread.invokeLater((Runnable) this::restoreWidget);
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
			if (isNotificationsSuppressedByPrivateTab() || privateReplyInputOpen)
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

		if (pendingInitialization)
		{
			privateReplyInputOpen = isPrivateReplyInputOpen();
			privateTabSelected = isPrivateTabSelected();
			initializeActivityState();
			pendingInitialization = false;
		}

		privateTabSelected = isPrivateTabSelected();
		if (isNotificationsSuppressedByPrivateTab())
		{
			clearUnreadMessages();
		}

		if (privateReplyInputOpen)
		{
			restoreWidget();
			return;
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender beforeRender)
	{
		if (client.getGameState() != GameState.LOGGED_IN || privateReplyInputOpen)
		{
			return;
		}

		applyPrivateMessageState();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			clearUnreadMessages();
			pendingInitialization = true;
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

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		final Widget widget = event.getMenuEntry().getWidget();
		if (widget == null)
		{
			return;
		}

		final int widgetId = widget.getId();
		if (!isChatTabWidget(widgetId))
		{
			return;
		}

		privateTabSelected = isPrivateTabWidget(widgetId);
		if (privateTabSelected && config.privateTabClickMarksRead())
		{
			clearUnreadMessages();
		}
	}

	private void resetActivity()
	{
		lastActivityMillis = System.currentTimeMillis();
		restoreWidget();
	}

	private void initializeActivityState()
	{
		restoreWidget();

		if (!privateReplyInputOpen && hasExistingPrivateMessages())
		{
			lastActivityMillis = System.currentTimeMillis() - config.fadeDelaySeconds() * 1000L;
			return;
		}

		lastActivityMillis = System.currentTimeMillis();
	}

	boolean shouldShowUnreadIndicator()
	{
		return unreadMessageCount > 0
			&& !privateReplyInputOpen
			&& !isNotificationsSuppressedByPrivateTab()
			&& isPrivateMessageFullyHidden(System.currentTimeMillis());
	}

	boolean shouldShowPrivateTabIndicator()
	{
		return shouldShowUnreadIndicator() && config.privateTabDisplay() != IndicatorDisplayOption.OFF;
	}

	boolean shouldShowMovableWidgetIndicator()
	{
		return shouldShowUnreadIndicator() && config.movableWidgetDisplay() != IndicatorDisplayOption.OFF;
	}

	boolean shouldRenderPrivateTabCount()
	{
		return config.privateTabDisplay() == IndicatorDisplayOption.COUNT && unreadMessageCount > 1;
	}

	boolean shouldRenderMovableWidgetCount()
	{
		return config.movableWidgetDisplay() == IndicatorDisplayOption.COUNT && unreadMessageCount > 1;
	}

	int getUnreadCount()
	{
		return unreadMessageCount;
	}

	private void applyPrivateMessageState()
	{
		final Widget privateChatWidget = client.getWidget(InterfaceID.PM_CHAT, 0);
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

		// Smooth float-based opacity calculation for better visual quality
		final float alphaProgress = (float) fadeElapsedMillis / fadeDurationMillis;
		final float clampedAlpha = Math.max(0.0f, Math.min(1.0f, alphaProgress));
		final int opacity = Math.round(clampedAlpha * 255f);
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
		final Widget privateChatWidget = client.getWidget(InterfaceID.PM_CHAT, 0);
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

	private boolean hasExistingPrivateMessages()
	{
		for (int childId : PRIVATE_MESSAGE_CHILD_IDS)
		{
			final Widget widget = client.getWidget(InterfaceID.PM_CHAT, childId);
			if (widget == null || widget.isHidden())
			{
				continue;
			}

			final String text = widget.getText();
			if (text != null && !text.trim().isEmpty())
			{
				return true;
			}
		}

		return false;
	}

	private boolean isPrivateTabSelected()
	{
		final Widget privateTabText = client.getWidget(InterfaceID.Chatbox.CHAT_PRIVATE_TEXT1);
		if (privateTabText == null)
		{
			return false;
		}

		final int privateTextColor = privateTabText.getTextColor();
		int maxOtherColor = Integer.MIN_VALUE;
		maxOtherColor = Math.max(maxOtherColor, getTabTextColor(InterfaceID.Chatbox.CHAT_ALL_TEXT1));
		maxOtherColor = Math.max(maxOtherColor, getTabTextColor(InterfaceID.Chatbox.CHAT_GAME_TEXT1));
		maxOtherColor = Math.max(maxOtherColor, getTabTextColor(InterfaceID.Chatbox.CHAT_PUBLIC_TEXT1));
		maxOtherColor = Math.max(maxOtherColor, getTabTextColor(InterfaceID.Chatbox.CHAT_FRIENDSCHAT_TEXT1));
		maxOtherColor = Math.max(maxOtherColor, getTabTextColor(InterfaceID.Chatbox.CHAT_CLAN_TEXT1));
		maxOtherColor = Math.max(maxOtherColor, getTabTextColor(InterfaceID.Chatbox.CHAT_TRADE_TEXT));
		return privateTextColor > maxOtherColor;
	}

	private int getTabTextColor(int componentId)
	{
		final Widget widget = client.getWidget(componentId);
		return widget != null ? widget.getTextColor() : Integer.MIN_VALUE;
	}

	private boolean isNotificationsSuppressedByPrivateTab()
	{
		return config.privateTabClickMarksRead() && privateTabSelected;
	}

	private boolean isChatTabWidget(int widgetId)
	{
		for (int chatTabComponentId : CHAT_TAB_COMPONENT_IDS)
		{
			if (chatTabComponentId == widgetId)
			{
				return true;
			}
		}

		return false;
	}

	private boolean isPrivateTabWidget(int widgetId)
	{
		for (int privateTabComponentId : PRIVATE_TAB_COMPONENT_IDS)
		{
			if (privateTabComponentId == widgetId)
			{
				return true;
			}
		}

		return false;
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
	PrivateMessageFadeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PrivateMessageFadeConfig.class);
	}
}
