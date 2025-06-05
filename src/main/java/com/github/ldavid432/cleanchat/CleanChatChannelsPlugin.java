package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.CleanChatUtil.sanitizeUsername;
import com.github.ldavid432.cleanchat.data.ChannelNameReplacement;
import com.github.ldavid432.cleanchat.data.ChatBlock;
import com.github.ldavid432.cleanchat.data.ChatTab;
import com.google.inject.Provides;
import java.util.Objects;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
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
	@Getter
	private CleanChatChannelsConfig config;

	@Inject
	@Getter
	private ChannelNameManager channelNameManager;

	@Inject
	private EventBus eventBus;

	@Provides
	CleanChatChannelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CleanChatChannelsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("Plugin enabled. Refreshing chat.");
			client.refreshChat();
		}

		eventBus.register(channelNameManager);
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(channelNameManager);

		// Remove all our shenanigans
		client.refreshChat();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (Objects.equals(event.getGroup(), CleanChatChannelsConfig.GROUP))
		{
			log.debug("Config changed. Refreshing chat.");
			client.refreshChat();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		// rebuildchatbox script
		if (event.getScriptId() != 84)
		{
			return;
		}

		if (!ChannelNameReplacement.anyEnabled(config) && !ChatBlock.anyEnabled(config))
		{
			return;
		}

		// FriendsChatManager is null at the first FriendsChatChanged after login so we have to add this check later
		channelNameManager.setFriendsChatNameIfNeeded();

		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		ChatTab selectedChatTab = ChatTab.of(client.getVarcIntValue(41));

		if (chatbox != null && selectedChatTab != ChatTab.CLOSED)
		{
			/*
			Most chats appear in this format as dynamic children:
				// bottom chat line
				[0] = username
				[1] = chat message
				[2] = sender (clan name, also includes timestamp if that plugin is on)
				[3] = rank icon
				// Next chat line
				[4] = next username
				etc...

			Friends chats appear in this format:
				[0] = sender + username
				[1] = chat message
				[2] = nothing
				[3] = rank icon?

			GIM broadcasts appear in this format:
				[0] = sender
				[1] = chat message
				[2] = nothing
				[3] = nothing

			These appear in the children array in this order even if the individual items aren't rendered
				ex: Username is hidden for broadcasts, rank icon is hidden for public chat */
			Widget[] chatWidgets = chatbox.getDynamicChildren().clone();
			int removedHeight = 0;

			// Since we only need to check either [2] or [0] of each line for the sender we can just iterate over every other child
			for (int i = 0; i < chatWidgets.length; i += 2)
			{
				int iconWidgetIndex = i + 1;
				int messageWidgetIndex = i - 1;
				int nameWidgetIndex = i - 2;

				// Friends chat & GIM broadcast widget ordering is different
				if (i % 4 == 0)
				{
					messageWidgetIndex = i + 1;
					// Empty widget
					nameWidgetIndex = i + 2;
					// Empty widget in case of GIM broadcasts
					iconWidgetIndex = i + 3;
				} // else i % 4 == 2

				int removedWidth = 0;

				// Need to use the message widget earlier than the others in order to check for blocks
				String message = null;
				if (isIndexValid(messageWidgetIndex, chatWidgets))
				{
					message = chatWidgets[messageWidgetIndex].getText();
				}

				boolean blockChat = shouldBlockMessage(message);

				Widget channelWidget = chatWidgets[i];
				if (!channelWidget.getText().isBlank())
				{
					// If the text is not blank we *should* be guaranteed a match
					for (ChannelNameReplacement channelNameToReplace : ChannelNameReplacement.values())
					{
						String plainChannelName = channelNameToReplace.getName(channelNameManager);
						String channelName = "[" + plainChannelName + "]";
						if (sanitizeUsername(channelWidget.getText()).contains(channelName))
						{
							blockChat = blockChat || checkGroupIronInClan(selectedChatTab, channelNameToReplace);

							if (channelNameToReplace.isEnabled(config)) {
								// Update widget text and removedWidth
								removedWidth = getTextLength(channelName) + updateChannelText(plainChannelName, channelWidget);
								break;
							}
						}
					}
				}

				channelWidget.setOriginalY(channelWidget.getOriginalY() + removedHeight); // Shift down
				channelWidget.setOriginalWidth(channelWidget.getOriginalWidth() - removedWidth);
				channelWidget.revalidate();

				processWidget(iconWidgetIndex, chatWidgets, removedWidth, removedHeight, blockChat);
				processWidget(messageWidgetIndex, chatWidgets, removedWidth, removedHeight, blockChat);
				processWidget(nameWidgetIndex, chatWidgets, removedWidth, removedHeight, blockChat);

				if (blockChat)
				{
					log.debug("Blocking message {}", message);
					// TODO: Check that this doesn't need to be before revalidate
					channelWidget.setHidden(true);
					// Height of 1 line
					removedHeight += 14;
					log.debug("Removed height {}", removedHeight);
				}
			}
		}
	}

	private boolean isIndexValid(int index, Object[] array)
	{
		return index >= 0 && index < array.length;
	}

	private void processWidget(int index, Widget[] chatWidgets, int removedWidth, int removedHeight, boolean hideLine)
	{
		if (isIndexValid(index, chatWidgets)) {
			Widget widget = chatWidgets[index];
			widget.setOriginalX(widget.getOriginalX() - removedWidth); // Shift left
			widget.setOriginalY(widget.getOriginalY() + removedHeight); // Shift down
			if (hideLine)
			{
				widget.setHidden(true);
			}
			widget.revalidate();
		}
	}

	private boolean shouldBlockMessage(String message)
	{
		if (message == null) {
			return false;
		}
		return Stream.of(ChatBlock.values()).anyMatch(block -> block.appliesTo(this, message, channelNameManager));
	}

	private boolean checkGroupIronInClan(ChatTab chatTab, ChannelNameReplacement channelNameToReplace)
	{
		return config.removeGroupIronFromClan() && chatTab == ChatTab.CLAN && channelNameToReplace == ChannelNameReplacement.GROUP_IRON;
	}

	/**
	 * @return Any extra space removed
	 */
	private int updateChannelText(String channelName, Widget channelWidget)
	{
		int removedWidth = 0;

		String newText = channelWidget.getText()
			.replace('\u00A0', ' ')
			// Account for color tags when removing name
			.replaceFirst("\\[.*" + channelName + ".*]", "");

		// Remove trailing spaces - probably only happens with timestamps turned on
		if (newText.endsWith(" ") || newText.endsWith("\u00A0"))
		{
			newText = newText.substring(0, newText.length() - 1);
			removedWidth += getTextLength(" ");
		}

		// Remove double spaces - mainly found in friends chat since it has sender + username
		if (newText.contains("  ") || newText.contains("\u00A0\u00A0"))
		{
			newText = newText.replaceFirst(" {2}|\u00A0{2}", " ");
			removedWidth += getTextLength(" ");
		}

		channelWidget.setText(newText);

		return removedWidth;
	}

}
