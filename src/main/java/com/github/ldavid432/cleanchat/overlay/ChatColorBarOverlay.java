package com.github.ldavid432.cleanchat.overlay;

import com.github.ldavid432.cleanchat.ChatWidgetGroup;
import java.awt.Graphics2D;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

@Slf4j
@Singleton
public class ChatColorBarOverlay extends BaseCleanChatOverlay
{

	@Override
	void render(Graphics2D graphics, int x, int y, ChatWidgetGroup group)
	{
		Widget chatbox = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);

		x = Math.min(
			Math.max(
				x + config.colorBarOffset(),
				chatbox != null ? chatbox.getCanvasLocation().getX() : 0
			),
			chatbox != null ? chatbox.getCanvasLocation().getX() + chatbox.getWidth() - 1 : 0
		);

		graphics.setColor(group.getChannelType() != null ? group.getChannelType().getColor(config) : config.noChannelColor());
		graphics.fillRect(x, y, config.colorBarWidth(), group.getHeight());
	}

	@Override
	boolean isEnabled()
	{
		return config.isColorBarEnabled();
	}
}
