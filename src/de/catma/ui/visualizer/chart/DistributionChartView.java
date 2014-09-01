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
package de.catma.ui.visualizer.chart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.computation.Distribution;
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.queryengine.result.computation.DistributionSelectionListener;
import de.catma.queryengine.result.computation.XYValues;
import de.catma.ui.Slider;
import de.catma.ui.component.ZoomableVerticalLayout;
import de.catma.ui.tabbedview.ClosableTab;

public class DistributionChartView extends VerticalLayout implements ClosableTab {
	
	private static final int ROW_LENGTH = 3;
	private String label;
	private Slider zoom;
	private Map<String, Chart> charts = new HashMap<String, Chart>();
	private ZoomableVerticalLayout zoomPanel;
	private int maxOccurrences;
	private DistributionSelectionListener distributionSelectionListener;
	
	public DistributionChartView(
		String label,
		DistributionComputation distributionComputation,
		DistributionSelectionListener distributionSelectionListener) {
		this.label = label;
		this.maxOccurrences = distributionComputation.getMaxOccurrences();
		this.distributionSelectionListener = distributionSelectionListener;
		initComponents(distributionComputation);
		initActions();
	}
	
	@Override
	public void beforeClientResponse(boolean initial) {
		super.beforeClientResponse(initial);
		Double val = (Double) zoom.getValue();
		if (val.intValue() != 100) {
			handleZoomRequest();
		}
	}

	private void initActions() {
		zoom.addValueListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				handleZoomRequest();
			}
		});
	}

	private void handleZoomRequest() {
		Double val = (Double) zoom.getValue();
		zoomPanel.zoom(val/100);
	}

	private void initComponents(DistributionComputation distributionComputation) {
		setSpacing(true);
		setMargin(new MarginInfo(true, false, false, false));
		addStyleName("distributionchartviewpanel");

		zoom = new Slider("Zoom", 0, 100, "%");
		zoom.setValue(100.0);
		
		addComponent(zoom);
		setComponentAlignment(zoom, Alignment.TOP_CENTER);

		zoomPanel = new ZoomableVerticalLayout();
		zoomPanel.setSizeFull();
		
		List<Distribution> distributions = distributionComputation.getDistributions();
		
		int rows = distributions.size()/ROW_LENGTH;
		
		if (distributions.size()%ROW_LENGTH!= 0) {
			rows++;
		}
		
		for (int rowIdx=0;rowIdx<rows;rowIdx++) {
			HorizontalLayout row = new HorizontalLayout();
			row.setSpacing(true);
			
			zoomPanel.addComponent(row);
			row.setWidth("100%");
			
			int rowLength = Math.min(distributions.size()-((rowIdx)*ROW_LENGTH), ROW_LENGTH);
			
			for (int colIdx=0; colIdx<rowLength; colIdx++) {
				Distribution distribution = 
						distributions.get((rowIdx*ROW_LENGTH)+colIdx);
				Chart chart = new Chart(distribution, maxOccurrences, distributionSelectionListener);
				chart.setWidth("300px");
				chart.setHeight("400px");
				row.addComponent(chart);
				charts.put(distribution.getId(), chart);
			}
		}
		
		addComponent(zoomPanel);
		
        setSizeFull();
	}
	
	public void addDistributionComputation(
			DistributionComputation distributionComputation) {
		
		for (Distribution distribution : distributionComputation.getDistributions()) {
			Chart chart = charts.get(distribution.getId());
			for (XYValues<Integer, Integer, QueryResultRow> series : distribution.getXySeries()) {
				chart.addSeries(series);
			}
		}
		
		if (maxOccurrences < distributionComputation.getMaxOccurrences()) {
			maxOccurrences = distributionComputation.getMaxOccurrences();
			for (Chart chart : charts.values()) {
				chart.setMaxOccurrences(maxOccurrences);
			}
		}
	}

	public void close() { /* noop */ }

	public void addClickshortCuts() { /* noop */ }
	
	public void removeClickshortCuts() { /* noop */ }
	
	@Override
	public String toString() {
		return "Distribution analysis for " + label;
	}
}
