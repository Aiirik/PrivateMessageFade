package com.privatemessagefade;

import java.awt.Color;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.ColorUtil;

final class PrivateMessageFadeUpdateNotice
{
	static final String NOTICE_ID = "1.1.3";
	static final String MESSAGE = "Private Message Fade plugin has updated. See the plugin config for new settings.";

	private static final Color NOTICE_COLOR = new Color(160, 45, 45);
	private static final String CONFIG_GROUP = "privatemessagefade";
	private static final String LAST_NOTICE_ID_KEY = "lastUpdateNoticeId";

	private PrivateMessageFadeUpdateNotice()
	{
	}

	static void announceIfNeeded(ConfigManager configManager, ChatMessageManager chatMessageManager)
	{
		final String lastNoticeId = configManager.getConfiguration(CONFIG_GROUP, LAST_NOTICE_ID_KEY);
		if (NOTICE_ID.equals(lastNoticeId))
		{
			return;
		}

		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(ColorUtil.wrapWithColorTag(MESSAGE, NOTICE_COLOR))
			.build());
		configManager.setConfiguration(CONFIG_GROUP, LAST_NOTICE_ID_KEY, NOTICE_ID);
	}
}
