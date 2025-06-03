package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.ChannelNameManager;
import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

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

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final Function<ChannelNameManager, String> getName;

	public static List<ChannelNameReplacement> getEnabledReplacements(CleanChatChannelsConfig config)
	{
		return Arrays.stream(values())
			.filter(replacement -> replacement.isEnabled.apply(config))
			.collect(Collectors.toList());
	}
}
