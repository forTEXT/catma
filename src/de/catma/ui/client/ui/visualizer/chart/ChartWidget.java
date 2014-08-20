package de.catma.ui.client.ui.visualizer.chart;

import java.util.Date;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FocusWidget;

import de.catma.ui.client.ui.visualizer.chart.shared.ChartOptions;

public class ChartWidget extends FocusWidget {

	private ChartJs chartJs;
	private HandlerRegistration attachHandlerReg;
	
	public ChartWidget() {
		super(Document.get().createDivElement());
		long chartId = new Date().getTime();
		getElement().setId("ChartWidget"+chartId);
	}

	void init(final ChartOptions chartOptions) {
		
		if (isAttached()) {
			chartJs = ChartJs.create(
					getElement().getId(), chartOptions);
			Logger.getLogger(ChartWidget.class.getName()).info("ATTACHED");
		}
		else {
			Logger.getLogger(ChartWidget.class.getName()).info("NOT ATTACHED");
			attachHandlerReg = addAttachHandler(new AttachEvent.Handler() {
				
				@Override
				public void onAttachOrDetach(AttachEvent event) {
					if (event.isAttached()) {
						chartJs = ChartJs.create(
								getElement().getId(), 
								chartOptions);
						if (attachHandlerReg != null) {
							attachHandlerReg.removeHandler();
						}				}
					
				}
			});
		}
	}
}
