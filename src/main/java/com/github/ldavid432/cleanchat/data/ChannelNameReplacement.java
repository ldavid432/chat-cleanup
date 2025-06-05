package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.ChannelNameManager;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import java.util.Arrays;
import java.util.function.Function;
import lombok.AllArgsConstructor;

// TODO: Rename
@AllArgsConstructor
public enum ChannelNameReplacement
{
	CLAN(CleanChatChannelsConfig::removeClanName, ChannelNameManager::getClanName),
	GUEST_CLAN(CleanChatChannelsConfig::removeGuestClanName, ChannelNameManager::getGuestClanName),
	FRIENDS_CHAT(CleanChatChannelsConfig::removeFriendsChatName, ChannelNameManager::getFriendsChatName),
	GROUP_IRON(CleanChatChannelsConfig::removeGroupIronName, ChannelNameManager::getGroupIronName);

	public String getName(ChannelNameManager channelNameManager)
	{
		return getName.apply(channelNameManager);
	}

	public boolean isEnabled(CleanChatChannelsConfig config)
	{
		return isEnabled.apply(config);
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final Function<ChannelNameManager, String> getName;

	public static boolean anyEnabled(CleanChatChannelsConfig config)
	{
		return Arrays.stream(values()).anyMatch(channel -> channel.isEnabled(config));
	}
}

