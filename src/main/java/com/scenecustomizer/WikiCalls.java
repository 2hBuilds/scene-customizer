package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import okhttp3.HttpUrl;

/**
 * What the look-up needs from the internet, so a test can stand in for it. Every call is asked in the background and
 * answered later, on any thread; a call carries a tag so that all the calls of one look-up can be cancelled at once.
 */
interface WikiCalls
{
	/** Asks for text; {@code ok} gets the body, {@code failed} runs when there is none. */
	void text(HttpUrl url, Object tag, Consumer<String> ok, Runnable failed);

	/** Asks for a picture, already scaled to fit 36 x 32; {@code failed} runs when there is none. */
	void picture(HttpUrl url, Object tag, Consumer<BufferedImage> ok, Runnable failed);

	/** Cancels every call of the tag that is still waiting. */
	void cancel(Object tag);
}
