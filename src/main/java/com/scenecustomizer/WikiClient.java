package com.scenecustomizer;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.ImageIcon;
import net.runelite.client.util.ImageUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The look-up's connection to the OSRS wiki, through RuneLite's own HTTP client handed in. Every call is asked in the
 * background ({@code enqueue}, never {@code execute}), carries the plugin's User-Agent and nothing about the player,
 * and goes only to {@link WikiUrls#HOST} over https. At most 10 calls wait and 60 start in a minute ({@link
 * CallBudget}); a call over the budget waits its turn first in, first out ({@link CallQueue}) and is refused only when
 * 100 already wait. A cancelled tag is remembered ({@link CancelledTags}): a call asked under it never starts. Text answers are kept 30 minutes ({@link AnswerCache}); a picture
 * is read up to 1 MB, decoded with the AWT toolkit and scaled down to fit 36 x 32. The callbacks run on the HTTP
 * client's threads.
 *
 * <p>Choice: a call that fails is never retried; one warning is logged the first time and the rest go to the debug
 * log. Choice: a text answer is read up to 1 MB too, and a longer one counts as a failure.
 */
final class WikiClient implements WikiCalls
{
	private static final Logger log = LoggerFactory.getLogger(WikiClient.class);

	/** The User-Agent every call carries. */
	static final String USER_AGENT = "2h NPC & Object Customizer (RuneLite plugin; github.com/2hBuilds/scene-customizer)";
	private static final long MAX_BYTES = 1_000_000L;

	private final OkHttpClient http;
	/** Guards everything below. */
	private final Object lock = new Object();
	private final ScheduledExecutorService executor;
	private final CallQueue queue = new CallQueue(System::nanoTime);
	private final CancelledTags cancelled = new CancelledTags();
	private boolean drainPending;
	private final AnswerCache cache = new AnswerCache();
	private final Map<Object, Set<Call>> byTag = new HashMap<>();
	private boolean warned;

	/**
	 * @param http RuneLite's own HTTP client
	 * @param executor RuneLite's injected executor, for the drain when only the minute cap holds calls back
	 */
	WikiClient(OkHttpClient http, ScheduledExecutorService executor)
	{
		this.http = http;
		this.executor = executor;
	}

	@Override
	public void text(HttpUrl url, Object tag, Consumer<String> ok, Runnable failed)
	{
		final String key = url.toString();
		final String kept;
		synchronized (lock)
		{
			if (cancelled.contains(tag))
			{
				return;
			}
			kept = cache.get(key, System.nanoTime());
		}
		if (kept != null)
		{
			ok.accept(kept);
			return;
		}
		ask(url, tag, failed, response ->
		{
			final String body = new String(readCapped(response), StandardCharsets.UTF_8);
			synchronized (lock)
			{
				cache.put(key, body, System.nanoTime());
			}
			return () -> ok.accept(body);
		});
	}

	@Override
	public void picture(HttpUrl url, Object tag, Consumer<BufferedImage> ok, Runnable failed)
	{
		ask(url, tag, failed, response ->
		{
			final BufferedImage picture = decode(readCapped(response));
			if (picture == null)
			{
				throw new IOException("the picture could not be decoded");
			}
			final BufferedImage fitted = fit(picture);
			return () -> ok.accept(fitted);
		});
	}

	@Override
	public void cancel(Object tag)
	{
		final Set<Call> calls;
		synchronized (lock)
		{
			cancelled.remember(tag);
			calls = byTag.remove(tag);
		}
		queue.cancel(tag);
		if (calls != null)
		{
			for (Call call : calls)
			{
				call.cancel();
			}
		}
	}

	/**
	 * Reads a 200 answer and gives back what to do with it; it may throw {@link IOException}, which counts as a failed
	 * call. What it gives back runs after the answer is closed, and a fault in it is not taken for a failed call.
	 */
	private interface Reader
	{
		Runnable read(Response response) throws IOException;
	}

	/**
	 * Asks for one call if the host, the scheme and the tag allow: it starts at once or waits its turn; {@code failed}
	 * runs at once only for a bad address or when 100 calls already wait.
	 */
	private void ask(HttpUrl url, Object tag, Runnable failed, Reader reader)
	{
		if (!"https".equals(url.scheme()) || !WikiUrls.HOST.equals(url.host()))
		{
			failed.run();
			return;
		}
		synchronized (lock)
		{
			if (cancelled.contains(tag))
			{
				return;
			}
		}
		queue.ask(tag, () -> start(url, tag, failed, reader), failed);
		scheduleDrain();
	}

