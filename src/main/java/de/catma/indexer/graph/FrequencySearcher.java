package de.catma.indexer.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

import de.catma.indexer.IndexBufferManagerName;
import de.catma.indexer.indexbuffer.IndexBufferManager;
import de.catma.indexer.indexbuffer.SourceDocumentIndexBuffer;
import de.catma.indexer.indexbuffer.TermFrequencyInfo;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.QueryId;
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
	private IndexBufferManager indexBufferManager;
	
	private Logger logger = Logger.getLogger(this.getClass().getName());

	public FrequencySearcher() {
		graphDb = 
			CatmaGraphDbName.CATMAGRAPHDB.getGraphDatabaseService();
		indexBufferManager = 
			IndexBufferManagerName.INDEXBUFFERMANAGER.getIndeBufferManager();
	}

	public QueryResult search(QueryId queryId, List<String> documentIdList,
			CompareOperator comp1, int freq1, CompareOperator comp2, int freq2) throws IOException {
		
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
		
		GroupedQueryResultSet groupedQueryResultSet = new GroupedQueryResultSet();	
		HashMap<String, LazyGraphDBPhraseQueryResult> phraseResultMapping = 
				new HashMap<String, LazyGraphDBPhraseQueryResult>();

		if (!nonBufferedDocumentIds.isEmpty()) {
			logger.info("graph based search for " + nonBufferedDocumentIds);

			try (Transaction transaction = graphDb.beginTx()) {
		
				Collection<Node> sourceDocNodes = new SourceDocSearcher().search(graphDb, nonBufferedDocumentIds);
				if (sourceDocNodes.isEmpty()) {
					logger.warning("no SourceDocument nodes found for " + nonBufferedDocumentIds);
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
							qr = new LazyGraphDBPhraseQueryResult(queryId, term);
							phraseResultMapping.put(term, qr);
						}
						else {
							qr = phraseResultMapping.get(term);
						}
						
						qr.addFrequency(localUri, freq);
					}
	
				}
				transaction.success();
			}
		}
		for (SourceDocumentIndexBuffer buffer : sourceDocumentIndexBuffers) {
			logger.info("buffered search for " + buffer.getSourceDocumentId());
			
			for (TermFrequencyInfo tfi : buffer.search(comp1, freq1, comp2, freq2)) {
				LazyGraphDBPhraseQueryResult qr = null;
				if (!phraseResultMapping.containsKey(tfi.getTerm())) {
					qr = new LazyGraphDBPhraseQueryResult(queryId, tfi.getTerm());
					phraseResultMapping.put(tfi.getTerm(), qr);
				}
				else {
					qr = phraseResultMapping.get(tfi.getTerm());
				}
				
				qr.addFrequency(buffer.getSourceDocumentId(), tfi.getFrequency());
			}
		}
		
		groupedQueryResultSet.addAll(phraseResultMapping.values());
		
		return groupedQueryResultSet;

	}

}
