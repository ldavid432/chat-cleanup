package com.github.ldavid432.cleanchat.data;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SelectedChatChannel
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

	public static SelectedChatChannel of(int value)
	{
		return Arrays.stream(values())
			.filter(channel -> channel.getValue() == value)
			.findFirst()
			.orElse(SelectedChatChannel.ALL);
	}
}