	/** Starts a call the queue has counted in the budget, unless its tag was cancelled meanwhile. */
	private void start(HttpUrl url, Object tag, Runnable failed, Reader reader)
	{
		final Request request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).tag(tag).build();
		final Call call;
		try
		{
			synchronized (lock)
			{
				if (cancelled.contains(tag))
				{
					call = null;
				}
				else
				{
					call = http.newCall(request);
					byTag.computeIfAbsent(tag, t -> new HashSet<>()).add(call);
				}
			}
			if (call == null)
			{
				queue.finished();
				scheduleDrain();
				return;
			}
			call.enqueue(new Callback()
			{
				@Override
				public void onFailure(Call c, IOException e)
				{
					ended(tag, c);
					complain(url, e.toString());
					if (!isCancelled(tag))
					{
						failed.run();
					}
				}

				@Override
				public void onResponse(Call c, Response response)
				{
					ended(tag, c);
					Runnable done = null;
					try (Response answer = response)
					{
						if (answer.code() == 200 && answer.body() != null)
						{
							done = reader.read(answer);
						}
						else
						{
							complain(url, "HTTP " + answer.code());
						}
					}
					catch (IOException | RuntimeException e)
					{
						complain(url, e.toString());
					}
					if (isCancelled(tag))
					{
						return;
					}
					if (done == null)
					{
						failed.run();
					}
					else
					{
						done.run();
					}
				}
			});
		}
		catch (RuntimeException e)
		{
			queue.finished();
			scheduleDrain();
			complain(url, e.toString());
			failed.run();
		}
	}

	private boolean isCancelled(Object tag)
	{
		synchronized (lock)
		{
			return cancelled.contains(tag);
		}
	}

	/** When calls wait only for the minute cap, schedules one drain for the moment the oldest start leaves the minute. */
	private void scheduleDrain()
	{
		final long delay;
		synchronized (lock)
		{
			if (drainPending)
			{
				return;
			}
			final long now = System.nanoTime();
			final long next = queue.nextDrainNanos(now);
			if (next < 0)
			{
				return;
			}
			delay = Math.max(0L, next - now);
			drainPending = true;
		}
		try
		{
			executor.schedule(() ->
			{
				synchronized (lock)
				{
					drainPending = false;
				}
				queue.drain();
				scheduleDrain();
			}, delay, TimeUnit.NANOSECONDS);
		}
		catch (RuntimeException e)
		{
			synchronized (lock)
			{
				drainPending = false;
			}
		}
	}

	/** Takes a call off the tag's list and gives its place in the budget back. */
	private void ended(Object tag, Call call)
	{
		synchronized (lock)
		{
			final Set<Call> calls = byTag.get(tag);
			if (calls != null)
			{
				calls.remove(call);
				if (calls.isEmpty())
				{
					byTag.remove(tag);
				}
			}
		}
		queue.finished();
		scheduleDrain();
	}

	/** One warning the first time, debug lines after. */
	private void complain(HttpUrl url, String why)
	{
		final boolean first;
		synchronized (lock)
		{
			first = !warned;
			warned = true;
		}
		if (first)
		{
			log.warn("A wiki look-up call failed ({}); look-ups may not work", why);
		}
		else
		{
			log.debug("Wiki call {} failed: {}", url, why);
		}
	}

	/** The answer's bytes, at most 1 MB; a longer answer is an error. */
	private static byte[] readCapped(Response response) throws IOException
	{
		final ResponseBody body = response.body();
		final byte[] bytes = body.byteStream().readNBytes((int) MAX_BYTES + 1);
		if (bytes.length > MAX_BYTES)
		{
			throw new IOException("the answer is over " + MAX_BYTES + " bytes");
		}
		return bytes;
	}

	/** The picture the bytes hold as an ARGB image, or null when the toolkit cannot read them. */
	private static BufferedImage decode(byte[] bytes)
	{
		if (bytes.length == 0)
		{
			return null;
		}
		try
		{
			final Image image = Toolkit.getDefaultToolkit().createImage(bytes);
			final ImageIcon icon = new ImageIcon(image);
			if (icon.getImageLoadStatus() != MediaTracker.COMPLETE || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0)
			{
				return null;
			}
			return ImageUtil.bufferedImageFromImage(icon.getImage());
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	/** The picture scaled down, never up, to fit 36 x 32. */
	private static BufferedImage fit(BufferedImage picture)
	{
		final Dimension size = CropMath.fit(picture.getWidth(), picture.getHeight());
		if (size.width == picture.getWidth() && size.height == picture.getHeight())
		{
			return picture;
		}
		return ImageUtil.resizeImage(picture, size.width, size.height);
	}
}
