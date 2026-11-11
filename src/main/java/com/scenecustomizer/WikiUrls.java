package com.scenecustomizer;

import java.util.List;
import okhttp3.HttpUrl;

/**
 * The five addresses the look-up asks the OSRS wiki: suggestions for the typed text, a page's own text, which titles
 * are monsters, the monsters' ids and picture files, and the pictures' small copies. Pure: it only builds addresses.
 * Every one is on {@link #HOST}, on {@code /api.php}, and asks for JSON in the {@code formatversion=2} shape.
 */
final class WikiUrls
{
	/** The one host the look-up talks to. */
	static final String HOST = "oldschool.runescape.wiki";
	/** The most titles one monster-template call takes. */
	static final int MOST_MONSTER_TITLES = 50;
	/** The most titles one forms call takes. */
	static final int MOST_FORM_TITLES = 20;
	/** The most files one thumbnails call takes. */
	static final int MOST_FILES = 50;
	private static final String MONSTER_TEMPLATE = "Template:Infobox Monster";

	private WikiUrls()
	{
	}

	/** Pages whose title starts with the text, with a note of which of them carry the monster template. */
	static HttpUrl suggestions(String text)
	{
		return api("query")
			.addQueryParameter("generator", "prefixsearch")
			.addQueryParameter("gpssearch", text)
			.addQueryParameter("gpslimit", "50")
			.addQueryParameter("prop", "templates")
			.addQueryParameter("tltemplates", MONSTER_TEMPLATE)
			.addQueryParameter("tllimit", "max")
			.build();
	}

	/** A page's own wikitext. */
	static HttpUrl wikitext(String title)
	{
		return api("parse")
			.addQueryParameter("page", title)
			.addQueryParameter("prop", "wikitext")
			.build();
	}

	/** Which of up to 50 titles carry the monster template (redirects followed). */
	static HttpUrl monsterTemplates(List<String> titles)
	{
		return api("query")
			.addQueryParameter("titles", String.join("|", titles))
			.addQueryParameter("redirects", "1")
			.addQueryParameter("prop", "templates")
			.addQueryParameter("tltemplates", MONSTER_TEMPLATE)
			.addQueryParameter("tllimit", "max")
			.build();
	}

	/** The monster infobox rows (page, name, ids, picture files) of up to 20 pages, from the wiki's bucket. */
	static HttpUrl forms(List<String> titles)
	{
		final StringBuilder conditions = new StringBuilder();
		for (int i = 0; i < titles.size(); i++)
		{
			if (i > 0)
			{
				conditions.append(',');
			}
			conditions.append("{'page_name','").append(escape(titles.get(i))).append("'}");
		}
		final String query = "bucket('infobox_monster').select('page_name','name','id','image').where(bucket.Or("
			+ conditions + ")).run()";
		return api("bucket").addQueryParameter("query", query).build();
	}

	/** The 64 px copies of up to 50 picture files. */
	static HttpUrl thumbnails(List<String> files)
	{
		return api("query")
			.addQueryParameter("titles", String.join("|", files))
			.addQueryParameter("prop", "imageinfo")
			.addQueryParameter("iiprop", "url|size")
			.addQueryParameter("iiurlwidth", "64")
			.build();
	}

	/** A backslash and a quote get a backslash in front, as the bucket's text needs. */
	private static String escape(String title)
	{
		return title.replace("\\", "\\\\").replace("'", "\\'");
	}

	private static HttpUrl.Builder api(String action)
	{
		return new HttpUrl.Builder()
			.scheme("https")
			.host(HOST)
			.addPathSegments("api.php")
			.addQueryParameter("action", action)
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2");
	}
}
