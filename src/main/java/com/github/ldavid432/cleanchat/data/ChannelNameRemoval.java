package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.ChannelNameManager;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ChannelNameRemoval
{
	CLAN(CleanChatChannelsConfig::removeClanName, ChannelNameManager::getClanNames),
	GUEST_CLAN(CleanChatChannelsConfig::removeGuestClanName, ChannelNameManager::getGuestClanNames),
	FRIENDS_CHAT(CleanChatChannelsConfig::removeFriendsChatName, ChannelNameManager::getFriendsChatNames),
	GROUP_IRON(CleanChatChannelsConfig::removeGroupIronName, ChannelNameManager::getGroupIronNames);

	public List<String> getNames(ChannelNameManager channelNameManager)
	{
		return getNames.apply(channelNameManager);
	}

	public boolean isEnabled(CleanChatChannelsConfig config)
	{
		return isEnabled.apply(config);
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final Function<ChannelNameManager, List<String>> getNames;

	public static boolean anyEnabled(CleanChatChannelsConfig config)
	{
		return Arrays.stream(values()).anyMatch(channel -> channel.isEnabled(config));
	}
}
