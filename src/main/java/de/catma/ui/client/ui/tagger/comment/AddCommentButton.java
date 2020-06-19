package de.catma.ui.client.ui.tagger.comment;

import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ButtonBase;

public class AddCommentButton extends ButtonBase {

	public AddCommentButton() {
		super(DOM.createDiv());
		addStyleName("add-comment-button");
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
			"<span class=\"v-icon v-icon-plus"
                + "\" style=\"font-family: Vaadin-Icons;\">&#x"
                + Integer.toHexString(0xE801) + ";</span>"
            +"<span class=\"v-icon v-icon-comment"
            + "\" style=\"font-family: Vaadin-Icons;\">&#x"
            + Integer.toHexString(0xE768) + ";</span>");
	}

	@Override
	public void setVisible(boolean visible) {
		getElement().getStyle().setVisibility(visible?Visibility.VISIBLE:Visibility.HIDDEN);
	}
	
}
