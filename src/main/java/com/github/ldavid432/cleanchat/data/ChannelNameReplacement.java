package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.ChannelNameManager;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ChannelNameReplacement
{
	CLAN(CleanChatChannelsConfig::removeClanName, ChannelNameManager::getClanName),
	GUEST_CLAN(CleanChatChannelsConfig::removeGuestClanName, ChannelNameManager::getGuestClanName),
	FRIENDS_CHAT(CleanChatChannelsConfig::removeFriendsChatName, ChannelNameManager::getFriendsChatName),
	GROUP_IRON(CleanChatChannelsConfig::removeGroupIronName, ChannelNameManager::getGroupIronName);

	public Set<String> getNames(ChannelNameManager channelNameManager)
	{
		return getNames.apply(channelNameManager);
	}

	public boolean isEnabled(CleanChatChannelsConfig config)
	{
		return isEnabled.apply(config);
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final Function<ChannelNameManager, Set<String>> getNames;

	public static boolean anyEnabled(CleanChatChannelsConfig config)
	{
		return Arrays.stream(values()).anyMatch(channel -> channel.isEnabled(config));
	}
}
