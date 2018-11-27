package de.catma.ui.component;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A native button that is very small and only renders an icon inside an i tag.
 * @author db
 */
public class IconButton extends Button {

	public IconButton(Resource icon, ClickListener listener) {
		super(icon, listener);
		setStyleName(ValoTheme.BUTTON_ICON_ONLY);
	}

	public IconButton(Resource icon) {
		super(icon);
		setStyleName(ValoTheme.BUTTON_ICON_ONLY);
	}

}
