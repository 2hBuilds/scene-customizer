package com.scenecustomizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Test;

/** {@link LookupFlow}, with a fake wiki and a fake executor that runs a task only when told. */
public class LookupFlowTest
{
	private final FakeWikiCalls calls = new FakeWikiCalls();
	private final List<Runnable> tasks = new ArrayList<>();
	private final List<LookupBarState> bar = new ArrayList<>();
	private final List<LookupResult> results = new ArrayList<>();
	private LookupFlow flow;

	@Before
	public void setUp()
	{
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		when(executor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(invocation ->
		{
			tasks.add(invocation.getArgument(0));
			return null;
		});
		flow = new LookupFlow(calls, executor, new WikiAnswers(new Gson()), bar::add, results::add);
	}

	private LookupBarState.State lastBar()
	{
		return bar.get(bar.size() - 1).state();
	}

	private LookupResult lastResult()
	{
		return results.get(results.size() - 1);
	}

	/** Answers every call the way the saved small answers do. */
	private static String cowWiki(HttpUrl url)
	{
		final String action = url.queryParameter("action");
		if ("parse".equals(action))
		{
			return WikiFixtures.read("wikitext-cow.json");
		}
		if ("bucket".equals(action))
		{
			return WikiFixtures.read("bucket.json");
		}
		if ("imageinfo".equals(url.queryParameter("prop")))
		{
			return WikiFixtures.read("thumbnails-cow.json");
		}
		return WikiFixtures.read("templates-cow.json");
	}

	@Test
	public void shortTextOrWikiOffAsksNothing()
	{
		flow.typed("co", true);
		assertEquals(LookupBarState.State.EMPTY, lastBar());
		flow.typed("cow", false);
		assertEquals(LookupBarState.State.OFF, lastBar());
		assertTrue(calls.asked.isEmpty());
		assertTrue(tasks.isEmpty());
	}

	@Test
	public void suggestionsAfterTheWait()
	{
		flow.typed("cow", true);
		assertTrue(calls.asked.isEmpty());
		assertEquals(1, tasks.size());
		tasks.get(0).run();
		assertEquals(LookupBarState.State.LOOKING, lastBar());
		assertEquals(1, calls.asked.size());
		assertEquals("prefixsearch", calls.asked.get(0).url.queryParameter("generator"));
		assertEquals("cow", calls.asked.get(0).url.queryParameter("gpssearch"));
		calls.asked.get(0).ok.accept(WikiFixtures.read("suggestions.json"));
		assertEquals(LookupBarState.State.SUGGESTIONS, lastBar());
		assertEquals(Arrays.asList("Cow", "Cow calf"), bar.get(bar.size() - 1).suggestions());

		flow.typed("cowh", true);
		tasks.get(1).run();
		calls.asked.get(1).ok.accept(WikiFixtures.read("suggestions-none.json"));
		assertEquals(LookupBarState.State.NONE, lastBar());

		flow.typed("cowhi", true);
		tasks.get(2).run();
		calls.asked.get(2).failed.run();
		assertEquals(LookupBarState.State.FAILED, lastBar());
	}

	@Test
	public void aNewKeyCancelsTheOldCall()
	{
		flow.typed("cow", true);
		tasks.get(0).run();
		final FakeWikiCalls.Asked first = calls.asked.get(0);
		final int shown = bar.size();
		flow.typed("cows", true);
		assertTrue(calls.cancelled.contains(first.tag));
		first.ok.accept(WikiFixtures.read("suggestions.json"));
		assertEquals(shown, bar.size());
		tasks.get(0).run();
		assertEquals(1, calls.asked.size());
	}

	@Test
	public void aPickBuildsTheList()
	{
		calls.auto = LookupFlowTest::cowWiki;
		flow.pick("Cow");
		assertEquals(LookupResult.State.LOADING, results.get(0).state());
		assertEquals("Cow", results.get(0).title());
		assertEquals(LookupResult.State.DONE, results.get(1).state());
		assertNull(results.get(1).thisMonster().get(0).picture());

		final LookupResult done = lastResult();
		assertEquals(LookupResult.State.DONE, done.state());
		assertEquals(1, done.thisMonster().size());
		assertEquals("Cow", done.thisMonster().get(0).name());
		assertEquals(Arrays.asList(2790, 2791, 5842), done.thisMonster().get(0).ids());
		assertEquals(1, done.linked().size());
		assertEquals("Cow calf", done.linked().get(0).name());
		assertEquals(Collections.singletonList(2801), done.linked().get(0).ids());
		assertNotNull(done.thisMonster().get(0).picture());
		assertNotNull(done.linked().get(0).picture());
	}

	@Test
	public void aNewPickDropsTheOldOnesAnswers()
	{
		flow.pick("Cow");
		final FakeWikiCalls.Asked cow = calls.asked.get(0);
		flow.pick("Goat");
		assertTrue(calls.cancelled.contains(cow.tag));
		final int asked = calls.asked.size();
		final int published = results.size();
		cow.ok.accept(WikiFixtures.read("wikitext-cow.json"));
		assertEquals(asked, calls.asked.size());
		assertEquals(published, results.size());
		assertEquals("Goat", lastResult().title());
	}

	@Test
	public void aFailureBeforeTheFormsFails()
	{
		flow.pick("Cow");
		calls.asked.get(0).failed.run();
		assertEquals(LookupResult.State.FAILED, lastResult().state());

		calls.auto = LookupFlowTest::cowWiki;
		calls.picturesFail = true;
		flow.pick("Cow");
		assertEquals(LookupResult.State.DONE, lastResult().state());
		assertEquals(1, lastResult().thisMonster().size());
		assertNull(lastResult().thisMonster().get(0).picture());
		assertNull(lastResult().linked().get(0).picture());
	}

	@Test
	public void aStaleWaitLeavesNoLooking()
	{
		flow.typed("cow", true);
		flow.typed("", true);
		assertEquals(LookupBarState.State.EMPTY, lastBar());
		final int shown = bar.size();
		tasks.get(0).run();
		assertTrue(calls.asked.isEmpty());
		assertEquals(shown, bar.size());
		assertEquals(LookupBarState.State.EMPTY, lastBar());
	}

	@Test
	public void stopCancelsThePickAndPublishesNothingMore()
	{
		flow.pick("Cow");
		final FakeWikiCalls.Asked cow = calls.asked.get(0);
		flow.stop();
		assertTrue(calls.cancelled.contains(cow.tag));
		final int asked = calls.asked.size();
		final int published = results.size();
		cow.ok.accept(WikiFixtures.read("wikitext-cow.json"));
		assertEquals(asked, calls.asked.size());
		assertEquals(published, results.size());
	}

	@Test
	public void resultsCarryTheirRunsGeneration()
	{
		calls.auto = LookupFlowTest::cowWiki;
		flow.pick("Cow");
		final int cowGeneration = lastResult().generation();
		final int before = results.size();
		flow.pick("Goat");
		assertTrue(results.size() > before);
		final int goatGeneration = results.get(before).generation();
		assertTrue(goatGeneration != cowGeneration);
		for (int i = before; i < results.size(); i++)
		{
			assertEquals("Goat", results.get(i).title());
			assertEquals(goatGeneration, results.get(i).generation());
		}
	}
}
