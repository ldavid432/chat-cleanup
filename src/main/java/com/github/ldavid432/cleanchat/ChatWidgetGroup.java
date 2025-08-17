package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLineCount;
import com.github.ldavid432.cleanchat.data.ChannelNameRemoval;
import static java.lang.Math.max;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.api.widgets.Widget;

@Getter
@RequiredArgsConstructor
class ChatWidgetGroup
{
	private final Widget channel;
	private final Widget rank;
	private final Widget name;
	private final Widget message;
	private final Widget clickBox;

	@Setter
	private ChannelNameRemoval channelType = null;

	private int indentSpaces = 0;

	public void onAllWidgets(Consumer<Widget> action)
	{
		Stream.of(channel, rank, name, message).forEach(action);
	}

	public void onNonChannelWidgets(Consumer<Widget> action)
	{
		Stream.of(rank, name, message).forEach(action);
	}

	public void place(final int y) {
		onAllWidgets(widget -> {
			widget.setOriginalY(y);
			widget.revalidate();
		});

		getClickBox().setOriginalY(y);
		getClickBox().revalidate();
	}

	public void calculateHeight()
	{
		if (!getMessage().getText().isEmpty() && getMessage().getWidth() > 0)
		{
			int numLines = getTextLineCount(getMessage().getText(), getMessage().getWidth(), indentSpaces);
			int height = numLines * 14; // Height of each line is always 14
			getMessage().setOriginalHeight(height);
			getMessage().revalidate();

			getClickBox().setOriginalHeight(height);
			getClickBox().revalidate();
		}
	}

	public void indent(CleanChatChannelsConfig config, String matchedChannelName, String widgetChannelText)
	{
		int startOfChannel = widgetChannelText.indexOf("[" + matchedChannelName + "]");
		int endOfChannel = startOfChannel + matchedChannelName.length() + 2;

		int indentWidth = 0;

		int channelWidth = 0;
		int prefixWidth = 0;

		// TODO: See if there's something that we are missing when measuring so we can avoid adding all these hardcoded offsets
		// Don't need to mess with indentation on messages we don't edit
		// TODO: Potentially handle other message types indent?
		if (getChannelType() != null)
		{
			switch (config.indentationMode())
			{
				// Intentionally fallthrough
				case START:
					String prefix = widgetChannelText.substring(0, startOfChannel);
					prefixWidth = getTextLength(prefix);
					indentWidth += prefixWidth;

					if (getChannelType().isEnabled(config))
					{
						if (getChannelType() == ChannelNameRemoval.FRIENDS_CHAT)
						{
							indentWidth += 1;
						}
						else
						{
							indentWidth -= 2;
						}

					}
				case CHANNEL:
					if (!getChannelType().isEnabled(config))
					{
						String channel = widgetChannelText.substring(startOfChannel, endOfChannel);
						channelWidth = getTextLength(channel);
						indentWidth += channelWidth;

						if (getChannelType() != ChannelNameRemoval.FRIENDS_CHAT)
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
					if (getChannelType() == ChannelNameRemoval.FRIENDS_CHAT)
					{
						// TODO: Can we switch back to getTextLength here?
						// For some reason the fc channel width is the entire length of the chatbox so we can't use getWidth
						int prefixChanelNameWidth = getMessage().getOriginalX() - getChannel().getOriginalX();
						nameWidth = prefixChanelNameWidth - prefixWidth - channelWidth;
					}
					else
					{
						if (!getName().getText().isEmpty() && !getName().isHidden())
						{
							nameWidth = getName().getWidth();
						}
					}

					if (!getRank().isHidden())
					{
						nameWidth += getRank().getWidth();
					}

					indentWidth += nameWidth;

					if (indentWidth > 0 && getChannelType() != ChannelNameRemoval.FRIENDS_CHAT)
					{
						indentWidth += 4;
					}
					else if (getChannelType() == ChannelNameRemoval.FRIENDS_CHAT)
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
			getMessage().setText(" ".repeat(indentSpaces) + getMessage().getText());
			getMessage().setOriginalX(getMessage().getOriginalX() - indentWidth);
			getMessage().setOriginalWidth(getMessage().getOriginalWidth() + indentWidth);
			getMessage().revalidate();
		}
	}

	// TODO: Only pass in regex and get the matched string from regex - or use regex before this call and pass in the results
	public void removeFromChannel(String text, String textRegex)
	{
		int removedWidth = getTextLength(text);

		String newText = getChannel().getText()
			// Account for color tags when removing name
			// TODO: Target the channel name more precisely, this should do for now to avoid targeting timestamps in brackets
			.replaceFirst(textRegex, "");

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

		getChannel().setText(newText);

		// Shift widgets X left if channel was removed
		int finalRemovedWidth = removedWidth;
		onNonChannelWidgets(widget -> {
			widget.setOriginalX(widget.getOriginalX() - finalRemovedWidth);
			widget.revalidate();
		});

		// Expand the width of messages if channel was removed
		getMessage().setOriginalWidth(getMessage().getWidth() + removedWidth);
		getMessage().revalidate();

		// Reduce channel width if it was removed
		getChannel().setOriginalWidth(getChannel().getOriginalWidth() - removedWidth);
		getChannel().revalidate();
	}

	public void block()
	{
		onAllWidgets(widget -> {
			widget.setHidden(true);
			widget.setOriginalY(0);
		});

		getClickBox().setHidden(true);
		getClickBox().setOriginalY(0);
	}

}
