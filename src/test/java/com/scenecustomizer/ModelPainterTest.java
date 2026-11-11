package com.scenecustomizer;

import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** {@link ModelPainter}: the picture drawn from a mesh. */
public class ModelPainterTest
{
	private static final int RED = (0 << 10) | (7 << 7) | 64;
	private static final int BLUE = (43 << 10) | (7 << 7) | 64;

	/** A mesh of quads (two triangles each) given as corner lists; one flat colour each. */
	private static Mesh quads(float[][][] corners, int[] colours, byte[] transparency, int colour3)
	{
		final int faces = corners.length * 2;
		final float[] x = new float[corners.length * 4];
		final float[] y = new float[x.length];
		final float[] z = new float[x.length];
		final int[] i1 = new int[faces];
		final int[] i2 = new int[faces];
		final int[] i3 = new int[faces];
		final int[] c1 = new int[faces];
		final int[] c3 = new int[faces];
		for (int q = 0; q < corners.length; q++)
		{
			for (int c = 0; c < 4; c++)
			{
				x[q * 4 + c] = corners[q][c][0];
				y[q * 4 + c] = corners[q][c][1];
				z[q * 4 + c] = corners[q][c][2];
			}
			i1[q * 2] = q * 4;
			i2[q * 2] = q * 4 + 1;
			i3[q * 2] = q * 4 + 2;
			i1[q * 2 + 1] = q * 4;
			i2[q * 2 + 1] = q * 4 + 2;
			i3[q * 2 + 1] = q * 4 + 3;
			c1[q * 2] = colours[q];
			c1[q * 2 + 1] = colours[q];
			c3[q * 2] = colour3;
			c3[q * 2 + 1] = colour3;
		}
		return new Mesh(x, y, z, i1, i2, i3, c1, c1.clone(), c3, transparency, null, null, null, null, null, faces);
	}

	private static float[][] square(float depth)
	{
		return new float[][] {{-50, -50, depth}, {50, -50, depth}, {50, 50, depth}, {-50, 50, depth}};
	}

	private static Mesh cube()
	{
		final float[][][] sides = new float[6][][];
		final float[][] c = {{-50, -50, -50}, {50, -50, -50}, {50, 50, -50}, {-50, 50, -50},
			{-50, -50, 50}, {50, -50, 50}, {50, 50, 50}, {-50, 50, 50}};
		final int[][] idx = {{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 2, 6, 5}};
		for (int s = 0; s < 6; s++)
		{
			sides[s] = new float[][] {c[idx[s][0]], c[idx[s][1]], c[idx[s][2]], c[idx[s][3]]};
		}
		return quads(sides, new int[] {RED, RED, RED, RED, RED, RED}, null, -1);
	}

	private static BufferedImage squareWithTransparency(int transparency)
	{
		final Mesh mesh = quads(new float[][][] {square(0)}, new int[] {RED},
			new byte[] {(byte) transparency, (byte) transparency}, -1);
		return ModelPainter.paint(mesh, t -> 0, t -> null);
	}

	@Test
	public void aSeeThroughFaceIsDrawnSeeThrough()
	{
		final BufferedImage picture = squareWithTransparency(128);
		assertNotNull(picture);
		final int argb = picture.getRGB(13, 16);
		final int alpha = argb >>> 24;
		assertTrue("alpha " + alpha, alpha > 100 && alpha < 155);
		assertTrue(((argb >> 16) & 0xFF) > ((argb >> 8) & 0xFF) && ((argb >> 16) & 0xFF) > (argb & 0xFF));
	}

	@Test
	public void solidFacesStaySolid()
	{
		final BufferedImage picture = squareWithTransparency(0);
		assertNotNull(picture);
		assertEquals(255, picture.getRGB(13, 16) >>> 24);
	}

	@Test
	public void aCubeIsCentredAndFitsTheBox()
	{
		final BufferedImage picture = ModelPainter.paint(cube(), t -> 0, t -> null);
		assertNotNull(picture);
		assertEquals(36, picture.getWidth());
		assertEquals(32, picture.getHeight());
		int minX = 99;
		int maxX = -1;
		int minY = 99;
		int maxY = -1;
		for (int y = 0; y < 32; y++)
		{
			for (int x = 0; x < 36; x++)
			{
				if ((picture.getRGB(x, y) >>> 24) >= 128)
				{
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
				}
			}
		}
		assertTrue(minX >= 1 && maxX <= 34 && minY >= 1 && maxY <= 30);
		assertTrue(minX <= 3 || maxX >= 32 || minY <= 3 || maxY >= 28);
		assertTrue(Math.abs((minX + maxX + 1) - 36) <= 2);
		assertTrue(Math.abs((minY + maxY + 1) - 32) <= 2);
	}

