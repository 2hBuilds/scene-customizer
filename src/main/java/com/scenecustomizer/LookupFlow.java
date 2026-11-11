package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import okhttp3.HttpUrl;

/**
 * The look-up's brain, with no Swing and no client in it. {@link #typed} turns the bar's text into a suggestions call
 * 350 ms after the last key; {@link #pick} runs the chain page text, links, monster titles, forms, thumbnails and
 * pictures, and publishes a {@link LookupResult} when it starts, when the forms are in, and as each picture arrives.
 * Nothing here waits: the wait is a task on the injected executor, the calls are answered later on other threads, and
 * every state change is guarded by one lock that is never held while a call is made or a result is published.
 *
 * <p>Cancelling: each burst of typing and each pick has a generation number and a tag. A new key cancels the waiting
 * task and the suggestions call in flight; a new pick cancels the old pick's calls and the suggestions; an answer of
 * an old generation is dropped on arrival. A bar state or a result is also published only if its generation is still
 * the current one at the moment of publishing (checked under the lock; the publish itself runs outside it).
 *
 * <p>Choice: a pick also cancels the suggestions, since the bar clears its list. Choice: a linked monster takes the
 * name of its page's first form, and its picture file is the first one any of its forms names; a This monster row's
 * is the first one of its own forms. Choice: a thumbnails call that fails, like a picture that fails, only leaves
 * pictures out. Choice: a page with no links goes straight to the forms of its own title.
 */
final class LookupFlow
{
	/** The wait after the last key, in ms. */
	static final long WAIT_MS = 350L;
	/** The fewest characters that are asked about. */
	static final int MIN_CHARS = 3;
	/** The most suggestions listed. */
	static final int MOST_SUGGESTIONS = 8;

	private final WikiCalls calls;
	private final ScheduledExecutorService executor;
	private final WikiAnswers answers;
	private final Consumer<LookupBarState> bar;
	private final Consumer<LookupResult> list;
	private final Object lock = new Object();
	private int typedGeneration;
	private ScheduledFuture<?> waiting;
	private Object suggestTag;
	private int pickGeneration;
	private Object pickTag;

	/**
	 * @param calls where the wiki is asked
	 * @param executor RuneLite's injected executor, for the 350 ms wait
	 * @param answers reads the answers
	 * @param bar told what the bar should show (called from any thread)
	 * @param list told the look-up's result (called from any thread)
	 */
	LookupFlow(WikiCalls calls, ScheduledExecutorService executor, WikiAnswers answers, Consumer<LookupBarState> bar,
		Consumer<LookupResult> list)
	{
		this.calls = calls;
		this.executor = executor;
		this.answers = answers;
		this.bar = bar;
		this.list = list;
	}

	/** The bar's text changed: ask for suggestions after the wait, or say why not. */
	void typed(String text, boolean wikiOn)
	{
		final int generation = cancelSuggestions();
		if (!wikiOn)
		{
			showBar(generation, LookupBarState.of(LookupBarState.State.OFF));
			return;
		}
		final String trimmed = text == null ? "" : text.trim();
		if (trimmed.length() < MIN_CHARS)
		{
			showBar(generation, LookupBarState.of(LookupBarState.State.EMPTY));
			return;
		}
		final ScheduledFuture<?> task = executor.schedule(() -> ask(generation, trimmed), WAIT_MS, TimeUnit.MILLISECONDS);
		synchronized (lock)
		{
			if (generation == typedGeneration)
			{
				waiting = task;
			}
		}
	}

	/** A suggestion was picked: start a new look-up of the title. */
	void pick(String title)
	{
		final Run run;
		final Object stale;
		synchronized (lock)
		{
			stale = pickTag;
			run = new Run(++pickGeneration, new Object(), title);
			pickTag = run.tag;
		}
		cancelSuggestions();
		if (stale != null)
		{
			calls.cancel(stale);
		}
		publish(run, LookupResult.State.LOADING);
		calls.text(WikiUrls.wikitext(title), run.tag, body -> gotText(run, body), () -> fail(run));
	}

