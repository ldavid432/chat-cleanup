package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.ChannelNameManager;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import static com.github.ldavid432.cleanchat.CleanChatChannelsConfig.DEFAULT_CUSTOM_CHANNEL_NAME;
import com.github.ldavid432.cleanchat.CleanChatUtil;
import static com.github.ldavid432.cleanchat.CleanChatUtil.CURRENT_CLAN_REPLACER;
import static com.github.ldavid432.cleanchat.CleanChatUtil.sanitizeName;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@AllArgsConstructor
public enum ChannelNameRemoval
{
	// TODO: Cache booleans & indent mode config values
	CLAN(
		CleanChatChannelsConfig::removeClanName,
		ChannelNameManager::getClanNames,
		(c, t) -> false,
		ChannelNameManager::getShortClanName,
		CleanChatChannelsConfig::removeClanRank
	),
	GUEST_CLAN(
		CleanChatChannelsConfig::removeGuestClanName,
		ChannelNameManager::getGuestClanNames,
		(c, t) -> false,
		ChannelNameManager::getShortGuestClanName,
		c -> false
	),
	FRIENDS_CHAT(
		CleanChatChannelsConfig::removeFriendsChatName,
		ChannelNameManager::getFriendsChatNames,
		(c, t) -> false,
		ChannelNameManager::getShortFriendsChatName,
		c -> false
	),
	GROUP_IRON(
		CleanChatChannelsConfig::removeGroupIronName,
		ChannelNameManager::getGroupIronNames,
		(config, tab) -> config.removeGroupIronFromClan() && tab == ChatTab.CLAN,
		ChannelNameManager::getShortGroupIronName,
		c -> false
	);

	public List<String> getNames(ChannelNameManager channelNameManager)
	{
		return getNames.apply(channelNameManager);
	}

	public boolean isEnabled(CleanChatChannelsConfig config)
	{
		return isEnabled.apply(config);
	}

	public boolean isTabBlocked(CleanChatChannelsConfig config, ChatTab tab)
	{
		return isTabBlocked.apply(config, tab);
	}

	public boolean isShortNameDefault(ChannelNameManager channelNameManager)
	{
		// Compares *un-substituted* shortName
		return Objects.equals(getShortName.apply(channelNameManager), DEFAULT_CUSTOM_CHANNEL_NAME);
	}

	public String getShortName(ChannelNameManager channelNameManager, String matchedName)
	{
		final String shortName = getShortName.apply(channelNameManager);

		if (shortName.contains(CURRENT_CLAN_REPLACER))
		{
			return shortName.replace(CURRENT_CLAN_REPLACER, matchedName);
		}
		else
		{
			return shortName;
		}
	}

	public boolean isRemoveRankEnabled(CleanChatChannelsConfig config)
	{
		return isRemoveRank.apply(config);
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final Function<ChannelNameManager, List<String>> getNames;
	private final BiFunction<CleanChatChannelsConfig, ChatTab, Boolean> isTabBlocked;
	private final Function<ChannelNameManager, String> getShortName;
	private final Function<CleanChatChannelsConfig, Boolean> isRemoveRank;

	public static Pair<ChannelNameRemoval, String> findChannelMatch(String channel, ChannelNameManager channelNameManager)
	{
		for (ChannelNameRemoval channelRemoval : ChannelNameRemoval.values())
		{
			if (channel == null)
			{
				break;
			}
			String widgetChannelName = sanitizeName(channel);
			String matchedChannelName = channelRemoval.getNames(channelNameManager).stream()
				.map(CleanChatUtil::sanitizeName)
				.filter(widgetChannelName::contains)
				.findFirst()
				.orElse(null);

			if (matchedChannelName != null)
			{
				return Pair.of(channelRemoval, matchedChannelName);
			}
		}

		return null;
	}
}
