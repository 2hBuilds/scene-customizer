package com.scenecustomizer;

/** The words of the card's status line: the recording's header and the live count. */
final class RecordingText
{
	private RecordingText()
	{
	}

	/** The first line: title, length in whole minutes (under 1 min below one), count of Things. */
	static String header(SessionView view)
	{
		final long minutes = view.lengthMs() / 60000L;
		final String length = minutes < 1 ? "under 1 min" : minutes + " min";
		return view.title() + " - " + length + " - " + view.thingCount() + " things";
	}

	/** The line shown while recording: minutes:seconds and the Thing count. */
	static String live(long elapsedMs, int things)
	{
		final long seconds = Math.max(0L, elapsedMs) / 1000L;
		return String.format("Recording %d:%02d - %d things", seconds / 60, seconds % 60, things);
	}
}
