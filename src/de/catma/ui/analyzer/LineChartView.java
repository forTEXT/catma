package de.catma.ui.analyzer;

import java.util.Arrays;
import java.util.LinkedHashSet;

import com.invient.vaadin.charts.Color.RGB;
import com.invient.vaadin.charts.InvientCharts;
import com.invient.vaadin.charts.InvientCharts.DecimalPoint;
import com.invient.vaadin.charts.InvientCharts.Series;
import com.invient.vaadin.charts.InvientCharts.SeriesType;
import com.invient.vaadin.charts.InvientCharts.XYSeries;
import com.invient.vaadin.charts.InvientChartsConfig;
import com.invient.vaadin.charts.InvientChartsConfig.AxisBase.AxisTitle;
import com.invient.vaadin.charts.InvientChartsConfig.AxisBase.NumberPlotLine;
import com.invient.vaadin.charts.InvientChartsConfig.AxisBase.NumberPlotLine.NumberValue;
import com.invient.vaadin.charts.InvientChartsConfig.CategoryAxis;
import com.invient.vaadin.charts.InvientChartsConfig.GeneralChartConfig.Margin;
import com.invient.vaadin.charts.InvientChartsConfig.HorzAlign;
import com.invient.vaadin.charts.InvientChartsConfig.Legend;
import com.invient.vaadin.charts.InvientChartsConfig.Legend.Layout;
import com.invient.vaadin.charts.InvientChartsConfig.LineConfig;
import com.invient.vaadin.charts.InvientChartsConfig.NumberYAxis;
import com.invient.vaadin.charts.InvientChartsConfig.Position;
import com.invient.vaadin.charts.InvientChartsConfig.VertAlign;
import com.invient.vaadin.charts.InvientChartsConfig.XAxis;
import com.invient.vaadin.charts.InvientChartsConfig.YAxis;
import com.vaadin.ui.HorizontalLayout;

public class LineChartView extends HorizontalLayout {
	
	public LineChartView(String source) {
		initComponents(source);
	}

	private void initComponents(String source) {
//		lineChart = new LineChart();
//		
//		lineChart.setOption("legend", "bottom");
//		lineChart.setOption("title", "Type occurrences in " + source);
//		lineChart.addXAxisLabel("Document Fragment in percent");
//		lineChart.setOption("width", 600);
//		lineChart.setOption("height", 400);
		showLine();
	}

    private void showLine() {
        InvientChartsConfig chartConfig = new InvientChartsConfig();
        chartConfig.getGeneralChartConfig().setType(SeriesType.LINE);
        chartConfig.getGeneralChartConfig().setMargin(new Margin());
        chartConfig.getGeneralChartConfig().getMargin().setRight(130);
        chartConfig.getGeneralChartConfig().getMargin().setBottom(25);

        chartConfig.getTitle().setX(-20);
        chartConfig.getTitle().setText("Monthly Average Temperature");
        chartConfig.getSubtitle().setText("Source: WorldClimate.com");
        chartConfig.getTitle().setX(-20);

        CategoryAxis categoryAxis = new CategoryAxis();
        categoryAxis.setCategories(Arrays.asList("Jan", "Feb", "Mar", "Apr",
                "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));
        LinkedHashSet<XAxis> xAxesSet = new LinkedHashSet<InvientChartsConfig.XAxis>();
        xAxesSet.add(categoryAxis);
        chartConfig.setXAxes(xAxesSet);

        NumberYAxis numberYAxis = new NumberYAxis();
        numberYAxis.setTitle(new AxisTitle("Temperature (°C)"));
        NumberPlotLine plotLine = new NumberPlotLine("TempAt0");
        plotLine.setValue(new NumberValue(0.0));
        plotLine.setWidth(1);
        plotLine.setZIndex(1);
        plotLine.setColor(new RGB(128, 128, 128));
        numberYAxis.addPlotLine(plotLine);
        LinkedHashSet<YAxis> yAxesSet = new LinkedHashSet<InvientChartsConfig.YAxis>();
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

        // Series data label formatter
        LineConfig lineCfg = new LineConfig();
        chartConfig.addSeriesConfig(lineCfg);
        // Tooltip formatter
        chartConfig
                .getTooltip()
                .setFormatterJsFunc(
                        "function() { "
                                + " return '<b>' + this.series.name + '</b><br/>' +  this.x + ': '+ this.y +'°C'"
                                + "}");

        InvientCharts chart = new InvientCharts(chartConfig);

        XYSeries seriesData = new XYSeries("Tokyo");
        seriesData.setSeriesPoints(getPoints(seriesData, 7.0, 6.9, 9.5, 14.5,
                18.2, 21.5, 25.2, 26.5, 23.3, 18.3, 13.9, 9.6));
        chart.addSeries(seriesData);

        seriesData = new XYSeries("New York");
        seriesData.setSeriesPoints(getPoints(seriesData, -0.2, 0.8, 5.7, 11.3,
                17.0, 22.0, 24.8, 24.1, 20.1, 14.1, 8.6, 2.5));
        chart.addSeries(seriesData);

        seriesData = new XYSeries("Berlin");
        seriesData.setSeriesPoints(getPoints(seriesData, -0.9, 0.6, 3.5, 8.4,
                13.5, 17.0, 18.6, 17.9, 14.3, 9.0, 3.9, 1.0));
        chart.addSeries(seriesData);

        seriesData = new XYSeries("London");
        seriesData.setSeriesPoints(getPoints(seriesData, 3.9, 4.2, 5.7, 8.5,
                11.9, 15.2, 17.0, 16.6, 14.2, 10.3, 6.6, 4.8));
        chart.addSeries(seriesData);

        chart.setSizeFull();
        chart.setStyleName("v-chart-min-width");
//        chart.setHeight("300px");
//        chart.setWidth("300px");
        addComponent(chart);
    }

    private static LinkedHashSet<DecimalPoint> getPoints(Series series,
            double... values) {
        LinkedHashSet<DecimalPoint> points = new LinkedHashSet<DecimalPoint>();
        for (double value : values) {
            points.add(new DecimalPoint(series, value));
        }
        return points;
    }

}
