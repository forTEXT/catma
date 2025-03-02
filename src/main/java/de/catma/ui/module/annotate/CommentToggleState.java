package de.catma.ui.module.annotate;

import com.vaadin.icons.VaadinIcons;

enum CommentToggleState {
	OFF(VaadinIcons.COMMENT_O, false, "<strong>OFF</strong> - click to display comments, click again to switch on live comments"),
	ON(VaadinIcons.COMMENT, true, "<strong>ON</strong> - click to switch on live comments, click again to hide comments"),
	LIVE(VaadinIcons.COMMENTS, true, "<strong>LIVE</strong> - click to hide comments, click again to display comments without live updates"),
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
		return String.format("Comments display: %s", description);
	}
	
}