	@Test
	public void nearFacesCoverFarFaces()
	{
		// Smaller z is nearer. The far (blue) square is listed after the near (red) one, so a plain draw order would hide red.
		final Mesh mesh = quads(new float[][][] {square(0), square(20)}, new int[] {RED, BLUE}, null, -1);
		final BufferedImage picture = ModelPainter.paint(mesh, t -> 0, t -> null);
		assertNotNull(picture);
		final int rgb = picture.getRGB(18, 16);
		assertTrue(((rgb >> 16) & 0xFF) > (rgb & 0xFF));
	}

	@Test
	public void hiddenAndClearFacesAreSkipped()
	{
		final float[][][] one = {square(0)};
		assertNull(ModelPainter.paint(quads(one, new int[] {RED}, null, -2), t -> 0, t -> null));
		assertNull(ModelPainter.paint(quads(one, new int[] {RED}, new byte[] {(byte) 255, (byte) 255}, -1), t -> 0, t -> null));
	}

	private static Mesh textured(Mesh flat)
	{
		final short[] textures = new short[flat.faceCount];
		java.util.Arrays.fill(textures, (short) 4);
		return new Mesh(flat.x, flat.y, flat.z, flat.indices1, flat.indices2, flat.indices3, flat.colours1,
			flat.colours2, flat.colours3, null, textures, null, null, null, null, flat.faceCount);
	}

	private static int[] filled(int value)
	{
		final int[] pixels = new int[16];
		java.util.Arrays.fill(pixels, value);
		return pixels;
	}

	@Test
	public void aTexturedFaceShowsItsTexture()
	{
		final Mesh mesh = textured(quads(new float[][][] {square(0)}, new int[] {127}, null, -1));
		final BufferedImage picture = ModelPainter.paint(mesh, t -> BLUE, t -> t == 4 ? filled(0xFF0000) : null);
		assertNotNull(picture);
		final int rgb = picture.getRGB(18, 16);
		assertTrue(((rgb >> 16) & 0xFF) > (rgb & 0xFF) + 50);
	}

	@Test
	public void clearTexelsAreHoles()
	{
		final Mesh mesh = textured(quads(new float[][][] {square(0)}, new int[] {127}, null, -1));
		assertNull(ModelPainter.paint(mesh, t -> BLUE, t -> filled(0)));
	}

	@Test
	public void theTextureTriangleMapsTheImage()
	{
		final Mesh one = new Mesh(new float[] {0, 100, 0}, new float[] {0, 0, 100}, new float[] {0, 0, 0},
			new int[] {0}, new int[] {1}, new int[] {2}, new int[] {127}, new int[] {127}, new int[] {-1}, null,
			new short[] {4}, new byte[] {0}, new int[] {0}, new int[] {1}, new int[] {2}, 1);
		final int[] pixels = new int[16];
		for (int y = 0; y < 4; y++)
		{
			for (int x = 0; x < 4; x++)
			{
				pixels[y * 4 + x] = x < 2 ? 0xFF0000 : 0x00FF00;
			}
		}
		final BufferedImage picture = ModelPainter.paint(one, t -> BLUE, t -> pixels);
		assertNotNull(picture);
		int minX = 99;
		int maxX = -1;
		for (int y = 0; y < 32; y++)
		{
			for (int x = 0; x < 36; x++)
			{
				if ((picture.getRGB(x, y) >>> 24) >= 128)
				{
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
				}
			}
		}
		long leftRed = 0;
		long leftGreen = 0;
		long rightRed = 0;
		long rightGreen = 0;
		for (int y = 0; y < 32; y++)
		{
			for (int x = 0; x < 36; x++)
			{
				final int rgb = picture.getRGB(x, y);
				if ((rgb >>> 24) < 128)
				{
					continue;
				}
				if (x <= minX + 3)
				{
					leftRed += (rgb >> 16) & 0xFF;
					leftGreen += (rgb >> 8) & 0xFF;
				}
				if (x >= maxX - 3)
				{
					rightRed += (rgb >> 16) & 0xFF;
					rightGreen += (rgb >> 8) & 0xFF;
				}
			}
		}
		assertTrue(leftRed > leftGreen);
		assertTrue(rightGreen > rightRed);
	}

	@Test
	public void noFacesGivesNull()
	{
		final Mesh empty = new Mesh(new float[0], new float[0], new float[0], new int[0], new int[0], new int[0],
			new int[0], new int[0], new int[0], null, null, null, null, null, null, 0);
		assertNull(ModelPainter.paint(empty, t -> 0, t -> null));
	}
}
