package com.scenecustomizer;

import java.util.Arrays;
import net.runelite.api.Model;

/**
 * A copy of a lit model's arrays, safe to paint from another thread while the game goes on changing the model. Pure
 * value; the arrays are shared inside the package only.
 */
public final class Mesh
{
	final float[] x;
	final float[] y;
	final float[] z;
	final int[] indices1;
	final int[] indices2;
	final int[] indices3;
	final int[] colours1;
	final int[] colours2;
	final int[] colours3;
	/** May be null. */
	final byte[] transparencies;
	/** May be null. */
	final short[] textures;
	/** May be null. Per face the index of its texture triangle; -1 means the face's own corners. */
	final byte[] textureFaces;
	/** May be null. Per texture triangle its corners P, M, N as vertex indices. */
	final int[] texIndices1;
	final int[] texIndices2;
	final int[] texIndices3;
	final int faceCount;

	Mesh(float[] x, float[] y, float[] z, int[] indices1, int[] indices2, int[] indices3, int[] colours1,
		int[] colours2, int[] colours3, byte[] transparencies, short[] textures, byte[] textureFaces, int[] texIndices1, int[] texIndices2,
		int[] texIndices3, int faceCount)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.indices1 = indices1;
		this.indices2 = indices2;
		this.indices3 = indices3;
		this.colours1 = colours1;
		this.colours2 = colours2;
		this.colours3 = colours3;
		this.transparencies = transparencies;
		this.textures = textures;
		this.textureFaces = textureFaces;
		this.texIndices1 = texIndices1;
		this.texIndices2 = texIndices2;
		this.texIndices3 = texIndices3;
		this.faceCount = faceCount;
	}

	/** Copies the model; client thread. Null for a null model, one with no faces, or one whose arrays are missing. */
	public static Mesh copyOf(Model model)
	{
		if (model == null || model.getFaceCount() <= 0)
		{
			return null;
		}
		final float[] x = model.getVerticesX();
		final float[] y = model.getVerticesY();
		final float[] z = model.getVerticesZ();
		final int[] i1 = model.getFaceIndices1();
		final int[] i2 = model.getFaceIndices2();
		final int[] i3 = model.getFaceIndices3();
		final int[] c1 = model.getFaceColors1();
		final int[] c2 = model.getFaceColors2();
		final int[] c3 = model.getFaceColors3();
		if (x == null || y == null || z == null || i1 == null || i2 == null || i3 == null || c1 == null || c2 == null
			|| c3 == null)
		{
			return null;
		}
		final int count = Math.min(model.getFaceCount(), Math.min(Math.min(i1.length, i2.length),
			Math.min(Math.min(i3.length, c1.length), Math.min(c2.length, c3.length))));
		if (count <= 0)
		{
			return null;
		}
		final byte[] transparencies = model.getFaceTransparencies();
		final short[] textures = model.getFaceTextures();
		final byte[] textureFaces = model.getTextureFaces();
		final int[] t1 = model.getTexIndices1();
		final int[] t2 = model.getTexIndices2();
		final int[] t3 = model.getTexIndices3();
		return new Mesh(x.clone(), y.clone(), z.clone(), Arrays.copyOf(i1, count), Arrays.copyOf(i2, count),
			Arrays.copyOf(i3, count), Arrays.copyOf(c1, count), Arrays.copyOf(c2, count), Arrays.copyOf(c3, count),
			transparencies == null ? null : Arrays.copyOf(transparencies, count),
			textures == null ? null : Arrays.copyOf(textures, count),
			textureFaces == null ? null : textureFaces.clone(),
			t1 == null ? null : t1.clone(), t2 == null ? null : t2.clone(), t3 == null ? null : t3.clone(), count);
	}
}
