package de.catma.ui.analyzer;

import com.vaadin.ui.Component;

import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.ui.tabbedview.TabbedView;

public class VisualizationManagerView extends TabbedView {
	
	public VisualizationManagerView() {
		super("No visualizations available");
	}

	private int addVisulization(
		String caption, DistributionComputation distributionComputation) {
		DistributionChartView lineChartView =
				new DistributionChartView(
						distributionComputation.getPercentSegmentSize());
		int id = getTabPosition(
				addClosableTab(
						lineChartView, "Distribution analysis for " + caption));
		
		lineChartView.addDistributionComputation(distributionComputation);
		return id;
	}

	public int addVisulization(
			Integer visualizationId, String caption, 
			DistributionComputation distributionComputation) {
		if (visualizationId == null) {
			return addVisulization(caption, distributionComputation);
		}
		else {
			Component comp = getComponent(visualizationId);
			if (comp != null) {
				((DistributionChartView)comp).addDistributionComputation(
						distributionComputation);
				return visualizationId;
			}
			else {
				return addVisulization(caption, distributionComputation);
			}
		}
	}
	
}
