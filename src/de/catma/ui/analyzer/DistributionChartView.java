package de.catma.ui.analyzer;

import java.util.LinkedHashSet;
import java.util.Map;

import com.invient.vaadin.charts.InvientCharts;
import com.invient.vaadin.charts.InvientCharts.DecimalPoint;
import com.invient.vaadin.charts.InvientCharts.SeriesType;
import com.invient.vaadin.charts.InvientCharts.XYSeries;
import com.invient.vaadin.charts.InvientChartsConfig;
import com.invient.vaadin.charts.InvientChartsConfig.AxisBase.AxisTitle;
import com.invient.vaadin.charts.InvientChartsConfig.GeneralChartConfig.Margin;
import com.invient.vaadin.charts.InvientChartsConfig.HorzAlign;
import com.invient.vaadin.charts.InvientChartsConfig.Legend;
import com.invient.vaadin.charts.InvientChartsConfig.Legend.Layout;
import com.invient.vaadin.charts.InvientChartsConfig.LineConfig;
import com.invient.vaadin.charts.InvientChartsConfig.NumberXAxis;
import com.invient.vaadin.charts.InvientChartsConfig.NumberYAxis;
import com.invient.vaadin.charts.InvientChartsConfig.Position;
import com.invient.vaadin.charts.InvientChartsConfig.VertAlign;
import com.invient.vaadin.charts.InvientChartsConfig.XAxis;
import com.invient.vaadin.charts.InvientChartsConfig.YAxis;
import com.vaadin.ui.HorizontalLayout;

import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.queryengine.result.computation.XYValues;
import de.catma.ui.tabbedview.ClosableTab;

public class DistributionChartView extends HorizontalLayout implements ClosableTab {
	
	private InvientCharts chart;

	public DistributionChartView(int xAxisSegmentSize) {
		initComponents(xAxisSegmentSize);
	}

	private void initComponents(int xAxisSegmentSize) {
		InvientChartsConfig chartConfig = new InvientChartsConfig();
		
        chartConfig.getGeneralChartConfig().setType(SeriesType.LINE);
        
        chartConfig.getGeneralChartConfig().setMargin(new Margin());
        chartConfig.getGeneralChartConfig().getMargin().setRight(130);
        chartConfig.getGeneralChartConfig().getMargin().setBottom(25);

        chartConfig.getTitle().setX(-20);
        chartConfig.getTitle().setText("Distribution chart");
        chartConfig.getTitle().setX(-20);

        NumberXAxis xAxis = new NumberXAxis();
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

        Legend legend = new Legend();
        legend.setLayout(Layout.VERTICAL);
        Position legendPos = new Position();
        legendPos.setAlign(HorzAlign.RIGHT);
        legendPos.setVertAlign(VertAlign.TOP);
        legendPos.setX(-10);
        legendPos.setY(100);
        legend.setPosition(legendPos);
        legend.setBorderWidth(0);
        chartConfig.setLegend(legend);

//         Series data label formatter
        LineConfig lineCfg = new LineConfig();
        chartConfig.addSeriesConfig(lineCfg);
        // Tooltip formatter
        chartConfig
                .getTooltip()
                .setFormatterJsFunc(
	                "function() { "
	                        + " return '<b>' + this.series.name + '</b><br/>' "
	                        + "+(this.x-"+xAxisSegmentSize
	                        + ")+'-'+ this.x + '%: '+ this.y +' occurrences'"
	                        + "}");

        chart = new InvientCharts(chartConfig);
        chart.setSizeFull();
        chart.setStyleName("v-chart-min-width");

        addComponent(chart);
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

	}
	
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
