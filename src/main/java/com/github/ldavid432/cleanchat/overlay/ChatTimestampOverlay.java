package com.github.ldavid432.cleanchat.overlay;

import com.github.ldavid432.cleanchat.ChatWidgetGroup;
import com.github.ldavid432.cleanchat.TimestampPluginIntegration;
import static com.github.ldavid432.cleanchat.TimestampPluginIntegration.TIMESTAMP_FORMAT_KEY;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.getTextLength;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.isChatboxTransparent;
import com.github.ldavid432.cleanchat.util.FormatterExtractor;
import com.github.ldavid432.cleanchat.util.SimpleDateFormatUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.timestamp.TimestampConfig;
import net.runelite.client.ui.FontManager;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Singleton
public class ChatTimestampOverlay extends BaseCleanChatOverlay
{

	@Inject
	private Client client;

	@Inject
	private TimestampPluginIntegration timestampIntegration;

	@Override
	protected boolean isEnabled()
	{
		return timestampIntegration.isFixedWidthTimestampEnabled();
	}

	private Color splitChatColor = null;

	@Override
	protected void render(Graphics2D graphics, int x, int y, ChatWidgetGroup group, boolean isPrivateSplitChat)
	{
		FormatterExtractor.ExtractionResult timestamp = group.getTimestamp();

		if (timestamp == null)
		{
			return;
		}

		int timestampY = y + 14;

		Color timestampColor = getTimestampColour(isPrivateSplitChat);
		graphics.setColor(timestampColor);

		graphics.setFont(FontManager.getRunescapeFont());

		AtomicInteger timestampX = new AtomicInteger(x);

		boolean isChatboxTransparent = isChatboxTransparent(client);

		FormatterExtractor.iterateOutputParts(timestamp, new FormatterExtractor.OutputPartConsumer()
		{
			@Override
			public void consumeSegment(FormatterExtractor.FormatSegment segment)
			{
				String paddedValue = StringUtils.leftPad(segment.value, segment.getEndIndex() - segment.getStartIndex(), "0");

				for (int i = 0; i < paddedValue.length(); i++)
				{
					String str = String.valueOf(paddedValue.charAt(i));

					if (isChatboxTransparent)
					{
						// Draw shadow
						graphics.setColor(Color.BLACK);
						graphics.drawString(str, timestampX.get() + 1, timestampY + 1);
						graphics.setColor(timestampColor);
					}
					graphics.drawString(str, timestampX.get(), timestampY);
					// Largest numbers are 6 pixels + 2 character spacing
					//  Currently does not account for letter size (Monday, January etc.)
					timestampX.addAndGet(6 + 2);
				}
			}

			@Override
			public void consumeText(String text, int startIndex, int endIndex)
			{
				if (isChatboxTransparent)
				{
					// Draw shadow
					graphics.setColor(Color.BLACK);
					graphics.drawString(text, timestampX.get() + 1, timestampY + 1);
					graphics.setColor(timestampColor);
				}
				graphics.drawString(text, timestampX.get(), timestampY);
				timestampX.addAndGet(graphics.getFontMetrics().stringWidth(text));
			}
		});
	}

	@Subscribe(priority = -0.1f) // run after TimestampPluginIntegration
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(TimestampConfig.GROUP) && event.getKey().equals(TIMESTAMP_FORMAT_KEY))
		{
			updateTemplate();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() == VarPlayerID.OPTION_CHAT_COLOUR_PRIVATE_SPLIT)
		{
			splitChatColor = new Color(client.getVarpValue(VarPlayerID.OPTION_CHAT_COLOUR_PRIVATE_SPLIT) - 1);
		}
	}

	public void startUp()
	{
		updateTemplate();
	}

	private void updateTemplate()
	{
		FormatterExtractor.ExtractionResult newTemplate =
			FormatterExtractor.createFromFormatString(timestampIntegration.getTimestampFormat());

		timestampIntegration.setTimestampTemplateWidth(0);
		timestampIntegration.setTimestampTemplate(newTemplate);

		if (newTemplate != null)
		{
			String format = timestampIntegration.getTimestampFormat();

			int i = 0;
			while (i < format.length())
			{
				char c = format.charAt(i);

				if (c == '\'')
				{
					i++;
					StringBuilder literal = new StringBuilder();
					while (i < format.length() && format.charAt(i) != '\'')
					{
						literal.append(format.charAt(i));
						i++;
					}
					if (i < format.length())
					{
						i++; // skip closing quote
					}
					timestampIntegration.setTimestampTemplateWidth(
						timestampIntegration.getTimestampTemplateWidth() + getTextLength(literal.toString(), client)
					);
					continue;
				}

				if (SimpleDateFormatUtil.isFormatChar(c))
				{
					int count = 1;
					while (i + count < format.length() && format.charAt(i + count) == c)
					{
						count++;
					}

					int displayWidth = SimpleDateFormatUtil.getExpandedSize(c, count);
					timestampIntegration.setTimestampTemplateWidth(
						timestampIntegration.getTimestampTemplateWidth() + ((6 + 2) * displayWidth)
					);

					i += count;
					continue;
				}

				// actual literal char, not a placeholder
				timestampIntegration.setTimestampTemplateWidth(
					timestampIntegration.getTimestampTemplateWidth() + getTextLength(String.valueOf(c), client)
				);
				i++;
			}
		}

		client.refreshChat();
	}

	private Color getTimestampColour(boolean isPrivateSplitChat)
	{
		if (isPrivateSplitChat)
		{
			if (splitChatColor == null)
			{
				splitChatColor = new Color(client.getVarpValue(VarPlayerID.OPTION_CHAT_COLOUR_PRIVATE_SPLIT) - 1);
			}
			return splitChatColor;
		}

		boolean isChatboxTransparent = client.isResized() && client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY) == 1;

		Color color = isChatboxTransparent ? timestampIntegration.getTransparentTimestampColor() : timestampIntegration.getOpaqueTimestampColor();

		if (color == null)
		{
			color = isChatboxTransparent ? Color.WHITE : Color.BLACK;
		}

		return color;
	}

}
