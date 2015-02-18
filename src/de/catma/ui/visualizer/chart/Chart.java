package de.catma.ui.visualizer.chart;

import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.xml.internal.txw2.IllegalSignatureException;
import com.vaadin.ui.AbstractComponent;

import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.computation.Distribution;
import de.catma.queryengine.result.computation.DistributionSelectionListener;
import de.catma.queryengine.result.computation.PlotBand;
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
					createConfiguration());
		}
	}

	private String createConfiguration() {
		try {
			JSONObject configuration = 
				new JSONObject(
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
				   	"}");	
			if (!distribution.getPlotBands().isEmpty()) {
				JSONArray plotBandsArray = createPlotBandsArray();
				configuration.getJSONObject("xAxis").put("plotBands", plotBandsArray);
			}
			JSONArray seriesArray = createSeriesArray();
			
			configuration.put("series", seriesArray);
			
			return configuration.toString();
		}
		catch (JSONException jse) {
			throw new IllegalStateException(jse);
		}
	}
	
	private JSONArray createPlotBandsArray() {
		try {
			JSONArray plotBandsArray = new JSONArray();
			for (PlotBand<Integer> plotBand : distribution.getPlotBands()) {
				JSONObject plotBandObject = new JSONObject();
				plotBandObject.put("color", "#"+plotBand.getHexColor());
				plotBandObject.put("from", plotBand.getStartPosition());
				plotBandObject.put("to", plotBand.getEndPosition());
				JSONObject labelObject = new JSONObject();
				labelObject.put("text", plotBand.getLabel());
				plotBandObject.put("label", labelObject);
				
				plotBandsArray.put(plotBandObject);
			}
			return plotBandsArray;
		}
		catch (JSONException je) {
			throw new IllegalSignatureException(je);
		}
	}

	private JSONArray createSeriesArray() {
		JSONArray seriesArray = new JSONArray();
		for (XYValues<Integer, Integer, QueryResultRow> values : distribution.getXySeries()) {
			seriesArray.put(createSeries(values));
		}

		return seriesArray;

	}

	private JSONObject createSeries(XYValues<Integer, Integer, QueryResultRow> values) {
		try {
			JSONArray dataArray = new JSONArray();
			for (Entry<Integer,Integer> pair : values) {
				JSONArray valueArray = new JSONArray();
				valueArray.put(Double.valueOf(pair.getKey()));
				valueArray.put(Double.valueOf(pair.getValue()));
				dataArray.put(valueArray);
			}
			JSONObject seriesObject = new JSONObject();
			seriesObject.put("data", dataArray);
			seriesObject.put("name", values.getKey().toString());
	
			return seriesObject;
		}
		catch (JSONException jse) {
			throw new IllegalStateException(jse);
		}
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
