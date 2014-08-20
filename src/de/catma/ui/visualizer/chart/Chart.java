package de.catma.ui.visualizer.chart;

import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vaadin.ui.AbstractComponent;

import de.catma.queryengine.result.computation.Distribution;
import de.catma.queryengine.result.computation.XYValues;
import de.catma.ui.client.ui.visualizer.chart.ChartClientRpc;

public class Chart extends AbstractComponent {
	
	public Chart(Distribution distribution) {
		getRpcProxy(ChartClientRpc.class).init(distribution.getSegmentSizeInPercent(), dataToJson(distribution));
	}

	private String dataToJson(Distribution distribution) {
		try {
			JSONArray seriesArray = new JSONArray();
			
			for (XYValues<Integer, Integer> values : distribution.getXySeries()) {
				JSONArray dataArray = new JSONArray();
				for (Entry<Integer,Integer> pair : values) {
					JSONArray valueArray = new JSONArray();
					valueArray.put(Double.valueOf(pair.getKey()));
					valueArray.put(Double.valueOf(pair.getValue()));
					dataArray.put(valueArray);
				}
				JSONObject dataObject = new JSONObject();
				dataObject.put("data", dataArray);
				seriesArray.put(dataObject);
			}
			
			return seriesArray.toString();
		}
		catch (JSONException jse) {
			throw new IllegalStateException(jse);
		}
	}
}