	/** Stops everything: no wait, no call, and no later answer changes anything. */
	void stop()
	{
		final Object stale;
		synchronized (lock)
		{
			stale = pickTag;
			pickTag = null;
			pickGeneration++;
		}
		cancelSuggestions();
		if (stale != null)
		{
			calls.cancel(stale);
		}
	}

	/**
	 * Drops the waiting task and the suggestions call in flight, and makes their answers stale. Gives back the new
	 * typing generation.
	 */
	private int cancelSuggestions()
	{
		final ScheduledFuture<?> task;
		final Object stale;
		final int generation;
		synchronized (lock)
		{
			generation = ++typedGeneration;
			task = waiting;
			waiting = null;
			stale = suggestTag;
			suggestTag = null;
		}
		if (task != null)
		{
			task.cancel(false);
		}
		if (stale != null)
		{
			calls.cancel(stale);
		}
		return generation;
	}

	/** Tells the bar, unless a newer key or pick came since the generation. */
	private void showBar(int generation, LookupBarState state)
	{
		if (currentTyping(generation))
		{
			bar.accept(state);
		}
	}

	/** The wait is over: ask, unless a newer key came. */
	private void ask(int generation, String text)
	{
		final Object tag = new Object();
		synchronized (lock)
		{
			if (generation != typedGeneration)
			{
				return;
			}
			waiting = null;
			suggestTag = tag;
		}
		showBar(generation, LookupBarState.of(LookupBarState.State.LOOKING));
		calls.text(WikiUrls.suggestions(text), tag, body -> suggested(generation, body), () -> suggestionsFailed(generation));
	}

	private void suggested(int generation, String body)
	{
		if (!currentTyping(generation))
		{
			return;
		}
		final List<String> titles;
		try
		{
			titles = answers.suggestions(body, MOST_SUGGESTIONS);
		}
		catch (RuntimeException e)
		{
			suggestionsFailed(generation);
			return;
		}
		showBar(generation,
			titles.isEmpty() ? LookupBarState.of(LookupBarState.State.NONE) : LookupBarState.suggestions(titles));
	}

	private void suggestionsFailed(int generation)
	{
		showBar(generation, LookupBarState.of(LookupBarState.State.FAILED));
	}

	private boolean currentTyping(int generation)
	{
		synchronized (lock)
		{
			return generation == typedGeneration;
		}
	}

	/** One look-up in progress. Its fields are guarded by the flow's lock. */
	private final class Run
	{
		final int generation;
		final Object tag;
		final String title;
		List<WikiMonster> thisMonster = new ArrayList<>();
		List<WikiMonster> linked = new ArrayList<>();
		boolean formsIn;
		boolean failed;
		final Map<String, String> addressOfFile = new HashMap<>();
		final Set<String> pictureAddressesAsked = new HashSet<>();

		Run(int generation, Object tag, String title)
		{
			this.generation = generation;
			this.tag = tag;
			this.title = title;
		}
	}

	private boolean alive(Run run)
	{
		synchronized (lock)
		{
			return run.generation == pickGeneration && !run.failed;
		}
	}

	/** Publishes what the run has now. */
	private void publish(Run run, LookupResult.State state)
	{
		final LookupResult result;
		synchronized (lock)
		{
			if (run.generation != pickGeneration)
			{
				return;
			}
			result = new LookupResult(run.generation, run.title, state, run.thisMonster, run.linked);
		}
		list.accept(result);
	}

	/** The run failed before the forms were in: say so once. */
	private void fail(Run run)
	{
		synchronized (lock)
		{
			if (run.generation != pickGeneration || run.failed || run.formsIn)
			{
				return;
			}
			run.failed = true;
		}
		publish(run, LookupResult.State.FAILED);
	}

