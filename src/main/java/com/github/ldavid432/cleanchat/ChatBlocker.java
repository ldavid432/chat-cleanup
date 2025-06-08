package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.WELCOME_MESSAGE;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getChatLineBuffer;
import com.github.ldavid432.cleanchat.data.ChatBlock;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class ChatBlocker
{
	@Inject
	private CleanChatChannelsConfig config;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getMessage().equals(WELCOME_MESSAGE))
		{
			log.debug("World hopped or logged in. Refreshing chat.");
			clientThread.invokeLater(this::processChatHistory);
		}

		if (!ChatBlock.anyEnabled(config))
		{
			return;
		}

		checkMessage(event.getMessageNode());
	}

	private void checkMessage(MessageNode messageNode) {
		if (shouldBlockMessage(messageNode.getValue(), messageNode.getType()))
		{
			ChatLineBuffer buffer = getChatLineBuffer(client, messageNode.getType());
			if (buffer != null)
			{
				log.debug("Removing message: {}", messageNode.getValue());
				buffer.removeMessageNode(messageNode);
				client.refreshChat();
			}
		}
	}

	private boolean shouldBlockMessage(String message, ChatMessageType chatMessageType)
	{
		if (message == null)
		{
			return false;
		}
		return Stream.of(ChatBlock.values()).anyMatch(block -> block.appliesTo(config, message, chatMessageType));
	}

	public void processChatHistory()
	{
		ChatBlock.getEnabledTypes(config)
			.flatMap(type -> {
				ChatLineBuffer buffer = getChatLineBuffer(client, type);
				if (buffer == null)
				{
					return Stream.empty();
				}
				return Arrays.stream(buffer.getLines().clone()).filter(Objects::nonNull).map(node -> Pair.of(type, node));
			})
			.sorted(Comparator.comparingInt(pair -> pair.getValue().getTimestamp()))
			.forEach(pair -> {
				MessageNode messageNode = pair.getValue();
				ChatMessageType type = pair.getKey();
				// Ignore message types that don't match (this will only happen with gim chat vs clan chat)
				if (messageNode == null || type != messageNode.getType())
				{
					return;
				}
				clientThread.invoke(() -> checkMessage(messageNode));
			});
	}
}
