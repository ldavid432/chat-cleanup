package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.data.ChatChannel;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.getTextLineCount;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.wrapWithBrackets;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.wrapWithChannelNameRegex;
import com.github.ldavid432.cleanchat.util.FormatterExtractor;
import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import static java.lang.Math.max;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

@Slf4j
@RequiredArgsConstructor
public class ChatWidgetGroup
{
	@NonNull
	@VisibleForTesting
	public final Widget channel;
	@NonNull
	@VisibleForTesting
	public final Widget rank;
	@NonNull
	@VisibleForTesting
	public final Widget name;
	@NonNull
	@VisibleForTesting
	public final Widget message;
	@NonNull
	@VisibleForTesting
	public final Widget clickBox;

	@Setter
	@Nullable
	public  ChatChannel channelType = null;

	public int messageIndent = 0;
	public int channelIndent = 0;

	// Actual widths of text, not necessarily widths of the Widgets since sometimes widgets are combined and/or not really what the parameter is named
	@VisibleForTesting
	public int channelWidth = 0;
	@VisibleForTesting
	public int prefixWidth = 0;
	@VisibleForTesting
	public int nameWidth = 0;
	@VisibleForTesting
	public int rankWidth = 0;

	@Getter
	@Nullable
	public  FormatterExtractor.ExtractionResult timestamp = null;

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
		// Find left-most widget
		return Stream.of(channel, message, name, rank)
			.filter(widget -> !widget.isHidden())
			.reduce((widget1, widget2) -> {
				if (widget1.getOriginalX() < widget2.getOriginalX())
				{
					return widget1;
				}
				else
				{
					return widget2;
				}
			}).orElseGet(() -> {
				if (channel.isHidden())
				{
					return message;
				}
				else
				{
					return channel;
				}
			}).getCanvasLocation().getX();
	}

	public Color getColor(CleanChatChannelsConfig config)
	{
		return channelType != null ? channelType.getColor(config) : config.noChannelColor();
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

	public void calculateHeight(Client client)
	{
		if (!message.getText().isEmpty() && message.getWidth() > 0)
		{
			int numLines = getTextLineCount(message.getText(), message.getWidth(), messageIndent / 3, client);
			int height = numLines * 14; // Height of each line is always 14
			message.setOriginalHeight(height);
			message.revalidate();

			clickBox.setOriginalHeight(height);
			clickBox.revalidate();
		}

		if ( channelType == null) return;

		if (channelType == ChatChannel.CLAN || channelType == ChatChannel.GUEST_CLAN)
		{
			log.debug("c -> r = {},", rank.getOriginalX() - (channel.getOriginalX() + channel.getWidth()));
			log.debug("r -> n = {},", name.getOriginalX() - (rank.getOriginalX() + rank.getWidth()));
			log.debug("n -> m = {},", message.getOriginalX() - (name.getOriginalX() + name.getWidth()));
		}
		else if (channelType == ChatChannel.FRIENDS_CHAT)
		{

		}
		else if (channelType == ChatChannel.GROUP_IRON)
		{
			log.debug("c -> n = {},", name.getOriginalX() - (channel.getOriginalX() + channel.getWidth()));
			log.debug("n -> m = {},", message.getOriginalX() - (name.getOriginalX() + name.getWidth()));
		}
	}

	public void calculateChannelIndent(CleanChatChannelsConfig config, String matchedChannelName, String widgetChannelText,
	                                   Client client)
	{
		if (channelType == ChatChannel.FRIENDS_CHAT)
		{
			// For some reason the fc channel width is the entire length of the chatbox so to make things easier we adjust that here
			shrink(channel, message.getOriginalX() - channel.getOriginalX());
		}

		int startOfChannel = widgetChannelText.indexOf(matchedChannelName);
		int endOfChannel = startOfChannel + matchedChannelName.length();

		int indentWidth = 0;

		if (channelType != null)
		{
			String prefix = widgetChannelText.substring(0, startOfChannel);
			prefixWidth = getTextLength(prefix, client);

			String channelText = widgetChannelText.substring(startOfChannel, endOfChannel);
			channelWidth = getTextLength(channelText, client);

			switch (config.indentationMode())
			{
				// Intentionally fallthrough
				case START:
					indentWidth += prefixWidth;
				case CHANNEL:
					if (!channelType.isChannelNameRemovalEnabled(config))
					{
						indentWidth += channelWidth;
					}
					else
					{
						indentWidth -= getTextLength(" ", client);
					}
				case NAME:
					// FC puts name + channel into the channel widget
					if (channelType == ChatChannel.FRIENDS_CHAT)
					{
						String nameText = widgetChannelText.substring(endOfChannel);
						nameWidth = getTextLength(nameText, client);
					}
					else if (!name.getText().isEmpty() && !name.isHidden())
					{
						nameWidth = name.getWidth();
					}

					if (!rank.isHidden())
					{
						rankWidth = rank.getWidth() + 3;
					}
					indentWidth += rankWidth;
					indentWidth += nameWidth;

					if (indentWidth > 0) //  && channelType != ChatChannel.FRIENDS_CHAT ?
					{
						indentWidth += 3;
					}
				case MESSAGE:
					// Already set by default
			}
		}

		if (indentWidth > 0)
		{
			messageIndent += indentWidth;

			// TODO: Should this be exact instead of rounded (/ 3 * 3)
			// TODO: Should this move to applyIndent?
			if (messageIndent > 0)
			{
				shiftLeft(message, indentWidth / 3 * 3);
				expand(message, indentWidth);
			}
		}
	}

	public void applyIndent()
	{
		if (channel.isHidden() && channelIndent > 0)
		{
			messageIndent += channelIndent;
		}

		if (messageIndent > 0)
		{
			// Using spaces to keep the first line at the initial position (+/-2 pixels)
			message.setText(" ".repeat(messageIndent / 3) + message.getText());
			message.revalidate();
		}

		if (channelIndent > 0 && !channel.isHidden())
		{
			channel.setText(" ".repeat(Math.max(1, channelIndent / 3)) + channel.getText());
			expand(channel, channelIndent);
		}
	}

	public void removeFromChannel(String text, Client client)
	{
		replaceChannelName(text, "", client);
	}

	public String replaceChannelName(String text, String newChannelName, Client client)
	{
		int currentWidth = getTextLength(wrapWithBrackets(text), client);
		int newWidth = getTextLength(newChannelName, client);
		int removedWidth = currentWidth - newWidth;

		String newText = channel.getText().replaceFirst(wrapWithChannelNameRegex(text), newChannelName);

		// Remove trailing spaces - probably only happens with timestamps turned on
		if (newText.endsWith(" "))
		{
			newText = newText.substring(0, newText.length() - 1);
			removedWidth += getTextLength(" ", client);
		}

		// Remove double spaces - mainly found in friends chat since it has sender + username
		if (newText.contains("  "))
		{
			newText = newText.replaceFirst(" {2}", " ");
			removedWidth += getTextLength(" ", client);
		}

		channel.setText(newText);

		// Shift widgets X left if channel was removed
		shiftLeft(rank, removedWidth);
		shiftLeft(name, removedWidth);
		shiftLeft(message, removedWidth);

		// Expand the width of messages if channel was removed
		expand(message, removedWidth);

		// Reduce channel width if it was removed
		shrink(channel, removedWidth);

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

	private void shiftRight(Widget widget, int width)
	{
		widget.setOriginalX(widget.getOriginalX() + width);
		widget.revalidate();
	}

	private void expand(Widget widget, int width)
	{
		widget.setOriginalWidth(widget.getOriginalWidth() + width);
		widget.revalidate();
	}

	private void shrink(Widget widget, int width)
	{
		widget.setOriginalWidth(widget.getOriginalWidth() - width);
		widget.revalidate();
	}

	private void place(Widget widget, int y)
	{
		widget.setOriginalY(y);
		widget.revalidate();
	}

	public void extractTimestamp(FormatterExtractor.ExtractionResult template, int timestampWidth, Client client)
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

		int originalTimestampWidth = getTextLength(widget.getText(), client) - getTextLength(timestamp.getRemainingText(), client);
		int timestampWidthAdded = max(0, timestampWidth - originalTimestampWidth);
		widget.setText(timestamp.getRemainingText());

		channelIndent += max(0, timestampWidth);

		shiftRight(rank, timestampWidthAdded);
		shiftRight(name, timestampWidthAdded);
		if (channelType != null)
		{
			messageIndent += timestampWidthAdded;
		}
		else
		{
			if (!name.isHidden() || !channel.isHidden())
			{
				shiftRight(message, timestampWidthAdded);
				shrink(message, timestampWidthAdded);
			}
			else
			{
				// Messages with just one widget don't need anything shifted, just message indented
				messageIndent += timestampWidthAdded;
			}
		}
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
