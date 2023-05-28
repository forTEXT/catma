package de.catma.ui.module.analyze.visualization.vega;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

import de.catma.backgroundservice.LogProgressListener;
import de.catma.queryengine.QueryJob;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.json.JSONQueryResultBuilder;
import de.catma.ui.module.analyze.QueryOptionsProvider;

public class JSONQueryResultRequestHandler implements RequestHandler {
	private Logger logger = Logger.getLogger(JSONQueryResultRequestHandler.class.getName());
	
	private CopyOnWriteArrayList<QueryResultRow> queryResult; // concurrent access!
	private final String queryResultUrlPath;
	private final QueryOptionsProvider queryOptionsProvider;
	private final String vegaViewIdPath;
	private final String queryUrlPath;
	
	public JSONQueryResultRequestHandler(
			QueryOptionsProvider queryOptionsProvider, 
			String queryResultUrlPath, String vegaViewId) {
		this.queryResult = new CopyOnWriteArrayList<QueryResultRow>();
		this.queryOptionsProvider = queryOptionsProvider;
		this.queryResultUrlPath = "/"+queryResultUrlPath.toLowerCase();
		this.vegaViewIdPath = "/"+vegaViewId.toLowerCase();
		this.queryUrlPath = vegaViewIdPath + "/query/"; 
	}

	@Override
	public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response)
			throws IOException {
		
		if (request.getPathInfo().toLowerCase().startsWith(vegaViewIdPath)) {
			if (request.getPathInfo().toLowerCase().equals(queryResultUrlPath)) {

				writeResponse(queryResult, response);
				
				return true;
			}
			else if (request.getPathInfo().toLowerCase().startsWith(queryUrlPath)) {
				String pathInfo = request.getPathInfo();
				String encodedQuery = pathInfo.substring(queryUrlPath.length());
				String query = URLDecoder.decode(encodedQuery, "UTF-8");
				logger.info("Query: " + query);
				
				QueryJob queryJob = new QueryJob(query, queryOptionsProvider.getQueryOptions());
				queryJob.setProgressListener(new LogProgressListener());
				try {
					QueryResult queryResult = queryJob.call();
					
					writeResponse(queryResult, response);
					
					return true;
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
		}		
		return false;
	}

	private void writeResponse(Iterable<QueryResultRow> queryResult, VaadinResponse response) throws IOException {
		//TODO:
//		response.setContentType("json");
		OutputStream outputStream = response.getOutputStream();
		ArrayNode jsonValues = 
				new JSONQueryResultBuilder().createJSONQueryResult(
						queryResult, 
						queryOptionsProvider.getQueryOptions().getRepository());
		response.setCacheTime(-1);
		response.setHeader("Access-Control-Allow-Origin", "https://vega.github.io");
		outputStream.write(jsonValues.toString().getBytes("UTF-8"));
	}

	public void addQuerResultRows(Iterable<QueryResultRow> rows) {
		rows.forEach(row -> queryResult.add(row));
	}

	public void removeQuerResultRows(Iterable<QueryResultRow> rows) {
		rows.forEach(row -> queryResult.remove(row));
	}

}
