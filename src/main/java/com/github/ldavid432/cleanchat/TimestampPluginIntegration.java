package com.github.ldavid432.cleanchat;

import com.github.ldavid432.cleanchat.util.FormatterExtractor;
import java.awt.Color;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.timestamp.TimestampConfig;
import net.runelite.client.util.ColorUtil;

@Slf4j
public class TimestampPluginIntegration
{
	private static final String RUNELITE_CONFIG_GROUP = "runelite";
	private static final String TIMESTAMP_PLUGIN_KEY = "timestampplugin";

	public static final String TIMESTAMP_FORMAT_KEY = "format";
	private static final String OPAQUE_TIMESTAMP_COLOR_KEY = "opaqueTimestamp";
	private static final String TRANSPARENT_TIMESTAMP_COLOR_KEY = "transparentTimestamp";

	@Inject
	private ConfigManager configManager;

	@Inject
	private Client client;

	@Inject
	private CleanChatChannelsConfig config;

	@Getter
	private boolean isEnabled;

	@Getter
	@Nullable
	private Color opaqueTimestampColor;

	@Getter
	@Nullable
	private Color transparentTimestampColor;

	@Getter
	private String timestampFormat;

	@Getter
	@Setter
	private int timestampTemplateWidth = 0;

	@Getter
	@Setter
	@Nullable
	private FormatterExtractor.ExtractionResult timestampTemplate = null;

	public void onStartup()
	{
		isEnabled = configManager.getConfiguration(RUNELITE_CONFIG_GROUP, TIMESTAMP_PLUGIN_KEY, Boolean.class) == Boolean.TRUE;
		opaqueTimestampColor = configManager.getConfiguration(TimestampConfig.GROUP, OPAQUE_TIMESTAMP_COLOR_KEY, Color.class);
		transparentTimestampColor = configManager.getConfiguration(TimestampConfig.GROUP, TRANSPARENT_TIMESTAMP_COLOR_KEY, Color.class);
		timestampFormat = configManager.getConfiguration(TimestampConfig.GROUP, TIMESTAMP_FORMAT_KEY);
	}

	public boolean isFixedWidthTimestampEnabled()
	{
		return config.isFixedWidthTimestampEnabled() && isEnabled();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (TimestampConfig.GROUP.equals(event.getGroup()))
		{
			switch (event.getKey())
			{
				case TIMESTAMP_FORMAT_KEY:
					timestampFormat = event.getNewValue();
					log.debug("Timestamp format changed. Refreshing chat.");
					client.refreshChat();
					break;
				case OPAQUE_TIMESTAMP_COLOR_KEY:
					opaqueTimestampColor = ColorUtil.fromString(event.getNewValue());
					break;
				case TRANSPARENT_TIMESTAMP_COLOR_KEY:
					transparentTimestampColor = ColorUtil.fromString(event.getNewValue());
					break;
			}
		}
		else if (RUNELITE_CONFIG_GROUP.equals(event.getGroup()) && TIMESTAMP_PLUGIN_KEY.equals(event.getKey()))
		{
			isEnabled = Boolean.parseBoolean(event.getNewValue());
			log.debug("Timestamp plugin toggled. Refreshing chat.");
			client.refreshChat();
		}
	}
}
