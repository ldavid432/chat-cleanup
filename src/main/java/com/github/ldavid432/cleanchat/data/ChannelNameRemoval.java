package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.ChannelNameManager;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import com.github.ldavid432.cleanchat.CleanChatUtil;
import static com.github.ldavid432.cleanchat.CleanChatUtil.sanitizeName;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@AllArgsConstructor
public enum ChannelNameRemoval
{
	CLAN(CleanChatChannelsConfig::removeClanName, ChannelNameManager::getClanNames),
	GUEST_CLAN(CleanChatChannelsConfig::removeGuestClanName, ChannelNameManager::getGuestClanNames),
	FRIENDS_CHAT(CleanChatChannelsConfig::removeFriendsChatName, ChannelNameManager::getFriendsChatNames),
	GROUP_IRON(
		CleanChatChannelsConfig::removeGroupIronName,
		ChannelNameManager::getGroupIronNames,
		(config, tab) -> config.removeGroupIronFromClan() && tab == ChatTab.CLAN
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

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final Function<ChannelNameManager, List<String>> getNames;
	private final BiFunction<CleanChatChannelsConfig, ChatTab, Boolean> isTabBlocked;

	ChannelNameRemoval(Function<CleanChatChannelsConfig, Boolean> isEnabled, Function<ChannelNameManager, List<String>> getNames) {
		this(isEnabled, getNames, (c, t) -> false);
	}

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
				.filter(channel1 -> widgetChannelName.contains("[" + channel1 + "]"))
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
