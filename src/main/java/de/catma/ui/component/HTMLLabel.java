package de.catma.ui.component;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

/**
 * sets the {@link ContentMode} to HTML after initialization completes
 * @author db
 *
 */
public class HTMLLabel extends Label {

	public HTMLLabel() {
		super();
		setContentMode(ContentMode.HTML);
	}

	public HTMLLabel(String text, ContentMode contentMode) {
		super(text, contentMode);
		setContentMode(ContentMode.HTML);
	}

	public HTMLLabel(String text) {
		super(text);
		setContentMode(ContentMode.HTML);
	}

}
