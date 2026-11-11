package com.scenecustomizer;

/** Every limit of the model in one place, so a busy scene cannot grow a session without end. */
final class Caps
{
	/** Named NPCs per session. */
	static final int NPC_NAMED = 300;
	/** Nameless NPCs per session. */
	static final int NPC_NAMELESS = 50;
	/** Named objects per session (the OBJECTS section only). */
	static final int OBJECT_NAMED = 300;
	/** Nameless objects per session (the OBJECTS section only). */
	static final int OBJECT_NAMELESS = 50;
	/** Named scenery things per session. */
	static final int SCENERY = 500;
	/** Effects per session. */
	static final int EFFECTS = 300;
	/** Animations per session. */
	static final int ANIMATIONS = 500;
	/** Spells per session. */
	static final int SPELLS = 100;
	/** Rows in a look-up's own-monster section. */
	static final int THIS_MONSTER = 10;
	/** Rows in a look-up's linked section. */
	static final int LINKED = 60;
	/** Ids kept per thing. */
	static final int IDS = 32;
	/** Places kept per thing. */
	static final int PLACES = 16;
	/** Hint names kept per thing. */
	static final int HINTS = 3;
	/** Pictures held per session. */
	static final int PICTURES = 400;
	/** Times a picture is tried for before the thing is left without. */
	static final int PICTURE_TRIES = 3;
	/** Rules kept. */
	static final int RULES = 500;
	/** Places kept per rule list. */
	static final int RULE_PLACES = 16;
	/** Recordings kept in memory. */
	static final int RECORDINGS_KEPT = 5;
	/** Look-ups kept in memory. */
	static final int LOOKUPS_KEPT = 5;

	private Caps()
	{
	}
}
