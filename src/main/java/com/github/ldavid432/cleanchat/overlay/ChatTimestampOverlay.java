package com.github.ldavid432.cleanchat.overlay;

import com.github.ldavid432.cleanchat.ChatWidgetGroup;
import com.github.ldavid432.cleanchat.TimestampPluginIntegration;
import static com.github.ldavid432.cleanchat.TimestampPluginIntegration.TIMESTAMP_FORMAT_KEY;
import static com.github.ldavid432.cleanchat.util.CleanChatUtil.getTextLength;
import com.github.ldavid432.cleanchat.util.FormatterExtractor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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
	private Client client;

	@Inject
	private TimestampPluginIntegration timestampIntegration;

	@Override
	boolean isEnabled()
	{
		return timestampIntegration.isFixedWidthTimestampEnabled();
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

		Color timestampColor = getTimestampColour();
		graphics.setColor(timestampColor);

		graphics.setFont(FontManager.getRunescapeFont());

		AtomicInteger timestampX = new AtomicInteger(x);

		boolean isChatboxTransparent = client.isResized() && client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY) == 1;

		FormatterExtractor.iterateOutputParts(timestamp, new FormatterExtractor.OutputPartConsumer()
		{
			@Override
			public void consumeSegment(FormatterExtractor.FormatSegment segment)
			{
				for (int i = 0; i < segment.value.length(); i++)
				{
					String str = String.valueOf(segment.value.charAt(i));

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

	public void startUp()
	{
		updateTemplate();
	}

	private void updateTemplate()
	{
		FormatterExtractor.ExtractionResult newTemplate = FormatterExtractor.createFromFormatString(timestampIntegration.getTimestampFormat());

		timestampIntegration.setTimestampTemplateWidth(0);
		timestampIntegration.setTimestampTemplate(newTemplate);

		if (newTemplate != null) {
			FormatterExtractor.iterateOutputParts(newTemplate, new FormatterExtractor.OutputPartConsumer()
			{
				@Override
				public void consumeSegment(FormatterExtractor.FormatSegment segment)
				{
					timestampIntegration.setTimestampTemplateWidth(timestampIntegration.getTimestampTemplateWidth() + ((6 + 2) * segment.tokenCount));
				}

				@Override
				public void consumeText(String text, int startIndex, int endIndex)
				{
					timestampIntegration.setTimestampTemplateWidth(timestampIntegration.getTimestampTemplateWidth() + getTextLength(text, client));
				}
			});
		}

		client.refreshChat();
	}

	private Color getTimestampColour()
	{
		boolean isChatboxTransparent = client.isResized() && client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY) == 1;

		Color color = isChatboxTransparent ? timestampIntegration.getTransparentTimestampColor() : timestampIntegration.getOpaqueTimestampColor();

		if (color == null)
		{
			color = isChatboxTransparent ? Color.WHITE : Color.BLACK;
		}

		return color;
	}

}
