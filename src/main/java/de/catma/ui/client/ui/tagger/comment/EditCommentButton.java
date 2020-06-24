package de.catma.ui.client.ui.tagger.comment;

import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ButtonBase;

public class EditCommentButton extends ButtonBase {

	public EditCommentButton() {
		super(DOM.createDiv());
		addStyleName("edit-comment-button");
		addStyleName("v-button");
		addStyleName("v-widget");
		addStyleName("icon-only");
		addStyleName("v-button-icon-only");
		addStyleName("button__icon");
		addStyleName("v-button-button__icon");
		addStyleName("flat v-button-flat");
		addStyleName("borderless");
		addStyleName("v-button-borderless");
		
		setHTML(
			"<span class=\"v-icon v-icon-edit"
                + "\" style=\"font-family: Vaadin-Icons;\">&#x"
                + Integer.toHexString(0xE771) + ";</span>");
	}

	@Override
	public void setVisible(boolean visible) {
		getElement().getStyle().setVisibility(visible?Visibility.VISIBLE:Visibility.HIDDEN);
	}
	
}
