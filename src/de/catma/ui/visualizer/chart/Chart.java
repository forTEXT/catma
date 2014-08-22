package de.catma.ui.visualizer.chart;

import java.util.Date;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vaadin.ui.AbstractComponent;

import de.catma.queryengine.result.computation.Distribution;
import de.catma.queryengine.result.computation.XYValues;
import de.catma.ui.client.ui.visualizer.chart.ChartClientRpc;

public class Chart extends AbstractComponent {
	
	private String chartId; 
	
	public Chart(Distribution distribution, int maxOccurrences) {
		chartId = "ChartWidget" + new Date().getTime();
		
		getRpcProxy(ChartClientRpc.class).init(
				chartId,
				createConfiguration(distribution, maxOccurrences));
	}

	private String createConfiguration(Distribution distribution, int maxOccurrences) {
		try {
			JSONObject configuration = 
				new JSONObject(
					"{"+
						"chart: {"+
						    "renderTo: '"+ chartId +"',"+
						    "zoomType: 'xy'"+
					    "},"+
					    "xAxis: {"+
					    	"tickInterval : "+ distribution.getSegmentSizeInPercent() + "," +
					        "max: 100," +
					    	"min: 0" +
					    "},"+
					    "plotOptions: {"+
					    	"series: {"+
					        	"allowPointSelect: false"+
					         "}"+
					    "},"+
					    "title: {"+
				        	"text: '"+distribution.getLabel()+"'"+
				       	"},"+
			       		"yAxis: {"+
			       			"title: {"+
			       				"text: 'Occurrences'"+
				            "},"+
			       			"min: 0," +
			       			"max: "+ maxOccurrences +
				        "}"+
				   	"}");	
			JSONArray seriesArray = new JSONArray();
			
			for (XYValues<Integer, Integer> values : distribution.getXySeries()) {
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
				seriesArray.put(seriesObject);
			}
			configuration.put("series", seriesArray);
			
			System.out.println(configuration.toString());
			
			return configuration.toString();
		}
		catch (JSONException jse) {
			throw new IllegalStateException(jse);
		}
	}
}
