package com.scenecustomizer;

import net.runelite.api.Model;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** {@link Mesh}: a copy of a model's arrays. */
public class MeshTest
{
	@Test
	public void texturesAreCopied()
	{
		final byte[] faces = {0, -1};
		final int[] t1 = {0};
		final int[] t2 = {1};
		final int[] t3 = {2};
		final Model model = mock(Model.class);
		when(model.getFaceCount()).thenReturn(1);
		when(model.getVerticesX()).thenReturn(new float[] {1, 2, 3});
		when(model.getVerticesY()).thenReturn(new float[] {4, 5, 6});
		when(model.getVerticesZ()).thenReturn(new float[] {7, 8, 9});
		when(model.getFaceIndices1()).thenReturn(new int[] {0});
		when(model.getFaceIndices2()).thenReturn(new int[] {1});
		when(model.getFaceIndices3()).thenReturn(new int[] {2});
		when(model.getFaceColors1()).thenReturn(new int[] {1});
		when(model.getFaceColors2()).thenReturn(new int[] {1});
		when(model.getFaceColors3()).thenReturn(new int[] {1});
		when(model.getTextureFaces()).thenReturn(faces);
		when(model.getTexIndices1()).thenReturn(t1);
		when(model.getTexIndices2()).thenReturn(t2);
		when(model.getTexIndices3()).thenReturn(t3);

		final Mesh mesh = Mesh.copyOf(model);
		assertNotNull(mesh);
		faces[0] = 5;
		t1[0] = 9;
		t2[0] = 9;
		t3[0] = 9;
		assertEquals(0, mesh.textureFaces[0]);
		assertEquals(0, mesh.texIndices1[0]);
		assertEquals(1, mesh.texIndices2[0]);
		assertEquals(2, mesh.texIndices3[0]);

		when(model.getTextureFaces()).thenReturn(null);
		when(model.getTexIndices1()).thenReturn(null);
		when(model.getTexIndices2()).thenReturn(null);
		when(model.getTexIndices3()).thenReturn(null);
		final Mesh plain = Mesh.copyOf(model);
		assertNull(plain.textureFaces);
		assertNull(plain.texIndices1);
		assertNull(plain.texIndices2);
		assertNull(plain.texIndices3);
	}

	@Test
	public void aCopyDoesNotFollowTheModel()
	{
		final float[] x = {1, 2, 3};
		final float[] y = {4, 5, 6};
		final float[] z = {7, 8, 9};
		final int[] i1 = {0};
		final int[] i2 = {1};
		final int[] i3 = {2};
		final int[] c1 = {100};
		final int[] c2 = {101};
		final int[] c3 = {102};
		final Model model = mock(Model.class);
		when(model.getFaceCount()).thenReturn(1);
		when(model.getVerticesX()).thenReturn(x);
		when(model.getVerticesY()).thenReturn(y);
		when(model.getVerticesZ()).thenReturn(z);
		when(model.getFaceIndices1()).thenReturn(i1);
		when(model.getFaceIndices2()).thenReturn(i2);
		when(model.getFaceIndices3()).thenReturn(i3);
		when(model.getFaceColors1()).thenReturn(c1);
		when(model.getFaceColors2()).thenReturn(c2);
		when(model.getFaceColors3()).thenReturn(c3);

		final Mesh mesh = Mesh.copyOf(model);
		assertNotNull(mesh);
		x[0] = 99;
		c1[0] = 99;
		i2[0] = 99;
		assertEquals(1.0f, mesh.x[0], 0.0f);
		assertEquals(100, mesh.colours1[0]);
		assertEquals(1, mesh.indices2[0]);
		assertEquals(1, mesh.faceCount);
		assertNull(Mesh.copyOf(null));
	}
}
