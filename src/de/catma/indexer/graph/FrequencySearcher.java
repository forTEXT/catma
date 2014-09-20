package de.catma.indexer.graph;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;

public class FrequencySearcher {
	
	private static class FrequencyEvaluator implements Evaluator {
	
		private CompareOperator comp1;
		private int freq1;
		private CompareOperator comp2;
		private int freq2;
		
		
		public FrequencyEvaluator(CompareOperator comp1, int freq1,
				CompareOperator comp2, int freq2) {
			super();
			this.comp1 = comp1;
			this.freq1 = freq1;
			this.comp2 = comp2;
			this.freq2 = freq2;
		}


		@Override
		public Evaluation evaluate(Path path) {
			Node n = path.endNode();

			if (n.hasLabel(NodeType.SourceDocument)) {
				return Evaluation.EXCLUDE_AND_CONTINUE;
			}
			else if (n.hasLabel(NodeType.Term)) {
				int freq = (Integer) n.getProperty(TermProperty.freq.name());
				
				if (comp1.getCondition().isTrue(freq, freq1)) {
					if (comp2 == null) {
						return Evaluation.INCLUDE_AND_PRUNE;
					}
					else if (comp2.getCondition().isTrue(freq, freq2)) {
						return Evaluation.INCLUDE_AND_PRUNE;
					}
				}
			}
			return Evaluation.EXCLUDE_AND_PRUNE;
		}
	}
	
	private GraphDatabaseService graphDb;
	private Logger logger = Logger.getLogger(this.getClass().getName());

	public FrequencySearcher() throws NamingException {
		graphDb = (GraphDatabaseService) new InitialContext().lookup(
				CatmaGraphDbName.CATMAGRAPHDB.name());
	}

	public QueryResult search(List<String> documentIdList,
			CompareOperator comp1, int freq1, CompareOperator comp2, int freq2) throws IOException {
		Transaction transaction = graphDb.beginTx();
		try {
			GroupedQueryResultSet groupedQueryResultSet = new GroupedQueryResultSet();	
	
			Collection<Node> sourceDocNodes = new SourceDocSearcher().search(graphDb, documentIdList);
			if (sourceDocNodes.isEmpty()) {
				logger.warning("no SourceDocument nodes found for " + documentIdList);
			}
			else {
				TraversalDescription termTraversal = 
					graphDb.traversalDescription()
						.depthFirst()
						.relationships(NodeRelationType.IS_PART_OF, Direction.INCOMING);
				
//				if ((freq1 > 0) || (!comp1.equals(CompareOperator.GREATERTHAN))) {
					termTraversal = 
						termTraversal.evaluator(
							new FrequencyEvaluator(comp1, freq1, comp2, freq2));
//				}

				Traverser termTraverser = termTraversal.traverse(sourceDocNodes);
				HashMap<String, LazyGraphDBPhraseQueryResult> phraseResultMapping = 
						new HashMap<String, LazyGraphDBPhraseQueryResult>();
				PathUtil pathUtil = new PathUtil();
				
				for (Path path : termTraverser) {
					Node sourceDocNode = path.startNode();
					String localUri = 
							(String) sourceDocNode.getProperty(
									SourceDocumentProperty.localUri.name());

					LazyGraphDBPhraseQueryResult qr = null;
					Node termNode = pathUtil.getFirstTermNode(path);
					String term = 
						(String) termNode.getProperty(TermProperty.literal.name());
					int freq = (Integer)termNode.getProperty(TermProperty.freq.name());
					
					if (!phraseResultMapping.containsKey(term)) {
						qr = new LazyGraphDBPhraseQueryResult(term);
						phraseResultMapping.put(term, qr);
					}
					else {
						qr = phraseResultMapping.get(term);
					}
					
					qr.addFrequency(localUri, freq);
				}

				groupedQueryResultSet.addAll(phraseResultMapping.values());
			}
			transaction.success();
			transaction.close();
			
			return groupedQueryResultSet;
		}
		catch (Exception e) {
			transaction.failure();
			transaction.close();
			throw new IOException(e);
		}
	}

}
