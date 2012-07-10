package de.catma.ui.analyzer;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.invient.vaadin.charts.Color.RGBA;
import com.invient.vaadin.charts.InvientCharts;
import com.invient.vaadin.charts.InvientCharts.DecimalPoint;
import com.invient.vaadin.charts.InvientCharts.SeriesType;
import com.invient.vaadin.charts.InvientCharts.XYSeries;
import com.invient.vaadin.charts.InvientChartsConfig;
import com.invient.vaadin.charts.InvientChartsConfig.AxisBase.AxisTitle;
import com.invient.vaadin.charts.InvientChartsConfig.AxisBase.NumberPlotBand;
import com.invient.vaadin.charts.InvientChartsConfig.AxisBase.NumberPlotBand.NumberRange;
import com.invient.vaadin.charts.InvientChartsConfig.AxisBase.PlotLabel;
import com.invient.vaadin.charts.InvientChartsConfig.LineConfig;
import com.invient.vaadin.charts.InvientChartsConfig.NumberXAxis;
import com.invient.vaadin.charts.InvientChartsConfig.NumberYAxis;
import com.invient.vaadin.charts.InvientChartsConfig.XAxis;
import com.invient.vaadin.charts.InvientChartsConfig.YAxis;
import com.vaadin.ui.HorizontalLayout;

import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.queryengine.result.computation.PlotBand;
import de.catma.queryengine.result.computation.XYValues;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.util.ColorConverter;

public class DistributionChartView extends HorizontalLayout implements ClosableTab {
	
	private InvientCharts chart;
	private NumberXAxis xAxis;

	public DistributionChartView(int xAxisSegmentSize) {
		initComponents(xAxisSegmentSize);
	}

	private void initComponents(int xAxisSegmentSize) {
		InvientChartsConfig chartConfig = new InvientChartsConfig();
		
        chartConfig.getGeneralChartConfig().setType(SeriesType.LINE);
        
//        chartConfig.getGeneralChartConfig().setMargin(new Margin());
//        chartConfig.getGeneralChartConfig().getMargin().setRight(130);
//        chartConfig.getGeneralChartConfig().getMargin().setBottom(150);

        chartConfig.getTitle().setX(-20);
        chartConfig.getTitle().setText("Distribution chart");
        chartConfig.getTitle().setX(-20);

        xAxis = new NumberXAxis();
        xAxis.setMin(0.0);
        xAxis.setMax(100.0);
        LinkedHashSet<XAxis> xAxesSet = new LinkedHashSet<InvientChartsConfig.XAxis>();
        xAxesSet.add(xAxis);
        chartConfig.setXAxes(xAxesSet);

        NumberYAxis numberYAxis = new NumberYAxis();
        numberYAxis.setTitle(new AxisTitle("Type Occurrences"));
        numberYAxis.setMin(0.0);
        LinkedHashSet<YAxis> yAxesSet = 
        		new LinkedHashSet<InvientChartsConfig.YAxis>();
        yAxesSet.add(numberYAxis);
        chartConfig.setYAxes(yAxesSet);

//        Legend legend = new Legend();
//        legend.setLayout(Layout.VERTICAL);
//        legend.setWidth(380);
//        Position legendPos = new Position();
//        legendPos.setAlign(HorzAlign.RIGHT);
//        legendPos.setVertAlign(VertAlign.TOP);
//        legendPos.setX(255);
//        legendPos.setY(100);
//        
//        legend.setPosition(legendPos);
//        legend.setBorderWidth(0);
//        chartConfig.setLegend(legend);

//         Series data label formatter
        LineConfig lineCfg = new LineConfig();
        chartConfig.addSeriesConfig(lineCfg);
        // Tooltip formatter
        chartConfig
                .getTooltip()
                .setFormatterJsFunc(
	                "function() { "
	                        + " return '<b>' + this.series.name + '</b><br/>' "
	                        + "+(this.x-"+(xAxisSegmentSize-(xAxisSegmentSize/2))
	                        + ")+'-'+ (this.x + " + (xAxisSegmentSize/2) + ") + "
	                        + "'%: '+ this.y +' occurrences'"
	                        + "}");
        chart = new InvientCharts(chartConfig);
        chart.setSizeFull();
//        chart.setHeight("400px");
//        chart.setWidth("600px");

        addComponent(chart);
        setSizeFull();
	}
	
	public void addDistributionComputation(
			DistributionComputation distributionComputation) {
		
        for (XYValues<Integer, Integer> values : 
        	distributionComputation.getXYSeriesCollection()) {

        	XYSeries seriesData = new XYSeries(values.getKey().toString());
        	
        	for (Map.Entry<Integer, Integer> entry : values) {
        		seriesData.addPoint(
        			new DecimalPoint(seriesData, entry.getKey(), entry.getValue()));
        	}

        	chart.addSeries(seriesData);
        }
		addPlotBands(distributionComputation.getPlotBands());
	}
	
	private void addPlotBands(Set<PlotBand> plotBands) {
		for (PlotBand plotBand : plotBands){
			addPlotBand(plotBand);
		}
	}

	private void addPlotBand(PlotBand plotBand) {
	    NumberPlotBand numberPlotBand = new NumberPlotBand(plotBand.getId());
		if (!xAxis.getPlotBands().contains(numberPlotBand)) {
		    numberPlotBand.setRange(new NumberRange(plotBand.getStart(), plotBand.getEnd()));
		    String label = plotBand.getLabel().substring(0,Math.min(plotBand.getLabel().length(), 10));
		    if (label.length() < plotBand.getLabel().length()) {
		    	label += "...";
		    }
		    PlotLabel plotLabel = new PlotLabel(label);
		    plotLabel.setRotation(-90);
		    numberPlotBand.setLabel(plotLabel);
		    int[] color = ColorConverter.getRandomNonDarkColor();
		    while (colorExists(color)) {
		    	color = ColorConverter.getRandomNonDarkColor();
		    }
		    numberPlotBand.setColor(new RGBA(color[0], color[1], color[2], 0.2f));
		    xAxis.addPlotBand(numberPlotBand);
		}
	}
	
	private boolean colorExists(int[] color) {
		for (NumberPlotBand numberPlotBand : xAxis.getPlotBands()) {
			
			RGBA existingColor = (RGBA)numberPlotBand.getColor();
			if ((existingColor.getRed() == color[0]) 
					&& (existingColor.getGreen() == color[1])
					&& (existingColor.getBlue() == color[2])) {
				return true;
			}
			
		}
		return false;
	}

	public void close() {
		// TODO Auto-generated method stub
		
	}

	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }
}
