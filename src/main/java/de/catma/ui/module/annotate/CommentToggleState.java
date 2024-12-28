package de.catma.ui.module.annotate;

import com.vaadin.icons.VaadinIcons;

enum CommentToggleState {
	OFF(VaadinIcons.COMMENT_O, false, "Comments are off, click to switch comments on, click again to switch on live comments."),
	ON(VaadinIcons.COMMENT, true, "Comments are on, click to switch on live comments, click again to switch off comments."),
	LIVE(VaadinIcons.COMMENTS, true, "Live comments are on, click to switch off comments, click again ot switch on comments without live refresh."),
	;
	private VaadinIcons icon;
	private boolean visible;
	private String description;

	private CommentToggleState(VaadinIcons icon, boolean visible, String description) {
		this.icon = icon;
		this.visible = visible;
		this.description = description;
	}
	
	public VaadinIcons getIcon() {
		return icon;
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public String getDescription() {
		return description;
	}
	
}