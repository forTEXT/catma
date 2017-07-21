package de.catma.ui.visualizer.chart;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;

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
		chartId = "ChartWidget" + new IDGenerator().generate(); //$NON-NLS-1$
		registerRpc(chartServerRpc);
		
		this.distribution = distribution;
		this.maxOccurrences = maxOccurrences;
		this.distributionSelectionListener = distributionSelectionListener;
	}
	
	private void fireResultRowSelected(String key, int x, int y) {
		List<QueryResultRow> rows = distribution.getQueryResultRows(key, x);
		if (rows == null) {
			Notification.show(Messages.getString("Chart.infoTitle"), Messages.getString("Chart.noOccurrencesForThisSlot"), Type.TRAY_NOTIFICATION); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			distributionSelectionListener.queryResultRowsSelected(distribution.getLabel(), rows, x, y);
		}
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
					"{"+ //$NON-NLS-1$
						"\"chart\": {"+ //$NON-NLS-1$
						    "\"renderTo\": \""+ chartId +"\","+ //$NON-NLS-1$ //$NON-NLS-2$
						    "\"zoomType\": \"xy\","+ //$NON-NLS-1$
						    "\"spacingBottom\": 50"+ //$NON-NLS-1$
					    "},"+ //$NON-NLS-1$
					    "\"xAxis\": {"+ //$NON-NLS-1$
					    	"\"tickInterval\" : "+ distribution.getSegmentSizeInPercent() + "," + //$NON-NLS-1$ //$NON-NLS-2$
					        "\"max\": 100," + //$NON-NLS-1$
					    	"\"min\": 0" + //$NON-NLS-1$
					    "},"+ //$NON-NLS-1$
					    "\"plotOptions\": {"+ //$NON-NLS-1$
					    	"\"series\": {"+ //$NON-NLS-1$
					        	"\"allowPointSelect\": \"false\","+ //$NON-NLS-1$
					        	"\"point\": {"+ //$NON-NLS-1$
					        		"\"events\": {"+ //$NON-NLS-1$
					        			"\"click\": \"null\""+ //$NON-NLS-1$
					        		"}"+ //$NON-NLS-1$
					        	"}"+ //$NON-NLS-1$
					         "}"+ //$NON-NLS-1$
					    "},"+ //$NON-NLS-1$
					    "\"title\": {"+ //$NON-NLS-1$
				        	"\"text\": \""+String.valueOf( //$NON-NLS-1$
				        		JsonStringEncoder.getInstance().quoteAsString(
				        				distribution.getLabel()))+"\","+ //$NON-NLS-1$
				        	"\"verticalAlign\": \"bottom\"" + //$NON-NLS-1$
				       	"},"+ //$NON-NLS-1$
			       		"\"yAxis\": {"+ //$NON-NLS-1$
			       			"\"title\": {"+ //$NON-NLS-1$
			       				"\"text\": \"Occurrences\""+ //$NON-NLS-1$
				            "},"+ //$NON-NLS-1$
			       			"\"min\": 0," + //$NON-NLS-1$
			       			"\"max\": "+ maxOccurrences + //$NON-NLS-1$
				        "}"+ //$NON-NLS-1$
				   	"}", ObjectNode.class);	 //$NON-NLS-1$

			ArrayNode seriesArray = createSeriesArray(distribution);
			
			configuration.set("series", seriesArray); //$NON-NLS-1$
			
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
		seriesObject.set("data", dataArray); //$NON-NLS-1$
		seriesObject.put("name", values.getKey().toString()); //$NON-NLS-1$

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
