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
package de.catma.ui.visualizer;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import de.catma.queryengine.result.computation.Distribution;
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.ui.Slider;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.visualizer.chart.Chart;

public class DistributionChartView extends VerticalLayout implements ClosableTab {
	
	private static final int ROW_LENGTH = 3;
	private String label;
	private List<Distribution> distributions = new ArrayList<Distribution>();
	private Slider zoom;
	private List<Chart> charts = new ArrayList<Chart>();

	public DistributionChartView(String label, DistributionComputation distributionComputation) {
		this.label = label;
		this.distributions.addAll(distributionComputation.getDistributions());
		initComponents(distributionComputation.getMaxOccurrences());
		initActions();
	}

	private void initActions() {
		zoom.addValueListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				adjustSize();
			}
		});
	}

	private void adjustSize() {
		Double val = (Double) zoom.getValue();
		for (Chart chart : charts) {
			chart.setWidth(((300/50)*val)+"px");
			chart.setHeight(((400/50)*val)+"px");
		}
		markAsDirty();
	}

	private void initComponents(int maxOccurrences) {
		setSpacing(true);
		
		int rows = distributions.size()/ROW_LENGTH;
		
		if (distributions.size()%ROW_LENGTH!= 0) {
			rows++;
		}
		
		for (int rowIdx=0;rowIdx<rows;rowIdx++) {
			HorizontalLayout row = new HorizontalLayout();
			row.setSpacing(true);
			
			addComponent(row);
			row.setHeight("400px");
			row.setWidth("100%");
			
			int rowLength = Math.min(distributions.size()-((rowIdx)*ROW_LENGTH), ROW_LENGTH);
			
			for (int colIdx=0; colIdx<rowLength; colIdx++) {
				Chart chart = new Chart(distributions.get((rowIdx*ROW_LENGTH)+colIdx), maxOccurrences);
				chart.setWidth("300px");
				chart.setHeight("400px");
				row.addComponent(chart);
				charts.add(chart);
			}
		}
		
		zoom = new Slider("Zoom", 0, 100, "%");
		zoom.setValue(50.0);
		
//		addComponent(zoom);
//		setComponentAlignment(zoom, Alignment.BOTTOM_CENTER);
		
        setSizeFull();
	}
	
	public void addDistributionComputation(
			DistributionComputation distributionComputation) {
//		
//        for (XYValues<Integer, Integer> values : 
//        	distributionComputation.getXYSeriesCollection()) {
//
//        	XYSeries seriesData = new XYSeries(values.getKey().toString());
//        	
//        	for (Map.Entry<Integer, Integer> entry : values) {
//        		seriesData.addPoint(
//        			new DecimalPoint(seriesData, entry.getKey(), entry.getValue()));
//        	}
//
//        	chart.addSeries(seriesData);
//        }
//		addPlotBands(distributionComputation.getPlotBands());
	}

	public void close() { /* noop */ }

	public void addClickshortCuts() { /* noop */ }
	
	public void removeClickshortCuts() { /* noop */ }
	
	@Override
	public String toString() {
		return "Distribution analysis for " + label;
	}
}
