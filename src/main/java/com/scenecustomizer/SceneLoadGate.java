package com.scenecustomizer;

import net.runelite.api.GameState;

/** Says whether object spawns count: not while a scene loads, and not on the tick it finishes. */
final class SceneLoadGate
{
	private boolean counts = true;
	private boolean loggedInSinceLoading;

	/** True when a spawn now is a real one. */
	boolean spawnsCount()
	{
		return counts;
	}

	/** Feeds a game state: LOADING closes the gate, LOGGED_IN lets the next tick open it. */
	void onGameState(GameState state)
	{
		if (state == GameState.LOADING)
		{
			counts = false;
			loggedInSinceLoading = false;
		}
		else if (state == GameState.LOGGED_IN)
		{
			loggedInSinceLoading = true;
		}
	}

	/** A game tick: opens the gate if a LOGGED_IN came since the LOADING. */
	void onTick()
	{
		if (!counts && loggedInSinceLoading)
		{
			counts = true;
		}
	}
}
