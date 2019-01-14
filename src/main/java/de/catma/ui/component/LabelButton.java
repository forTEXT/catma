package de.catma.ui.component;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.themes.ValoTheme;

/**
 * a styled label button visible on homepage
 * @author db
 *
 */
public class LabelButton extends NativeButton {

	public LabelButton(String labeltext, ClickListener listener) {
		super(labeltext, listener);
		addStyleNames(ValoTheme.BUTTON_ICON_ONLY, "button__label", MaterialTheme.BUTTON_FLAT,MaterialTheme.BUTTON_BORDERLESS);
	}

	public LabelButton(String labeltext) {
		super(labeltext);
		addStyleNames(ValoTheme.BUTTON_ICON_ONLY, "button__label", MaterialTheme.BUTTON_FLAT, MaterialTheme.BUTTON_BORDERLESS);
	}
}
