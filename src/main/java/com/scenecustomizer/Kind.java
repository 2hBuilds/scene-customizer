package com.scenecustomizer;

/** What sort of thing a recording or a look-up found. */
enum Kind
{
	/** A non-player character. */
	NPC,
	/** A scene object. */
	OBJECT,
	/** A graphic or a projectile. */
	EFFECT,
	/** An NPC's animation. */
	ANIMATION,
	/** A spell the player cast. */
	SPELL
}
