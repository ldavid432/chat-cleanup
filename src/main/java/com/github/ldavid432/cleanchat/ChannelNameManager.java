package com.github.ldavid432.cleanchat;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.FriendsChatChanged;
import net.runelite.client.eventbus.Subscribe;

/**
 * Tracks current and previous chat channel names for use in the ChatChannelReplacer
 */
@Slf4j
public class ChannelNameManager
{

	@Inject
	private Client client;

	// Store these as Sets so that even if you leave a channel the chats will still be "cleaned"
	@Getter
	private final Set<String> clanName = new HashSet<>();
	@Getter
	private final Set<String> guestClanName = new HashSet<>();
	@Getter
	private final Set<String> friendsChatName = new HashSet<>();
	@Getter
	private final Set<String> groupIronName = new HashSet<>();

	public void startup()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			updateFriendsChatName();

			ClanChannel clanChannel = client.getClanChannel(ClanID.CLAN);
			if (clanChannel != null)
			{
				clanName.add(clanChannel.getName());
			}

			ClanChannel groupIronChannel = client.getClanChannel(ClanID.CLAN);
			if (groupIronChannel != null)
			{
				groupIronName.add(groupIronChannel.getName());
			}

			ClanChannel guestClanChannel = client.getGuestClanChannel();
			if (guestClanChannel != null)
			{
				guestClanName.add(guestClanChannel.getName());
			}
		}
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		String channelName = event.getClanChannel() != null ? event.getClanChannel().getName() : null;

		if (channelName == null)
		{
			return;
		}

		switch (event.getClanId())
		{
			case CleanChatUtil.GUEST_CLAN:
				guestClanName.add(channelName);
				break;
			case ClanID.CLAN:
				clanName.add(channelName);
				break;
			case ClanID.GROUP_IRONMAN:
				groupIronName.add(channelName);
				break;
		}
	}

	@Subscribe
	public void onFriendsChatChanged(FriendsChatChanged event)
	{
		if (event.isJoined())
		{
			updateFriendsChatName();
		}
	}

	public void updateFriendsChatName()
	{
		FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		// This is null at the first FriendsChatChanged after login
		if (friendsChatManager != null)
		{
			friendsChatName.add(friendsChatManager.getName());
		}
	}
}
