package de.catma.indexer.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.neo4j.cypher.internal.compiler.v2_0.functions.E;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import de.catma.document.Range;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TermInfo;
import de.catma.indexer.graph.SourceDocumentInserter.RelType;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class CollocationSearcher2 {
	
	private static class NodePostionComparator implements Comparator<Node> {
		public int compare(Node o1, Node o2) {
			return ((Integer)o1.getProperty("position")).compareTo((Integer)o2.getProperty("position"));
		}
	}

	private final static NodePostionComparator NODE_POSTION_COMPARATOR = new NodePostionComparator();
	
	public QueryResult search(QueryResult baseResult,
			QueryResult collocationConditionResult, int spanContextSize,
			SpanDirection direction) throws IOException {
		QueryResultRowArray searchResult = new QueryResultRowArray();
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( "C:/data/projects/catma/graphdb" );
		try {
//		graphDb.bidirectionalTraversalDescription().
//		graphDb.traversalDescription().expand(expander)
			
			Label sourceDocLabel = DynamicLabel.label( "SourceDocument" );
			final Label positionLabel = DynamicLabel.label("Position");
			
			HashMap<String,Set<QueryResultRow>> baseResultRowsByDocument = new HashMap<String, Set<QueryResultRow>>();
			for (QueryResultRow baseRow : baseResult) {
				 String sourceDocumentId = baseRow.getSourceDocumentId();
				 Set<QueryResultRow> resultRows = baseResultRowsByDocument.get(sourceDocumentId);
				 if (resultRows == null) {
					 resultRows = new HashSet<QueryResultRow>();
					 baseResultRowsByDocument.put(sourceDocumentId, resultRows);
				 }
				 resultRows.add(baseRow);
			}

			HashMap<String,Set<QueryResultRow>> collocResultRowsByDocument = new HashMap<String, Set<QueryResultRow>>();
			for (QueryResultRow collocRow : collocationConditionResult) {
				 String sourceDocumentId = collocRow.getSourceDocumentId();
				 Set<QueryResultRow> resultRows = collocResultRowsByDocument.get(sourceDocumentId);
				 if (resultRows == null) {
					 resultRows = new HashSet<QueryResultRow>();
					 collocResultRowsByDocument.put(sourceDocumentId, resultRows);
				 }
				 resultRows.add(collocRow);
			}

			Transaction transaction = graphDb.beginTx();
			for (Map.Entry<String,Set<QueryResultRow>> entry : baseResultRowsByDocument.entrySet()) {
				final String sourceDocumentId = entry.getKey();
				final Set<QueryResultRow> baseQueryResultRows = entry.getValue();
				
				final Set<QueryResultRow> collocQueryResultRows = collocResultRowsByDocument.get(sourceDocumentId);
				
				ResourceIterable<Node> sourceDocNodes = 
						graphDb.findNodesByLabelAndProperty(sourceDocLabel, "localUri", sourceDocumentId);
				ResourceIterator<Node> iterator = sourceDocNodes.iterator();
				Node sourceDocNode = iterator.next();
				iterator.close();
				
				TraversalDescription positionsOfBaseQueryResultRowsTraversal = graphDb.traversalDescription()
				.depthFirst()
				.relationships(RelType.IS_PART_OF).relationships(RelType.HAS_POSITION, Direction.OUTGOING).relationships(RelType.ADJACENT_TO).evaluator(
						new Evaluator() {
							public Evaluation evaluate(Path path) {
								Node endNode = path.endNode();
								
								if (endNode.hasLabel(positionLabel)) {
									if (isInRange(baseQueryResultRows, endNode)) {
										return Evaluation.INCLUDE_AND_CONTINUE;
									}
									else {
										return Evaluation.EXCLUDE_AND_PRUNE;
									}
								}
								
								
								
								return Evaluation.EXCLUDE_AND_CONTINUE;
							}
						});
				
				Traverser positionsOfBaseQueryResultRowsTraverser = positionsOfBaseQueryResultRowsTraversal.traverse(sourceDocNode);
				Map<QueryResultRow, TreeSet<Node>> orderedBaseNodesByRow = 
					createdOrderedNodesByRow(positionsOfBaseQueryResultRowsTraverser, baseQueryResultRows);
				
				TraversalDescription positionsOfCollocQueryResultRowsTraversal = graphDb.traversalDescription()
				.depthFirst()
				.relationships(RelType.IS_PART_OF).relationships(RelType.HAS_POSITION, Direction.OUTGOING).relationships(RelType.ADJACENT_TO).evaluator(
						new Evaluator() {
							public Evaluation evaluate(Path path) {
								Node endNode = path.endNode();
								
								if (endNode.hasLabel(positionLabel)) {
									if (isInRange(collocQueryResultRows, endNode)) {
										return Evaluation.INCLUDE_AND_CONTINUE;
									}
									else {
										return Evaluation.EXCLUDE_AND_PRUNE;
									}
								}
								
								
								
								return Evaluation.EXCLUDE_AND_CONTINUE;
							}
						});
				
				Traverser positionsOfCollocQueryResultRowsTraverser = positionsOfCollocQueryResultRowsTraversal.traverse(sourceDocNode);
				Map<QueryResultRow, TreeSet<Node>> orderedCollocNodesByRow = 
					createdOrderedNodesByRow(positionsOfCollocQueryResultRowsTraverser, collocQueryResultRows);

				Set<List<TermInfo>> collocTermInfos = new HashSet<List<TermInfo>>();
				
				for (Map.Entry<QueryResultRow, TreeSet<Node>> collocNodesEntry : orderedCollocNodesByRow.entrySet()) {
					ArrayList<TermInfo> curTermInfos = new ArrayList<TermInfo>();
					for (Node n : collocNodesEntry.getValue()) {
						curTermInfos.add(new TermInfo(
								(String)n.getProperty("literal"),
								(Integer)n.getProperty("start"),
								(Integer)n.getProperty("end"),
								(Integer)n.getProperty("position")));
					}
					collocTermInfos.add(curTermInfos);
				}
				
				System.out.println(collocTermInfos);
				
				for (Map.Entry<QueryResultRow, TreeSet<Node>> orderedNodesEntry : orderedBaseNodesByRow.entrySet()) {
					Node first = orderedNodesEntry.getValue().first();
					Node last = orderedNodesEntry.getValue().last();
					
					SpanContext context = new SpanContext(sourceDocumentId);
					for (Node n : getContext(first, spanContextSize, Direction.INCOMING)) {
						context.addBackwardToken(
							new TermInfo(
								(String)n.getProperty("literal"),
								(Integer)n.getProperty("start"),
								(Integer)n.getProperty("end"),
								(Integer)n.getProperty("position")));
					}
					for (Node n : getContext(last, spanContextSize, Direction.OUTGOING)) {
						context.addForwardToken(
							new TermInfo(
								(String)n.getProperty("literal"),
								(Integer)n.getProperty("start"),
								(Integer)n.getProperty("end"),
								(Integer)n.getProperty("position")));
					}
					
					System.out.println(context);
					
					boolean found = false;
					
					for (List<TermInfo> collocTerms : collocTermInfos) {
						if (context.contains(collocTerms)) {
							found = true;
							break;
						}
					}
					
					if (found) {
						searchResult.add(orderedNodesEntry.getKey());
					}
					
				}
			}
			transaction.success();
			transaction.close();
			graphDb.shutdown();
			
		}
		catch (Exception e) {
			e.printStackTrace();
			graphDb.shutdown();
		}
		return searchResult;
	}

	private Map<QueryResultRow, TreeSet<Node>> createdOrderedNodesByRow(Traverser traverser, Set<QueryResultRow> resultRows) {
		Label positionLabel = DynamicLabel.label("Position");
		
		Map<QueryResultRow, TreeSet<Node>> orderedNodesByRange = new HashMap<QueryResultRow, TreeSet<Node>>();
		
		for (Node n : traverser.nodes()) {
			if (n.hasLabel(positionLabel)) {
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
			if (current.hasRelationship(direction, RelType.ADJACENT_TO)) {
				Relationship rsAdjacentTo = current.getRelationships(direction, RelType.ADJACENT_TO).iterator().next();
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
					(Integer)node.getProperty("start"), 
					(Integer)node.getProperty("end"));
		for (QueryResultRow row : queryResultRows) {
			if (row.getRange().hasOverlappingRange(range)) {
				return row;
			}
		}
		return null;
	}

	private boolean isInRange(Set<QueryResultRow> queryResultRows, Node node) {
		Range range =  
			new Range(
				(Integer)node.getProperty("start"), 
				(Integer)node.getProperty("end"));
		for (QueryResultRow row : queryResultRows) {
			if (row.getRange().hasOverlappingRange(range)) {
				return true;
			}
		}
		return false;
	}
}
