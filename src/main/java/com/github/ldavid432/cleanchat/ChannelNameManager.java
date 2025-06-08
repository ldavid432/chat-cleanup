package com.github.ldavid432.cleanchat;

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

@Slf4j
public class ChannelNameManager
{

	private final Client client;

	@Inject
	ChannelNameManager(Client client)
	{
		this.client = client;
	}

	@Getter
	private String clanName = null;
	@Getter
	private String guestClanName = null;
	@Getter
	private String friendsChatName = null;
	@Getter
	private String groupIronName = null;
	private boolean isInFriendsChat = false;

	public void startup()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			isInFriendsChat = client.getFriendsChatManager() != null;
			setFriendsChatName();

			ClanChannel clanChannel = client.getClanChannel(ClanID.CLAN);
			if (clanChannel != null)
			{
				clanName = clanChannel.getName();
			}

			ClanChannel groupIronChannel = client.getClanChannel(ClanID.CLAN);
			if (groupIronChannel != null)
			{
				groupIronName = groupIronChannel.getName();
			}

			ClanChannel guestClanChannel = client.getGuestClanChannel();
			if (guestClanChannel != null)
			{
				guestClanName = guestClanChannel.getName();
			}
		}
	}

	public void shutDown()
	{
		clanName = null;
		groupIronName = null;
		guestClanName = null;
		friendsChatName = null;
		isInFriendsChat = false;
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		String channelName = event.getClanChannel() != null ? event.getClanChannel().getName() : null;

		log.debug("Connected to clan: {} - ID: {}", channelName, event.getClanId());

		switch (event.getClanId())
		{
			// TODO: Test guest clan chat
			case -1:
				guestClanName = channelName;
				break;
			case ClanID.CLAN:
				clanName = channelName;
				break;
			case ClanID.GROUP_IRONMAN:
				groupIronName = channelName;
				break;
		}
	}

	@Subscribe
	public void onFriendsChatChanged(FriendsChatChanged event)
	{
		isInFriendsChat = event.isJoined();

		if (isInFriendsChat)
		{
			setFriendsChatName();
		}
		else
		{
			friendsChatName = null;
		}
	}

	public void setFriendsChatNameIfNeeded()
	{
		if (isInFriendsChat && friendsChatName == null)
		{
			setFriendsChatName();
		}
	}

	private void setFriendsChatName()
	{
		FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		// This is null at the first FriendsChatChanged after login
		if (friendsChatManager != null)
		{
			friendsChatName = friendsChatManager.getName();
		}
	}
}
