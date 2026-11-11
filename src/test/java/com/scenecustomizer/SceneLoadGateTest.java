package com.scenecustomizer;

import net.runelite.api.GameState;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link SceneLoadGate}: spawns during a load do not count. */
public class SceneLoadGateTest
{
	@Test
	public void spawnsDuringALoadDoNotCount()
	{
		final SceneLoadGate gate = new SceneLoadGate();
		assertTrue(gate.spawnsCount());
		gate.onGameState(GameState.LOADING);
		assertFalse(gate.spawnsCount());
		gate.onGameState(GameState.LOGGED_IN);
		assertFalse(gate.spawnsCount());
		gate.onTick();
		assertTrue(gate.spawnsCount());
		gate.onGameState(GameState.LOADING);
		gate.onTick();
		assertFalse(gate.spawnsCount());
	}
}
