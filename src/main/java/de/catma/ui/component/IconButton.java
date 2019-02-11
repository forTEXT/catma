package de.catma.ui.component;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A native button that is very small and only renders an icon inside an i tag.
 * @author db
 */
public class IconButton extends NativeButton {

	public IconButton(Resource icon, ClickListener listener) {
		super(null, listener);
		setIcon(icon);
		addStyleNames(ValoTheme.BUTTON_ICON_ONLY,"button__icon",MaterialTheme.BUTTON_FLAT,MaterialTheme.BUTTON_BORDERLESS);
	}

	public IconButton(Resource icon) {
		super(null);
		setIcon(icon);
		addStyleNames(ValoTheme.BUTTON_ICON_ONLY,"button__icon",MaterialTheme.BUTTON_FLAT,MaterialTheme.BUTTON_BORDERLESS);
	}

}
