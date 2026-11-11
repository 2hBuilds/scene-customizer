package com.scenecustomizer;

/** A heading in the sidebar's list, in the order the headings are shown. */
enum Section
{
	/** NPCs seen. */
	NPCS("NPCs"),
	/** Objects that spawned while recording. */
	OBJECTS("Objects"),
	/** Named objects near the player (open at first since 2026-11-10, the user's wish). */
	SCENERY("Scenery nearby"),
	/** Graphics and projectiles. */
	EFFECTS("Effects"),
	/** NPC animations. */
	ANIMATIONS("Animations"),
	/** The player's own casts. */
	SPELLS("Your spells"),
	/** A look-up's own monster. */
	THIS_MONSTER("This monster"),
	/** Monsters linked from the looked-up wiki page. */
	LINKED("Linked on its wiki page");

	private final String title;

	Section(String title)
	{
		this.title = title;
	}

	/** The heading's words. */
	String title()
	{
		return title;
	}

	/** True when the section starts folded. */
	boolean foldedAtFirst()
	{
		return false;
	}
}
