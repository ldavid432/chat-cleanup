package com.github.ldavid432.cleanchat.overlay;

import com.github.ldavid432.cleanchat.ChatWidgetGroup;
import com.github.ldavid432.cleanchat.CleanChatChannelsPlugin;
import static com.github.ldavid432.cleanchat.CleanChatUtil.getTextLength;
import com.github.ldavid432.cleanchat.util.FormatterExtractor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.timestamp.TimestampConfig;
import net.runelite.client.ui.FontManager;

@Slf4j
@Singleton
public class ChatTimestampOverlay extends BaseCleanChatOverlay
{

	@Inject
	private TimestampConfig timestampConfig;

	@Inject
	private CleanChatChannelsPlugin plugin;

	@Override
	boolean isEnabled()
	{
		return plugin.isFixedWidthTimestampEnabled();
	}

	@Override
	void render(Graphics2D graphics, int x, int y, ChatWidgetGroup group)
	{
		FormatterExtractor.ExtractionResult timestamp = group.getTimestamp();

		if (timestamp == null)
		{
			return;
		}

		int timestampY = y + 14;

		graphics.setColor(getTimestampColour());

		graphics.setFont(FontManager.getRunescapeFont());

		AtomicInteger timestampX = new AtomicInteger(x);

		FormatterExtractor.iterateOutputParts(timestamp, new FormatterExtractor.OutputPartConsumer()
		{
			@Override
			public void consumeSegment(FormatterExtractor.FormatSegment segment)
			{
				for (int i = 0; i < segment.value.length(); i++)
				{
					graphics.drawString(String.valueOf(segment.value.charAt(i)), timestampX.get(), timestampY);
					// Largest numbers are 6 pixels + 2 character spacing
					//  Currently does not account for letter size (Monday, January etc.)
					timestampX.addAndGet(6 + 2);

				}
			}

			@Override
			public void consumeText(String text, int startIndex, int endIndex)
			{
				graphics.drawString(text, timestampX.get(), timestampY);
				timestampX.addAndGet(graphics.getFontMetrics().stringWidth(text));
			}
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(TimestampConfig.GROUP) && event.getKey().equals("format"))
		{
			updateTemplate();
		}
	}

	public void startUp()
	{
		updateTemplate();
	}

	private void updateTemplate()
	{
		plugin.setTimestampTemplateWidth(0);
		plugin.setTimestampTemplate(FormatterExtractor.createFromFormatString(timestampConfig.timestampFormat()));

		FormatterExtractor.iterateOutputParts(plugin.getTimestampTemplate(), new FormatterExtractor.OutputPartConsumer()
		{
			@Override
			public void consumeSegment(FormatterExtractor.FormatSegment segment)
			{
				plugin.setTimestampTemplateWidth(plugin.getTimestampTemplateWidth() + ((6 + 2) * segment.tokenCount));
			}

			@Override
			public void consumeText(String text, int startIndex, int endIndex)
			{
				plugin.setTimestampTemplateWidth(plugin.getTimestampTemplateWidth() + getTextLength(text));
			}
		});

		client.refreshChat();
	}

	private Color getTimestampColour()
	{
		boolean isChatboxTransparent = client.isResized() && client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY) == 1;

		Color color = isChatboxTransparent ? timestampConfig.transparentTimestamp() : timestampConfig.opaqueTimestamp();

		if (color == null)
		{
			color = isChatboxTransparent ? Color.WHITE : Color.BLACK;
		}

		return color;
	}

}
