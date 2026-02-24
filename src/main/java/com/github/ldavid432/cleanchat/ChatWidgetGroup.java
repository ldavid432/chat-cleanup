package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLineCount;
import static com.github.ldavid432.cleanchat.CleanChatUtil.wrapWithBrackets;
import static com.github.ldavid432.cleanchat.CleanChatUtil.wrapWithChannelNameRegex;
import com.github.ldavid432.cleanchat.data.ChatChannel;
import com.github.ldavid432.cleanchat.data.ChatTab;
import static java.lang.Math.max;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;

@Slf4j
@RequiredArgsConstructor
class ChatWidgetGroup
{
	private final Widget channel;
	private final Widget rank;
	private final Widget name;
	private final Widget message;
	private final Widget clickBox;

	@Setter
	private ChatChannel channelType = null;

	private int indentSpaces = 0;

	public String getChannelText()
	{
		return channel.getText();
	}

	public int getHeight()
	{
		return message.getHeight();
	}

	public void place(final int y) {
		place(channel, y);
		place(rank, y);
		place(name, y);
		place(message, y);

		clickBox.setOriginalY(y);
		clickBox.setHidden(false);
		clickBox.revalidate();
	}

	public void calculateHeight()
	{
		if (!message.getText().isEmpty() && message.getWidth() > 0)
		{
			int numLines = getTextLineCount(message.getText(), message.getWidth(), indentSpaces);
			int height = numLines * 14; // Height of each line is always 14
			message.setOriginalHeight(height);
			message.revalidate();

			clickBox.setOriginalHeight(height);
			clickBox.revalidate();
		}
	}

	public void indent(CleanChatChannelsConfig config, String matchedChannelName, String widgetChannelText)
	{
		int startOfChannel = widgetChannelText.indexOf(matchedChannelName);
		int endOfChannel = startOfChannel + matchedChannelName.length();

		int indentWidth = 0;

		int channelWidth = 0;
		int prefixWidth = 0;

		// TODO: See if there's something that we are missing when measuring so we can avoid adding all these hardcoded offsets
		// Don't need to mess with indentation on messages we don't edit
		// TODO: Potentially handle other message types indent?
		if (channelType != null)
		{
			switch (config.indentationMode())
			{
				// Intentionally fallthrough
				case START:
					String prefix = widgetChannelText.substring(0, startOfChannel);
					prefixWidth = getTextLength(prefix);
					indentWidth += prefixWidth;

					if (channelType.isChannelNameRemovalEnabled(config))
					{
						if (channelType == ChatChannel.FRIENDS_CHAT)
						{
							indentWidth += 1;
						}
						else
						{
							indentWidth -= 2;
						}

					}
				case CHANNEL:
					if (!channelType.isChannelNameRemovalEnabled(config))
					{
						String channel = widgetChannelText.substring(startOfChannel, endOfChannel);
						channelWidth = getTextLength(channel);
						indentWidth += channelWidth;

						if (channelType != ChatChannel.FRIENDS_CHAT)
						{
							indentWidth += 1;
						}
						else
						{
							indentWidth += 4;
						}

					}
				case NAME:
					int nameWidth = 0;
					// FC puts name + channel into the channel widget
					if (channelType == ChatChannel.FRIENDS_CHAT)
					{
						// TODO: Can we switch back to getTextLength here?
						// For some reason the fc channel width is the entire length of the chatbox so we can't use getWidth
						int prefixChanelNameWidth = message.getOriginalX() - channel.getOriginalX();
						nameWidth = prefixChanelNameWidth - prefixWidth - channelWidth;
					}
					else
					{
						if (!name.getText().isEmpty() && !name.isHidden())
						{
							nameWidth = name.getWidth();
						}
					}

					if (!rank.isHidden())
					{
						nameWidth += rank.getWidth();
					}

					indentWidth += nameWidth;

					if (indentWidth > 0 && channelType != ChatChannel.FRIENDS_CHAT)
					{
						indentWidth += 4;
					}
					else if (channelType == ChatChannel.FRIENDS_CHAT)
					{
						indentWidth -= 4;
					}
				case MESSAGE:
					// Already set by default
			}
		}

		if (indentWidth > 0) {
			indentSpaces = max(0, indentWidth / 3);
		} else {
			indentSpaces = 0;
		}

		if (indentSpaces > 0)
		{
			// Using spaces to keep the first line at the initial position (+/-2 pixels)
			message.setText(" ".repeat(indentSpaces) + message.getText());
			message.setOriginalX(message.getOriginalX() - indentWidth);
			message.setOriginalWidth(message.getOriginalWidth() + indentWidth);
			message.revalidate();
		}
	}

	public void removeFromChannel(String text)
	{
		replaceChannelName(text, "");
	}

	public String replaceChannelName(String text, String newChannelName)
	{
		int currentWidth = getTextLength(wrapWithBrackets(text));
		int newWidth = getTextLength(newChannelName);
		int removedWidth = currentWidth - newWidth;

		String newText = channel.getText()
			// TODO: Target the channel name more precisely, this should do for now to avoid targeting timestamps in brackets
			.replaceFirst(wrapWithChannelNameRegex(text), newChannelName);

		// Remove trailing spaces - probably only happens with timestamps turned on
		if (newText.endsWith(" "))
		{
			newText = newText.substring(0, newText.length() - 1);
			removedWidth += getTextLength(" ");
		}

		// Remove double spaces - mainly found in friends chat since it has sender + username
		if (newText.contains("  "))
		{
			newText = newText.replaceFirst(" {2}", " ");
			removedWidth += getTextLength(" ");
		}

		channel.setText(newText);

		// Shift widgets X left if channel was removed
		shiftLeft(rank, removedWidth);
		shiftLeft(name, removedWidth);
		shiftLeft(message, removedWidth);

		// Expand the width of messages if channel was removed
		message.setOriginalWidth(message.getOriginalWidth() + removedWidth);
		message.revalidate();

		// Reduce channel width if it was removed
		channel.setOriginalWidth(channel.getOriginalWidth() - removedWidth);
		channel.revalidate();

		return newText;
	}

	public void removeRank()
	{
		if (!rank.isHidden()) {
			rank.setHidden(true);

			int removedWidth = rank.getWidth();

			shiftLeft(name, removedWidth);
			shiftLeft(message, removedWidth);

			// Expand the width of messages if rank was removed
			expand(message, removedWidth);
		}
	}

	private void shiftLeft(Widget widget, int width)
	{
		widget.setOriginalX(widget.getOriginalX() - width);
		widget.revalidate();
	}

	private void expand(Widget widget, int width)
	{
		widget.setOriginalWidth(widget.getOriginalWidth() + width);
		widget.revalidate();
	}

	private void place(Widget widget, int y)
	{
		widget.setOriginalY(y);
		widget.revalidate();
	}

}
