package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.data.ChatChannel;
import com.github.ldavid432.cleanchat.data.GameModeIcon;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.getTextLineCount;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.imgTagRegex;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.wrapWithBrackets;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.wrapWithChannelNameRegex;
import com.github.ldavid432.cleanchat.util.FormatterExtractor;
import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatWidgetGroup
{
	@Nullable
	@VisibleForTesting
	public final Widget channel;
	@Nullable
	@VisibleForTesting
	public Widget rank;
	@Nullable
	@VisibleForTesting
	public final Widget name;
	@NonNull
	@VisibleForTesting
	public final Widget message;
	@NonNull
	@VisibleForTesting
	public final Widget clickBox;
	@NonNull
	@VisibleForTesting
	public final List<Widget> unused;

	@Nullable
	@Getter
	private final ChatChannel.ChannelMatch channelMatch;

	@Getter(onMethod_ = @VisibleForTesting)
	@Nullable
	private final ChatChannel channelType;

	// X of group at creation within game canvas
	@Getter
	private int x;
	// X of group at creation within parent
	private int widgetX;

	public int messageIndent = 0;
	public int channelIndent = 0;
	public int nameIndent = 0;

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
	public FormatterExtractor.ExtractionResult timestamp = null;

	public ChatWidgetGroup(@Nullable Widget channel, @Nullable Widget rank, @Nullable Widget name,
	                        @NonNull Widget message, @NonNull Widget clickBox, @NonNull List<Widget> unused, @Nullable ChatChannel.ChannelMatch channelMatch)
	{
		this(channel, name, message, clickBox, unused, channelMatch, channelMatch != null ? channelMatch.getChannel() : null);
		this.rank = rank;
		Widget leftmost = Stream.of(channel, message, name, rank)
			.filter(widget -> widget != null && !widget.isHidden())
			.reduce((widget1, widget2) -> {
				if (widget1.getOriginalX() < widget2.getOriginalX())
				{
					return widget1;
				}
				else
				{
					return widget2;
				}
			})
			.orElseGet(() -> Objects.requireNonNullElse(channel, message));

		x = leftmost.getCanvasLocation().getX();
		widgetX = leftmost.getOriginalX();
	}

	private void onNonNullWidgets(Consumer<Widget> action, Widget... widgets)
	{
		Stream.of(widgets)
			.filter(Objects::nonNull)
			.forEach(action);
	}

	public int getHeight()
	{
		return message.getHeight();
	}

	public int getY()
	{
		return Objects.requireNonNullElse(channel, message).getCanvasLocation().getY();
	}

	public Color getColor(CleanChatChannelsConfig config)
	{
		return channelType != null ? channelType.getColor(config) : config.noChannelColor();
	}

	public void place(final int y)
	{
		// TODO: Do we really need to place unused?
		unused.forEach(widget -> place(widget, y));
		onNonNullWidgets(widget -> place(widget, y), channel, rank, name, message);

		clickBox.setOriginalY(y);
		clickBox.setHidden(false);
		clickBox.revalidate();
	}

	public void calculateHeight(Client client, int lineHeight)
	{
		if (!message.getText().isEmpty() && message.getWidth() > 0)
		{
			int numLines = getTextLineCount(message.getText(), message.getWidth(), messageIndent / 3, client);
			int height = numLines * lineHeight;
			message.setOriginalHeight(height);
			message.revalidate();

			clickBox.setOriginalHeight(height);
			clickBox.revalidate();
		}
	}

	public void calculateWidgetSizes(Client client)
	{
		if (name != null)
		{
			nameWidth = getTextLength(name.getText(), client) + 3;
		}
	}

	public void calculateChannelWidgetSizes(String matchedChannelName, String widgetChannelText,
	                                        Client client)
	{
		if (channelType == ChatChannel.FRIENDS_CHAT && channel != null)
		{
			// For some reason the fc channel width is the entire length of the chatbox so to make things easier we adjust that here
			shrink(channel, message.getOriginalX() - channel.getOriginalX());
		}

		int startOfChannel = widgetChannelText.indexOf(matchedChannelName);
		int endOfChannel = startOfChannel + matchedChannelName.length();

		String prefix = widgetChannelText.substring(0, startOfChannel);
		prefixWidth = getTextLength(prefix, client);

		String channelText = widgetChannelText.substring(startOfChannel, endOfChannel);
		channelWidth = getTextLength(channelText, client);

		// FC puts name + channel into the channel widget
		if (channelType == ChatChannel.FRIENDS_CHAT)
		{
			String nameText = widgetChannelText.substring(endOfChannel);
			nameWidth = getTextLength(nameText, client);
		}
		else if (name != null)
		{
			nameWidth = name.getWidth() + 3;
		}
		if (rank != null)
		{
			rankWidth = rank.getWidth();
		}
	}

	public void calculateMessageIndent(CleanChatChannelsConfig config)
	{
		int indentWidth = 0;
		switch (config.indentationMode())
		{
			// Intentionally fallthrough
			case START:
				indentWidth += prefixWidth;
			case CHANNEL:
				if (channelType != null && !channelType.isChannelNameRemovalEnabled(config))
				{
					indentWidth += channelWidth;
				}
			case NAME:
				indentWidth += rankWidth;
				indentWidth += nameWidth;
			case MESSAGE:
				// Already set by default
		}

		if (indentWidth > 0)
		{
			messageIndent += indentWidth;

			// TODO: Should this be combined with applyIndent?
			if (messageIndent > 0)
			{
				shiftLeft(message, indentWidth);
				expand(message, indentWidth);
			}
		}
	}

	public void applyIndent()
	{
		if (messageIndent > 0)
		{
			indent(message, messageIndent);
		}

		if (channelIndent > 0 && channel != null)
		{
			indent(channel, channelIndent);
		}

		if (nameIndent > 0 && name != null)
		{
			indent(name, nameIndent);
		}
	}

	public void removeFromChannel(String text, Client client, @NonNull Widget channel)
	{
		replaceChannelName(text, "", client, channel);
	}

	public String replaceChannelName(String text, String newChannelName, Client client, @NonNull Widget channel)
	{
		return replaceText(wrapWithBrackets(text), newChannelName, wrapWithChannelNameRegex(text), client, channel,
			rank, name, message);
	}

	public String replaceText(String originalText, String replacementText, String originalTextRegex, Client client,
							  @NonNull Widget widget, Widget... widgetsToShift)
	{
		int currentWidth = getTextLength(originalText, client);
		int newWidth = getTextLength(replacementText, client);
		int removedWidth = currentWidth - newWidth;

		String originalWidgetText = widget.getText();
		String newText = widget.getText().replaceFirst(originalTextRegex, replacementText);
		if (newText.equals(originalWidgetText))
		{
			return originalWidgetText;
		}

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

		widget.setText(newText);

		// Shift widgets X left if text was removed
		for (Widget widget1 : widgetsToShift)
		{
			shiftLeft(widget1, removedWidth);
		}

		// Expand the width of messages if text was removed
		expand(message, removedWidth);

		// Reduce widget width if text was removed
		shrink(widget, removedWidth);

		return newText;
	}

	public void removeRank()
	{
		if (rank != null)
		{
			rank.setHidden(true);

			int removedWidth = rank.getWidth();

			if (name != null)
			{
				shiftLeft(name, removedWidth);
			}
			shiftLeft(message, removedWidth);

			// Expand the width of messages if rank was removed
			expand(message, removedWidth);
			rank = null;
		}
	}

	private void shiftLeft(Widget widget, int width)
	{
		if (widget == null) return;
		// TODO: It would be nice to actually make the shifting perform the correct amount instead of
		//  resorting to a hard cap. Currently this only seems to be an issue for guest clan chat
		widget.setOriginalX(Math.max(widget.getOriginalX() - width, widgetX));
		widget.revalidate();
	}

	private void shiftRight(Widget widget, int width)
	{
		if (widget == null) return;
		widget.setOriginalX(widget.getOriginalX() + width);
		widget.revalidate();
	}

	private void expand(Widget widget, int width)
	{
		if (widget == null) return;
		widget.setOriginalWidth(widget.getOriginalWidth() + width);
		widget.revalidate();
	}

	private void shrink(Widget widget, int width)
	{
		if (widget == null) return;
		widget.setOriginalWidth(widget.getOriginalWidth() - width);
		widget.revalidate();
	}

	private void indent(Widget widget, int width)
	{
		// Using spaces to keep the first line at the initial position (+/-2 pixels)
		widget.setText(" ".repeat((int) Math.ceil(width / 3d)) + widget.getText());
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

		// TODO: Surely this can be abstracted
		if (channel != null)
		{
			timestamp = FormatterExtractor.extractFromText(template, channel.getText());
			if (timestamp != null)
			{
				channelIndent += max(0, timestampWidth);
				processExtractedTimestamp(
					timestamp,
					channel,
					client,
					timestampWidth,
					rank,
					name
				);
			}
		}

		if (name != null && timestamp == null)
		{
			timestamp = FormatterExtractor.extractFromText(template, name.getText());
			if (timestamp != null)
			{
				nameIndent += max(0, timestampWidth);
				processExtractedTimestamp(
					timestamp,
					name,
					client,
					timestampWidth
				);
			}
		}

		if (timestamp == null)
		{
			timestamp = FormatterExtractor.extractFromText(template, message.getText());
			if (timestamp != null)
			{
				messageIndent += max(0, timestampWidth);
				processExtractedTimestamp(
					timestamp,
					message,
					client,
					timestampWidth
				);
			}
		}
	}

	private void processExtractedTimestamp(FormatterExtractor.ExtractionResult timestamp, Widget widget, Client client,
		int timestampWidth, Widget... widgetsToShift)
	{
		int originalTimestampWidth = getTextLength(widget.getText(), client) - getTextLength(timestamp.getRemainingText(), client);
		int timestampWidthAdded = max(0, timestampWidth - originalTimestampWidth);
		widget.setText(timestamp.getRemainingText());

		messageIndent += timestampWidthAdded;

		for (Widget widgetToShift : widgetsToShift)
		{
			shiftRight(widgetToShift, timestampWidthAdded);
		}
	}

	public void hideGameModeIcons(Set<GameModeIcon> hiddenIcons, Client client)
	{
		if (name != null || (channelType == ChatChannel.FRIENDS_CHAT && channel != null))
		{
			String icons = hiddenIcons.stream().flatMap(i -> i.getIconIds().stream().map(Object::toString)).collect(Collectors.joining("|"));
			Widget widget = channelType == ChatChannel.FRIENDS_CHAT && channel != null ? channel : name;
			Matcher matcher = Pattern.compile(".*" + imgTagRegex(icons) + ".*").matcher(widget.getText());
			if (matcher.matches() && matcher.groupCount() >= 1)
			{
				replaceText(
					"<img=" + matcher.group(1) + ">",
					"",
					imgTagRegex(icons),
					client,
					widget,
					message
				);
			}
		}
	}

	@Override
	public String toString()
	{
		return "ChatWidgetGroup{\n" +
			(channel != null ? "channel=" + channel.getText() + "\n" : "") +
			(rank != null ? "rank=" + rank.getSpriteId() + "\n" : "") +
			(name != null ? "name=" + name.getText() + "\n" : "") +
			"message=" + message.getText() + "\n" +
			"channelType=" + (channelType != null ? channelType.name() : null) + ",\n" +
			"channelWidth=" + channelWidth + "\n" +
			"prefixWidth=" + prefixWidth + "\n" +
			"nameWidth=" + nameWidth + "\n" +
			"rankWidth=" + rankWidth + "\n" +
			"timestamp=" + (timestamp != null ? timestamp.getFormattedOutput() : null) + "\n" +
			'}';
	}

	/* TODO: clean up this comment
	If 2 is present match channel against it - > message is at 1, name is at 0 if present, 3 is rank if present
	else
		if 1 is hidden then 0 is message, everything else is empty
		else
			match channel against 0, 1 is message,
				if match channel is 0
				else name is 0

			fallback message is 0
	 */
	public static ChatWidgetGroup of(Widget widget0, Widget widget1, Widget widget2, Widget widget3, Widget clickBox,
									 @Nullable ChannelNameManager channelNameManager)
	{
		Widget rank = null;
		Widget channel = null;
		Widget name = null;
		Widget message;
		ChatChannel.ChannelMatch match = null;
		List<Widget> unused = new ArrayList<>();

		if (!widget2.isHidden())
		{
			channel = widget2;
			message = widget1;

			match = ChatChannel.findChannelMatch(widget2.getText(), channelNameManager);
			if (match == null)
			{
				// TODO: Do we need to error here?
			}

			if (!widget3.isHidden())
			{
				rank = widget3;
			}
			else
			{
				unused.add(widget3);
			}
			if (!widget0.isHidden())
			{
				name = widget0;
			}
			else
			{
				unused.add(widget0);
			}
		}
		else
		{
			unused.add(widget2);
			unused.add(widget3);

			if (widget1.isHidden())
			{
				message = widget0;
				unused.add(widget1);
			}
			else
			{
				message = widget1;

				match = ChatChannel.findChannelMatch(widget0.getText(), channelNameManager);
				if (match != null)
				{
					channel = widget0;
				}
				else
				{
					name = widget0;
				}
			}
		}

		return new ChatWidgetGroup(
			channel,
			rank,
			name,
			message,
			clickBox,
			unused,
			match
		);
	}

}
