package de.catma.queryengine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.Indexer;
import de.catma.indexer.WildcardTermExtractor;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;

public class WildcardQuery extends Query {

	private String wildcardPhrase;
	
	public WildcardQuery(Phrase phraseQuery) {
		wildcardPhrase = phraseQuery.getPhrase();
	}
	
	@Override
	protected QueryResult execute() throws Exception {
    	QueryOptions queryOptions = getQueryOptions();

        WildcardTermExtractor termExtractor =
        		new WildcardTermExtractor(
        				wildcardPhrase,
        				queryOptions.getUnseparableCharacterSequences(),
        				queryOptions.getUserDefinedSeparatingCharacters(),
        				queryOptions.getLocale());
        
        List<String> termList =  termExtractor.getOrderedTerms();
        		
        Indexer indexer = queryOptions.getIndexer();
        
        QueryResult result = indexer.searchWildcardPhrase(
        	queryOptions.getRelevantSourceDocumentIDs(), termList);
        
        Repository repository = queryOptions.getRepository();
        Set<SourceDocument> toBeUnloaded = new HashSet<SourceDocument>();
        
        for (QueryResultRow row  : result) {
        	SourceDocument sd = 
        			repository.getSourceDocument(row.getSourceDocumentId());
        	
        	if (!sd.isLoaded()) {
    			//TODO: unload SourceDocuments to free space if tobeUnloaded.size() > 10
        		toBeUnloaded.add(sd);
        	}

        	row.setPhrase(sd.getContent(row.getRange()));
        }
        
        for (SourceDocument sd : toBeUnloaded) {
        	sd.unload();
        }
        
        return result;
	}
}
