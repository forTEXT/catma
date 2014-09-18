package de.catma.indexer.graph;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import de.catma.document.Range;
import de.catma.indexer.graph.SourceDocumentIndexer.RelType;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class PhraseSearcher {

	@SuppressWarnings("rawtypes")
	public static class PhraseSearchExpander implements PathExpander { 
		
		private Direction direction = Direction.OUTGOING;
		
		public PhraseSearchExpander() {
		}
		
		private PhraseSearchExpander(Direction direction) {
			super();
			this.direction = direction;
		}

		@Override
		public Iterable<Relationship> expand(Path path,
				BranchState state) {
			Node n = path.endNode();
			
//			System.out.println("EVALUTE " + path + " dir " + direction);
			
			if (n.hasLabel(NodeType.SourceDocument)) {
//				System.out.println("SourceDocNode " + n.getProperty(SourceDocumentProperty.title.name()));
				return n.getRelationships(RelType.IS_PART_OF, direction.reverse());
			}
			else if (n.hasLabel(NodeType.Term)) {
//				System.out.println("Term " +  n.getProperty(TermProperty.literal.name()));
				return n.getRelationships(RelType.HAS_POSITION, direction);
			}
			else if (n.hasLabel(NodeType.Position)) {
//				System.out.println("Position " + n.getProperty(PositionProperty.literal.name()) + " @ " + n.getProperty(PositionProperty.position.name()));
				return n.getRelationships(RelType.ADJACENT_TO, direction); 
			}

			return path.endNode().getRelationships();
		}
		
		@Override
		public PathExpander reverse() {
			return new PhraseSearchExpander(Direction.INCOMING);
		}
	}

	private static class PhraseSearchEvaluator implements Evaluator {
		
		private List<String> termList;
		
		public PhraseSearchEvaluator(List<String> termList) {
			super();
			this.termList = termList;
		}

		@Override
		public Evaluation evaluate(Path path) {
			Node n = path.endNode();
			
			if (n.hasLabel(NodeType.SourceDocument)) {
//				System.out.println("E-SourceDocNode " + n.getProperty(SourceDocumentProperty.title.name()));
				return Evaluation.EXCLUDE_AND_CONTINUE;
			}
			else if (n.hasLabel(NodeType.Term)) {
//				System.out.println("E-Term " +  n.getProperty(TermProperty.literal.name()));
				String literal = (String)n.getProperty(TermProperty.literal.name());
				if(termList.get(0).equals(literal)) {
					return Evaluation.EXCLUDE_AND_CONTINUE;
				}
				else {
					return Evaluation.EXCLUDE_AND_PRUNE;
				}
			}
			else if (n.hasLabel(NodeType.Position)) {
//				System.out.println("E-Position " + n.getProperty(PositionProperty.literal.name()) + " @ " + n.getProperty(PositionProperty.position.name()));
				
				String term = termList.get(path.length()-2);
				String literal = (String) n.getProperty(PositionProperty.literal.name());
				if (term.equals(literal)) {
					if (termList.size() == path.length()-1) {
						return Evaluation.INCLUDE_AND_PRUNE;
					}
					else {
						return Evaluation.EXCLUDE_AND_CONTINUE;
					}
				}
				else {
					return Evaluation.EXCLUDE_AND_PRUNE;
				}
			}
			
			return Evaluation.EXCLUDE_AND_PRUNE;
		}
	}
	
	private GraphDatabaseService graphDb;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public PhraseSearcher() throws NamingException {
		graphDb = (GraphDatabaseService) new InitialContext().lookup(
						CatmaGraphDbName.CATMAGRAPHDB.name());
	}

	public QueryResult search(List<String> documentIdList, String phrase,
			List<String> termList, int limit) throws IOException {
		PathUtil pathUtil = new PathUtil();
		
		Transaction transaction = graphDb.beginTx();
		try {
			QueryResultRowArray searchResult = new QueryResultRowArray();
	
			Collection<Node> sourceDocNodes = new SourceDocSearcher().search(graphDb, documentIdList);
			if (sourceDocNodes.isEmpty()) {
				logger.warning("no SourceDocument nodes found for " + documentIdList);
			}
			else {
				TraversalDescription positionsTraversal = 
					graphDb.traversalDescription()
						.depthFirst()
						.evaluator(new PhraseSearchEvaluator(termList))
//						.expand(new PhraseSearchExpander());
						.relationships(RelType.IS_PART_OF, Direction.INCOMING)
						.relationships(RelType.HAS_POSITION, Direction.OUTGOING)
						.relationships(RelType.ADJACENT_TO, Direction.OUTGOING);
				
				Traverser positionsTraverser = positionsTraversal.traverse(sourceDocNodes);
				for (Path p : positionsTraverser) {
//					System.out.println("RESULT " + p);
					
					Node sourceDocNode = p.startNode();
					String localUri = 
							(String) sourceDocNode.getProperty(
									SourceDocumentProperty.localUri.name());
					
					Node firstPositionNode = pathUtil.getFirstPositionNode(p);
					int start = (Integer) firstPositionNode.getProperty(PositionProperty.start.name());
					int end = (Integer) p.endNode().getProperty(PositionProperty.end.name());
					Range range = new Range(start, end);
					
					searchResult.add(new QueryResultRow(localUri, range, phrase));
					
					if ((limit != 0) && (limit==searchResult.size())) {
						break;
					}
				}
			}			
			transaction.success();
			transaction.close();
			return searchResult;
		}
		catch (Exception e) {
			transaction.failure();
			transaction.close();
			throw new IOException(e);
		}
	}



	public QueryResult searchWildcard(List<String> documentIdList,
			List<String> termList, int limit) {
		// TODO Auto-generated method stub
		return null;
	}

}
