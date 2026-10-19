package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.overlay.ChatColorBarOverlay;
import com.github.ldavid432.cleanchat.overlay.ChatTimestampOverlay;
import com.github.ldavid432.cleanchat.util.ChangeLogUtil;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Clean Chat",
	description = "Hide clan name and more to clean your chat channels, includes GIM, friends, and clan chats",
	tags = {"clean", "chat", "clan", "friends", "gim", "group", "iron", "ironman", "channel", "color", "hide", "remove", "custom", "timestamp"}
)
public class CleanChatChannelsPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private ChannelNameManager channelNameManager;

	@Inject
	private ChatWidgetEditor chatWidgetEditor;

	@Inject
	private ChatBlocker chatBlocker;

	@Inject
	private EventBus eventBus;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ChatColorBarOverlay colorBarOverlay;

	@Inject
	private ChatTimestampOverlay timestampOverlay;

	@Inject
	private TimestampPluginIntegration timestampIntegration;

	@Inject
	private ChatWidgetHider hider;

	@Inject
	private ConfigManager configManager;

	@Provides
	CleanChatChannelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CleanChatChannelsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		timestampIntegration.onStartup();

		eventBus.register(chatBlocker);
		eventBus.register(chatWidgetEditor);
		eventBus.register(channelNameManager);
		eventBus.register(timestampIntegration);
		eventBus.register(hider);
		channelNameManager.startup();
		eventBus.register(timestampOverlay);
		overlayManager.add(timestampOverlay);
		timestampOverlay.startUp();
		overlayManager.add(colorBarOverlay);
		hider.onStartup();

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("Plugin enabled. Refreshing chat.");
			client.refreshChat();
		}

		ChangeLogUtil.handleChangelog(configManager, chatMessageManager, client);
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(chatBlocker);
		eventBus.unregister(chatWidgetEditor);
		eventBus.unregister(channelNameManager);
		eventBus.unregister(timestampIntegration);
		eventBus.unregister(hider);
		channelNameManager.shutdown();
		overlayManager.remove(timestampOverlay);
		eventBus.unregister(timestampOverlay);
		overlayManager.remove(colorBarOverlay);

		// Remove all our shenanigans
		log.debug("Plugin disabled. Refreshing chat.");
		hider.onShutdown();
		client.refreshChat();
	}

}
