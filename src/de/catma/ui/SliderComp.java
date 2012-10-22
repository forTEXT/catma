package de.catma.ui;

import com.vaadin.ui.ClientWidget;
import com.vaadin.ui.Slider;

import de.catma.ui.client.ui.VSliderComp;

@ClientWidget(VSliderComp.class)
public class SliderComp extends Slider {


	public SliderComp() {
	}

	public SliderComp(double min, double max, int resolution) {
		super(min, max, resolution);
	}

	public SliderComp(int min, int max) {
		super(min, max);
	}

	public SliderComp(String caption, int min, int max) {
		super(caption, min, max);
	}

	public SliderComp(String caption) {
		super(caption);
	}

}