	private void gotText(Run run, String body)
	{
		if (!alive(run))
		{
			return;
		}
		final List<String> links;
		try
		{
			links = answers.links(body, run.title);
		}
		catch (RuntimeException e)
		{
			fail(run);
			return;
		}
		final List<HttpUrl> urls = new ArrayList<>();
		for (List<String> batch : batches(links, WikiUrls.MOST_MONSTER_TITLES))
		{
			urls.add(WikiUrls.monsterTemplates(batch));
		}
		askAll(run, urls, bodies -> gotTemplates(run, bodies));
	}

	private void gotTemplates(Run run, List<String> bodies)
	{
		final Set<String> monsters = new LinkedHashSet<>();
		try
		{
			for (String body : bodies)
			{
				monsters.addAll(answers.monsterTitles(body));
			}
		}
		catch (RuntimeException e)
		{
			fail(run);
			return;
		}
		monsters.remove(run.title);
		final List<String> monsterPages = new ArrayList<>(monsters);
		final List<String> pages = new ArrayList<>();
		pages.add(run.title);
		pages.addAll(monsterPages);
		final List<HttpUrl> urls = new ArrayList<>();
		for (List<String> batch : batches(pages, WikiUrls.MOST_FORM_TITLES))
		{
			urls.add(WikiUrls.forms(batch));
		}
		askAll(run, urls, formBodies -> gotForms(run, monsterPages, formBodies));
	}

	private void gotForms(Run run, List<String> monsterPages, List<String> bodies)
	{
		final List<WikiForm> forms = new ArrayList<>();
		try
		{
			for (String body : bodies)
			{
				forms.addAll(answers.forms(body));
			}
		}
		catch (RuntimeException e)
		{
			fail(run);
			return;
		}
		final List<WikiMonster> own = ownMonsters(forms, run.title);
		final List<WikiMonster> linkedMonsters = new ArrayList<>();
		for (String page : monsterPages)
		{
			final WikiMonster monster = pageMonster(forms, page);
			if (monster != null)
			{
				linkedMonsters.add(monster);
			}
		}
		synchronized (lock)
		{
			if (run.generation != pickGeneration || run.failed)
			{
				return;
			}
			run.thisMonster = own;
			run.linked = linkedMonsters;
			run.formsIn = true;
		}
		publish(run, LookupResult.State.DONE);
		askPictures(run);
	}

	/** The forms of the title's own page, grouped by name with their ids merged in order. */
	private static List<WikiMonster> ownMonsters(List<WikiForm> forms, String title)
	{
		final Map<String, List<WikiForm>> byName = new LinkedHashMap<>();
		for (WikiForm form : forms)
		{
			if (form.page().equals(title))
			{
				byName.computeIfAbsent(form.name(), n -> new ArrayList<>()).add(form);
			}
		}
		final List<WikiMonster> monsters = new ArrayList<>();
		for (Map.Entry<String, List<WikiForm>> entry : byName.entrySet())
		{
			monsters.add(merged(entry.getKey(), entry.getValue()));
		}
		return monsters;
	}

	/** One monster for a linked page: the page's first form's name and all its forms' ids; null when it has no form. */
	private static WikiMonster pageMonster(List<WikiForm> forms, String page)
	{
		final List<WikiForm> ofPage = new ArrayList<>();
		for (WikiForm form : forms)
		{
			if (form.page().equals(page))
			{
				ofPage.add(form);
			}
		}
		return ofPage.isEmpty() ? null : merged(ofPage.get(0).name(), ofPage);
	}

	private static WikiMonster merged(String name, List<WikiForm> forms)
	{
		final Set<Integer> ids = new LinkedHashSet<>();
		String image = null;
		for (WikiForm form : forms)
		{
			ids.addAll(form.ids());
			if (image == null)
			{
				image = form.image();
			}
		}
		return new WikiMonster(name, new ArrayList<>(ids), image, null);
	}

