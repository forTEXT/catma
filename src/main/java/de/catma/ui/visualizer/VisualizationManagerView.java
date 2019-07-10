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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;

import de.catma.document.repository.Repository;
import de.catma.document.source.KeywordInContext;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.queryengine.result.computation.DistributionSelectionListener;
import de.catma.ui.analyzer.QueryOptionsProvider;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tabbedview.TabbedView;
import de.catma.ui.visualizer.chart.DistributionChartView;
import de.catma.ui.visualizer.doubletree.DoubleTreeView;
import de.catma.ui.visualizer.vega.VegaView;

public class VisualizationManagerView extends TabbedView {
	
	public VisualizationManagerView() {
		super(Messages.getString("VisualizationManagerView.noVizAvailable")); //$NON-NLS-1$
	}

	private int addVisualization(
		String caption,
		DistributionComputation distributionComputation, 
		DistributionSelectionListener distributionSelectionListener) {
		
		DistributionChartView distChartView =
				new DistributionChartView(caption, distributionComputation, distributionSelectionListener);
		int id = getTabPosition(
				addClosableTab(
						distChartView, distChartView.toString()));
		
		return id;
	}

	public int addVisualization(
			Integer visualizationId, String caption, 
			DistributionComputation distributionComputation, DistributionSelectionListener distributionSelectionListener) {
		if (visualizationId == null) {
			return addVisualization(caption, distributionComputation, distributionSelectionListener);
		}
		else {
			Component comp = getComponent(visualizationId);
			if (comp != null) {
				((DistributionChartView)comp).addDistributionComputation(
						distributionComputation);
				return visualizationId;
			}
			else {
				return addVisualization(caption, distributionComputation, distributionSelectionListener);
			}
		}
	}

	/** 
	 * Additional handling for charts because when closing a non active chart,
	 * the active chart becomes empty. In addition to the standard behaviour this
	 * method removes and reinserts all remaining {@link DistributionChartView}s.
	 * 
	 * @see de.catma.ui.tabbedview.TabbedView#onTabClose(com.vaadin.ui.TabSheet, com.vaadin.ui.Component)
	 */
	@Override
	public void onTabClose(TabSheet tabsheet, Component tabContent) {
		Iterator<Component>  compIter = tabsheet.iterator();
		HashSet<Component> contents = new HashSet<Component>();
		while (compIter.hasNext()){
			contents.add(compIter.next());
		}
		boolean hasLeft = (tabsheet.getComponentCount() > 1);
		
		super.onTabClose(tabsheet, tabContent);
		
		if (hasLeft) {
			tabsheet.removeAllComponents();
			contents.remove(tabContent);
			
			for (Component c : contents) {
				addClosableTab(
					(ClosableTab)c, 
					c.toString());
			}

		}
	}

	public void addDoubleTree(List<KeywordInContext> kwics) {
		DoubleTreeView dtView = new DoubleTreeView(kwics);
		
		addClosableTab(dtView, dtView.toString());
		
	}

	public void addVega(QueryResult queryResult, QueryOptionsProvider queryOptionsProvider, Repository repository) {
		VegaView vegaView = new VegaView(queryResult, queryOptionsProvider, repository);
		
		addClosableTab(vegaView, vegaView.toString());
	}
}
