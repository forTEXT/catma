package de.catma.ui.client.ui.visualizer.chart;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FocusWidget;

public class ChartWidget extends FocusWidget {

	private ChartJs chartJs;
	
	public ChartWidget() {
		super(Document.get().createDivElement());
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
}
