package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What a look-up has found so far: the picked title, how far it is, the monsters of the title's own page and the
 * monsters linked from it, and the generation of the look-up run it belongs to (a later run has a higher one, so a
 * late result of an old run can be told apart). It is published again as each picture arrives. Immutable.
 */
final class LookupResult
{
	/** How far the look-up is. */
	enum State
	{
		/** The wiki has not given the monsters yet. */
		LOADING,
		/** The monsters are in; pictures may still be coming. */
		DONE,
		/** The wiki did not answer before the monsters were in. */
		FAILED
	}

	private final int generation;
	private final String title;
	private final State state;
	private final List<WikiMonster> thisMonster;
	private final List<WikiMonster> linked;

	LookupResult(int generation, String title, State state, List<WikiMonster> thisMonster, List<WikiMonster> linked)
	{
		this.generation = generation;
		this.title = title;
		this.state = state;
		this.thisMonster = Collections.unmodifiableList(new ArrayList<>(thisMonster));
		this.linked = Collections.unmodifiableList(new ArrayList<>(linked));
	}

	/** The generation of the look-up run this result belongs to. */
	int generation()
	{
		return generation;
	}

	/** The picked page's title. */
	String title()
	{
		return title;
	}

	/** How far the look-up is. */
	State state()
	{
		return state;
	}

	/** The monsters of the title's own page, one per name. */
	List<WikiMonster> thisMonster()
	{
		return thisMonster;
	}

	/** The monsters linked from the page, one per linked page. */
	List<WikiMonster> linked()
	{
		return linked;
	}
}
