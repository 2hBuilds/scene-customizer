package com.scenecustomizer;

/** What one line of a row's menu does when pressed. */
enum RowAction
{
	/** Outline on or off. */
	OUTLINE,
	/** The outline's colour. */
	COLOUR,
	/** The outline's width. */
	WIDTH,
	/** An NPC's hide, on or off. */
	HIDE,
	/** An object is shown. */
	SHOW,
	/** An object is hidden at its places. */
	HIDE_HERE,
	/** An object is hidden wherever its name appears. */
	HIDE_EVERYWHERE,
	/** Copy the ids. */
	COPY,
	/** Take a wiki row out of the list. */
	REMOVE
}
