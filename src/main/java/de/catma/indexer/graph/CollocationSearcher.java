package de.catma.indexer.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import de.catma.document.Range;
import de.catma.indexer.IndexBufferManagerName;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TermInfo;
import de.catma.indexer.indexbuffer.IndexBufferManager;
import de.catma.indexer.indexbuffer.SourceDocumentIndexBuffer;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

class CollocationSearcher {
	
	private final class IsInRangeEvaluator implements Evaluator {
		private final Set<QueryResultRow> testResultRowSet;

		private IsInRangeEvaluator(Set<QueryResultRow> testResultRowSet) {
			this.testResultRowSet = testResultRowSet;
		}

		public Evaluation evaluate(Path path) {
			Node endNode = path.endNode();
			
			if (endNode.hasLabel(NodeType.Position)) {
				if (isInRange(endNode)) {
					return Evaluation.INCLUDE_AND_CONTINUE;
				}
				else {
					return Evaluation.EXCLUDE_AND_PRUNE;
				}
			}
			
			return Evaluation.EXCLUDE_AND_CONTINUE;
		}

		private boolean isInRange(Node node) {
			Range range =  
				new Range(
					(Integer)node.getProperty(PositionProperty.start.name()), 
					(Integer)node.getProperty(PositionProperty.end.name()));
			for (QueryResultRow row : testResultRowSet) {
				if (row.getRange().hasOverlappingRange(range)) {
					return true;
				}
			}
			return false;
		}
	}

	private static class NodePostionComparator implements Comparator<Node> {
		public int compare(Node o1, Node o2) {
			return ((Integer)o1.getProperty(PositionProperty.position.name())).compareTo(
					(Integer)o2.getProperty(PositionProperty.position.name()));
		}
	}

	private final static NodePostionComparator NODE_POSTION_COMPARATOR = new NodePostionComparator();
	
