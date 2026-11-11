package com.scenecustomizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** {@link WikiAnswers}: small answers in the shape of the real ones. */
public class WikiAnswersTest
{
	private final WikiAnswers answers = new WikiAnswers(new Gson());

	@Test
	public void suggestionsKeepMonstersInIndexOrder()
	{
		final String json = WikiFixtures.read("suggestions.json");
		assertEquals(Arrays.asList("Cow", "Cow calf"), answers.suggestions(json, 8));
		assertEquals(Collections.singletonList("Cow"), answers.suggestions(json, 1));
	}

	@Test
	public void linksFromThePagesOwnText()
	{
		assertEquals(Arrays.asList("Cow calf", "Bones", "Raw beef", "Dairy cow"),
			answers.links(WikiFixtures.read("wikitext-cow.json"), "Cow"));
	}

	@Test
	public void monsterTitlesAreThePagesWithTheTemplate()
	{
		assertEquals(Arrays.asList("Cow calf", "Dairy cow"), answers.monsterTitles(WikiFixtures.read("templates.json")));
	}

	@Test
	public void formsFromTheBucket()
	{
		final List<WikiForm> forms = answers.forms(WikiFixtures.read("bucket.json"));
		assertEquals(3, forms.size());
		assertEquals("Cow", forms.get(0).page());
		assertEquals(Arrays.asList(2790, 2791), forms.get(0).ids());
		assertEquals("File:Cow.png", forms.get(0).image());
		assertEquals("Cow", forms.get(1).name());
		assertEquals(Collections.singletonList(5842), forms.get(1).ids());
		assertNull(forms.get(1).image());
		assertEquals("Cow calf", forms.get(2).page());
		assertEquals(Collections.singletonList(2801), forms.get(2).ids());
	}

	@Test
	public void thumbnailsOnlyFromTheWikisHost()
	{
		final Map<String, String> found = answers.thumbnails(WikiFixtures.read("thumbnails.json"));
		assertEquals(Collections.singleton("File:Cow.png"), found.keySet());
		assertEquals("https://oldschool.runescape.wiki/images/thumb/Cow.png/64px-Cow.png", found.get("File:Cow.png"));

		final Map<String, String> normalized = answers.thumbnails(WikiFixtures.read("thumbnails-normalized.json"));
		assertEquals(Collections.singleton("File:Cow_calf.png"), normalized.keySet());
	}
}
