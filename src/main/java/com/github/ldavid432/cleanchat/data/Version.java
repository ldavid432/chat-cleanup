package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import java.awt.Color;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ColorUtil;

// Probably overkill and won't be used often
public enum Version
{
	ONE,
	TWO,
	;

	public static final Version PREVIOUS = values()[values().length - 2];
	private static final Version LATEST = values()[values().length - 1];

	public static void checkLatest(CleanChatChannelsConfig config, Client client, ClientThread clientThread)
	{
		if (config.getLastSeenVersion() != LATEST)
		{
			config.setLstSeenVersion(LATEST);

			clientThread.invokeLater(() -> client.addChatMessage(
					ChatMessageType.CONSOLE,
					"",
					ColorUtil.wrapWithColorTag(
						"Clean Chat has been updated to 2.0! This update is mainly a major rework to the plugin internals. " +
							"If you run into any issues please report them on the GitHub.",
						Color.RED
					),
					"",
					false
				)
			);
		}
	}

}
