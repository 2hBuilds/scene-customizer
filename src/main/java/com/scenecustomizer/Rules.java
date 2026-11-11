package com.scenecustomizer;

import com.google.gson.Gson;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.runelite.client.config.ConfigManager;

/**
 * The plugin's rules: a {@link RuleStore} read from the config key {@code rules} when {@link #load} runs, written
 * back after every change. Choice: the methods are synchronized (the Swing thread writes, the client thread reads);
 * listeners run after the write, outside the lock.
 */
final class Rules implements RuleEditor
{
	/** The config key. */
	static final String KEY = "rules";

	private final ConfigManager configManager;
	private final Gson gson;
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
	private RuleStore store;

	Rules(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
		this.store = new RuleStore(gson);
	}

	/** Reads the stored rules; bad or missing JSON gives none. */
	synchronized void load()
	{
		store = RuleStore.fromJson(gson, configManager.getConfiguration(SceneCustomizerConfig.GROUP, KEY));
	}

	@Override
	public synchronized Rule get(Kind kind, String name)
	{
		return store.get(kind, name);
	}

	@Override
	public void put(Rule rule)
	{
		synchronized (this)
		{
			store.put(rule);
			save();
		}
		changed();
	}

	@Override
	public void remove(Kind kind, String name)
	{
		synchronized (this)
		{
			store.remove(kind, name);
			save();
		}
		changed();
	}

	@Override
	public void clear()
	{
		synchronized (this)
		{
			store = new RuleStore(gson);
			save();
		}
		changed();
	}

	@Override
	public void addListener(Runnable onChange)
	{
		listeners.add(onChange);
	}

	@Override
	public synchronized List<Rule> all()
	{
		return store.all();
	}

	private void save()
	{
		configManager.setConfiguration(SceneCustomizerConfig.GROUP, KEY, store.toJson());
	}

	private void changed()
	{
		for (Runnable listener : listeners)
		{
			listener.run();
		}
	}
}
