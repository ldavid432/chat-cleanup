/*
 * Copyright (c) 2018, Magic fTail
 * Copyright (c) 2019, osrs-music-map <osrs-music-map@users.noreply.github.com>
 * Copyright (c) 2026, Lake David
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.VARC_INT_CHAT_TAB;
import com.github.ldavid432.cleanchat.data.ChannelNameRemoval;
import com.github.ldavid432.cleanchat.data.ChatBlock;
import com.github.ldavid432.cleanchat.data.ChatTab;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.eventbus.Subscribe;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@Singleton
public class ChatBlocker
{
	@Inject
	private CleanChatChannelsConfig config;

	@Inject
	private ChannelNameManager channelNameManager;

	@Inject
	private Client client;

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		// Yoinked from core RL

		if (!"chatFilterCheck".equals(event.getEventName()))
		{
			return;
		}

		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		Object[] objectStack = client.getObjectStack();
		int objectStackSize = client.getObjectStackSize();

		final int messageType = intStack[intStackSize - 2];
		final int messageId = intStack[intStackSize - 1];
		String message = (String) objectStack[objectStackSize - 1];

		final MessageNode messageNode = client.getMessages().get(messageId);
		final String channel = messageNode.getSender();

		// end core RL

		ChatTab selectedChatTab = ChatTab.of(client.getVarcIntValue(VARC_INT_CHAT_TAB));

		boolean blockChat = Stream.of(ChatBlock.values()).anyMatch(block -> block.appliesTo(config, message));

		if (!blockChat && !message.isEmpty())
		{
			Pair<ChannelNameRemoval, String> match = ChannelNameRemoval.findChannelMatch(channel, channelNameManager);
			if (match == null) {
				return;
			}

			ChannelNameRemoval channelRemoval = match.getLeft();

			blockChat = channelRemoval.isTabBlocked(config, selectedChatTab);
		}

		// Totally bizarre situation where after hopping the clan instruction becomes the name and the previous 'did you know?' becomes the message
		//  channel=nothing, name=clan instruction, message=did you know? ...
		if (!blockChat && ChatBlock.CLAN_INSTRUCTION.appliesTo(config, channel))
		{
			blockChat = true;
		}

		if (blockChat)
		{
			intStack[intStackSize - 3] = 0;
		}
	}
}
