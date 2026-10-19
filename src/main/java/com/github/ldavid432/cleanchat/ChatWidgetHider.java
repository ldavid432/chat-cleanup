package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatChannelsConfig.HIDE_BOT_SEPARATOR_KEY;
import static com.github.ldavid432.cleanchat.CleanChatChannelsConfig.HIDE_SCROLLBAR_KEY;
import static com.github.ldavid432.cleanchat.CleanChatChannelsConfig.HIDE_TOP_SEPARATOR_KEY;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.SCRIPT_REBUILD_CHATBOX;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.isChatboxTransparent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

/**
 * Handles visibility of non-message chat widgets (i.e. scrollbar, separators)
 */
@Slf4j
@Singleton
public class ChatWidgetHider
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CleanChatChannelsConfig config;

	public void onStartup()
	{
		clientThread.invoke(() -> {
			handleScrollbarVisibility();
			handleSeparatorVisibility();
		});
	}

	public void onShutdown()
	{
		clientThread.invoke(() -> {
			handleScrollbarVisibility(false);
			handleSeparatorVisibility(false, false);
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (Objects.equals(event.getGroup(), CleanChatChannelsConfig.GROUP))
		{
			log.debug("Config changed. Refreshing chat.");
			client.refreshChat();

			switch (event.getKey())
			{
				case HIDE_SCROLLBAR_KEY:
					clientThread.invoke(() -> handleScrollbarVisibility());
					break;
				case HIDE_BOT_SEPARATOR_KEY:
				case HIDE_TOP_SEPARATOR_KEY:
					clientThread.invoke(() -> handleSeparatorVisibility());
					break;
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.CHATBOX)
		{
			handleScrollbarVisibility();
			handleSeparatorVisibility();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == SCRIPT_REBUILD_CHATBOX)
		{
			clientThread.invokeLater(() -> handleSeparatorVisibility());
		}
	}

	private void handleScrollbarVisibility()
	{
		handleScrollbarVisibility(config.hideScrollbar());
	}

	private void handleScrollbarVisibility(boolean hideScrollbar)
	{
		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		Widget scrollBarContainer = client.getWidget(InterfaceID.Chatbox.CHATSCROLLBAR);
		if (chatbox != null && scrollBarContainer != null)
		{
			scrollBarContainer.setHidden(hideScrollbar);

			// width mode is MINUS, so we if we want to match the parent width we use 0
			if (hideScrollbar)
			{
				chatbox.setOriginalWidth(0);
			}
			else
			{
				chatbox.setOriginalWidth(scrollBarContainer.getWidth());
			}
			chatbox.revalidate();

			client.refreshChat();
		}
	}

	private void handleSeparatorVisibility()
	{
		handleSeparatorVisibility(config.isHideTopSeparator(), config.isHideBottomSeparator());
	}

	private void handleSeparatorVisibility(boolean hideTop, boolean hideBottom)
	{
		Widget chatDisplay = client.getWidget(InterfaceID.Chatbox.CHATDISPLAY);
		if (chatDisplay != null)
		{
			if (isChatboxTransparent(client))
			{
				// In transparent chat mode there are 2 separators, and they are each made up of many smaller widgets
				Widget[] dynamicChildren = chatDisplay.getDynamicChildren();
				for (int i = 0; i < dynamicChildren.length; i++)
				{
					boolean isTop = i % 2 == 0;
					Widget widget = dynamicChildren[i];
					widget.setHidden(isTop ? hideTop : hideBottom);
				}
			}
			else
			{
				// Only the bottom separator exists in non-transparent chat mode
				Optional<Widget> separator = Arrays.stream(chatDisplay.getDynamicChildren()).findFirst();
				separator.ifPresent(sep -> sep.setHidden(hideBottom));
			}
		}
	}
}
