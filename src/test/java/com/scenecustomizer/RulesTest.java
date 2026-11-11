package com.scenecustomizer;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link Rules}: read at start, written at each change. */
public class RulesTest
{
	private static Rule npc(String name)
	{
		return new Rule(Kind.NPC, name, new Outline(0x00C85A, 3), Collections.<Place>emptyList(), HideScope.OFF,
			Collections.<Place>emptyList());
	}

	@Test
	public void readsAtStartAndWritesEachChange()
	{
		final Gson gson = new Gson();
		final RuleStore seed = new RuleStore(gson);
		seed.put(npc("Vorkath"));
		final ConfigManager config = mock(ConfigManager.class);
		when(config.getConfiguration("scene-customizer", "rules")).thenReturn(seed.toJson());
		final Rules rules = new Rules(config, gson);
		rules.load();
		assertNotNull(rules.get(Kind.NPC, "Vorkath"));

		final AtomicInteger heard = new AtomicInteger();
		rules.addListener(heard::incrementAndGet);
		rules.put(npc("Zombified Spawn"));
		final ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
		verify(config).setConfiguration(eq("scene-customizer"), eq("rules"), json.capture());
		final RuleStore written = RuleStore.fromJson(gson, json.getValue());
		assertEquals(2, written.all().size());
		assertEquals(1, heard.get());

		rules.remove(Kind.NPC, "Vorkath");
		verify(config, times(2)).setConfiguration(eq("scene-customizer"), eq("rules"), anyString());
		assertNull(rules.get(Kind.NPC, "Vorkath"));
		assertEquals(2, heard.get());
	}

	@Test
	public void clearRemovesEveryRuleWithOneWrite()
	{
		final ConfigManager config = mock(ConfigManager.class);
		final Rules rules = new Rules(config, new Gson());
		rules.put(npc("A"));
		rules.put(npc("B"));
		rules.put(npc("C"));
		final AtomicInteger heard = new AtomicInteger();
		rules.addListener(heard::incrementAndGet);
		rules.clear();
		assertEquals(0, rules.all().size());
		verify(config, times(1)).setConfiguration("scene-customizer", "rules", "[]");
		assertEquals(1, heard.get());
	}
}
