package com.scenecustomizer;

/** What the rules say about one object: hide its id, hide this very object, outline it. Never changed. */
final class ObjectDecision
{
	/** Hide every object with this id (the rule hides everywhere). */
	final boolean hideId;
	/** Hide this object by its hash (the rule hides here, and this is one of its places). */
	final boolean hideHash;
	/** The outline to draw, or null. */
	final Outline outline;

	ObjectDecision(boolean hideId, boolean hideHash, Outline outline)
	{
		this.hideId = hideId;
		this.hideHash = hideHash;
		this.outline = outline;
	}

	/** True when the rules say nothing about the object. */
	boolean isEmpty()
	{
		return !hideId && !hideHash && outline == null;
	}
}
