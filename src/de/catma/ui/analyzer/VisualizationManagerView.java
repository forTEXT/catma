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
package de.catma.ui.analyzer;

import java.util.HashSet;
import java.util.Iterator;

import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;

import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tabbedview.TabbedView;

public class VisualizationManagerView extends TabbedView {
	
	public VisualizationManagerView() {
		super("No visualizations available. To generate visualization use the Analyzer.");
	}

	private int addVisualization(
		String caption, DistributionComputation distributionComputation) {
		DistributionChartView distChartView =
				new DistributionChartView(
						distributionComputation.getPercentSegmentSize(), caption);
		int id = getTabPosition(
				addClosableTab(
						distChartView, "Distribution analysis for " + caption));
		
		distChartView.addDistributionComputation(distributionComputation);
		return id;
	}

	public int addVisualization(
			Integer visualizationId, String caption, 
			DistributionComputation distributionComputation) {
		if (visualizationId == null) {
			return addVisualization(caption, distributionComputation);
		}
		else {
			Component comp = getComponent(visualizationId);
			if (comp != null) {
				((DistributionChartView)comp).addDistributionComputation(
						distributionComputation);
				return visualizationId;
			}
			else {
				return addVisualization(caption, distributionComputation);
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
		Iterator<Component>  compIter = tabsheet.getComponentIterator();
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
					"Distribution analysis for " + ((DistributionChartView)c).getLabel());
			}

		}
	}
}
