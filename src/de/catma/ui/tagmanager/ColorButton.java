package de.catma.ui.tagmanager;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.Button;
import com.vaadin.ui.ClientWidget;
import com.vaadin.ui.ClientWidget.LoadStyle;

import de.catma.ui.client.ui.tagmanager.VColorButton;


@ClientWidget(value = VColorButton.class, loadStyle = LoadStyle.EAGER)
public class ColorButton extends Button {

	private String color;
	
	public ColorButton(String color, ClickListener listener) {
		super("", listener);
		this.color = color;
	}

	public void setColor(String color) {
		this.color = color;
		requestRepaint();
	}
	
	@Override
	public synchronized void paintContent(PaintTarget target)
			throws PaintException {
		super.paintContent(target);
		
		target.addAttribute(VColorButton.COLOR_ATTRIBUTE, color);
	}

	
}
