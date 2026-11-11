package com.scenecustomizer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Model;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link OutlineOverlay}: see-through objects get a drawn outline, solid ones RuneLite's. */
public class OutlineOverlayTest
{
	private static GameObject object(byte[] transparencies, Shape hull)
	{
		final Model model = mock(Model.class);
		when(model.getFaceCount()).thenReturn(transparencies.length);
		when(model.getFaceTransparencies()).thenReturn(transparencies);
		final int[] colours3 = new int[transparencies.length];
		java.util.Arrays.fill(colours3, -1);
		when(model.getFaceColors3()).thenReturn(colours3);
		final GameObject object = mock(GameObject.class);
		when(object.getRenderable()).thenReturn(model);
		when(object.getConvexHull()).thenReturn(hull);
		return object;
	}

	@Test
	public void aSeeThroughObjectGetsItsHullOutline()
	{
		final Shape hull = new Rectangle2D.Double(1, 2, 30, 40);
		final Graphics2D graphics = mock(Graphics2D.class);

		final ModelOutlineRenderer seeThroughRenderer = mock(ModelOutlineRenderer.class);
		render(object(new byte[] {(byte) 128, 64}, hull), seeThroughRenderer, graphics);
		verify(graphics).draw(hull);
		verify(seeThroughRenderer, never()).drawOutline(any(GameObject.class), anyInt(), any(Color.class), anyInt());

		final Graphics2D solidGraphics = mock(Graphics2D.class);
		final ModelOutlineRenderer solidRenderer = mock(ModelOutlineRenderer.class);
		final GameObject solid = object(new byte[] {0, 0}, hull);
		render(solid, solidRenderer, solidGraphics);
		verify(solidRenderer).drawOutline(any(GameObject.class), anyInt(), any(Color.class), anyInt());
		verify(solidGraphics, never()).draw(any(Shape.class));
	}

	private static void render(GameObject object, ModelOutlineRenderer renderer, Graphics2D graphics)
	{
		final Decisions decisions = new Decisions();
		decisions.setObjects(Collections.<Integer>emptySet(), Collections.<Long>emptySet(),
			Collections.singletonList(new OutlinedObject(object, new Outline(0xFF0000, 2))), Collections.<Long>emptySet());
		new OutlineOverlay(mock(Client.class), renderer, decisions).render(graphics);
	}
}
