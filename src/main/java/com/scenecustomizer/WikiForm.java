package com.scenecustomizer;

import java.util.Collections;
import java.util.List;

/** One row of the wiki's monster infobox: the page it is on, the name, the ids that parse, the first picture file. */
final class WikiForm
{
	private final String page;
	private final String name;
	private final List<Integer> ids;
	private final String image;

	WikiForm(String page, String name, List<Integer> ids, String image)
	{
		this.page = page;
		this.name = name;
		this.ids = Collections.unmodifiableList(ids);
		this.image = image;
	}

	/** The wiki page's title. */
	String page()
	{
		return page;
	}

	/** The name the wiki gives this form. */
	String name()
	{
		return name;
	}

	/** The ids, in the answer's order. */
	List<Integer> ids()
	{
		return ids;
	}

	/** The first picture file, or null. */
	String image()
	{
		return image;
	}
}
