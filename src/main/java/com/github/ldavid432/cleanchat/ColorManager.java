package com.github.ldavid432.cleanchat;

import java.awt.Color;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ChatColorConfig;
import net.runelite.client.ui.JagexColors;

@Singleton
@AllArgsConstructor(onConstructor_ = {@Inject})
public class ColorManager
{
	private final static Color OPAQUE_CLAN_GUEST_CHAT_DEFAULT = new Color(5, 124, 5);
	private final static Color OPAQUE_CHAT_DEFAULT = new Color(129, 5, 5);
	private final static Color OPAQUE_BROADCAST_DEFAULT = new Color(5, 5, 5);
	private final static Color TRANSPARENT_BROADCAST_DEFAULT = new Color(254, 254, 254);
	private final static Color TRANSPARENT_FRIEND_CHAT_DEFAULT = new Color(238, 83, 83);
	private final static Color TRANSPARENT_CLAN_GUEST_CHAT_DEFAULT = new Color(5, 211, 5);
	private final static Color OPAQUE_USERNAME_DEFAULT = Color.BLACK;
	private final static Color TRANSPARENT_USERNAME_DEFAULT = Color.WHITE;

	@Inject
	private ChatColorConfig chatColorConfig;

	@Inject
	private Client client;

	private boolean isChatboxTransparent()
	{
		return client.isResized() && client.getVarbitValue(VarbitID.CHATBOX_TRANSPARENCY) == 1;
	}

	@Nonnull
	public Color getGimNameColor()
	{
		Color color = isChatboxTransparent() ? chatColorConfig.transparentClanChannelName() : chatColorConfig.opaqueClanChannelName();
		if (color == null)
		{
			color = isChatboxTransparent() ? JagexColors.CHAT_FC_NAME_TRANSPARENT_BACKGROUND : JagexColors.CHAT_FC_NAME_OPAQUE_BACKGROUND;
		}
		return color;
	}

	@Nonnull
	public Color getUsernameColor(ChatMessageType chatMessageType)
	{
		Color color = getRuneLiteUsernameColor(chatMessageType);
		if (color == null)
		{
			color = isChatboxTransparent() ? TRANSPARENT_USERNAME_DEFAULT : OPAQUE_USERNAME_DEFAULT;
		}
		return color;
	}

	private Color getRuneLiteUsernameColor(ChatMessageType chatMessageType)
	{
		switch (chatMessageType)
		{
			case FRIENDSCHAT:
				return isChatboxTransparent() ? chatColorConfig.transparentFriendsChatUsernames() : chatColorConfig.opaqueFriendsChatUsernames();
			case CLAN_CHAT:
			case CLAN_GIM_CHAT:
				return isChatboxTransparent() ? chatColorConfig.transparentClanChatUsernames() : chatColorConfig.opaqueClanChatUsernames();
			case CLAN_GUEST_CHAT:
				return isChatboxTransparent() ? chatColorConfig.transparentClanChatGuestUsernames() : chatColorConfig.opaqueClanChatGuestUsernames();
			default:
				return null;
		}
	}

	@Nonnull
	public Color getMessageColor(ChatMessageType chatMessageType)
	{
		Color color;

		color = getRuneLiteMessageColor(chatMessageType);

		if (color == null)
		{
			int varP = getSettingsColorVarPlayerID(chatMessageType);
			if (varP != -1)
			{
				int value = client.getVarpValue(varP);
				// 0 is returned if the client hasn't set a value - As a color 0 should only correspond to transparent black so we should be ok
				if (value != 0)
				{
					value -= 1; // Not sure why but core does this
					color = new Color(value);
				}
			}
		}

		if (color == null)
		{
			color = getJagexDefaultMessageColor(chatMessageType);
		}

		if (color == null)
		{
			color = isChatboxTransparent() ? JagexColors.CHAT_PUBLIC_TEXT_TRANSPARENT_BACKGROUND : JagexColors.CHAT_PUBLIC_TEXT_OPAQUE_BACKGROUND;
		}

		return color;
	}

