package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The pictures already drawn, kept by the kind and id of the thing ({@code "npc:" + id}, {@code "object:" + id}) so a
 * thing drawn once shows its picture at once in every later recording and look-up. The newest 1,000 are kept; the
 * oldest go first. Choice: every method is synchronized, so any thread may use it.
 */
public final class PictureMemory
{
	/** How many pictures are kept. */
	public static final int MOST = 1000;

	private final Map<String, BufferedImage> pictures = new LinkedHashMap<>();

	/** Keeps the picture under the id; an id already held is replaced and counts as the newest. */
	public synchronized void put(String id, BufferedImage picture)
	{
		if (id == null || picture == null)
		{
			return;
		}
		pictures.remove(id);
		pictures.put(id, picture);
		while (pictures.size() > MOST)
		{
			pictures.remove(pictures.keySet().iterator().next());
		}
	}

	/** The picture under the id; null when there is none. */
	public synchronized BufferedImage get(String id)
	{
		return pictures.get(id);
	}

	/** Forgets every picture. */
	public synchronized void clear()
	{
		pictures.clear();
	}
}
