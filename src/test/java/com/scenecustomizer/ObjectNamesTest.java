package com.scenecustomizer;

import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link ObjectNames}: the impostor's name wins, and answers are cached until a load. */
public class ObjectNamesTest
{
	@Test
	public void theImpostorsNameWinsAndIsCached()
	{
		final Client client = mock(Client.class);
		final ObjectComposition def = mock(ObjectComposition.class);
		final ObjectComposition impostor = mock(ObjectComposition.class);
		when(def.getName()).thenReturn("Closed door");
		when(def.getImpostorIds()).thenReturn(new int[] {7});
		when(def.getImpostor()).thenReturn(impostor);
		when(impostor.getName()).thenReturn("Door");
		when(client.getObjectDefinition(100)).thenReturn(def);
		final ObjectComposition nullNamed = mock(ObjectComposition.class);
		when(nullNamed.getName()).thenReturn("null");
		when(client.getObjectDefinition(101)).thenReturn(nullNamed);

		final ObjectNames names = new ObjectNames(client);
		assertEquals("Door", names.nameOf(100));
		assertNull(names.nameOf(101));
		assertNull(names.nameOf(102));
		assertEquals("Door", names.nameOf(100));
		verify(client, times(1)).getObjectDefinition(100);
		names.clear();
		assertEquals("Door", names.nameOf(100));
		verify(client, times(2)).getObjectDefinition(100);
	}

	@Test
	public void impostorNamesAreAskedAgainAfterARefresh()
	{
		final Client client = mock(Client.class);
		final ObjectComposition def = mock(ObjectComposition.class);
		final ObjectComposition impostor = mock(ObjectComposition.class);
		when(def.getImpostorIds()).thenReturn(new int[] {7});
		when(def.getImpostor()).thenReturn(impostor);
		when(impostor.getName()).thenReturn("Door");
		when(client.getObjectDefinition(100)).thenReturn(def);
		final ObjectComposition rock = mock(ObjectComposition.class);
		when(rock.getName()).thenReturn("Rock");
		when(client.getObjectDefinition(200)).thenReturn(rock);

		final ObjectNames names = new ObjectNames(client);
		assertEquals("Door", names.nameOf(100));
		assertEquals("Door", names.nameOf(100));
		verify(client, times(1)).getObjectDefinition(100);
		assertEquals("Rock", names.nameOf(200));
		names.refreshImpostors();
		assertEquals("Door", names.nameOf(100));
		verify(client, times(2)).getObjectDefinition(100);
		assertEquals("Rock", names.nameOf(200));
		verify(client, times(1)).getObjectDefinition(200);
	}
}
