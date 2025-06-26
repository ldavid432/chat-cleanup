package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatChannelsConfig.CURRENT_VERSION;
import static com.github.ldavid432.cleanchat.CleanChatUtil.WELCOME_MESSAGE;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getChatLineBuffer;
import com.github.ldavid432.cleanchat.data.ChatBlock;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@PluginDescriptor(
	name = "Clean Chat",
	description = "Hide clan name and more to clean your chat channels, includes GIM, friends, and clan chats",
	tags = {"clean", "chat", "clan", "friends", "gim", "group", "iron", "ironman", "channel"}
)
public class CleanChatChannelsPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private CleanChatChannelsConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChannelNameManager channelNameManager;

	@Inject
	private ChannelNameReplacer channelNameReplacer;

	@Inject
	private EventBus eventBus;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Provides
	CleanChatChannelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CleanChatChannelsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		eventBus.register(channelNameReplacer);
		eventBus.register(channelNameManager);
		channelNameManager.startup();

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("Plugin enabled. Refreshing chat.");
			processAllChatHistory();
			client.refreshChat();
		} else if (client.getGameState() != GameState.LOGGED_IN && config.getLastSeenVersion() < CURRENT_VERSION) {
			config.setLstSeenVersion(CURRENT_VERSION);
			// Since last seen version wasn't in 1.0 checking for only it will trigger for everyone who installs the plugin.
			//  By only triggering this during startup and while not logged in we can "better" attempt to determine if this is a previous install or not.
			//  Still not totally accurate but better than nothing.
			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(
						ColorUtil.wrapWithColorTag(
							"Clean Chat has been updated to 2.0! This update is mainly a major rework to the plugin internals. " +
								"If you run into any issues please report them on the GitHub.",
							Color.RED
						)
					)
				.build());
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(channelNameReplacer);
		eventBus.unregister(channelNameManager);
		channelNameManager.shutdown();

		// Remove all our shenanigans
		log.debug("Plugin disabled. Refreshing chat.");
		client.refreshChat();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (Objects.equals(event.getGroup(), CleanChatChannelsConfig.GROUP))
		{
			log.debug("Config changed. Refreshing chat.");
			processAllChatHistory();
			client.refreshChat();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		// This is when chat history sends old chats, so we wait a bit for it to populate and then run our stuff
		if (event.getMessage().equals(WELCOME_MESSAGE))
		{
			log.debug("World hopped or logged in. Refreshing chat.");
			// Only process blocks because we want to wait for the individual chats to connect before replacing
			//  This makes startup much less jarring
			clientThread.invokeLater(this::processAllChatHistory);
		}

		if (ChatBlock.getEnabledTypes(config).noneMatch(type -> type == event.getType()))
		{
			return;
		}

		processBlocks(event.getMessageNode());
	}

	// TODO: Move blocking into a separate class
	private void processBlocks(MessageNode messageNode)
	{
		// The messages we are blocking are game messages so we shouldn't need to check messageNode.getRuneLiteFormatMessage()
		if (shouldBlockMessage(messageNode.getValue(), messageNode.getType()))
		{
			log.debug("Blocking message: {}", messageNode.getValue());
			removeChatMessage(messageNode);
			client.refreshChat();
		}
	}

	private boolean shouldBlockMessage(String message, ChatMessageType chatMessageType)
	{
		return Stream.of(ChatBlock.values()).anyMatch(it -> it.appliesTo(config, message, chatMessageType));
	}

	private void removeChatMessage(MessageNode messageNode)
	{
		ChatLineBuffer buffer = getChatLineBuffer(client, messageNode.getType());
		if (buffer != null)
		{
			buffer.removeMessageNode(messageNode);
		}
	}

	private void processAllChatHistory()
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
				clientThread.invoke(() -> processBlocks(messageNode));
			});
	}
}
