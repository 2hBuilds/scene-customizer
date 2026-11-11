package com.scenecustomizer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The player's rules, newest change first, and their JSON form for the one config key. */
final class RuleStore
{
	private final Gson gson;
	private final List<Rule> rules = new ArrayList<>();

	RuleStore(Gson gson)
	{
		this.gson = gson;
	}

	/** The rule of that kind and exact (case-sensitive) name, or null. */
	Rule get(Kind kind, String name)
	{
		final int at = indexOf(kind, name);
		return at < 0 ? null : rules.get(at);
	}

	/** All rules, newest change first. */
	List<Rule> all()
	{
		return Collections.unmodifiableList(new ArrayList<>(rules));
	}

	/**
	 * Stores the rule first, replacing the one of the same kind and name; a rule that does nothing removes it
	 * instead. Over the cap the oldest change is dropped.
	 */
	void put(Rule rule)
	{
		remove(rule.kind, rule.name);
		if (rule.doesNothing())
		{
			return;
		}
		rules.add(0, rule);
		while (rules.size() > Caps.RULES)
		{
			rules.remove(rules.size() - 1);
		}
	}

	/** Removes the rule of that kind and name, if any. */
	void remove(Kind kind, String name)
	{
		final int at = indexOf(kind, name);
		if (at >= 0)
		{
			rules.remove(at);
		}
	}

	private int indexOf(Kind kind, String name)
	{
		for (int i = 0; i < rules.size(); i++)
		{
			final Rule r = rules.get(i);
			if (r.kind == kind && r.name.equals(name))
			{
				return i;
			}
		}
		return -1;
	}

	/** The rules as a JSON list, newest first. */
	String toJson()
	{
		final JsonArray list = new JsonArray();
		for (Rule r : rules)
		{
			final JsonObject o = new JsonObject();
			o.addProperty("kind", r.kind.name());
			o.addProperty("name", r.name);
			if (r.outline != null)
			{
				final JsonObject out = new JsonObject();
				out.addProperty("rgb", r.outline.rgb);
				out.addProperty("width", r.outline.width);
				o.add("outline", out);
			}
			o.add("outlinePlaces", placesToJson(r.outlinePlaces));
			o.addProperty("hide", r.hide.name());
			o.add("hidePlaces", placesToJson(r.hidePlaces));
			list.add(o);
		}
		return gson.toJson(list);
	}

	private static JsonArray placesToJson(List<Place> places)
	{
		final JsonArray list = new JsonArray();
		for (Place p : places)
		{
			final JsonObject o = new JsonObject();
			o.addProperty("x", p.x);
			o.addProperty("y", p.y);
			o.addProperty("plane", p.plane);
			list.add(o);
		}
		return list;
	}

	/**
	 * Reads a store back. Choice: null, blank, unreadable or not-a-list text gives an empty store, and a single
	 * unreadable entry is skipped; the order in the text is kept.
	 */
	static RuleStore fromJson(Gson gson, String json)
	{
		final RuleStore store = new RuleStore(gson);
		if (json == null || json.trim().isEmpty())
		{
			return store;
		}
		final JsonElement root;
		try
		{
			root = gson.fromJson(json, JsonElement.class);
		}
		catch (RuntimeException e)
		{
			return store;
		}
		if (root == null || !root.isJsonArray())
		{
			return store;
		}
		for (JsonElement entry : root.getAsJsonArray())
		{
			try
			{
				final Rule rule = ruleFromJson(entry.getAsJsonObject());
				if (!rule.doesNothing() && store.indexOf(rule.kind, rule.name) < 0 && store.rules.size() < Caps.RULES)
				{
					store.rules.add(rule);
				}
			}
			catch (RuntimeException e)
			{
				continue;
			}
		}
		return store;
	}

	private static Rule ruleFromJson(JsonObject o)
	{
		Outline outline = null;
		if (o.has("outline") && o.get("outline").isJsonObject())
		{
			final JsonObject out = o.getAsJsonObject("outline");
			outline = new Outline(out.get("rgb").getAsInt(), out.get("width").getAsInt());
		}
		return new Rule(Kind.valueOf(o.get("kind").getAsString()), o.get("name").getAsString(), outline,
			placesFromJson(o.get("outlinePlaces")), HideScope.valueOf(o.get("hide").getAsString()),
			placesFromJson(o.get("hidePlaces")));
	}

	private static List<Place> placesFromJson(JsonElement element)
	{
		final List<Place> places = new ArrayList<>();
		if (element != null && element.isJsonArray())
		{
			for (JsonElement e : element.getAsJsonArray())
			{
				final JsonObject p = e.getAsJsonObject();
				places.add(new Place(p.get("x").getAsInt(), p.get("y").getAsInt(), p.get("plane").getAsInt()));
			}
		}
		return places;
	}
}