	private Color getRuneLiteMessageColor(ChatMessageType chatMessageType)
	{
		switch (chatMessageType)
		{
			case FRIENDSCHAT:
				return isChatboxTransparent() ? chatColorConfig.transparentFriendsChatMessage() : chatColorConfig.opaqueFriendsChatMessage();
			case CLAN_CHAT:
			case CLAN_GIM_CHAT:
				return isChatboxTransparent() ? chatColorConfig.transparentClanChatMessage() : chatColorConfig.opaqueClanChatMessage();
			case CLAN_MESSAGE:
			case CLAN_GIM_MESSAGE:
				return isChatboxTransparent() ? chatColorConfig.transparentClanChatInfo() : chatColorConfig.opaqueClanChatInfo();
			case CLAN_GUEST_CHAT:
				return isChatboxTransparent() ? chatColorConfig.transparentClanChatGuestMessage() : chatColorConfig.opaqueClanChatGuestMessage();
			case CLAN_GUEST_MESSAGE:
				return isChatboxTransparent() ? chatColorConfig.transparentClanChatGuestInfo() : chatColorConfig.opaqueClanChatGuestInfo();
			default:
				return null;
		}
	}

	// These end up being used when the user hasn't configured a color yet (even though the settings show a default color)
	private Color getJagexDefaultMessageColor(ChatMessageType type)
	{
		switch (type)
		{
			case CLAN_GUEST_CHAT:
				return isChatboxTransparent() ? TRANSPARENT_CLAN_GUEST_CHAT_DEFAULT : OPAQUE_CLAN_GUEST_CHAT_DEFAULT;
			case FRIENDSCHAT:
				return isChatboxTransparent() ? TRANSPARENT_FRIEND_CHAT_DEFAULT : OPAQUE_CHAT_DEFAULT;
			case CLAN_CHAT:
			case CLAN_GIM_CHAT:
				return OPAQUE_CHAT_DEFAULT;
			case CLAN_MESSAGE:
			case CLAN_GUEST_MESSAGE:
			case CLAN_GIM_MESSAGE:
				return isChatboxTransparent() ? TRANSPARENT_BROADCAST_DEFAULT : OPAQUE_BROADCAST_DEFAULT;
			default:
				return null;
		}
	}

	private int getSettingsColorVarPlayerID(ChatMessageType type)
	{
		switch (type)
		{
			case FRIENDSCHAT:
				return isChatboxTransparent() ? VarPlayerID.OPTION_CHAT_COLOUR_FRIENDSCHAT_TRANSPARENT : VarPlayerID.OPTION_CHAT_COLOUR_FRIENDSCHAT_OPAQUE;
			case CLAN_CHAT:
				return isChatboxTransparent() ? VarPlayerID.OPTION_CHAT_COLOUR_CLANCHAT_TRANSPARENT : VarPlayerID.OPTION_CHAT_COLOUR_CLANCHAT_OPAQUE;
			case CLAN_GUEST_CHAT:
				return isChatboxTransparent() ? VarPlayerID.OPTION_CHAT_COLOUR_GUESTCLAN_TRANSPARENT : VarPlayerID.OPTION_CHAT_COLOUR_GUESTCLAN_OPAQUE;
			case CLAN_GIM_CHAT:
				return isChatboxTransparent() ? VarPlayerID.OPTION_CHAT_COLOUR_GIMCHAT_TRANSPARENT : VarPlayerID.OPTION_CHAT_COLOUR_GIMCHAT_OPAQUE;
			case CLAN_MESSAGE:
				return isChatboxTransparent() ? VarPlayerID.OPTION_CHAT_COLOUR_CLANBROADCAST_TRANSPARENT : VarPlayerID.OPTION_CHAT_COLOUR_CLANBROADCAST_OPAQUE;
			case CLAN_GIM_MESSAGE:
				return isChatboxTransparent() ? VarPlayerID.OPTION_CHAT_COLOUR_GIMBROADCAST_TRANSPARENT : VarPlayerID.OPTION_CHAT_COLOUR_GIMBROADCAST_OPAQUE;
			default:
				return -1;
		}
	}
}
