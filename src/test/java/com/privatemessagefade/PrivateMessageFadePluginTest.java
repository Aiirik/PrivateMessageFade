package com.privatemessagefade;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PrivateMessageFadePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PrivateMessageFadePlugin.class);
		RuneLite.main(args);
	}
}
