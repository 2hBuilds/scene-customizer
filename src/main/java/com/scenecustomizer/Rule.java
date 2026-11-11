package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** What the player chose to do with one named NPC or object: outline it, hide it, or both. */
final class Rule
{
	/** NPC or OBJECT. */
	final Kind kind;
	/** The game's name, matched exactly. */
	final String name;
	/** The outline, or null for none. */
	final Outline outline;
	/** Where an object is outlined; always empty for an NPC. */
	final List<Place> outlinePlaces;
	/** How far it is hidden. */
	final HideScope hide;
	/** Where an object is hidden when the scope is HERE; always empty for an NPC. */
	final List<Place> hidePlaces;

	/**
	 * Choice: a null hide means OFF and null place lists mean empty; an NPC's place lists are dropped. An object
	 * outline with no places becomes no outline, and an object hide of HERE with no places becomes OFF.
	 *
	 * @throws IllegalArgumentException when the kind is not NPC or OBJECT
	 */
	Rule(Kind kind, String name, Outline outline, List<Place> outlinePlaces, HideScope hide, List<Place> hidePlaces)
	{
		if (kind != Kind.NPC && kind != Kind.OBJECT)
		{
			throw new IllegalArgumentException("A rule is for an NPC or an object, not " + kind);
		}
		this.kind = kind;
		this.name = name;
		this.outlinePlaces = kind == Kind.NPC ? Collections.<Place>emptyList() : capped(outlinePlaces);
		this.hidePlaces = kind == Kind.NPC ? Collections.<Place>emptyList() : capped(hidePlaces);
		final HideScope scope = hide == null ? HideScope.OFF : hide;
		this.outline = kind == Kind.OBJECT && this.outlinePlaces.isEmpty() ? null : outline;
		this.hide = kind == Kind.OBJECT && scope == HideScope.HERE && this.hidePlaces.isEmpty() ? HideScope.OFF : scope;
	}

	private static List<Place> capped(List<Place> places)
	{
		final LinkedHashSet<Place> kept = new LinkedHashSet<>();
		if (places != null)
		{
			for (Place place : places)
			{
				if (place != null && kept.size() < Caps.RULE_PLACES)
				{
					kept.add(place);
				}
			}
		}
		return Collections.unmodifiableList(new ArrayList<>(kept));
	}

	/** True when the rule has no outline and no hide. */
	boolean doesNothing()
	{
		return outline == null && hide == HideScope.OFF;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Rule))
		{
			return false;
		}
		final Rule r = (Rule) o;
		return kind == r.kind && Objects.equals(name, r.name) && Objects.equals(outline, r.outline)
			&& outlinePlaces.equals(r.outlinePlaces) && hide == r.hide && hidePlaces.equals(r.hidePlaces);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(kind, name, outline, outlinePlaces, hide, hidePlaces);
	}
}
