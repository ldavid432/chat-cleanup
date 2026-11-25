package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.data.ChatTab;
import com.github.ldavid432.cleanchat.util.CleanChatUtil;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

@Singleton
public class PrivateMessageEditor
{
	@Inject
	private Client client;

	@Inject
	private CleanChatChannelsConfig config;

	@Inject
	private TimestampPluginIntegration timestampIntegration;

	@Getter
	private List<ChatWidgetGroup> chatWidgetGroups = List.of();

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == CleanChatUtil.SCRIPT_REBUILD_PMBOX)
		{
			processWidgets();
		}
	}

	// TODO: see if we can abstract any of this with ChatWidgetEditor
	private void processWidgets()
	{
		Widget pmBox = client.getWidget(InterfaceID.PmChat.CONTAINER);
		if (pmBox != null)
		{
			Widget[] chatWidgets = pmBox.getDynamicChildren().clone();
			Widget[] clickBoxWidgets = pmBox.getStaticChildren().clone();

			chatWidgetGroups = Stream.iterate(0, i -> i < chatWidgets.length, i -> i + 4)
				.filter(i -> !chatWidgets[i].isHidden() || !chatWidgets[i + 1].isHidden())
				.map(i -> new ChatWidgetGroup(
					null,
					null,
					chatWidgets[i],
					chatWidgets[i + 1],
					clickBoxWidgets[i / 4],
					List.of(chatWidgets[i + 2], chatWidgets[i + 3]),
					null
				))
				.peek(group -> {
					if (config.removePrivateToFrom() && group.name != null)
					{
						group.replaceText("To ", "", "To ", client, group.name, group.message);
						group.replaceText("From ", "", "From ", client, group.name, group.message);
					}

					group.calculateWidgetSizes(client);

					group.calculateMessageIndent(config, client);

					if (!config.hiddenGameModeIcons().isEmpty())
					{
						group.hideGameModeIcons(config.hiddenGameModeIcons(), client);
					}

					if (timestampIntegration.isFixedWidthTimestampEnabled())
					{
						group.extractTimestamp(timestampIntegration.getTimestampTemplate(), timestampIntegration.getTimestampTemplateWidth(), client);
					}

					group.applyIndent();

					group.calculateHeight(client, 13);
				})
				.collect(Collectors.toList());

			Widget chatbox = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
			Widget chatboxControls = client.getWidget(InterfaceID.Chatbox.CONTROLS);
			ChatTab selectedChatTab = ChatTab.of(client.getVarcIntValue(VarClientID.CHAT_VIEW));

			int y;
			if (selectedChatTab == ChatTab.CLOSED)
			{
				assert chatboxControls != null;
				y = chatboxControls.getOriginalY() + chatboxControls.getHeight();
			}
			else if (client.isResized())
			{
				assert chatbox != null;
				y = chatbox.getOriginalY() + chatbox.getHeight();
			}
			else
			{
				assert chatbox != null;
				y = chatbox.getOriginalY();
			}

			for (ChatWidgetGroup group : chatWidgetGroups)
			{
				group.place(y);

				y += group.getHeight();
			}
		}
	}
}
