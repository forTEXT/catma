
package de.catma.ui.dialog;

import java.text.NumberFormat;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;

import de.catma.ui.component.Slider;

public class SliderInputDialog extends AbstractOkCancelDialog<Double> {
	
	private Slider slider;

	public SliderInputDialog(
			String caption, 
			String inputLabel, 
			int min,
			int max,
			Double initialValue,
			String unit,
			SaveCancelListener<Double> saveCancelListener) {
		super(caption, saveCancelListener);
		this.slider = new Slider(inputLabel, min, max, unit);
		this.slider.setValue(initialValue);
	}
	
	public SliderInputDialog(
			String caption, 
			String inputLabel, 
			int min,
			int max,
			String unit,
			SaveCancelListener<Double> saveCancelListener) {
		super(caption, saveCancelListener);
		this.slider = new Slider(inputLabel, min, max, unit);
	}
	
	public SliderInputDialog(
			String caption, 
			String inputLabel, 
			int min,
			int max,
			Double initialValue,
			String unit,
			NumberFormat formatter,
			SaveCancelListener<Double> saveCancelListener) {
		super(caption, saveCancelListener);
		this.slider = new Slider(inputLabel, min, max, unit, formatter);
		this.slider.setValue(initialValue);
	}

	@Override
	protected void addContent(ComponentContainer content) {
		this.slider.setWidth("100%");
		content.addComponent(slider);
		if (content instanceof AbstractOrderedLayout aol) {
			aol.setExpandRatio(slider, 1.0f);
		}
	}
	
	@Override
	protected Double getResult() {
		return slider.getValue();
	}

	public void setResolution(int resolution) {
		slider.setResolution(resolution);
	}

	public void setValue(Double value) throws ValueOutOfBoundsException {
		slider.setValue(value);
	}

	protected void layoutWindow(){
		setWidth("20%");
		setHeight("30%");
	}
	
}
