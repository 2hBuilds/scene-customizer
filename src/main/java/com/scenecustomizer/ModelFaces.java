package com.scenecustomizer;

/** Questions about a model's faces that need no client. Pure. */
final class ModelFaces
{
	private ModelFaces()
	{
	}

	/**
	 * True when every face that is not hidden (third colour of -2) has a transparency above 0 and at least one such
	 * face exists. Null transparencies give false.
	 */
	static boolean allSeeThrough(byte[] transparencies, int[] colours3)
	{
		if (transparencies == null || colours3 == null)
		{
			return false;
		}
		boolean any = false;
		for (int f = 0; f < Math.min(transparencies.length, colours3.length); f++)
		{
			if (colours3[f] == -2)
			{
				continue;
			}
			if ((transparencies[f] & 0xFF) == 0)
			{
				return false;
			}
			any = true;
		}
		return any;
	}
}
