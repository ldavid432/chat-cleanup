package com.github.ldavid432.cleanchat.overlay;

import com.github.ldavid432.cleanchat.ChatWidgetEditor;
import com.github.ldavid432.cleanchat.ChatWidgetGroup;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
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
	protected CleanChatChannelsConfig config;

	abstract boolean isEnabled();

	BaseCleanChatOverlay()
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

		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		ChatTab selectedChatTab = ChatTab.of(client.getVarcIntValue(VarClientID.CHAT_VIEW));

		if (chatbox == null || chatbox.isHidden() || selectedChatTab == ChatTab.CLOSED)
		{
			return null;
		}

		graphics.setClip(chatbox.getCanvasLocation().getX(), chatbox.getCanvasLocation().getY(), chatbox.getWidth(), chatbox.getHeight());

		List<ChatWidgetGroup> allChats = List.copyOf(chatWidgetEditor.getChatWidgetGroups());
		List<ChatWidgetGroup> visibleChats = new ArrayList<>();
		ChatWidgetGroup previousGroup = null;
		boolean foundStart = false;

		for (ChatWidgetGroup group : allChats)
		{
			boolean isVisibleInChat = group.getY() >= chatbox.getCanvasLocation().getY() &&
				group.getY() + group.getHeight() <= chatbox.getCanvasLocation().getY() + chatbox.getHeight();

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

		visibleChats.forEach(group -> {
			int x = group.getX();
			int y = group.getY();

			render(graphics, x, y, group);
		});

		return null;
	}

	/**
	 * Called on each visible chat message
	 */
	abstract void render(Graphics2D graphics, int x, int y, ChatWidgetGroup group);
}
