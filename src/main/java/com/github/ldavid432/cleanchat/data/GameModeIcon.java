package com.github.ldavid432.cleanchat.data;

import java.util.List;
import lombok.Getter;

public enum GameModeIcon
{
	IRONMAN(
		2,  // ironman
		3,  // UIM
		10, // HCIM
		41, // GIM
		42, // HCGIM
		43  // UGIM
	),
	LEAGUES(22),
	HARDCORE_MAIN(56),
	QUEST_SPEEDRUNNING(56),
	;

	@Getter
	final List<Integer> iconIds;

	GameModeIcon(Integer... iconIds)
	{
		this.iconIds = List.of(iconIds);
	}
}
