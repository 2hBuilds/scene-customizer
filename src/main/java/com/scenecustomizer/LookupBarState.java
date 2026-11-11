package com.scenecustomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** What the look-up bar should show: one of six states and, for {@link State#SUGGESTIONS}, the suggested titles. */
final class LookupBarState
{
	/** The six things the bar can be doing. */
	enum State
	{
		/** The wiki look-up is switched off in the settings menu. */
		OFF,
		/** Nothing asked: the text is under three characters. */
		EMPTY,
		/** A suggestions call is waiting for its answer. */
		LOOKING,
		/** Up to eight suggested titles are listed. */
		SUGGESTIONS,
		/** The wiki knows no monster by that name. */
		NONE,
		/** The wiki did not answer. */
		FAILED
	}

	private final State state;
	private final List<String> suggestions;

	private LookupBarState(State state, List<String> suggestions)
	{
		this.state = state;
		this.suggestions = Collections.unmodifiableList(new ArrayList<>(suggestions));
	}

	/** A state that carries no titles. */
	static LookupBarState of(State state)
	{
		return new LookupBarState(state, Collections.<String>emptyList());
	}

	/** The suggestions state with its titles. */
	static LookupBarState suggestions(List<String> titles)
	{
		return new LookupBarState(State.SUGGESTIONS, titles);
	}

	/** The state. */
	State state()
	{
		return state;
	}

	/** The suggested titles, empty except in the suggestions state. */
	List<String> suggestions()
	{
		return suggestions;
	}
}
