package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.util.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.getTextLineCount;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.wrapWithBrackets;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.wrapWithChannelNameRegex;
import com.github.ldavid432.cleanchat.data.ChatChannel;
import com.github.ldavid432.cleanchat.util.FormatterExtractor;
import static java.lang.Math.max;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;

@Slf4j
@RequiredArgsConstructor
public class ChatWidgetGroup
{
	private final Widget channel;
	private final Widget rank;
	private final Widget name;
	private final Widget message;
	private final Widget clickBox;

	@Getter
	@Setter
	@Nullable
	private ChatChannel channelType = null;

	private int messageIndentSpaces = 0;
	private int channelIndentSpaces = 0;

	@Getter
	@Nullable
	private FormatterExtractor.ExtractionResult timestamp = null;

	public String getChannelText()
	{
		return channel.getText();
	}

	public int getHeight()
	{
		return message.getHeight();
	}

	public int getY()
	{
		if (channel.isHidden())
		{
			return message.getCanvasLocation().getY();
		}
		return channel.getCanvasLocation().getY();
	}

	public int getX()
	{
		if (channel.isHidden())
		{
			return message.getCanvasLocation().getX();
		}
		return channel.getCanvasLocation().getX();
	}

	public void place(final int y)
	{
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
			int numLines = getTextLineCount(message.getText(), message.getWidth(), messageIndentSpaces);
			int height = numLines * 14; // Height of each line is always 14
			message.setOriginalHeight(height);
			message.revalidate();

			clickBox.setOriginalHeight(height);
			clickBox.revalidate();
		}
	}

	public void calculateChannelIndent(CleanChatChannelsConfig config, String matchedChannelName, String widgetChannelText,
									   int timestampWidth, boolean isFixedWidthTimestampEnabled)
	{
		if (channelType == ChatChannel.FRIENDS_CHAT)
		{
			// For some reason the fc channel width is the entire length of the chatbox so to make things easier we adjust that here
			channel.setOriginalWidth(message.getOriginalX() - channel.getOriginalX());
			channel.revalidate();
		}

		int startOfChannel = widgetChannelText.indexOf(matchedChannelName);
		int endOfChannel = startOfChannel + matchedChannelName.length();

		int indentWidth = 0;

		int channelWidth = 0;
		int prefixWidth = 0;

		// TODO: See if there's something that we are missing when measuring so we can avoid adding all these hardcoded offsets
		if (channelType != null)
		{
			switch (config.indentationMode())
			{
				// Intentionally fallthrough
				case START:
					// extractTimestamp handles indents for start already
					if (isFixedWidthTimestampEnabled)
					{
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
						if (isFixedWidthTimestampEnabled)
						{
							nameWidth = (channel.getWidth() - timestampWidth) - channelWidth;
						}
						else
						{
							nameWidth = channel.getWidth() - prefixWidth - channelWidth;
						}
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

		if (indentWidth > 0)
		{
			messageIndentSpaces += max(0, indentWidth / 3);

			if (messageIndentSpaces > 0)
			{
				message.setOriginalX(message.getOriginalX() - indentWidth);
				message.setOriginalWidth(message.getOriginalWidth() + indentWidth);
				message.revalidate();
			}
		}
	}

	public void applyIndent()
	{
		if (channel.isHidden() && channelIndentSpaces > 0)
		{
			messageIndentSpaces += channelIndentSpaces;
		}

		if (messageIndentSpaces > 0)
		{
			// Using spaces to keep the first line at the initial position (+/-2 pixels)
			message.setText(" ".repeat(messageIndentSpaces) + message.getText());
			message.revalidate();
		}

		if (channelIndentSpaces > 0 && !channel.isHidden())
		{
			channel.setText(" ".repeat(channelIndentSpaces) + channel.getText());
			channel.revalidate();
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
		if (!rank.isHidden())
		{
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

	public void extractTimestamp(FormatterExtractor.ExtractionResult template, int timestampWidth)
	{
		if (template == null)
		{
			timestamp = null;
			return;
		}

		Widget widget;
		Widget oppositeWidget;

		if (!message.getText().isEmpty())
		{
			widget = message;
			oppositeWidget = channel;
		}
		else
		{
			widget = channel;
			oppositeWidget = message;
		}

		timestamp = FormatterExtractor.extractFromText(template, widget.getText());

		if (timestamp == null)
		{
			timestamp = FormatterExtractor.extractFromText(template, oppositeWidget.getText());

			if (timestamp == null)
			{
				log.debug("Timestamp could not be extracted from template: `{}`, widget:`{}`, or opposite widget:`{}`", template, widget.getText(), oppositeWidget.getText());
				return;
			}
			else
			{
				widget = oppositeWidget;
			}
		}

		widget.setText(timestamp.getRemainingText());

		channelIndentSpaces += max(0, timestampWidth / 3);
	}

	@Override
	public String toString()
	{
		return "ChatWidgetGroup{" + ",\n" +
			"channel=" + channel.getText() + ",\n" +
			"rank=" + rank.getSpriteId() + ",\n" +
			"name=" + name.getText() + ",\n" +
			"message=" + message.getText() + ",\n" +
			"channelType=" + (channelType != null ? channel.getName() : null) + ",\n" +
			"timestamp=" + (timestamp != null ? timestamp.getFormattedOutput() : null) + "\n" +
			'}';
	}
}
