package com.scenecustomizer;

import java.util.List;

/** What the sidebar needs of the player's rules: read one, save one, take one away, hear of changes. */
interface RuleEditor
{
	/** The rule of that kind and exact name, or null. */
	Rule get(Kind kind, String name);

	/** Saves the rule; a rule that does nothing removes the stored one instead. */
	void put(Rule rule);

	/** Removes the rule of that kind and name, if any. */
	void remove(Kind kind, String name);

	/** Removes every rule with one write; the listeners run once. */
	void clear();

	/** Every rule, newest change first. Choice: the Changes list needs it, so it is on the editor. */
	List<Rule> all();

	/** Runs the listener after every change. */
	void addListener(Runnable onChange);
}
