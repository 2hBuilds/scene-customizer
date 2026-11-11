package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/** A monster the wiki lists: a name, its ids in order, the picture file the wiki names, and its picture once it came. */
final class WikiMonster
{
	private final String name;
	private final List<Integer> ids;
	private final String image;
	private final BufferedImage picture;

	WikiMonster(String name, List<Integer> ids, String image, BufferedImage picture)
	{
		this.name = name;
		this.ids = Collections.unmodifiableList(ids);
		this.image = image;
		this.picture = picture;
	}

	/** The wiki's name for it. */
	String name()
	{
		return name;
	}

	/** The ids, merged in the order the wiki gave them. */
	List<Integer> ids()
	{
		return ids;
	}

	/** The picture file's title, or null. */
	String image()
	{
		return image;
	}

	/** The picture, or null while it has not come. */
	BufferedImage picture()
	{
		return picture;
	}

	/** The same monster with a picture. */
	WikiMonster withPicture(BufferedImage newPicture)
	{
		return new WikiMonster(name, ids, image, newPicture);
	}
}
