package de.catma.indexer.graph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.restlet.data.MediaType;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;

public class SourceDocumentInserter {
	
	public static final String ENDPOINT_URI = "http://localhost:7474/db/data/";

	public enum RelType implements RelationshipType {
		IS_PART_OF,
		ADJACENT_TO,
		HAS_POSITION,
		;
	}
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private OutputStreamWriter osw;
	
	public SourceDocumentInserter() {
		File f = new File("c:/test/bla.txt");
		if (f.exists()) {
			f.delete();
		}
		
		try {
			f.createNewFile();
			osw = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	public void insert(SourceDocument sourceDocument) throws IOException {
		try {
			logger.info("start indexing sourcedocument");
			
			List<String> unseparableCharacterSequences = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
						.getIndexInfoSet().getUnseparableCharacterSequences();
			List<Character> userDefinedSeparatingCharacters = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
						.getIndexInfoSet().getUserDefinedSeparatingCharacters();
			Locale locale = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
					.getIndexInfoSet().getLocale();
			
			TermExtractor termExtractor = 
					new TermExtractor(
						sourceDocument.getContent(), 
						unseparableCharacterSequences, 
						userDefinedSeparatingCharacters, 
						locale);
			
			Map<String, List<TermInfo>> terms = termExtractor.getTerms();
	
			logger.info("term extraction finished");
			
			StringBuilder builder = new StringBuilder();
			
			builder.append("MERGE (sd:SourceDocument {localUri:'");
			builder.append(sourceDocument.getID());
			builder.append("',");
			builder.append("title:'");
			builder.append(sourceDocument.toString());
			builder.append("'})");

			System.out.println(builder.toString());

			ClientResource tranResource = postOpenTransaction(builder.toString(), ENDPOINT_URI + "transaction");
			builder = new StringBuilder();
			
			Map<TermInfo,Integer> termInfoToNodeId = new HashMap<>();
			TreeSet<TermInfo> orderedTermInfos = new TreeSet<TermInfo>(TermInfo.TOKENOFFSETCOMPARATOR);
			
			int termNodeIdx = 1;
			int positionNodeIdx = 1;
			int pathIdx = 1;
			
			builder.append("MATCH (sd:SourceDocument) WHERE sd.localUri = '" + sourceDocument.getID()+ "' ");
			for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
				
				String term = entry.getKey();
				List<TermInfo> termInfos = entry.getValue();

				builder.append(" CREATE ");
				builder.append("(t");
				builder.append(termNodeIdx);
				builder.append(":Term {literal:'");
				builder.append(escape(term));
				builder.append("'})-[:IS_PART_OF {sourceDoc:'"+sourceDocument.getID()+"'}]->(sd) ");

				pathIdx++;
//				if (pathIdx > 10) {
//					break;
//				}
	//			for (TermInfo ti : termInfos) {
	//				orderedTermInfos.add(ti);

	//				builder.append("CREATE (t"+termNodeIdx);
	//				builder.append(")-[:HAS_POSITION]->p");
	//				builder.append(positionNodeIdx);
	//				builder.append(":Position {position:");
	//				builder.append(ti.getTokenOffset());
	//				builder.append(", start:");
	//				builder.append(ti.getRange().getStartPoint());
	//				builder.append(", end:");
	//				builder.append(ti.getRange().getEndPoint());
	//				builder.append(", literal:'");
	//				builder.append(ti.getTerm());
	//				builder.append("'})");
	//
	//				termInfoToNodeId.put(ti, positionNodeIdx);
	//
	//				positionNodeIdx++;
	//			}
				
				System.out.println(termNodeIdx);
				termNodeIdx++;
			}
			IOUtils.write(builder.toString(),osw);
			postInTransaction(builder.toString(), tranResource);
			builder = new StringBuilder();
			osw.close();
	//
	//		TermInfo prevTi = null;
	//		for (TermInfo ti : orderedTermInfos) {
	//
	//			if (prevTi != null) {
	//				builder.append("CREATE (p");
	//				builder.append(termInfoToNodeId.get(prevTi));
	//				builder.append(")-[:ADJACENT_TO]->(p");
	//				builder.append(termInfoToNodeId.get(ti));
	//				builder.append(")");
	//			}
	//			prevTi = ti;
	//		}

			postCloseTransaction(tranResource);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String escape(String term) {
		if (term.contains("'")) {
			term  = term.replace("'", "\\'");
		}
		if (term.contains("\\")) {
			term = term.replace("\\", "\\\\");
		}
		if (term.contains("\"")) {
			term = term.replace("\"", "\\\"");
		}
		return term;
	}


	private ClientResource postOpenTransaction(String statement, String location) throws ResourceException, IOException, JSONException {
		ClientResource resource = new ClientResource(location);
		StringBuilder builder = new StringBuilder();
		builder.append("{\"statements\" : [ {\"statement\" : \"");
		builder.append(statement);
		builder.append("\"} ]}");
		resource.accept(MediaType.APPLICATION_JSON);
		
		Representation payload = new JsonRepresentation(builder.toString());
	
		Representation response = resource.post(payload, MediaType.APPLICATION_JSON);
		response.write(System.out);

		String commitUri = 
			((Series<?>)resource.getResponseAttributes().get("org.restlet.http.headers")).getFirstValue("Location");
		System.out.println(commitUri);
		
		ClientResource tranResource = new ClientResource(commitUri);
		tranResource.accept(MediaType.APPLICATION_JSON);
		
		System.out.println();
		return tranResource;
	}
	
	private void postInTransaction(String statement, ClientResource tranResource) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append("{\"statements\" : [ {\"statement\" : \"");
		builder.append(statement);
		builder.append("\"} ]}");
		Representation payload = new JsonRepresentation(builder.toString());
		Representation response = tranResource.post(payload, MediaType.APPLICATION_JSON);
		response.write(System.out);
		System.out.println();
	}
	
	private void postCloseTransaction(ClientResource tranResource) throws IOException {
		ClientResource commitResource = new ClientResource(tranResource.getRequest().getResourceRef()+"/commit");
		commitResource.accept(MediaType.APPLICATION_JSON);
		StringBuilder builder = new StringBuilder();
		builder.append("{\"statements\" : [ {\"statement\" : \"");
		builder.append("MATCH (sd:`SourceDocument`) RETURN sd");
		builder.append("\"} ]}");

		Representation response = commitResource.post(new JsonRepresentation(builder.toString()), MediaType.APPLICATION_JSON);
		response.write(System.out);
		System.out.println();
	}
	
	public void insert2(SourceDocument sourceDocument) throws IOException {

		logger.info("start indexing sourcedocument");
		
		List<String> unseparableCharacterSequences = 
				sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
					.getIndexInfoSet().getUnseparableCharacterSequences();
		List<Character> userDefinedSeparatingCharacters = 
				sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
					.getIndexInfoSet().getUserDefinedSeparatingCharacters();
		Locale locale = 
				sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
				.getIndexInfoSet().getLocale();
		
		TermExtractor termExtractor = 
				new TermExtractor(
					sourceDocument.getContent(), 
					unseparableCharacterSequences, 
					userDefinedSeparatingCharacters, 
					locale);
		
		Map<String, List<TermInfo>> terms = termExtractor.getTerms();

		logger.info("term extraction finished");
		logger.info("starting graphdb");
		
		GraphDatabaseService graphDb = null;
		
		try {
			graphDb = (GraphDatabaseService) new InitialContext().lookup(CatmaGraphDbName.CATMAGRAPHDB.name());
		} catch (NamingException e) {
			throw new IOException(e);
		}
		
		logger.info("graph db started");
		
		Label sourceDocLabel = DynamicLabel.label( "SourceDocument" );
//		inserter.createDeferredSchemaIndex( so ).on( "name" ).create();
		
		Transaction tx = graphDb.beginTx();
		
		Node sdNode = graphDb.createNode(sourceDocLabel);
		
		sdNode.setProperty("localUri", sourceDocument.getID());
		sdNode.setProperty("title", sourceDocument.toString());
		
		Label termLabel = DynamicLabel.label("Term");
		Label positionLabel = DynamicLabel.label("Position");
		
		Map<TermInfo,Long> termInfoToNodeId = new HashMap<TermInfo, Long>();
		TreeSet<TermInfo> orderedTermInfos = new TreeSet<TermInfo>(TermInfo.TOKENOFFSETCOMPARATOR);
		
		for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
			String term = entry.getKey();
			List<TermInfo> termInfos = entry.getValue();
			
			Node termNode = graphDb.createNode(termLabel);
			termNode.setProperty("literal", term);
			
			for (TermInfo ti : termInfos) {
				orderedTermInfos.add(ti);
				
				Node positionNode = graphDb.createNode(positionLabel);
				positionNode.setProperty("position", ti.getTokenOffset());
				positionNode.setProperty("start", ti.getRange().getStartPoint());
				positionNode.setProperty("end", ti.getRange().getEndPoint());
				positionNode.setProperty("literal", ti.getTerm());
				
				termInfoToNodeId.put(ti, positionNode.getId());
				Relationship rsHasPosition = termNode.createRelationshipTo(positionNode, RelType.HAS_POSITION);
				rsHasPosition.setProperty("sourceDoc", sourceDocument.getID());
				
				termNode.createRelationshipTo(sdNode, RelType.IS_PART_OF);
			}
		}
		
		TermInfo prevTi = null;
		for (TermInfo ti : orderedTermInfos) {

			if (prevTi != null) {
				graphDb.getNodeById(termInfoToNodeId.get(prevTi)).createRelationshipTo(
					graphDb.getNodeById(termInfoToNodeId.get(ti)), 
					RelType.ADJACENT_TO);
			}
			prevTi = ti;
		}

		tx.success();
		tx.close();
		logger.info("insertion of source document finished");
		graphDb.index();
		logger.info("indexing finished");
	}
	
}


