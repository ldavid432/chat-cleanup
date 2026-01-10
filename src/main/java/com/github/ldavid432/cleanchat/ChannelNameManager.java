package com.github.ldavid432.cleanchat;

import static com.github.ldavid432.cleanchat.CleanChatChannelsConfig.DEFAULT_CUSTOM_CHANNEL_NAME;
import static com.github.ldavid432.cleanchat.CleanChatUtil.MAX_CHANNEL_LIST_SIZE;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.FriendsChatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

/**
 * Tracks current and previous chat channel names for use in the ChatWidgetEditor
 */
@Slf4j
@Singleton
public class ChannelNameManager
{

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CleanChatChannelsConfig config;

	// Store these as a collection so that even if you leave a channel the chats will still be "cleaned"
	@Getter
	private final List<String> clanNames = new ArrayList<>();
	@Getter
	private final List<String> guestClanNames = new ArrayList<>();
	@Getter
	private final List<String> friendsChatNames = new ArrayList<>();
	@Getter
	private final List<String> groupIronNames = new ArrayList<>();

	@Getter
	private String shortClanName = DEFAULT_CUSTOM_CHANNEL_NAME;
	@Getter
	private String shortGuestClanName = DEFAULT_CUSTOM_CHANNEL_NAME;
	@Getter
	private String shortFriendsChatName = DEFAULT_CUSTOM_CHANNEL_NAME;
	@Getter
	private String shortGroupIronName = DEFAULT_CUSTOM_CHANNEL_NAME;

	public void startup()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() -> {
				updateFriendsChatName();

				ClanChannel clanChannel = client.getClanChannel(ClanID.CLAN);
				if (clanChannel != null)
				{
					addName(clanNames, clanChannel.getName());
				}

				ClanChannel groupIronChannel = client.getClanChannel(ClanID.GROUP_IRONMAN);
				if (groupIronChannel != null)
				{
					addName(groupIronNames, groupIronChannel.getName());
				}

				ClanChannel guestClanChannel = client.getGuestClanChannel();
				if (guestClanChannel != null)
				{
					addName(guestClanNames, guestClanChannel.getName());
				}
			});
		}

		updateShortNames();
	}

	public void shutdown()
	{
		clanNames.clear();
		groupIronNames.clear();
		guestClanNames.clear();
		friendsChatNames.clear();
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
				addName(guestClanNames, channelName);
				break;
			case ClanID.CLAN:
				addName(clanNames, channelName);
				break;
			case ClanID.GROUP_IRONMAN:
				addName(groupIronNames, channelName);
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
			addName(friendsChatNames, friendsChatManager.getName());
		}
	}

	private void addName(List<String> nameList, String name)
	{
		if (nameList.contains(name) || name == null)
		{
			return;
		}

		if (nameList.size() == MAX_CHANNEL_LIST_SIZE)
		{
			nameList.remove(0);
		}

		nameList.add(name);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (Objects.equals(event.getGroup(), CleanChatChannelsConfig.GROUP) && event.getKey().startsWith("short"))
		{
			updateShortNames();
		}
	}

	private void updateShortNames()
	{
		shortClanName = config.getShortClanName();
		shortGuestClanName = config.getShortGuestClanName();
		shortFriendsChatName = config.getShortFriendsName();
		shortGroupIronName = config.getShortGroupIronName();
	}
}
