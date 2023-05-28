package de.catma.ui.component;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A small, flat and borderless button that only renders an icon by default.
 */
public class IconButton extends Button {
	public IconButton(Resource icon) {
		super(icon);
		addStyleNames(ValoTheme.BUTTON_ICON_ONLY, MaterialTheme.BUTTON_FLAT, MaterialTheme.BUTTON_BORDERLESS, "icon-button");
	}

	public IconButton(Resource icon, ClickListener listener) {
		this(icon);
		this.addClickListener(listener);
	}
}
