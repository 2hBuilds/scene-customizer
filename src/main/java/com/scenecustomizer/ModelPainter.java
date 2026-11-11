package com.scenecustomizer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import net.runelite.client.util.ImageUtil;

/**
 * Draws a {@link Mesh} as a small picture: seen from slightly above and turned an eighth of a turn, fitted in the
 * 36 x 32 box with a 1 px margin, on a clear background. Pure; safe off the client thread.
 */
public final class ModelPainter
{
	/** The turn round the vertical axis, in radians. */
	static final double YAW = 0.785;
	/** The tilt round the horizontal axis, in radians. */
	static final double PITCH = 0.35;

	/** The picture size. */
	public static final int WIDTH = 36;
	public static final int HEIGHT = 32;

	private static final int SCALE_UP = 3;

	private ModelPainter()
	{
	}

	/** The texture ids the visible faces of the mesh use. */
	public static Set<Integer> texturesOf(Mesh mesh)
	{
		final Set<Integer> used = new HashSet<>();
		if (mesh.textures != null)
		{
			for (int f = 0; f < mesh.faceCount; f++)
			{
				if (!skipped(mesh, f) && mesh.textures[f] >= 0)
				{
					used.add((int) mesh.textures[f]);
				}
			}
		}
		return used;
	}

	/**
	 * The mesh drawn 36 x 32, or null when nothing visible remains. {@code textureColour} gives a texture's game
	 * colour; {@code texturePixels} gives a texture's square array of pixels (0 = clear), or null when unknown.
	 */
	public static BufferedImage paint(Mesh mesh, IntUnaryOperator textureColour, IntFunction<int[]> texturePixels)
	{
		if (mesh == null)
		{
			return null;
		}
		final double cy = Math.cos(YAW);
		final double sy = Math.sin(YAW);
		final double cp = Math.cos(PITCH);
		final double sp = Math.sin(PITCH);
		final int vertices = Math.min(mesh.x.length, Math.min(mesh.y.length, mesh.z.length));
		final double[] sx = new double[vertices];
		final double[] sY = new double[vertices];
		final double[] depth = new double[vertices];
		for (int v = 0; v < vertices; v++)
		{
			final double x1 = mesh.x[v] * cy + mesh.z[v] * sy;
			final double z1 = -mesh.x[v] * sy + mesh.z[v] * cy;
			sx[v] = x1;
			sY[v] = mesh.y[v] * cp - z1 * sp;
			depth[v] = mesh.y[v] * sp + z1 * cp;
		}
		final Integer[] order = new Integer[mesh.faceCount];
		int visible = 0;
		double minX = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		for (int f = 0; f < mesh.faceCount; f++)
		{
			if (skipped(mesh, f) || !inRange(mesh, f, vertices))
			{
				continue;
			}
			order[visible++] = f;
			for (int v : new int[] {mesh.indices1[f], mesh.indices2[f], mesh.indices3[f]})
			{
				minX = Math.min(minX, sx[v]);
				maxX = Math.max(maxX, sx[v]);
				minY = Math.min(minY, sY[v]);
				maxY = Math.max(maxY, sY[v]);
			}
		}
		if (visible == 0)
		{
			return null;
		}
		final Integer[] faces = Arrays.copyOf(order, visible);
		final double[] faceDepth = new double[mesh.faceCount];
		for (int f : faces)
		{
			faceDepth[f] = (depth[mesh.indices1[f]] + depth[mesh.indices2[f]] + depth[mesh.indices3[f]]) / 3.0;
		}
		// Far to near: the biggest depth first.
		Arrays.sort(faces, Comparator.<Integer>comparingDouble(f -> faceDepth[f]).reversed());

		final int canvasW = WIDTH * SCALE_UP;
		final int canvasH = HEIGHT * SCALE_UP;
		final double scale = Math.min((WIDTH - 2) * SCALE_UP / Math.max(1.0e-6, maxX - minX),
			(HEIGHT - 2) * SCALE_UP / Math.max(1.0e-6, maxY - minY));
		final double offsetX = canvasW / 2.0 - scale * (minX + maxX) / 2.0;
		final double offsetY = canvasH / 2.0 - scale * (minY + maxY) / 2.0;

		final BufferedImage big = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = big.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			for (int f : faces)
			{
				final int[] pixels = mesh.textures != null && mesh.textures[f] >= 0
					? texturePixels.apply(mesh.textures[f]) : null;
				final int side = pixels == null ? 0 : (int) Math.round(Math.sqrt(pixels.length));
				if (side > 0 && side * side == pixels.length)
				{
					fillTextured(big, mesh, f, pixels, side, sx, sY, offsetX, offsetY, scale, alphaOf(mesh, f));
					continue;
				}
				final Path2D.Double triangle = new Path2D.Double();
				triangle.moveTo(offsetX + scale * sx[mesh.indices1[f]], offsetY + scale * sY[mesh.indices1[f]]);
				triangle.lineTo(offsetX + scale * sx[mesh.indices2[f]], offsetY + scale * sY[mesh.indices2[f]]);
				triangle.lineTo(offsetX + scale * sx[mesh.indices3[f]], offsetY + scale * sY[mesh.indices3[f]]);
				triangle.closePath();
				g.setColor(new Color((alphaOf(mesh, f) << 24) | (JagexColors.hslToRgb(faceColour(mesh, f, textureColour)) & 0xFFFFFF), true));
				g.fill(triangle);
			}
		}
		finally
		{
			g.dispose();
		}
		if (isClear(big))
		{
			return null;
		}
		return ImageUtil.resizeImage(big, WIDTH, HEIGHT);
	}

	/** True when no pixel of the picture has any colour in it (faces of clear texels leave holes). */
	private static boolean isClear(BufferedImage image)
	{
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
				{
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Fills one textured face pixel by pixel. Choice: the picture is orthographic, so the corners' texture places are
	 * interpolated with screen-space barycentric weights; the nearest texel is read, and a texel of 0 is a hole.
	 */
	private static void fillTextured(BufferedImage big, Mesh mesh, int f, int[] pixels, int side, double[] sx,
		double[] sY, double offsetX, double offsetY, double scale, int alpha)
	{
		final int[] corner = {mesh.indices1[f], mesh.indices2[f], mesh.indices3[f]};
		final float[] us = {0f, 1f, 0f};
		final float[] vs = {0f, 0f, 1f};
		final int tri = mesh.textureFaces != null && f < mesh.textureFaces.length ? mesh.textureFaces[f] : -1;
		if (tri >= 0 && mesh.texIndices1 != null && mesh.texIndices2 != null && mesh.texIndices3 != null
			&& tri < mesh.texIndices1.length && tri < mesh.texIndices2.length && tri < mesh.texIndices3.length)
		{
			final int p = mesh.texIndices1[tri];
			final int m = mesh.texIndices2[tri];
			final int n = mesh.texIndices3[tri];
			final int count = Math.min(mesh.x.length, Math.min(mesh.y.length, mesh.z.length));
			if (p >= 0 && p < count && m >= 0 && m < count && n >= 0 && n < count)
			{
				final float[] pp = {mesh.x[p], mesh.y[p], mesh.z[p]};
				final float[] mm = {mesh.x[m], mesh.y[m], mesh.z[m]};
				final float[] nn = {mesh.x[n], mesh.y[n], mesh.z[n]};
				for (int c = 0; c < 3; c++)
				{
					final float[] uv = TextureUv.uv(pp, mm, nn,
						new float[] {mesh.x[corner[c]], mesh.y[corner[c]], mesh.z[corner[c]]});
					us[c] = uv[0];
					vs[c] = uv[1];
				}
			}
		}
		final double[] px = new double[3];
		final double[] py = new double[3];
		for (int c = 0; c < 3; c++)
		{
			px[c] = offsetX + scale * sx[corner[c]];
			py[c] = offsetY + scale * sY[corner[c]];
		}
		final double denom = (px[1] - px[0]) * (py[2] - py[0]) - (px[2] - px[0]) * (py[1] - py[0]);
		if (Math.abs(denom) < 1.0e-9)
		{
			return;
		}
		final double light = Math.max(0.15, Math.min(1.0, (mesh.colours1[f] & 0x7F) / 127.0));
		final int x0 = Math.max(0, (int) Math.floor(Math.min(px[0], Math.min(px[1], px[2]))));
		final int x1 = Math.min(big.getWidth() - 1, (int) Math.ceil(Math.max(px[0], Math.max(px[1], px[2]))));
		final int y0 = Math.max(0, (int) Math.floor(Math.min(py[0], Math.min(py[1], py[2]))));
		final int y1 = Math.min(big.getHeight() - 1, (int) Math.ceil(Math.max(py[0], Math.max(py[1], py[2]))));
		for (int j = y0; j <= y1; j++)
		{
			for (int i = x0; i <= x1; i++)
			{
				final double cx = i + 0.5;
				final double cy = j + 0.5;
				final double w2 = ((cx - px[0]) * (py[2] - py[0]) - (px[2] - px[0]) * (cy - py[0])) / denom;
				final double w3 = ((px[1] - px[0]) * (cy - py[0]) - (cx - px[0]) * (py[1] - py[0])) / denom;
				final double w1 = 1.0 - w2 - w3;
				if (w1 < -1.0e-6 || w2 < -1.0e-6 || w3 < -1.0e-6)
				{
					continue;
				}
				double u = w1 * us[0] + w2 * us[1] + w3 * us[2];
				double v = w1 * vs[0] + w2 * vs[1] + w3 * vs[2];
				u -= Math.floor(u);
				v -= Math.floor(v);
				final int tx = Math.min(side - 1, (int) (u * side));
				final int ty = Math.min(side - 1, (int) (v * side));
				final int texel = pixels[ty * side + tx] & 0xFFFFFF;
				if (texel == 0)
				{
					continue;
				}
				final int r = (int) (((texel >> 16) & 0xFF) * light);
				final int gr = (int) (((texel >> 8) & 0xFF) * light);
				final int b = (int) ((texel & 0xFF) * light);
				big.setRGB(i, j, 0xFF000000 | (r << 16) | (gr << 8) | b);
			}
		}
	}

	/** The fill alpha of a face: 255 minus its transparency (unsigned); 255 when the mesh has none. */
	private static int alphaOf(Mesh mesh, int f)
	{
		return mesh.transparencies == null ? 255 : 255 - (mesh.transparencies[f] & 0xFF);
	}

	/** The pixel {@code below} (ARGB) with the colour {@code rgb} laid over it at {@code alpha}: source-over blending. */
	private static int over(int below, int alpha, int rgb)
	{
		if (alpha >= 255)
		{
			return 0xFF000000 | rgb;
		}
		final double sa = alpha / 255.0;
		final double da = (below >>> 24) / 255.0;
		final double oa = sa + da * (1.0 - sa);
		if (oa <= 0.0)
		{
			return 0;
		}
		int out = (int) Math.round(oa * 255.0) << 24;
		for (int shift = 16; shift >= 0; shift -= 8)
		{
			final double s = (rgb >> shift) & 0xFF;
			final double d = (below >> shift) & 0xFF;
			out |= (int) Math.round((s * sa + d * da * (1.0 - sa)) / oa) << shift;
		}
		return out;
	}

	private static boolean skipped(Mesh mesh, int f)
	{
		return mesh.colours3[f] == -2 || (mesh.transparencies != null && (mesh.transparencies[f] & 0xFF) == 255);
	}

	private static boolean inRange(Mesh mesh, int f, int vertices)
	{
		return mesh.indices1[f] >= 0 && mesh.indices1[f] < vertices && mesh.indices2[f] >= 0
			&& mesh.indices2[f] < vertices && mesh.indices3[f] >= 0 && mesh.indices3[f] < vertices;
	}

	private static int faceColour(Mesh mesh, int f, IntUnaryOperator textureColour)
	{
		final int c1 = mesh.colours1[f];
		if (mesh.textures != null && mesh.textures[f] >= 0)
		{
			return (textureColour.applyAsInt(mesh.textures[f]) & ~0x7F) | (c1 & 0x7F);
		}
		if (mesh.colours3[f] == -1)
		{
			return c1;
		}
		final int light = ((c1 & 0x7F) + (mesh.colours2[f] & 0x7F) + (mesh.colours3[f] & 0x7F)) / 3;
		return (c1 & ~0x7F) | light;
	}
}