	/** Asks for the small copies of every picture file, then for each distinct picture. */
	private void askPictures(Run run)
	{
		final Set<String> files = new LinkedHashSet<>();
		synchronized (lock)
		{
			for (WikiMonster m : run.thisMonster)
			{
				addFile(files, m);
			}
			for (WikiMonster m : run.linked)
			{
				addFile(files, m);
			}
		}
		for (List<String> batch : batches(new ArrayList<>(files), WikiUrls.MOST_FILES))
		{
			calls.text(WikiUrls.thumbnails(batch), run.tag, body -> gotThumbnails(run, body), () ->
			{
			});
		}
	}

	private static void addFile(Set<String> files, WikiMonster monster)
	{
		if (monster.image() != null)
		{
			files.add(monster.image());
		}
	}

	private void gotThumbnails(Run run, String body)
	{
		if (!alive(run))
		{
			return;
		}
		final Map<String, String> addresses;
		try
		{
			addresses = answers.thumbnails(body);
		}
		catch (RuntimeException e)
		{
			return;
		}
		final List<HttpUrl> toFetch = new ArrayList<>();
		synchronized (lock)
		{
			for (Map.Entry<String, String> entry : addresses.entrySet())
			{
				run.addressOfFile.put(entry.getKey(), entry.getValue());
				final HttpUrl url = HttpUrl.parse(entry.getValue());
				if (url != null && run.pictureAddressesAsked.add(entry.getValue()))
				{
					toFetch.add(url);
				}
			}
		}
		for (HttpUrl url : toFetch)
		{
			final String address = url.toString();
			calls.picture(url, run.tag, picture -> gotPicture(run, address, picture), () ->
			{
			});
		}
	}

	/** A picture came: every monster whose file has that address gets it, and the result is published again. */
	private void gotPicture(Run run, String address, BufferedImage picture)
	{
		synchronized (lock)
		{
			if (run.generation != pickGeneration || run.failed)
			{
				return;
			}
			run.thisMonster = withPicture(run, run.thisMonster, address, picture);
			run.linked = withPicture(run, run.linked, address, picture);
		}
		publish(run, LookupResult.State.DONE);
	}

	/** The caller holds the lock. */
	private static List<WikiMonster> withPicture(Run run, List<WikiMonster> monsters, String address, BufferedImage picture)
	{
		final List<WikiMonster> out = new ArrayList<>();
		for (WikiMonster m : monsters)
		{
			final String mine = m.image() == null ? null : run.addressOfFile.get(m.image());
			out.add(address.equals(mine) && m.picture() == null ? m.withPicture(picture) : m);
		}
		return out;
	}

	/**
	 * Asks every address in the background and, when all the answers are in, hands the bodies over in the order asked.
	 * One failure fails the run and the hand-over never happens. With nothing to ask the hand-over is at once.
	 */
	private void askAll(Run run, List<HttpUrl> urls, Consumer<List<String>> whenAll)
	{
		if (urls.isEmpty())
		{
			whenAll.accept(Collections.<String>emptyList());
			return;
		}
		final String[] bodies = new String[urls.size()];
		final AtomicInteger pending = new AtomicInteger(urls.size());
		for (int i = 0; i < urls.size(); i++)
		{
			final int at = i;
			calls.text(urls.get(i), run.tag, body ->
			{
				if (!alive(run))
				{
					return;
				}
				bodies[at] = body;
				if (pending.decrementAndGet() == 0)
				{
					whenAll.accept(Arrays.asList(bodies));
				}
			}, () -> fail(run));
		}
	}

	/** The titles split into groups of at most {@code size}, in order. */
	private static List<List<String>> batches(List<String> titles, int size)
	{
		final List<List<String>> out = new ArrayList<>();
		for (int i = 0; i < titles.size(); i += size)
		{
			out.add(new ArrayList<>(titles.subList(i, Math.min(titles.size(), i + size))));
		}
		return out;
	}
}