	/**
	 * @param baseResult
	 * @param collocationConditionResult
	 * @param spanContextSize
	 * @param direction only {@link SpanDirection#BOTH} supported!
	 * @return
	 * @throws IOException
	 */
	public QueryResult search(QueryResult baseResult,
			QueryResult collocationConditionResult, int spanContextSize,
			SpanDirection direction) throws IOException {
		QueryResultRowArray searchResult = new QueryResultRowArray();
		try {
			GraphDatabaseService graphDb = 
				CatmaGraphDbName.CATMAGRAPHDB.getGraphDatabaseService();
			
			IndexBufferManager indexBufferManager = 
				IndexBufferManagerName.INDEXBUFFERMANAGER.getIndeBufferManager();

			// set up mapping SourceDoc->rows of base result
			HashMap<String,Set<QueryResultRow>> baseResultRowsByDocument = 
					new HashMap<String, Set<QueryResultRow>>();
			for (QueryResultRow baseRow : baseResult) {
				 String sourceDocumentId = baseRow.getSourceDocumentId();
				 Set<QueryResultRow> resultRows = baseResultRowsByDocument.get(sourceDocumentId);
				 if (resultRows == null) {
					 resultRows = new HashSet<QueryResultRow>();
					 baseResultRowsByDocument.put(sourceDocumentId, resultRows);
				 }
				 resultRows.add(baseRow);
			}

			HashMap<String,Set<QueryResultRow>> collocResultRowsByDocument = 
					new HashMap<String, Set<QueryResultRow>>();
			
			// set up mapping SourceDoc->rows of colloc result
			for (QueryResultRow collocRow : collocationConditionResult) {
				 String sourceDocumentId = collocRow.getSourceDocumentId();
				 Set<QueryResultRow> resultRows = collocResultRowsByDocument.get(sourceDocumentId);
				 if (resultRows == null) {
					 resultRows = new HashSet<QueryResultRow>();
					 collocResultRowsByDocument.put(sourceDocumentId, resultRows);
				 }
				 resultRows.add(collocRow);
			}

			ArrayList<SourceDocumentIndexBuffer> sourceDocumentIndexBuffers =
					new ArrayList<>();
			List<String> bufferedDocumentIds = new ArrayList<>();
			for (String sourceDocumentId : baseResultRowsByDocument.keySet()) {
				SourceDocumentIndexBuffer sourceDocumentIndexBuffer = 
						indexBufferManager.get(sourceDocumentId);
				if (sourceDocumentIndexBuffer != null) {
					sourceDocumentIndexBuffers.add(sourceDocumentIndexBuffer);
					bufferedDocumentIds.add(sourceDocumentId);
				}
			}

			SourceDocSearcher sourceDocSearcher = new SourceDocSearcher();
			if (bufferedDocumentIds.size() != baseResultRowsByDocument.keySet().size()) {
				PathUtil pathUtil = new PathUtil();
				try (Transaction transaction = graphDb.beginTx()) {
					for (Map.Entry<String,Set<QueryResultRow>> entry 
							: baseResultRowsByDocument.entrySet()) {
						
						final String sourceDocumentId = entry.getKey();
						
						if (!bufferedDocumentIds.contains(sourceDocumentId)) {
							final Set<QueryResultRow> baseQueryResultRows = 
									entry.getValue();
							
							final Set<QueryResultRow> collocQueryResultRows = 
									collocResultRowsByDocument.get(sourceDocumentId);
							
							if (collocQueryResultRows != null) {
								Node sourceDocNode = 
									sourceDocSearcher.search(graphDb, sourceDocumentId);
								//traversal to get all nodes of the base query rows
								TraversalDescription positionsOfBaseQueryResultRowsTraversal = 
									graphDb.traversalDescription()
									.depthFirst()
									.relationships(NodeRelationType.IS_PART_OF)
									.relationships(NodeRelationType.HAS_POSITION, Direction.OUTGOING)
									.relationships(NodeRelationType.ADJACENT_TO)
									.evaluator(new IsInRangeEvaluator(baseQueryResultRows));
								
								Traverser positionsOfBaseQueryResultRowsTraverser = 
										positionsOfBaseQueryResultRowsTraversal.traverse(
												sourceDocNode);
								
								Map<QueryResultRow, TreeSet<Node>> orderedBaseNodesByRow = 
									createdOrderedNodesByRow(
										positionsOfBaseQueryResultRowsTraverser, 
										baseQueryResultRows);
								
								TraversalDescription positionsOfCollocQueryResultRowsTraversal = 
									graphDb.traversalDescription()
									.depthFirst()
									.relationships(NodeRelationType.IS_PART_OF)
									.relationships(NodeRelationType.HAS_POSITION, Direction.OUTGOING)
									.relationships(NodeRelationType.ADJACENT_TO)
									.evaluator(new IsInRangeEvaluator(collocQueryResultRows));
								
								Traverser positionsOfCollocQueryResultRowsTraverser = 
										positionsOfCollocQueryResultRowsTraversal.traverse(
												sourceDocNode);
								
								Map<QueryResultRow, TreeSet<Node>> orderedCollocNodesByRow = 
									createdOrderedNodesByRow(
											positionsOfCollocQueryResultRowsTraverser, 
											collocQueryResultRows);
				
								Set<List<TermInfo>> collocTermInfos = new HashSet<List<TermInfo>>();
								
								for (Map.Entry<QueryResultRow, TreeSet<Node>> collocNodesEntry 
										: orderedCollocNodesByRow.entrySet()) {
									
									ArrayList<TermInfo> curTermInfos = new ArrayList<TermInfo>();
									for (Node n : collocNodesEntry.getValue()) {
										curTermInfos.add(new TermInfo(
//											(String)n.getProperty(PositionProperty.literal.name()),
											pathUtil.getLiteralFromPosition(n),
											(Integer)n.getProperty(PositionProperty.start.name()),
											(Integer)n.getProperty(PositionProperty.end.name()),
											(Integer)n.getProperty(PositionProperty.position.name())));
									}
									collocTermInfos.add(curTermInfos);
								}
								
								for (Map.Entry<QueryResultRow, TreeSet<Node>> orderedNodesEntry 
										: orderedBaseNodesByRow.entrySet()) {
									
									Node first = orderedNodesEntry.getValue().first();
									Node last = orderedNodesEntry.getValue().last();
									
									SpanContext spanContext = new SpanContext(sourceDocumentId);
									for (Node n : getContext(first, spanContextSize, Direction.INCOMING)) {
										spanContext.addBackwardToken(
											new TermInfo(
//												(String)n.getProperty(PositionProperty.literal.name()),
												pathUtil.getLiteralFromPosition(n),
												(Integer)n.getProperty(PositionProperty.start.name()),
												(Integer)n.getProperty(PositionProperty.end.name()),
												(Integer)n.getProperty(PositionProperty.position.name())));
									}
									
									for (Node n : getContext(last, spanContextSize, Direction.OUTGOING)) {
										spanContext.addForwardToken(
											new TermInfo(
//												(String)n.getProperty(PositionProperty.literal.name()),
												pathUtil.getLiteralFromPosition(n),
												(Integer)n.getProperty(PositionProperty.start.name()),
												(Integer)n.getProperty(PositionProperty.end.name()),
												(Integer)n.getProperty(PositionProperty.position.name())));
									}
									
									if (spanContext.containsAny(collocTermInfos)) {
										searchResult.add(orderedNodesEntry.getKey());
									}
									
								}
							}
						}
					}
					transaction.success();
				}
			}
			
			for (SourceDocumentIndexBuffer buffer : sourceDocumentIndexBuffers) {
				for (QueryResultRow row : 
					buffer.search(
							baseResultRowsByDocument.get(buffer.getSourceDocumentId()),
							collocResultRowsByDocument.get(buffer.getSourceDocumentId()),
							spanContextSize,
							direction)) {
					searchResult.add(row);
				}
			}
			return searchResult;
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	

	private Map<QueryResultRow, TreeSet<Node>> createdOrderedNodesByRow(
			Traverser traverser, Set<QueryResultRow> resultRows) {
		
		Map<QueryResultRow, TreeSet<Node>> orderedNodesByRange = 
				new HashMap<QueryResultRow, TreeSet<Node>>();
		
		for (Node n : traverser.nodes()) {
			if (n.hasLabel(NodeType.Position)) {
				QueryResultRow row = getRangeFor(resultRows, n);
				TreeSet<Node> orderedNodes = orderedNodesByRange.get(row);
				if (orderedNodes == null) {
					orderedNodes = new TreeSet<Node>(NODE_POSTION_COMPARATOR);
					orderedNodesByRange.put(row, orderedNodes);
				}
				orderedNodes.add(n);
			}
		}
		return orderedNodesByRange;
	}

	private List<? extends Node> getContext(Node first, int spanContextSize, Direction direction) {
		List<Node> result = new ArrayList<Node>();
		Node current = first;
		for (int i=0; i<spanContextSize; i++) {
			if (current.hasRelationship(direction, NodeRelationType.ADJACENT_TO)) {
				Relationship rsAdjacentTo = 
						current.getRelationships(
							direction, NodeRelationType.ADJACENT_TO).iterator().next();
				current = rsAdjacentTo.getOtherNode(current);
				result.add(current);
			}
			else {
				break;
			}
		}
		
		return result;
	}

	private QueryResultRow getRangeFor(Set<QueryResultRow> queryResultRows, Node node) {
		Range range =  
				new Range(
					(Integer)node.getProperty(PositionProperty.start.name()), 
					(Integer)node.getProperty(PositionProperty.end.name()));
		for (QueryResultRow row : queryResultRows) {
			if (row.getRange().hasOverlappingRange(range)) {
				return row;
			}
		}
		return null;
	}

	public SpanContext getSpanContextFor(String sourceDocumentId, Range range,
			int spanContextSize, SpanDirection direction) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<TermInfo> getTermInfosFor(String sourceDocumentId, Range range) {
		// TODO Auto-generated method stub
		return null;
	}
}
