package com.scenecustomizer;

import java.util.Collections;
import java.util.List;

/** How one menu press changes a rule. Pure. */
final class RuleEdits
{
	/** The outline colour a new outline starts with (cyan). */
	static final int DEFAULT_RGB = 0x00D6D6;
	/** The outline width a new outline starts with. */
	static final int DEFAULT_WIDTH = 2;

	private RuleEdits()
	{
	}

	/**
	 * The rule after the press. The other half of the rule is kept. A rule that does nothing means "delete it". An
	 * action that is not an edit (COPY, REMOVE) gives back the current rule, or an empty one when there is none.
	 *
	 * @param value the colour (0xRRGGBB) for COLOUR, the width (1-6) for WIDTH, else unused
	 */
	static Rule apply(ThingView t, Rule current, RowAction action, int value)
	{
		Outline outline = current == null ? null : current.outline;
		List<Place> outlinePlaces = current == null ? Collections.<Place>emptyList() : current.outlinePlaces;
		HideScope hide = current == null ? HideScope.OFF : current.hide;
		List<Place> hidePlaces = current == null ? Collections.<Place>emptyList() : current.hidePlaces;
		final boolean hadOutline = outline != null;
		switch (action)
		{
			case OUTLINE:
				if (hadOutline)
				{
					outline = null;
					outlinePlaces = Collections.<Place>emptyList();
				}
				else
				{
					outline = new Outline(DEFAULT_RGB, DEFAULT_WIDTH);
					outlinePlaces = t.places();
				}
				break;
			case COLOUR:
				outline = new Outline(value, hadOutline ? outline.width : DEFAULT_WIDTH);
				outlinePlaces = hadOutline ? outlinePlaces : t.places();
				break;
			case WIDTH:
				outline = new Outline(hadOutline ? outline.rgb : DEFAULT_RGB, value);
				outlinePlaces = hadOutline ? outlinePlaces : t.places();
				break;
			case HIDE:
				hide = hide == HideScope.EVERYWHERE ? HideScope.OFF : HideScope.EVERYWHERE;
				hidePlaces = Collections.<Place>emptyList();
				break;
			case SHOW:
				hide = HideScope.OFF;
				hidePlaces = Collections.<Place>emptyList();
				break;
			case HIDE_HERE:
				hide = HideScope.HERE;
				hidePlaces = t.places();
				break;
			case HIDE_EVERYWHERE:
				hide = HideScope.EVERYWHERE;
				hidePlaces = Collections.<Place>emptyList();
				break;
			default:
				break;
		}
		return new Rule(t.kind(), t.name(), outline, outlinePlaces, hide, hidePlaces);
	}
}
