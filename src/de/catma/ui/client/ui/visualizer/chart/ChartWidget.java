package de.catma.ui.client.ui.visualizer.chart;

import java.util.Date;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FocusWidget;

public class ChartWidget extends FocusWidget {

	private ChartJs chartJs;
	private HandlerRegistration attachHandlerReg;
	
	public ChartWidget() {
		super(Document.get().createDivElement());
		long chartId = new Date().getTime();
		getElement().setId("ChartWidget"+chartId);
	}

	void init(final double tickInterval, final String series) {
		if (isAttached()) {
			chartJs = ChartJs.create(getElement().getId(), tickInterval, series);
		}
		else {
			attachHandlerReg = addAttachHandler(new AttachEvent.Handler() {
				
				@Override
				public void onAttachOrDetach(AttachEvent event) {
					if (event.isAttached()) {
						chartJs = ChartJs.create(getElement().getId(), tickInterval, series);
						if (attachHandlerReg != null) {
							attachHandlerReg.removeHandler();
						}				}
					
				}
			});
		}
	}
}
