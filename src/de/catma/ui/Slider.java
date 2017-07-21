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

import java.text.DecimalFormat;
import java.text.NumberFormat;

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
	private NumberFormat formatter;
	
	public Slider(String caption, int min, int max, String unit) {
		this(caption, min, max, unit, new DecimalFormat("#")); //$NON-NLS-1$
	}
	
	/**
	 * @param caption a caption above the slider
	 * @param min minimum context
	 * @param max maximum context
	 * @param unit description of the unit, displayed next to the current value
	 */
	public Slider(String caption, int min, int max, final String unit, NumberFormat formatter) {
		addStyleName("c-slider"); //$NON-NLS-1$
		if (caption == null) {
			addStyleName("hidden-caption-c-slider"); //$NON-NLS-1$
		}
		this.setCaption(caption);
		this.formatter = formatter;
		setSizeUndefined();
		HorizontalLayout sliderLayout = new HorizontalLayout();
		sliderLayout.setSpacing(true);
		this.sliderComp = new com.vaadin.ui.Slider(min, max);
		minLabel = new Label(formatter.format(min));
		maxLabel = new Label(formatter.format(max));
		
		sliderLayout.addComponent(minLabel);
		sliderLayout.addComponent(sliderComp);
		sliderLayout.addComponent(maxLabel);
		
		addComponent(sliderLayout);
		setComponentAlignment(sliderLayout, Alignment.MIDDLE_CENTER);
		
		final Label current = new Label(formatter.format(sliderComp.getValue()));
		current.setWidth("100%"); //$NON-NLS-1$
		
		current.addStyleName("slider-centered-text"); //$NON-NLS-1$
		
		addComponent(current);
		setComponentAlignment(current, Alignment.MIDDLE_CENTER);
		
		sliderComp.addValueChangeListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				current.setValue(
					Slider.this.formatter.format(
						(Double)event.getProperty().getValue())
						+ (unit.isEmpty()?"":" ")  //$NON-NLS-1$ //$NON-NLS-2$
						+ unit);
			}
		});
		
	}

	public void setMax(double max) {
		sliderComp.setMax(max);
		maxLabel.setValue(formatter.format(max));
	}

	public void setMin(double min) {
		sliderComp.setMin(min);
		minLabel.setValue(formatter.format(min));
	}

	public void setResolution(int resolution) {
		sliderComp.setResolution(resolution);
	}

	public Object getValue() {
		return sliderComp.getValue();
	}

	public void addValueListener(ValueChangeListener listener) {
		sliderComp.addValueChangeListener(listener);
	}

	public void setValue(Double value) throws ValueOutOfBoundsException {
		sliderComp.setValue(value);
	}

	public void setImmediate(boolean immediate) {
		sliderComp.setImmediate(immediate);
	}
	
}
