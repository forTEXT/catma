package de.catma.ui.tagmanager;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

import de.catma.core.tag.TagDefinition;
import de.catma.core.util.ColorConverter;

public class ColorButtonColumnGenerator implements ColumnGenerator {
	
	public static interface ColorButtonListener {
		public void colorButtonClicked(TagDefinition tagDefinition);
	}

	private ColorButtonListener colorButtonListener;
	
	public ColorButtonColumnGenerator(ColorButtonListener colorButtonListener) {
		this.colorButtonListener = colorButtonListener;
	}

	public Object generateCell(
			Table source, final Object itemId, Object columnId) {
		if (itemId instanceof TagDefinition) {
			ColorButton colorButton = new ColorButton(
				new ColorConverter(((TagDefinition)itemId).getColor()).toHex(), 
				new ClickListener() {
				
					public void buttonClick(ClickEvent event) {
						if (itemId instanceof TagDefinition) {
							colorButtonListener.colorButtonClicked(
									(TagDefinition)itemId);
						}
					}
			});
			
			return colorButton;
		}
		
		return new Label();
	}

}
