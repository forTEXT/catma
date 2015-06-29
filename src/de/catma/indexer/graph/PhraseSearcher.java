package de.catma.indexer.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import de.catma.document.Range;
import de.catma.indexer.EqualsMatcher;
import de.catma.indexer.IndexBufferManagerName;
import de.catma.indexer.SQLWildcardMatcher;
import de.catma.indexer.TermMatcher;
import de.catma.indexer.indexbuffer.IndexBufferManager;
import de.catma.indexer.indexbuffer.SourceDocumentIndexBuffer;
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
				return n.getRelationships(NodeRelationType.IS_PART_OF, direction.reverse());
			}
			else if (n.hasLabel(NodeType.Term)) {
//				System.out.println("Term " +  n.getProperty(TermProperty.literal.name()));
				return n.getRelationships(NodeRelationType.HAS_POSITION, direction);
			}
			else if (n.hasLabel(NodeType.Position)) {
//				System.out.println("Position " + n.getProperty(PositionProperty.literal.name()) + " @ " + n.getProperty(PositionProperty.position.name()));
				return n.getRelationships(NodeRelationType.ADJACENT_TO, direction); 
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
		private TermMatcher termMatcher;
		private PathUtil pathUtil = new PathUtil();
		
		public PhraseSearchEvaluator(List<String> termList, boolean withWildcards) {
			super();
			this.termList = termList;
			
			if (withWildcards) {
				termMatcher = new SQLWildcardMatcher();
			}
			else {
				termMatcher = new EqualsMatcher();
			}
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

				// Term nodes are always the first literal of the matchable part of the path
				// so we match against the first term of our term list
				if(termMatcher.match(termList.get(0), literal)) {
					return Evaluation.EXCLUDE_AND_CONTINUE;
				}
				else {
					return Evaluation.EXCLUDE_AND_PRUNE;
				}
			}
			else if (n.hasLabel(NodeType.Position)) {

//				System.out.println("E-Position " + n.getProperty(PositionProperty.literal.name()) + " @ " + n.getProperty(PositionProperty.position.name()));

				List<String> literals = fillWithLiteralsFromPath(path);
				
				// try to get a match with the literals from the path
				int matchCount = getMatchCount(literals);
				
				// do we have a full match
				if (matchCount==termList.size()) {
					return Evaluation.INCLUDE_AND_PRUNE;
				}
				// partial match, we continue our search along this path
				else if (matchCount > 0) {
					return Evaluation.EXCLUDE_AND_CONTINUE;
				}
				// no match
				return Evaluation.EXCLUDE_AND_PRUNE;
			}
			
			return Evaluation.EXCLUDE_AND_PRUNE;
		}

		/**
		 * @param path the path of literals
		 * @return a list filled with literals from the path going backwards, 
		 * the list has as much as termList.size() tokens if available.
		 */
		private List<String> fillWithLiteralsFromPath(Path path) {
			List<String> literals = new ArrayList<>();
			
			for (Node node : path.reverseNodes()) {
				
				if (node.hasLabel(NodeType.Term)) { // head of Positions chain of this path
					return literals;
				}
				
				if (node.hasLabel(NodeType.Position)) { // should always be a Position node at this point
//					String literal = (String) node.getProperty(PositionProperty.literal.name());
					
					literals.add(0, pathUtil.getLiteralFromPosition(node));
					
					if (literals.size() == termList.size()) {
						return literals;
					}
				}				
			}
			
			return literals;
		}

		/**
		 * @param literals
		 * @return the number of literals that matched the termList entries
		 */
		private int getMatchCount(List<String> literals) {
			// we have a shrinking window lookig onto the literals
			for (int matchWindowSize=termList.size(); matchWindowSize>0; matchWindowSize--) {
				// fill up the window with literals up to the current window size
				List<String> matchWindow = new ArrayList<String>();
				for (int i=matchWindowSize; i>0; i--) {
					if (i>literals.size()) { // we don't have enough literals for the desired matchWindowSize
						break;
					}
					matchWindow.add(literals.get(literals.size()-i));
				}
				// how many literals from the window matched?
				int matchCount = getMatchCountForWindow(matchWindow);
				// do we have a match count? if not, we continue by shrinking the window
				if (matchCount > 0) {
					return matchCount;
				}
			}
			return 0;
		}

		/**
		 * @param matchWindow the current window of literals
		 * @return the number of literals of the matchWindow that matched the 
		 * termList entries 
		 */
		private int getMatchCountForWindow(List<String> matchWindow) {
			for (int matchIndex=0; matchIndex<matchWindow.size(); matchIndex++) {
				
				if (termList.size() <= matchIndex) {
					return matchIndex;
				}
				
				if (!termMatcher.match(termList.get(matchIndex), matchWindow.get(matchIndex))) {
					return matchIndex;
				}
			}
			return matchWindow.size();
		}

	}
	
	private GraphDatabaseService graphDb;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private IndexBufferManager indexBufferManager;
	
	public PhraseSearcher() {
		graphDb = (GraphDatabaseService)
				CatmaGraphDbName.CATMAGRAPHDB.getGraphDatabaseService();
		indexBufferManager = 
				IndexBufferManagerName.INDEXBUFFERMANAGER.getIndeBufferManager();
	}

	public QueryResult search(List<String> documentIdList, String phrase,
			List<String> termList, int limit) throws IOException {
		return search(documentIdList, phrase, termList, limit, false);
	}
	
	private QueryResult search(List<String> documentIdList, String phrase,
			List<String> termList, int limit, boolean withWildcards) throws IOException {
		
		ArrayList<SourceDocumentIndexBuffer> sourceDocumentIndexBuffers = new ArrayList<>();
		List<String> nonBufferedDocumentIds = new ArrayList<>();
		for (String sourceDocumentId : documentIdList) {
			SourceDocumentIndexBuffer sourceDocumentIndexBuffer = 
					indexBufferManager.get(sourceDocumentId);
			if (sourceDocumentIndexBuffer != null) {
				sourceDocumentIndexBuffers.add(sourceDocumentIndexBuffer);
			}
			else {
				nonBufferedDocumentIds.add(sourceDocumentId);
			}
		}

		
		PathUtil pathUtil = new PathUtil();
		
		
		QueryResultRowArray searchResult = new QueryResultRowArray();
		
		if (!nonBufferedDocumentIds.isEmpty()) {
			try (Transaction transaction = graphDb.beginTx()) {
				Collection<Node> sourceDocNodes = new SourceDocSearcher().search(graphDb, nonBufferedDocumentIds);
				if (sourceDocNodes.isEmpty()) {
					logger.warning("no SourceDocument nodes found for " + nonBufferedDocumentIds);
				}
				else {
					TraversalDescription positionsTraversal = 
						graphDb.traversalDescription()
							.depthFirst()
							.evaluator(new PhraseSearchEvaluator(termList, withWildcards))
	//						.expand(new PhraseSearchExpander());
							.relationships(NodeRelationType.IS_PART_OF, Direction.INCOMING)
							.relationships(NodeRelationType.HAS_POSITION, Direction.OUTGOING)
							.relationships(NodeRelationType.ADJACENT_TO, Direction.OUTGOING);
					
					Traverser positionsTraverser = positionsTraversal.traverse(sourceDocNodes);
					for (Path p : positionsTraverser) {
//						System.out.println("RESULT " + p);
						
						Node sourceDocNode = p.startNode();
						String localUri = 
								(String) sourceDocNode.getProperty(
										SourceDocumentProperty.localUri.name());
						
						Node firstPositionNode = 
								pathUtil.getPositionNodeBackwardsAt(
										p, termList.size());
						
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
			}
		}
		
		if ((limit == 0) || (limit > searchResult.size())) {
			for (SourceDocumentIndexBuffer buffer : sourceDocumentIndexBuffers) {
				int curSize = searchResult.size();
				for (QueryResultRow row : 
					buffer.search(
							phrase, 
							termList, 
							(limit==0)?0:limit-curSize, withWildcards)) {
					searchResult.add(row);
				}
			}
		}
		
		return searchResult;
	}



	public QueryResult searchWildcard(List<String> documentIdList,
			List<String> termList, int limit) throws IOException {
		return search(documentIdList, null, termList, limit, true);
	}

}
