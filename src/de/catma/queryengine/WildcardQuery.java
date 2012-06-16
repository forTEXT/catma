package de.catma.queryengine;

import de.catma.indexer.Indexer;
import de.catma.queryengine.result.QueryResult;

public class WildcardQuery extends Query {
	
	private String wildcardPhrase;
	
	public WildcardQuery(Phrase phraseQuery) {
		wildcardPhrase = phraseQuery.getPhrase();
	}

	@Override
	protected QueryResult execute() throws Exception {
    	QueryOptions queryOptions = getQueryOptions();
    	
        Indexer indexer = queryOptions.getIndexer();
        
//        indexer.searchWildcardPhrase(
//        	queryOptions.getRelevantSourceDocumentIDs(), wildcardPhrase);
        
		return null;
	}

}
