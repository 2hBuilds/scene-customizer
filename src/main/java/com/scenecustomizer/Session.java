package com.scenecustomizer;

/** A recording or a look-up: a title, a clock, a state and its Things. Used from one thread only. */
final class Session
{
	/** The two sorts of session. */
	enum SessionKind
	{
		/** Caught from the game. */
		RECORDING,
		/** Read from the wiki. */
		LOOKUP
	}

	/** Where a session stands. */
	enum SessionState
	{
		/** A recording is running. */
		RECORDING,
		/** Finished. */
		DONE,
		/** A look-up is waiting for the wiki. */
		LOADING,
		/** A look-up that did not work. */
		FAILED
	}

	private final SessionKind kind;
	private final String title;
	private final long startedEpochMs;
	private final ThingTable table = new ThingTable();
	private SessionState state;
	private long lengthMs;

	/** Choice: a recording starts RECORDING and a look-up starts LOADING. */
	Session(SessionKind kind, String title, long startedEpochMs)
	{
		this.kind = kind;
		this.title = title;
		this.startedEpochMs = startedEpochMs;
		this.state = kind == SessionKind.RECORDING ? SessionState.RECORDING : SessionState.LOADING;
	}

	/** The session's Things. */
	ThingTable table()
	{
		return table;
	}

	/** Ends a recording after the given length. */
	void stop(long lengthMs)
	{
		this.lengthMs = lengthMs;
		this.state = SessionState.DONE;
	}

	/** Marks a look-up as failed. */
	void fail()
	{
		this.state = SessionState.FAILED;
	}

	/** Marks a look-up as finished. */
	void done()
	{
		this.state = SessionState.DONE;
	}

	/** The kind. */
	SessionKind kind()
	{
		return kind;
	}

	/** A frozen copy. */
	SessionView view()
	{
		return new SessionView(kind, title, state, startedEpochMs, lengthMs, table);
	}
}
