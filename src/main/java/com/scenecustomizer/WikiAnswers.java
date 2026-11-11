package com.scenecustomizer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import okhttp3.HttpUrl;

/**
 * Reads the wiki's JSON answers (the {@code formatversion=2} shape) into plain lists. Pure: the injected Gson is
 * handed in, nothing is asked, and an answer of the wrong shape gives an empty result. Text that is not JSON at all
 * makes Gson throw its parse exception, which the caller treats as a failed call.
 */
final class WikiAnswers
{
	private final Gson gson;

	WikiAnswers(Gson gson)
	{
		this.gson = gson;
	}

	/** The titles of the pages that carry the monster template, sorted by the answer's {@code index}, at most {@code most}. */
	List<String> suggestions(String json, int most)
	{
		final List<JsonObject> monsters = new ArrayList<>();
		for (JsonObject page : pages(json))
		{
			if (carriesTemplate(page))
			{
				monsters.add(page);
			}
		}
		monsters.sort((a, b) -> Integer.compare(indexOf(a), indexOf(b)));
		final List<String> titles = new ArrayList<>();
		for (JsonObject page : monsters)
		{
			if (titles.size() >= most)
			{
				break;
			}
			titles.add(title(page));
		}
		return titles;
	}

	/**
	 * The targets of the page's own {@code [[...]]} links, in order, each once: the part before {@code |} and
	 * {@code #}, trimmed, underscores as spaces, the first letter upper-case. A target with a colon (a file or a
	 * category) and the page's own title are left out.
	 */
	List<String> links(String json, String ownTitle)
	{
		final JsonObject root = object(json);
		final JsonObject parse = root == null ? null : child(root, "parse");
		final String text = parse == null ? null : string(parse, "wikitext");
		final Set<String> targets = new LinkedHashSet<>();
		if (text == null)
		{
			return new ArrayList<>();
		}
		int from = 0;
		while (true)
		{
			final int open = text.indexOf("[[", from);
			if (open < 0)
			{
				break;
			}
			final int close = text.indexOf("]]", open + 2);
			if (close < 0)
			{
				break;
			}
			final String inner = text.substring(open + 2, close);
			if (inner.contains("[["))
			{
				// Choice: a link inside another's caption is read on its own, from the inner opening.
				from = open + 2 + inner.lastIndexOf("[[");
				continue;
			}
			from = close + 2;
			final String target = target(inner);
			if (target != null && !target.equals(ownTitle))
			{
				targets.add(target);
			}
		}
		return new ArrayList<>(targets);
	}

	/** The titles of the pages that carry the monster template, in the answer's order. */
	List<String> monsterTitles(String json)
	{
		final List<String> titles = new ArrayList<>();
		for (JsonObject page : pages(json))
		{
			if (carriesTemplate(page))
			{
				titles.add(title(page));
			}
		}
		return titles;
	}

	/** One form per bucket row: page, name, the ids that are whole numbers, the first picture file or null. */
	List<WikiForm> forms(String json)
	{
		final List<WikiForm> forms = new ArrayList<>();
		final JsonObject root = object(json);
		final JsonElement rows = root == null ? null : root.get("bucket");
		if (rows == null || !rows.isJsonArray())
		{
			return forms;
		}
		for (JsonElement element : rows.getAsJsonArray())
		{
			if (!element.isJsonObject())
			{
				continue;
			}
			final JsonObject row = element.getAsJsonObject();
			final String page = string(row, "page_name");
			if (page == null)
			{
				continue;
			}
			final String name = string(row, "name");
			final List<Integer> ids = new ArrayList<>();
			for (String id : strings(row.get("id")))
			{
				try
				{
					ids.add(Integer.parseInt(id.trim()));
				}
				catch (NumberFormatException e)
				{
					// Choice: an id that is not a whole number is skipped, the rest of the row kept.
					continue;
				}
			}
			final List<String> images = strings(row.get("image"));
			forms.add(new WikiForm(page, name == null ? page : name, ids, images.isEmpty() ? null : images.get(0)));
		}
		return forms;
	}

