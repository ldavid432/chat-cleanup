package com.github.ldavid432.cleanchat.data;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ChatChannel
{
	ALL(0),
	GAME(1),
	PUBLIC(2),
	PRIVATE(3),
	CHANNEL(4),
	CLAN(5),
	TRADE(6),
	CLOSED(1337);

	@Getter
	private final int value;

	public static ChatChannel of(int value)
	{
		return Arrays.stream(values())
			.filter(chatChannel -> chatChannel.getValue() == value)
			.findFirst()
			.orElse(ChatChannel.ALL);
	}
}
