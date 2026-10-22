package com.privatemessagefade;

import java.lang.reflect.Proxy;
import java.lang.reflect.Field;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrivateMessageFadePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PrivateMessageFadePlugin.class);
		RuneLite.main(args);
	}

	@Test
	public void fadesPrivateMessagesAndLoginNoticesButPreservesCountdowns()
	{
		final TestWidget message = new TestWidget("To Alice: Hello");
		final TestWidget notice = new TestWidget("Alice has logged in.");
		final TestWidget countdown = new TestWidget("System update in: 5:00");
		final TestWidget container = new TestWidget("", message, notice, countdown);

		assertTrue(PrivateMessageFadePlugin.hasVisiblePrivateMessageWidget(container.widget));
		PrivateMessageFadePlugin.applyOpacityToPrivateMessageWidgets(container.widget, 180);
		PrivateMessageFadePlugin.setPrivateMessageWidgetsHidden(container.widget, true);

		assertEquals(180, message.opacity);
		assertTrue(message.hidden);
		assertEquals(180, notice.opacity);
		assertTrue(notice.hidden);
		assertEquals(0, countdown.opacity);
		assertFalse(countdown.hidden);
	}

	@Test
	public void fadesMessageBesideLoginNoticeInSameRow()
	{
		final TestWidget message = new TestWidget("To Alice: Hello");
		final TestWidget notice = new TestWidget("Alice has logged in.");
		final TestWidget row = new TestWidget("", message, notice);
		final TestWidget container = new TestWidget("", row);

		PrivateMessageFadePlugin.applyOpacityToPrivateMessageWidgets(container.widget, 255);
		PrivateMessageFadePlugin.setPrivateMessageWidgetsHidden(container.widget, true);

		assertEquals(255, message.opacity);
		assertTrue(message.hidden);
		assertEquals(255, notice.opacity);
		assertTrue(notice.hidden);
	}

	@Test
	public void loginNoticeRestoresSplitChatAndResetsTimer() throws Exception
	{
		final TestWidget notice = new TestWidget("Alice has logged out.");
		final TestWidget container = new TestWidget("", notice);
		PrivateMessageFadePlugin.applyOpacityToPrivateMessageWidgets(container.widget, 255);
		PrivateMessageFadePlugin.setPrivateMessageWidgetsHidden(container.widget, true);

		final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class},
			(proxy, method, args) ->
			{
				if (method.getName().equals("getWidget") && args.length == 2
					&& args[0].equals(InterfaceID.PM_CHAT) && args[1].equals(0))
				{
					return container.widget;
				}
				throw new UnsupportedOperationException(method.getName());
			});
		final PrivateMessageFadePlugin plugin = new PrivateMessageFadePlugin();
		final Field clientField = PrivateMessageFadePlugin.class.getDeclaredField("client");
		clientField.setAccessible(true);
		clientField.set(plugin, client);
		setLoginLogoutAlert(plugin, false);
		final ChatMessage loginMessage = new ChatMessage();
		loginMessage.setType(ChatMessageType.LOGINLOGOUTNOTIFICATION);

		final long before = System.currentTimeMillis();
		plugin.onChatMessage(loginMessage);
		final Field activityField = PrivateMessageFadePlugin.class.getDeclaredField("lastActivityMillis");
		activityField.setAccessible(true);
		assertTrue(activityField.getLong(plugin) >= before);
		assertEquals(0, notice.opacity);
		assertFalse(notice.hidden);
		assertEquals(0, plugin.getUnreadCount());
	}

	@Test
	public void loginLogoutUnreadAlertIsOptionalAndDoesNotAffectPrivateMessages() throws Exception
	{
		final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class},
			(proxy, method, args) ->
			{
				if (method.getName().equals("getWidget"))
				{
					return null;
				}
				throw new UnsupportedOperationException(method.getName());
			});
		final PrivateMessageFadePlugin plugin = new PrivateMessageFadePlugin();
		final Field clientField = PrivateMessageFadePlugin.class.getDeclaredField("client");
		clientField.setAccessible(true);
		clientField.set(plugin, client);
		final ChatMessage notice = new ChatMessage();
		notice.setType(ChatMessageType.LOGINLOGOUTNOTIFICATION);

		setLoginLogoutAlert(plugin, false);
		plugin.onChatMessage(notice);
		assertEquals(0, plugin.getUnreadCount());
		assertFalse(plugin.shouldShowUnreadIndicator());

		setLoginLogoutAlert(plugin, true);
		plugin.onChatMessage(notice);
		assertEquals(1, plugin.getUnreadCount());
		final Field activityField = PrivateMessageFadePlugin.class.getDeclaredField("lastActivityMillis");
		activityField.setAccessible(true);
		activityField.setLong(plugin, System.currentTimeMillis() - 60_000);
		assertTrue(plugin.shouldShowUnreadIndicator());

		final ChatMessage privateMessage = new ChatMessage();
		privateMessage.setType(ChatMessageType.PRIVATECHAT);
		plugin.onChatMessage(privateMessage);
		assertEquals(2, plugin.getUnreadCount());
	}

	private static void setLoginLogoutAlert(PrivateMessageFadePlugin plugin, boolean enabled) throws Exception
	{
		final Field configField = PrivateMessageFadePlugin.class.getDeclaredField("config");
		configField.setAccessible(true);
		configField.set(plugin, new PrivateMessageFadeConfig()
		{
			@Override
			public boolean loginLogoutUnreadAlert()
			{
				return enabled;
			}
		});
	}

	private static final class TestWidget
	{
		private final Widget widget;
		private final String text;
		private final Widget[] children;
		private int opacity;
		private boolean hidden;

		private TestWidget(String text, TestWidget... children)
		{
			this.text = text;
			this.children = new Widget[children.length];
			for (int i = 0; i < children.length; i++)
			{
				this.children[i] = children[i].widget;
			}
			widget = (Widget) Proxy.newProxyInstance(Widget.class.getClassLoader(), new Class<?>[]{Widget.class},
				(proxy, method, args) ->
				{
					switch (method.getName())
					{
						case "getText": return this.text;
						case "isHidden": return hidden;
						case "setHidden": hidden = (boolean) args[0]; return null;
						case "setOpacity": opacity = (int) args[0]; return proxy;
						case "getDynamicChildren": return this.children;
						case "getStaticChildren":
						case "getNestedChildren": return null;
						default: throw new UnsupportedOperationException(method.getName());
					}
				});
		}
	}
}
