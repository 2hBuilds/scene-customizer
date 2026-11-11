package com.scenecustomizer;

import net.runelite.api.MenuAction;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** {@link SpellNames}: only a cast counts, and only the spell's name is kept. */
public class SpellNamesTest
{
	@Test
	public void castOnAnNpcKeepsOnlyTheSpell()
	{
		assertEquals("Ice Barrage", SpellNames.fromMenu("Cast", MenuAction.WIDGET_TARGET_ON_NPC,
			"<col=00ff00>Ice Barrage</col><col=ffffff> -> <col=ffff00>Vorkath<col=ff00ff>  (level-732)"));
	}

	@Test
	public void castOnAPlayerKeepsNoPlayerName()
	{
		assertEquals("Ice Barrage", SpellNames.fromMenu("Cast", MenuAction.WIDGET_TARGET_ON_PLAYER,
			"<col=00ff00>Ice Barrage</col><col=ffffff> -> <col=ffffff>Zezima  (level-126)"));
	}

	@Test
	public void pickingASpellIsNotACast()
	{
		assertNull(SpellNames.fromMenu("Cast", MenuAction.WIDGET_TARGET, "<col=00ff00>Ice Barrage</col>"));
	}

	@Test
	public void aSpellWithNoTargetCounts()
	{
		assertEquals("Vengeance", SpellNames.fromMenu("Cast", MenuAction.CC_OP, "<col=00ff00>Vengeance</col>"));
		assertEquals("Vengeance",
			SpellNames.fromMenu("Cast", MenuAction.CC_OP_LOW_PRIORITY, "<col=00ff00>Vengeance</col>"));
	}

	@Test
	public void otherClicksAreNotSpells()
	{
		assertNull(SpellNames.fromMenu("Attack", MenuAction.NPC_SECOND_OPTION, "<col=ffff00>Vorkath"));
		assertNull(SpellNames.fromMenu("cast", MenuAction.CC_OP, "x"));
		assertNull(SpellNames.fromMenu("Cast", null, "x"));
	}
}
