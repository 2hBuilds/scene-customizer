package com.scenecustomizer;

/** How far a rule hides its thing. */
enum HideScope
{
	/** Not hidden. */
	OFF,
	/** Hidden at the rule's places only. */
	HERE,
	/** Hidden wherever its name appears. */
	EVERYWHERE
}
