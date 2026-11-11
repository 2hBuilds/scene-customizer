package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.NPCComposition;

/**
 * Puts a look-up's wiki monsters into a session's table under the GAME's names. Client thread only: it asks the client
 * for each id's definition. An id the client does not know, or whose definition has no name, keeps the wiki's name.
 *
 * <p>Choice: the name is read id by id, so ids that the game names differently become different rows. Choice: a This
 * monster row's options are its ids' options joined in order, each once, nulls and blanks dropped; a monster the wiki
 * gives no usable id is still listed, under the wiki's name.
 */
final class WikiNames
{
	private WikiNames()
	{
	}

	/**
	 * Adds the monsters to the table in the section. Meant to be called once per look-up run: calling it again would
	 * bring back a row the player removed (use {@link #pictures} for later results). Each row is first drawn from the
	 * game's model for its id; the wiki's picture is kept for a row the client has no model for.
	 *
	 * <p>Choice: when a model is being painted for a row, the wiki picture is not stored at once, so the model's wins; a
	 * later wiki result still fills a row that has no picture by then.
	 *
	 * @param withActions true for This monster rows, which also get the right-click options of their ids
	 */
	static void fill(Client client, ModelPictures drawn, ThingTable table, List<WikiMonster> monsters, Section section, boolean withActions)
	{
		for (WikiMonster monster : monsters)
		{
			final Map<ThingKey, List<String>> options = new LinkedHashMap<>();
			if (monster.ids().isEmpty())
			{
				final ThingKey key = table.wikiMonster(monster.name(), Collections.<Integer>emptyList(), section);
				take(table, key, monster, options, null, false);
			}
			for (int id : monster.ids())
			{
				final NPCComposition definition = client.getNpcDefinition(id);
				final String gameName = definition == null ? null : Names.clean(definition.getName());
				final ThingKey key = table.wikiMonster(gameName == null ? monster.name() : gameName,
					Collections.singletonList(id), section);
				final boolean drawing = key != null && drawn.draw(table, key, Collections.singletonList(id));
				take(table, key, monster, options, withActions ? definition : null, drawing);
			}
			for (Map.Entry<ThingKey, List<String>> entry : options.entrySet())
			{
				table.setActions(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Hands the monsters' pictures to the rows that still exist; a row the player removed is not made again. Client
	 * thread only.
	 */
	static void pictures(Client client, ThingTable table, List<WikiMonster> monsters, Section section)
	{
		for (WikiMonster monster : monsters)
		{
			if (monster.picture() == null)
			{
				continue;
			}
			if (monster.ids().isEmpty())
			{
				pictureTo(table, monster.name(), section, monster);
			}
			for (int id : monster.ids())
			{
				final NPCComposition definition = client.getNpcDefinition(id);
				final String gameName = definition == null ? null : Names.clean(definition.getName());
				pictureTo(table, gameName == null ? monster.name() : gameName, section, monster);
			}
		}
	}

	private static void pictureTo(ThingTable table, String name, Section section, WikiMonster monster)
	{
		final String clean = Names.clean(name);
		if (clean != null)
		{
			table.pictureTaken(ThingKey.named(Kind.NPC, section, clean), monster.picture());
		}
	}

	private static void take(ThingTable table, ThingKey key, WikiMonster monster, Map<ThingKey, List<String>> options,
		NPCComposition definition, boolean drawing)
	{
		if (key == null)
		{
			return;
		}
		if (!drawing && monster.picture() != null)
		{
			table.pictureTaken(key, monster.picture());
		}
		if (definition == null)
		{
			return;
		}
		final List<String> kept = options.computeIfAbsent(key, k -> new ArrayList<>());
		final String[] actions = definition.getActions();
		if (actions == null)
		{
			return;
		}
		for (String action : actions)
		{
			if (action != null && !action.trim().isEmpty() && !kept.contains(action))
			{
				kept.add(action);
			}
		}
	}
}
