package de.catma.ui;

import com.vaadin.data.Property.ConversionException;
import com.vaadin.data.Property.ReadOnlyException;
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
	
	public Slider(String caption, int min, int max) {
		this.setCaption(caption);
		
		HorizontalLayout sliderLayout = new HorizontalLayout();
		sliderLayout.setSpacing(true);
		this.sliderComp = new com.vaadin.ui.Slider(min, max);
		minLabel = new Label(String.valueOf(min));
		maxLabel = new Label(String.valueOf(max));
		
		sliderLayout.addComponent(minLabel);
		sliderLayout.addComponent(sliderComp);
		sliderLayout.addComponent(maxLabel);
		
		addComponent(sliderLayout);
		setComponentAlignment(sliderLayout, Alignment.MIDDLE_CENTER);
		
		final Label current = new Label(sliderComp.getValue().toString());
		current.setWidth("100%");
		current.addStyleName("centered-text");
		
		addComponent(current);
		setComponentAlignment(current, Alignment.MIDDLE_CENTER);
		
		sliderComp.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				current.setValue(event.getProperty().getValue());
			}
		});
		
	}

	public void setMax(double max) {
		sliderComp.setMax(max);
		maxLabel.setValue(max);
	}

	public void setMin(double min) {
		sliderComp.setMin(min);
		minLabel.setValue(min);
	}

	public void setResolution(int resolution) {
		sliderComp.setResolution(resolution);
	}

	public Object getValue() {
		return sliderComp.getValue();
	}

	public void addListener(ValueChangeListener listener) {
		sliderComp.addListener(listener);
	}

	public void setValue(Double value) throws ValueOutOfBoundsException {
		sliderComp.setValue(value);
	}

	public void setValue(double value) throws ValueOutOfBoundsException {
		sliderComp.setValue(value);
	}

	public void setValue(Object newValue) throws ReadOnlyException,
			ConversionException {
		sliderComp.setValue(newValue);
	}

	public void setImmediate(boolean immediate) {
		super.setImmediate(immediate);
		sliderComp.setImmediate(immediate);
	}
	
	
	
	
	
	
}
