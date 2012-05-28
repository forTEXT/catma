package de.catma.ui.dialog;

import java.util.Map;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.ClientWidget;

import de.catma.ui.client.ui.tagmanager.VColorField;
import de.catma.ui.client.ui.tagmanager.shared.ColorFieldMessageAttribute;


@ClientWidget(VColorField.class)
public class ColorField extends AbstractField {
	
	private boolean dirty;
	
	public ColorField(String hexColor) {
		if ((hexColor != null) && (!hexColor.isEmpty())) {
			setValue(hexColor);
		}
	}
	
	public ColorField() {
	}
	
	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);
		if ((getValue() != null) && (dirty || target.isFullRepaint())) {
			target.addAttribute(
					ColorFieldMessageAttribute.COLOR_SET.name(), 
					getValue().toString());
			dirty = false;
		}
	}
	
	@Override
	public void changeVariables(Object source, Map<String, Object> variables) {
		super.changeVariables(source, variables);
		
		if (variables.containsKey(ColorFieldMessageAttribute.COLOR_SET.name())) {
			String hexColor = (String)variables.get(
						ColorFieldMessageAttribute.COLOR_SET.name());
			setValue(hexColor, false);
		}
	}
	

	@Override
	public Class<?> getType() {
		return String.class;
	}
	
	@Override
	public void setValue(Object newValue) throws ReadOnlyException,
			ConversionException {
		super.setValue(newValue, true);
		dirty = true;
	}
	
	public String getHexColor() {
		return getValue().toString();
	}
}
