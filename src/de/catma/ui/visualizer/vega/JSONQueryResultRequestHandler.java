package de.catma.ui.visualizer.vega;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

import de.catma.document.repository.Repository;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.json.JSONQueryResultBuilder;

public class JSONQueryResultRequestHandler implements RequestHandler {
	
	private QueryResult queryResult;
	private String queryResultUrlPath;
	private Repository repository;
	
	public JSONQueryResultRequestHandler(QueryResult queryResult, Repository repository, String queryResultUrlPath) {
		super();
		this.queryResult = queryResult;
		this.repository = repository;
		this.queryResultUrlPath = queryResultUrlPath.toLowerCase();
	}

	@Override
	public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response)
			throws IOException {
		
		if (request.getPathInfo().toLowerCase().equals("/"+queryResultUrlPath)) {
			OutputStream outputStream = response.getOutputStream();
			
			ArrayNode jsonValues = new JSONQueryResultBuilder().createJSONQueryResult(queryResult, repository);
			
			outputStream.write(jsonValues.toString().getBytes("UTF-8"));

			return true;
		}
		
		//TODO: queries for multiple datasources
		
		return false;
	}


}
