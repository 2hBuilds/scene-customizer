package com.scenecustomizer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import okhttp3.HttpUrl;
import org.junit.Test;

/** {@link WikiUrls}: the exact addresses the look-up asks. */
public class WikiUrlsTest
{
	private static Map<String, String> parameters(HttpUrl url)
	{
		final Map<String, String> all = new HashMap<>();
		for (String name : url.queryParameterNames())
		{
			all.put(name, url.queryParameter(name));
		}
		return all;
	}

	@Test
	public void everyCallsParameters()
	{
		final HttpUrl suggestions = WikiUrls.suggestions("cow");
		assertEquals("oldschool.runescape.wiki", suggestions.host());
		assertEquals("/api.php", suggestions.encodedPath());
		final Map<String, String> expected = new HashMap<>();
		expected.put("action", "query");
		expected.put("generator", "prefixsearch");
		expected.put("gpssearch", "cow");
		expected.put("gpslimit", "50");
		expected.put("prop", "templates");
		expected.put("tltemplates", "Template:Infobox Monster");
		expected.put("tllimit", "max");
		expected.put("format", "json");
		expected.put("formatversion", "2");
		assertEquals(expected, parameters(suggestions));

		final HttpUrl forms = WikiUrls.forms(Arrays.asList("Vet'ion", "Cow"));
		assertEquals("oldschool.runescape.wiki", forms.host());
		assertEquals("/api.php", forms.encodedPath());
		assertEquals("bucket", forms.queryParameter("action"));
		assertEquals("json", forms.queryParameter("format"));
		assertEquals("2", forms.queryParameter("formatversion"));
		assertEquals("bucket('infobox_monster').select('page_name','name','id','image')"
			+ ".where(bucket.Or({'page_name','Vet\\'ion'},{'page_name','Cow'})).run()", forms.queryParameter("query"));

		final HttpUrl text = WikiUrls.wikitext("Cow");
		assertEquals("parse", text.queryParameter("action"));
		assertEquals("Cow", text.queryParameter("page"));
		assertEquals("wikitext", text.queryParameter("prop"));
		assertEquals("2", text.queryParameter("formatversion"));

		final HttpUrl templates = WikiUrls.monsterTemplates(Arrays.asList("A", "B"));
		assertEquals("A|B", templates.queryParameter("titles"));
		assertEquals("1", templates.queryParameter("redirects"));
		assertEquals("Template:Infobox Monster", templates.queryParameter("tltemplates"));

		final HttpUrl thumbnails = WikiUrls.thumbnails(Arrays.asList("File:A.png", "File:B.png"));
		assertEquals("File:A.png|File:B.png", thumbnails.queryParameter("titles"));
		assertEquals("imageinfo", thumbnails.queryParameter("prop"));
		assertEquals("url|size", thumbnails.queryParameter("iiprop"));
		assertEquals("64", thumbnails.queryParameter("iiurlwidth"));
	}
}
