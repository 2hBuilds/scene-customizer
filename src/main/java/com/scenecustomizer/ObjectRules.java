package com.scenecustomizer;

/** Works out what the object rules say about one object. */
final class ObjectRules
{
	private static final ObjectDecision NOTHING = new ObjectDecision(false, false, null);

	private ObjectRules()
	{
	}

	/**
	 * Nothing for a blocked id, a null name or a name no OBJECT rule has. Else: {@code hideId} when the rule hides
	 * everywhere; {@code hideHash} when it hides here and its hide places hold {@code place}; the outline when the
	 * rule has one and its outline places hold {@code place}. Choice: {@code hash} is not needed to decide (the
	 * caller keeps it for the answer it files); a null place matches no place.
	 */
	static ObjectDecision decide(int id, long hash, String name, Place place, RuleLookup rules)
	{
		if (name == null || BlockList.isBlocked(id))
		{
			return NOTHING;
		}
		final Rule rule = rules.object(name);
		if (rule == null)
		{
			return NOTHING;
		}
		final boolean hideId = rule.hide == HideScope.EVERYWHERE;
		final boolean hideHash = rule.hide == HideScope.HERE && place != null && rule.hidePlaces.contains(place);
		final Outline outline = rule.outline != null && place != null && rule.outlinePlaces.contains(place)
			? rule.outline : null;
		return new ObjectDecision(hideId, hideHash, outline);
	}
}
