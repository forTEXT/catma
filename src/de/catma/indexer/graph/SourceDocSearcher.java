package de.catma.indexer.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

class SourceDocSearcher {

	public Node search(GraphDatabaseService graphDb, String documentId) {
		Collection<Node> result = search(graphDb, Collections.singletonList(documentId));
		if (result.isEmpty()) {
			return null;
		}
		else {
			return result.iterator().next();
		}
	}
	public Collection<Node> search(GraphDatabaseService graphDb, List<String> documentIdList) {
		ArrayList<Node> sourceDocNodes = new ArrayList<>(documentIdList.size());
		
		for (String sourceDocumentId : documentIdList) {
			ResourceIterator<Node> iterator = 
					graphDb.findNodes(
							NodeType.SourceDocument, 
							SourceDocumentProperty.localUri.name(), 
							sourceDocumentId);

			if (iterator.hasNext()) {
				Node sourceDocNode = iterator.next();
				sourceDocNodes.add(sourceDocNode);
			}
			iterator.close();
		}
		
		
		return sourceDocNodes;
	}
	
	

}
