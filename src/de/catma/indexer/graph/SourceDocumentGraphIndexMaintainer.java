package de.catma.indexer.graph;

import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import de.catma.repository.db.maintenance.SourceDocumentIndexMaintainer;

public class SourceDocumentGraphIndexMaintainer implements
		SourceDocumentIndexMaintainer {
	
	private Logger logger = Logger.getLogger(SourceDocumentGraphIndexMaintainer.class.getName());

	@Override
	public int checkSourceDocumentIndex(int maxObjectCount, int offset) throws Exception {
		
		GraphDatabaseService graphDatabaseService = 
					CatmaGraphDbName.CATMAGRAPHDB.getGraphDatabaseService();
		
		
		try (Transaction tx = graphDatabaseService.beginTx()) {
			
			ResourceIterable<Node> sdIterable = graphDatabaseService.findNodesByLabelAndProperty(
					NodeType.SourceDocument, 
					SourceDocumentProperty.deleted.name(), 
					Boolean.TRUE);
			
			int delCount = 0;
			
			ResourceIterator<Node> sdIterator = sdIterable.iterator();
			if (sdIterator.hasNext()) {
				Node sdNode = sdIterator.next();
				logger.info(
					"found node " + sdNode + " " 
					+ sdNode.getProperty(SourceDocumentProperty.localUri.name()) 
					+ " " + sdNode.getProperty(SourceDocumentProperty.title.name()));
				
				sdIterator.close();
				
				for (Relationship isPartOf :
					sdNode.getRelationships(
							Direction.INCOMING, NodeRelationType.IS_PART_OF)) {
					
					Node termNode = isPartOf.getStartNode();
					
					logger.info("found term node " + termNode + " " + termNode.getProperty(TermProperty.literal.name()));
					for (Relationship hasPosition : 
						termNode.getRelationships(Direction.OUTGOING, NodeRelationType.HAS_POSITION)) {
						Node positionNode = hasPosition.getEndNode();
						
						logger.info(
							"found position node " + positionNode + " " 
									+ termNode.getProperty(TermProperty.literal.name()) 
									+ "@[" + positionNode.getProperty(PositionProperty.start.name()) 
									+ ","
									+ positionNode.getProperty(PositionProperty.end.name())
									+ "]");
						for (Relationship isAdjacentTo : 
								positionNode.getRelationships(NodeRelationType.ADJACENT_TO)) {
							logger.info("deleting isAdjacentTo" + isAdjacentTo);
							isAdjacentTo.delete();
							delCount++;
							
							if (delCount > maxObjectCount) {
								tx.success();
								return offset; //not modified here, not used either
							}
						}
						logger.info("deleting hasPosition " + hasPosition);
						hasPosition.delete();
						delCount++;
						logger.info("deleting position " + positionNode);
						positionNode.delete();
						delCount++;
						if (delCount > maxObjectCount) {
							tx.success();
							return offset; //not modified here, not used either
						}
					}
					logger.info("deleting isPartOf " + isPartOf);
					isPartOf.delete();
					delCount++;
					
					logger.info("deleting term " + termNode);
					termNode.delete();
					delCount++;
					
					if (delCount > maxObjectCount) {
						tx.success();
						return offset; //not modified here, not used either
					}
				}
				logger.info("deleting sd " + sdNode);
				sdNode.delete();
				tx.success();
			}
			else {
				sdIterator.close();
			}
		}		

		return offset; //not modified here, not used either
	}

}
