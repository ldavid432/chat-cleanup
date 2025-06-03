package com.github.ldavid432.cleanchat.data;

import com.github.ldavid432.cleanchat.CleanChatChannelsConfig;
import com.github.ldavid432.cleanchat.CleanChatChannelsPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ChannelNameReplacement
{
	CLAN(CleanChatChannelsConfig::removeClanName, CleanChatChannelsPlugin::getClanName),
	GUEST_CLAN(CleanChatChannelsConfig::removeGuestClanName, CleanChatChannelsPlugin::getGuestClanName),
	FRIENDS_CHAT(CleanChatChannelsConfig::removeFriendsChatName, CleanChatChannelsPlugin::getFriendsChatName),
	GROUP_IRON(CleanChatChannelsConfig::removeGroupIronName, CleanChatChannelsPlugin::getGroupIronName);

	public String getName(CleanChatChannelsPlugin plugin)
	{
		return getName.apply(plugin);
	}

	private final Function<CleanChatChannelsConfig, Boolean> isEnabled;
	private final Function<CleanChatChannelsPlugin, String> getName;

	public static List<ChannelNameReplacement> getEnabledReplacements(CleanChatChannelsConfig config)
	{
		return Arrays.stream(values())
			.filter(replacement -> replacement.isEnabled.apply(config))
			.collect(Collectors.toList());
	}
}
