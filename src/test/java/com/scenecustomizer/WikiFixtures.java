package com.scenecustomizer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Reads the small saved wiki answers in {@code src/test/resources/com/scenecustomizer/wiki/}. */
final class WikiFixtures
{
	private WikiFixtures()
	{
	}

	/** The text of one saved answer. */
	static String read(String name)
	{
		try (InputStream in = WikiFixtures.class.getResourceAsStream("/com/scenecustomizer/wiki/" + name))
		{
			if (in == null)
			{
				throw new IllegalStateException("no saved answer " + name);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
