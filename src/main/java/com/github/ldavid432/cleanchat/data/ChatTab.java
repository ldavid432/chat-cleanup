package com.github.ldavid432.cleanchat.data;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Enum 5620 https://abextm.github.io/cache2/#/viewer/enum/5620
@AllArgsConstructor
public enum ChatTab
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

	public static ChatTab of(int value)
	{
		return Arrays.stream(values())
			.filter(channel -> channel.getValue() == value)
			.findFirst()
			.orElse(ChatTab.ALL);
	}
}
