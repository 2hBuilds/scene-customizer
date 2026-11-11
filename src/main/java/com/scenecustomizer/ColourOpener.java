package com.scenecustomizer;

import java.awt.Color;
import java.awt.Component;
import java.util.function.Consumer;

/** Opens RuneLite's colour picker (the plugin supplies it; alpha is hidden). Swing thread only. */
interface ColourOpener
{
	/**
	 * Opens the picker.
	 *
	 * @param parent the component the picker belongs to
	 * @param title the picker window's title
	 * @param current the colour the picker starts on
	 * @param onChange told each colour the picker settles on while it is open
	 * @param onClose run when the picker closes
	 */
	void open(Component parent, String title, Color current, Consumer<Color> onChange, Runnable onClose);
}
