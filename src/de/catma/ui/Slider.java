/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.VerticalLayout;


public class Slider extends VerticalLayout {

	private com.vaadin.ui.Slider sliderComp;
	private Label minLabel;
	private Label maxLabel;
	
	public Slider(String caption, int min, int max, final String unit) {
		this.setCaption(caption);
		setSizeUndefined();
		HorizontalLayout sliderLayout = new HorizontalLayout();
		sliderLayout.setSpacing(true);
		this.sliderComp = new SliderComp(min, max);
		minLabel = new Label(String.valueOf(min));
		maxLabel = new Label(String.valueOf(max));
		
		sliderLayout.addComponent(minLabel);
		sliderLayout.addComponent(sliderComp);
		sliderLayout.addComponent(maxLabel);
		
		addComponent(sliderLayout);
		setComponentAlignment(sliderLayout, Alignment.MIDDLE_CENTER);
		
		final Label current = new Label(sliderComp.getValue().toString());
		current.setWidth("100%");
		
		current.addStyleName("slider-centered-text");
		
		addComponent(current);
		setComponentAlignment(current, Alignment.MIDDLE_CENTER);
		
		sliderComp.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				current.setValue(
					event.getProperty().getValue() + (unit.isEmpty()?"":" ") + unit);
			}
		});
		
	}

	public void setMax(double max) {
		sliderComp.setMax(max);
		maxLabel.setValue(String.valueOf(max));
	}

	public void setMin(double min) {
		sliderComp.setMin(min);
		minLabel.setValue(String.valueOf(min));
	}

	public void setResolution(int resolution) {
		sliderComp.setResolution(resolution);
	}

	public Object getValue() {
		return sliderComp.getValue();
	}

	public void addListener(ValueChangeListener listener) {
		sliderComp.addValueChangeListener(listener);
	}

	public void setValue(Double value) throws ValueOutOfBoundsException {
		sliderComp.setValue(value);
	}

	public void setImmediate(boolean immediate) {
		super.setImmediate(immediate);
		sliderComp.setImmediate(immediate);
	}
	
}
