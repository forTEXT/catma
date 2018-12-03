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
		addStyleNames(ValoTheme.BUTTON_ICON_ONLY,"button__icon");
	}

	public IconButton(Resource icon) {
		super(icon);
		addStyleNames(ValoTheme.BUTTON_ICON_ONLY,"button__icon");
	}

}
