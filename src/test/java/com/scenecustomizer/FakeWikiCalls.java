package com.scenecustomizer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import okhttp3.HttpUrl;

/**
 * A stand-in for the wiki connection. It writes down every call. With {@link #auto} set it answers at once (a null
 * body means the call failed); without, each call waits until the test answers it by hand.
 */
final class FakeWikiCalls implements WikiCalls
{
	/** One call that was asked. */
	static final class Asked
	{
		HttpUrl url;
		Object tag;
		Consumer<String> ok;
		Runnable failed;
		Consumer<BufferedImage> pictureOk;
	}

	final List<Asked> asked = new ArrayList<>();
	final List<Object> cancelled = new ArrayList<>();
	Function<HttpUrl, String> auto;
	boolean picturesFail;

	@Override
	public void text(HttpUrl url, Object tag, Consumer<String> ok, Runnable failed)
	{
		final Asked call = new Asked();
		call.url = url;
		call.tag = tag;
		call.ok = ok;
		call.failed = failed;
		asked.add(call);
		if (auto != null)
		{
			final String body = auto.apply(url);
			if (body == null)
			{
				failed.run();
			}
			else
			{
				ok.accept(body);
			}
		}
	}

	@Override
	public void picture(HttpUrl url, Object tag, Consumer<BufferedImage> ok, Runnable failed)
	{
		final Asked call = new Asked();
		call.url = url;
		call.tag = tag;
		call.failed = failed;
		call.pictureOk = ok;
		asked.add(call);
		if (auto != null)
		{
			if (picturesFail)
			{
				failed.run();
			}
			else
			{
				ok.accept(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
			}
		}
	}

	@Override
	public void cancel(Object tag)
	{
		cancelled.add(tag);
	}
}
