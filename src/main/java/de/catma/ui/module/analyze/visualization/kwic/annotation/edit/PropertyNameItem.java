package de.catma.ui.module.analyze.visualization.kwic.annotation.edit;

import com.vaadin.icons.VaadinIcons;

class PropertyNameItem implements ActionItem {
	
	private String name;
	
	public PropertyNameItem(String name) {
		super();
		this.name = name;
	}

	@Override
	public String getPropertyName() {
		return this.name;
	}
	
	@Override
	public String getActionDescription() {
		return null; // intended
	}
	
	@Override
	public String getAddValueIcon() {
		return VaadinIcons.PLUS.getHtml();
	}
	
	@Override
	public String getReplaceValueIcon() {
		return VaadinIcons.FLIP_V.getHtml();
	}
	
	@Override
	public String getDeleteValueIcon() {
		return VaadinIcons.MINUS.getHtml();
	}

	@Override
	public PropertyAction getPropertyAction() {
		return null; // intended
	}

}
