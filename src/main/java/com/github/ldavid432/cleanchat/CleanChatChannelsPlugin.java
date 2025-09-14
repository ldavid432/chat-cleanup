package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatChannelsConfig.CURRENT_VERSION;
import static com.github.ldavid432.cleanchat.CleanChatChannelsConfig.HIDE_SCROLLBAR_KEY;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.Objects;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetSizeMode;
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
	private ClientThread clientThread;

	@Inject
	private CleanChatChannelsConfig config;

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
			client.refreshChat();
		} else if (client.getGameState() != GameState.LOGGED_IN && config.getLastSeenVersion() < CURRENT_VERSION) {
			int lastSeenVersion = config.getLastSeenVersion();
			config.setLstSeenVersion(CURRENT_VERSION);
			// Since last seen version wasn't in 1.0 checking for only it will trigger for everyone who installs the plugin.
			//  By only triggering this during startup and while not logged in we can "better" attempt to determine if this is a previous install or not.
			//  Still not totally accurate but better than nothing.
			if (lastSeenVersion < 1) {
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
			client.refreshChat();

			if (Objects.equals(event.getKey(), HIDE_SCROLLBAR_KEY))
			{
				clientThread.invoke(this::handleScrollbarVisibility);
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.CHATBOX)
		{
			handleScrollbarVisibility();
		}
	}

	private void handleScrollbarVisibility()
	{
		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		Widget scrollBarContainer = client.getWidget(InterfaceID.Chatbox.CHATSCROLLBAR);
		if (chatbox != null && scrollBarContainer != null) {
			scrollBarContainer.setHidden(config.hideScrollbar());

			if (config.hideScrollbar()) {
				chatbox.setOriginalWidth(chatbox.getWidth() + scrollBarContainer.getWidth());
			} else {
				chatbox.setOriginalWidth(chatbox.getWidth() - scrollBarContainer.getWidth());
			}
			chatbox.setWidthMode(WidgetSizeMode.ABSOLUTE);
			chatbox.revalidate();

			client.refreshChat();
		}
	}

}
