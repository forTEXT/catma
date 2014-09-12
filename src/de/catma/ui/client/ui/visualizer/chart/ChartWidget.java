package de.catma.ui.client.ui.visualizer.chart;

import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;

import de.catma.ui.client.ui.zoomableverticallayout.ZoomHandler;
import de.catma.ui.client.ui.zoomableverticallayout.impl.FirefoxImplZoomHandler;

public class ChartWidget extends FocusWidget {

	private static ZoomHandler zoomHandlerimpl = 
			GWT.create(FirefoxImplZoomHandler.class);  

	private ChartJs chartJs;

	private double lenseZoomFactor = 1.0;

	
	public ChartWidget() {
		super(Document.get().createDivElement());
		sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT);

		addMouseOverHandler(new MouseOverHandler() {
			
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (lenseZoomFactor > 1.0) {
					zoomHandlerimpl.zoom(getElement(), lenseZoomFactor);
				}
			}
		});
		
		addMouseOutHandler(new MouseOutHandler() {
			
			@Override
			public void onMouseOut(MouseOutEvent event) {
				if (lenseZoomFactor > 1.0) {
					zoomHandlerimpl.zoom(getElement(), 1.0);
				}
			}
		});
	}

	void init(String chartId, String configuration, ChartClickListener chartClickListener) {
		getElement().setId(chartId);
		chartJs = ChartJs.create(configuration, chartClickListener);
	}

	public void addSeries(String series) {
		chartJs.addSeries(series);
	}

	public void setYAxisExtremes(int min, int max) {
		chartJs.setYAxisExtremes(min, max);
	}

	public void setLenseZoom(double factor) {
		this.lenseZoomFactor = factor;
	}
}