	/**
	 * Each asked file title to the address of its small copy, only when that address is on the wiki's own host. A
	 * title the answer says it {@code normalized} is keyed by the title that was asked.
	 */
	Map<String, String> thumbnails(String json)
	{
		final Map<String, String> asked = new LinkedHashMap<>();
		final JsonObject root = object(json);
		final JsonObject query = root == null ? null : child(root, "query");
		final Map<String, String> askedBy = new LinkedHashMap<>();
		if (query != null && query.get("normalized") != null && query.get("normalized").isJsonArray())
		{
			for (JsonElement element : query.getAsJsonArray("normalized"))
			{
				if (element.isJsonObject())
				{
					final String from = string(element.getAsJsonObject(), "from");
					final String to = string(element.getAsJsonObject(), "to");
					if (from != null && to != null)
					{
						askedBy.put(to, from);
					}
				}
			}
		}
		for (JsonObject page : pages(json))
		{
			final String title = title(page);
			final JsonElement info = page.get("imageinfo");
			if (title == null || info == null || !info.isJsonArray() || info.getAsJsonArray().size() == 0
				|| !info.getAsJsonArray().get(0).isJsonObject())
			{
				continue;
			}
			final String thumb = string(info.getAsJsonArray().get(0).getAsJsonObject(), "thumburl");
			final HttpUrl url = thumb == null ? null : HttpUrl.parse(thumb);
			if (url != null && "https".equals(url.scheme()) && WikiUrls.HOST.equals(url.host()))
			{
				asked.put(askedBy.containsKey(title) ? askedBy.get(title) : title, thumb);
			}
		}
		return asked;
	}

	/** The cleaned target of a link's inside, or null when it is not a page of the main space or is empty. */
	private static String target(String inner)
	{
		String target = inner;
		final int bar = target.indexOf('|');
		if (bar >= 0)
		{
			target = target.substring(0, bar);
		}
		final int hash = target.indexOf('#');
		if (hash >= 0)
		{
			target = target.substring(0, hash);
		}
		target = target.replace('_', ' ').trim();
		if (target.isEmpty() || target.indexOf(':') >= 0)
		{
			return null;
		}
		return target.substring(0, 1).toUpperCase(Locale.ROOT) + target.substring(1);
	}

	private List<JsonObject> pages(String json)
	{
		final List<JsonObject> pages = new ArrayList<>();
		final JsonObject root = object(json);
		final JsonObject query = root == null ? null : child(root, "query");
		final JsonElement list = query == null ? null : query.get("pages");
		if (list != null && list.isJsonArray())
		{
			for (JsonElement element : list.getAsJsonArray())
			{
				if (element.isJsonObject())
				{
					pages.add(element.getAsJsonObject());
				}
			}
		}
		return pages;
	}

	private JsonObject object(String json)
	{
		final JsonElement root = gson.fromJson(json, JsonElement.class);
		return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
	}

	private static JsonObject child(JsonObject parent, String key)
	{
		final JsonElement element = parent.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private static boolean carriesTemplate(JsonObject page)
	{
		final JsonElement templates = page.get("templates");
		return templates != null && templates.isJsonArray() && templates.getAsJsonArray().size() > 0;
	}

	private static int indexOf(JsonObject page)
	{
		final JsonElement index = page.get("index");
		return index != null && index.isJsonPrimitive() && index.getAsJsonPrimitive().isNumber()
			? index.getAsInt() : Integer.MAX_VALUE;
	}

	private static String title(JsonObject page)
	{
		return string(page, "title");
	}

	private static String string(JsonObject object, String key)
	{
		final JsonElement element = object.get(key);
		return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
	}

	/** The strings of an array, or of a lone string; nothing for anything else. */
	private static List<String> strings(JsonElement element)
	{
		final List<String> out = new ArrayList<>();
		if (element == null)
		{
			return out;
		}
		if (element.isJsonPrimitive())
		{
			out.add(element.getAsString());
		}
		else if (element.isJsonArray())
		{
			final JsonArray array = element.getAsJsonArray();
			for (JsonElement item : array)
			{
				if (item.isJsonPrimitive())
				{
					out.add(item.getAsString());
				}
			}
		}
		return out;
	}
}
