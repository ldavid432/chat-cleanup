package com.github.ldavid432.cleanchat.overlay;

import com.github.ldavid432.cleanchat.ChatWidgetEditor;
import com.github.ldavid432.cleanchat.ChatWidgetGroup;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import com.github.ldavid432.cleanchat.PrivateMessageEditor;
import com.github.ldavid432.cleanchat.data.ChatTab;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Abstract overlay that renders on each chat message and is clipped to the chatbox
 */
@Slf4j
public abstract class BaseCleanChatOverlay extends Overlay
{
	@Inject
	protected Client client;

	@Inject
	protected ChatWidgetEditor chatWidgetEditor;

	@Inject
	protected PrivateMessageEditor privateMessageEditor;

	@Inject
	protected CleanChatChannelsConfig config;

	protected abstract boolean isEnabled();

	protected BaseCleanChatOverlay()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.MANUAL);

		drawAfterInterface(InterfaceID.CHATBOX);
	}

	@Override
	final public Dimension render(Graphics2D graphics)
	{
		if (!isEnabled())
		{
			return null;
		}

		ChatTab selectedChatTab = ChatTab.of(client.getVarcIntValue(VarClientID.CHAT_VIEW));
		if (selectedChatTab != ChatTab.CLOSED)
		{
			renderOnChats(client.getWidget(InterfaceID.Chatbox.SCROLLAREA),
				List.copyOf(chatWidgetEditor.getChatWidgetGroups()), graphics, false);
		}

		renderOnChats(client.getWidget(InterfaceID.PmChat.CONTAINER),
			List.copyOf(privateMessageEditor.getChatWidgetGroups()), graphics, true);

		return null;
	}

	private void renderOnChats(Widget container, List<ChatWidgetGroup> chatWidgetGroups, Graphics2D graphics, boolean isPrivateSplitChat)
	{
		if (container == null || container.isHidden() || chatWidgetGroups.isEmpty()) return;

		graphics.setClip(container.getCanvasLocation().getX(), container.getCanvasLocation().getY(), container.getWidth(), container.getHeight());
		List<ChatWidgetGroup> visibleChats = new ArrayList<>();
		ChatWidgetGroup previousGroup = null;
		boolean foundStart = false;

		for (ChatWidgetGroup group : chatWidgetGroups)
		{
			boolean isVisibleInChat = group.getY() >= container.getCanvasLocation().getY() &&
				group.getY() + group.getHeight() <= container.getCanvasLocation().getY() + container.getHeight();

			if (isVisibleInChat)
			{
				visibleChats.add(group);

				if (!foundStart)
				{
					foundStart = true;
					if (previousGroup != null)
					{
						// Element before first fully visible element
						visibleChats.add(0, previousGroup);
					}
				}
			}
			else if (foundStart)
			{
				// Element after last fully visible element
				visibleChats.add(group);
				break;
			}

			previousGroup = group;
		}

		for (ChatWidgetGroup group: visibleChats)
		{
			render(graphics, group.getX(), group.getY(), group, isPrivateSplitChat);
		}
	}

	/**
	 * Called on each visible chat message
	 */
	protected abstract void render(Graphics2D graphics, int x, int y, ChatWidgetGroup group, boolean isPrivateSplitChat);
}
