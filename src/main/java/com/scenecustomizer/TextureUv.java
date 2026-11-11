package com.scenecustomizer;

/**
 * Where a point sits on a texture: the point's place on the plane of a texture triangle P, M, N, with u running from P
 * to M and v from P to N. Pure.
 */
final class TextureUv
{
	private TextureUv()
	{
	}

	/** The (u, v) of the point x on the plane of P, M, N; (0, 0) for a degenerate triangle. */
	static float[] uv(float[] p, float[] m, float[] n, float[] x)
	{
		final double[] a = {m[0] - p[0], m[1] - p[1], m[2] - p[2]};
		final double[] b = {n[0] - p[0], n[1] - p[1], n[2] - p[2]};
		final double[] t = {x[0] - p[0], x[1] - p[1], x[2] - p[2]};
		final double[] c = cross(a, b);
		final double[] bc = cross(b, c);
		final double[] ac = cross(a, c);
		final double du = dot(a, bc);
		final double dv = dot(b, ac);
		if (Math.abs(du) < 1.0e-9 || Math.abs(dv) < 1.0e-9)
		{
			return new float[] {0f, 0f};
		}
		return new float[] {(float) (dot(t, bc) / du), (float) (dot(t, ac) / dv)};
	}

	private static double[] cross(double[] a, double[] b)
	{
		return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
	}

	private static double dot(double[] a, double[] b)
	{
		return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
	}
}
