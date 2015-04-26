package de.catma.ui.visualizer.chart;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.ui.AbstractComponent;

import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.computation.Distribution;
import de.catma.queryengine.result.computation.DistributionSelectionListener;
import de.catma.queryengine.result.computation.XYValues;
import de.catma.ui.client.ui.visualizer.chart.ChartClientRpc;
import de.catma.ui.client.ui.visualizer.chart.ChartServerRpc;
import de.catma.util.IDGenerator;

public class Chart extends AbstractComponent {
	private ChartServerRpc chartServerRpc = new ChartServerRpc() {
		
		@Override
		public void onChartPointClick(String seriesName, int x, int y) {
			fireResultRowSelected(seriesName, x, y);
		}
	};
	private String chartId;
	private Distribution distribution;
	private int maxOccurrences;
//	private double lenseZoomFactor;
	private DistributionSelectionListener distributionSelectionListener; 
	
	public Chart(Distribution distribution, int maxOccurrences, DistributionSelectionListener distributionSelectionListener) {
		chartId = "ChartWidget" + new IDGenerator().generate();
		registerRpc(chartServerRpc);
		
		this.distribution = distribution;
		this.maxOccurrences = maxOccurrences;
		this.distributionSelectionListener = distributionSelectionListener;
	}
	
	private void fireResultRowSelected(String key, int x, int y) {
		List<QueryResultRow> rows = distribution.getQueryResultRows(key, x);
		distributionSelectionListener.queryResultRowsSelected(distribution.getLabel(), rows, x, y);
	}

	@Override
	public void beforeClientResponse(boolean initial) {
		super.beforeClientResponse(initial);
		if (initial) {
			getRpcProxy(ChartClientRpc.class).init(
					chartId,
					createConfiguration(distribution, maxOccurrences));
		}
	}

	private String createConfiguration(Distribution distribution, int maxOccurrences) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode configuration = 
				mapper.readValue(
					"{"+
						"chart: {"+
						    "renderTo: '"+ chartId +"',"+
						    "zoomType: 'xy',"+
						    "spacingBottom: 50"+
					    "},"+
					    "xAxis: {"+
					    	"tickInterval : "+ distribution.getSegmentSizeInPercent() + "," +
					        "max: 100," +
					    	"min: 0" +
					    "},"+
					    "plotOptions: {"+
					    	"series: {"+
					        	"allowPointSelect: false,"+
					        	"point: {"+
					        		"events: {"+
					        			"click: null"+
					        		"}"+
					        	"}"+
					         "}"+
					    "},"+
					    "title: {"+
				        	"text: '"+distribution.getLabel()+"',"+
				        	"verticalAlign: bottom" +
				       	"},"+
			       		"yAxis: {"+
			       			"title: {"+
			       				"text: 'Occurrences'"+
				            "},"+
			       			"min: 0," +
			       			"max: "+ maxOccurrences +
				        "}"+
				   	"}", ObjectNode.class);	

			ArrayNode seriesArray = createSeriesArray(distribution);
			
			configuration.set("series", seriesArray);
			
			return configuration.toString();
		}
		catch (IOException jse) {
			throw new IllegalStateException(jse);
		}
	}
	
	private ArrayNode createSeriesArray(Distribution distribution) {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode seriesArray = factory.arrayNode();
		for (XYValues<Integer, Integer, QueryResultRow> values : distribution.getXySeries()) {
			seriesArray.add(createSeries(values));
		}

		return seriesArray;

	}

	private ObjectNode createSeries(XYValues<Integer, Integer, QueryResultRow> values) {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode dataArray = factory.arrayNode();
		for (Entry<Integer,Integer> pair : values) {
			ArrayNode valueArray = factory.arrayNode();
			valueArray.add(Double.valueOf(pair.getKey()));
			valueArray.add(Double.valueOf(pair.getValue()));
			dataArray.add(valueArray);
		}
		ObjectNode seriesObject = factory.objectNode();
		seriesObject.set("data", dataArray);
		seriesObject.put("name", values.getKey().toString());

		return seriesObject;
	}

	public void addSeries(XYValues<Integer, Integer, QueryResultRow> series) {
		this.distribution.add(series);
		getRpcProxy(ChartClientRpc.class).addSeries(
			createSeries(series).toString());		
	}
	
	public void setMaxOccurrences(int maxOccurrences) {
		this.maxOccurrences = maxOccurrences;
		getRpcProxy(ChartClientRpc.class).setYAxisExtremes(0, maxOccurrences);
	}
	
	public void setLenseZoomFactor(double factor) {
		getRpcProxy(ChartClientRpc.class).setLenseZoom(factor);
	}
}
