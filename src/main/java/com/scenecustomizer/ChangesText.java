package com.scenecustomizer;

/** The words that say what a rule does, for the Changes list. */
final class ChangesText
{
	private ChangesText()
	{
	}

	/**
	 * The parts joined by " - ": the outline ({@code Outline, width 3}; an object's is {@code Outline here, width
	 * 3}), then the hide ({@code Hidden} for an NPC, {@code Hidden everywhere} for an object, {@code Hidden here}
	 * with the count of places in brackets when there are more than one).
	 */
	static String describe(Rule rule)
	{
		final StringBuilder out = new StringBuilder();
		final boolean object = rule.kind == Kind.OBJECT;
		if (rule.outline != null)
		{
			out.append(object ? "Outline here, width " : "Outline, width ").append(rule.outline.width);
		}
		String hidden = null;
		if (rule.hide == HideScope.EVERYWHERE)
		{
			hidden = object ? "Hidden everywhere" : "Hidden";
		}
		else if (rule.hide == HideScope.HERE)
		{
			hidden = "Hidden here" + (rule.hidePlaces.size() > 1 ? " (" + rule.hidePlaces.size() + " places)" : "");
		}
		if (hidden != null)
		{
			out.append(out.length() > 0 ? " - " : "").append(hidden);
		}
		return out.toString();
	}
}
