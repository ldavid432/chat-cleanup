package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.data.Version;
import com.google.inject.Provides;
import java.util.Objects;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

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
	private ChannelNameManager channelNameManager;

	@Inject
	private ChannelNameReplacer channelNameReplacer;

	@Inject
	private ChatBlocker chatBlocker;

	@Inject
	private EventBus eventBus;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CleanChatChannelsConfig config;

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
		eventBus.register(chatBlocker);
		channelNameManager.startup();

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("Plugin enabled. Refreshing chat.");
			chatBlocker.processChatHistory();
			client.refreshChat();

			Version.checkLatest(config, client, clientThread);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(channelNameReplacer);
		eventBus.unregister(channelNameManager);
		eventBus.unregister(chatBlocker);

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
			chatBlocker.processChatHistory();
			client.refreshChat();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			Version.checkLatest(config, client, clientThread);
		}
	}

}
