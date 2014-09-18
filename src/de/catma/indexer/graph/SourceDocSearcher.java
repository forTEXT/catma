package de.catma.indexer.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;

class SourceDocSearcher {

	public Collection<Node> search(GraphDatabaseService graphDb, List<String> documentIdList) {
		ArrayList<Node> sourceDocNodes = new ArrayList<>(documentIdList.size());
		
		for (String sourceDocumentId : documentIdList) {
			ResourceIterable<Node> sourceDocNodesResult = 
					graphDb.findNodesByLabelAndProperty(
							NodeType.SourceDocument, 
							SourceDocumentProperty.localUri.name(), 
							sourceDocumentId);
			ResourceIterator<Node> iterator = sourceDocNodesResult.iterator();
			if (iterator.hasNext()) {
				Node sourceDocNode = iterator.next();
				sourceDocNodes.add(sourceDocNode);
			}
			iterator.close();
		}
		
		
		return sourceDocNodes;
	}
	
	

}
