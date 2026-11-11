package com.scenecustomizer;

import java.util.Objects;

/** What identifies a row: by name for things with one, by id for the rest. */
final class ThingKey
{
	/** The kind. */
	final Kind kind;
	/** The section the row sits in. */
	final Section section;
	/** Null unless the kind is EFFECT. */
	final EffectKind effectKind;
	/** The cleaned name; null when keyed by id. */
	final String name;
	/** The id; -1 when keyed by name. */
	final int id;

	private ThingKey(Kind kind, Section section, EffectKind effectKind, String name, int id)
	{
		this.kind = kind;
		this.section = section;
		this.effectKind = effectKind;
		this.name = name;
		this.id = id;
	}

	/** A key by name. */
	static ThingKey named(Kind kind, Section section, String name)
	{
		return new ThingKey(kind, section, null, name, -1);
	}

	/** A key by id; the effect kind is null unless the kind is EFFECT. */
	static ThingKey byId(Kind kind, Section section, EffectKind effectKind, int id)
	{
		return new ThingKey(kind, section, effectKind, null, id);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof ThingKey))
		{
			return false;
		}
		final ThingKey k = (ThingKey) o;
		return kind == k.kind && section == k.section && effectKind == k.effectKind && id == k.id
			&& Objects.equals(name, k.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(kind, section, effectKind, name, id);
	}

	@Override
	public String toString()
	{
		return kind + "/" + section + "/" + (name != null ? name : String.valueOf(id));
	}
}
