package com.privatemessagefade;

import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.Set;
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
	private static final int CHAT_VIEW_PRIVATE = 3;

	private static final int[] PRIVATE_MESSAGE_CHILD_IDS =
	{
		PmChat.PM1 & 0xFFFF,
		PmChat.PM2 & 0xFFFF,
		PmChat.PM3 & 0xFFFF,
		PmChat.PM4 & 0xFFFF,
		PmChat.PM5 & 0xFFFF
	};

	private static final Set<ChatMessageType> PRIVATE_MESSAGE_TYPES = EnumSet.of(
		ChatMessageType.PRIVATECHAT,
		ChatMessageType.PRIVATECHATOUT,
		ChatMessageType.MODPRIVATECHAT
	);

	private static final Set<Integer> PRIVATE_TAB_COMPONENT_IDS = Set.of(
		InterfaceID.Chatbox.CHAT_PRIVATE,
		InterfaceID.Chatbox.CHAT_PRIVATE_GRAPHIC,
		InterfaceID.Chatbox.CHAT_PRIVATE_TEXT1,
		InterfaceID.Chatbox.CHAT_PRIVATE_FILTER
	);

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
	private int lastAppliedOpacity = -1;
	private boolean initializeOnNextLoggedIn;

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
		initializeOnNextLoggedIn = client.getGameState() != GameState.LOGGED_IN;
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
		initializeOnNextLoggedIn = false;
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
			initializeActivityState();
			pendingInitialization = false;
		}

		if (isNotificationsSuppressedByPrivateTab())
		{
			clearUnreadMessages();
		}

		if (privateReplyInputOpen)
		{
			restoreWidget();
			return;
		}

		if (shouldKeepSplitChatOpenOnPrivateTab())
		{
			lastActivityMillis = System.currentTimeMillis();
			restoreWidget();
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
		final GameState currentGameState = gameStateChanged.getGameState();
		if (shouldInitializeOnFutureLoggedIn(currentGameState))
		{
			initializeOnNextLoggedIn = true;
			return;
		}

		if (currentGameState == GameState.LOGGED_IN && initializeOnNextLoggedIn)
		{
			clearUnreadMessages();
			pendingInitialization = true;
			initializeOnNextLoggedIn = false;
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
				resetActivity();
			}
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

		final boolean clickedPrivateTab = isPrivateTabWidget(widgetId);
		if (clickedPrivateTab && config.privateTabClickMarksRead())
		{
			clearUnreadMessages();
		}

		if (clickedPrivateTab && config.openSplitChatOnPrivateTab())
		{
			resetActivity();
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
			final long now = System.currentTimeMillis();
			lastActivityMillis = now;
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
		return shouldShowUnreadIndicator() && config.privateTabDisplay() != PrivateMessageFadeConfig.IndicatorDisplayOption.OFF;
	}

	boolean shouldShowMovableWidgetIndicator()
	{
		return shouldShowUnreadIndicator() && config.movableWidgetDisplay() != PrivateMessageFadeConfig.IndicatorDisplayOption.OFF;
	}

	boolean shouldRenderPrivateTabCount()
	{
		return config.privateTabDisplay() == PrivateMessageFadeConfig.IndicatorDisplayOption.COUNT && unreadMessageCount > 1;
	}

	boolean shouldRenderMovableWidgetCount()
	{
		return config.movableWidgetDisplay() == PrivateMessageFadeConfig.IndicatorDisplayOption.COUNT && unreadMessageCount > 1;
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
			applyOpacityIfChanged(privateChatWidget, 0);
			privateChatWidget.setHidden(true);
			return;
		}

		final long now = System.currentTimeMillis();
		final long hideAtMillis = getHideAtMillis();
		if (now < hideAtMillis)
		{
			applyOpacityIfChanged(privateChatWidget, 0);
			privateChatWidget.setHidden(false);
			return;
		}

		if (!config.enableFadeEffect())
		{
			applyOpacityIfChanged(privateChatWidget, 255);
			privateChatWidget.setHidden(true);
			return;
		}

		privateChatWidget.setHidden(false);

		final long fadeDurationMillis = Math.max(1L, config.fadeDurationSeconds() * 1000L);
		final long fadeElapsedMillis = now - hideAtMillis;
		if (fadeElapsedMillis >= fadeDurationMillis)
		{
			applyOpacityIfChanged(privateChatWidget, 255);
			privateChatWidget.setHidden(true);
			return;
		}

		final float alphaProgress = (float) fadeElapsedMillis / fadeDurationMillis;
		final float clampedAlpha = Math.max(0.0f, Math.min(1.0f, alphaProgress));
		final int opacity = Math.round(clampedAlpha * 255f);
		applyOpacityIfChanged(privateChatWidget, opacity);
	}

	private void applyOpacityIfChanged(Widget widget, int opacity)
	{
		if (opacity != lastAppliedOpacity)
		{
			applyOpacityToWidgetTree(widget, opacity);
			lastAppliedOpacity = opacity;
		}
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
		applyOpacityToWidgetTree(privateChatWidget, 0);
	}

	private boolean isPrivateReplyInputOpen()
	{
		return client.getVarcIntValue(VarClientID.MESLAYERMODE) == InputType.PRIVATE_MESSAGE.getType();
	}

	private boolean hasExistingPrivateMessages()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}

		for (int childId : PRIVATE_MESSAGE_CHILD_IDS)
		{
			final Widget widget = client.getWidget(InterfaceID.PM_CHAT, childId);
			if (widget == null || widget.isHidden())
			{
				continue;
			}

			final String text = widget.getText();
			if (text != null && !text.isBlank())
			{
				return true;
			}
		}

		return false;
	}

	private boolean isNotificationsSuppressedByPrivateTab()
	{
		return config.privateTabClickMarksRead() && isPrivateTabSelected();
	}

	private boolean shouldKeepSplitChatOpenOnPrivateTab()
	{
		return isPrivateTabSelected() && config.keepSplitChatOpenOnPrivateTab();
	}

	private boolean isPrivateTabSelected()
	{
		return client.getVarcIntValue(VarClientID.CHAT_VIEW) == CHAT_VIEW_PRIVATE;
	}

	private boolean isChatTabWidget(int widgetId)
	{
		return widgetId >= InterfaceID.Chatbox.CHAT_ALL && widgetId <= InterfaceID.Chatbox.CHAT_TRADE_FILTER;
	}

	private boolean isPrivateTabWidget(int widgetId)
	{
		return PRIVATE_TAB_COMPONENT_IDS.contains(widgetId);
	}

	private void clearUnreadMessages()
	{
		unreadMessageCount = 0;
	}

	private static boolean shouldInitializeOnFutureLoggedIn(GameState gameState)
	{
		return gameState == GameState.UNKNOWN
			|| gameState == GameState.LOGIN_SCREEN
			|| gameState == GameState.LOGGING_IN
			|| gameState == GameState.CONNECTION_LOST
			|| gameState == GameState.HOPPING;
	}

	private static void applyOpacityToWidgetTree(Widget widget, int opacity)
	{
		widget.setOpacity(opacity);
		applyOpacityRecursively(widget.getChildren(), opacity);
	}

	private static void applyOpacityRecursively(Widget[] widgets, int opacity)
	{
		if (widgets == null)
		{
			return;
		}

		for (Widget widget : widgets)
		{
			if (widget != null)
			{
				applyOpacityToWidgetTree(widget, opacity);
			}
		}
	}

	private static boolean isPrivateMessage(ChatMessageType messageType)
	{
		return PRIVATE_MESSAGE_TYPES.contains(messageType);
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
