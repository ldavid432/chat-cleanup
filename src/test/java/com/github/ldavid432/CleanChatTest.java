package com.github.ldavid432;

import com.github.ldavid432.cleanchat.CleanChatChannelsPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CleanChatTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CleanChatChannelsPlugin.class);
		RuneLite.main(args);
	}
}
