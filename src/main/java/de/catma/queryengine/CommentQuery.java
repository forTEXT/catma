package de.catma.queryengine;

import java.util.List;

import de.catma.document.source.SourceDocument;
import de.catma.indexer.Indexer;
import de.catma.indexer.WildcardTermExtractor;
import de.catma.project.Project;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;

public class CommentQuery extends Query {
	
	private String commentPhrase;

	public CommentQuery(Phrase phrase) {
		this.commentPhrase = phrase.getPhrase();
	}

	@Override
	protected QueryResult execute() throws Exception {
    	QueryOptions queryOptions = getQueryOptions();
        WildcardTermExtractor termExtractor =
        		new WildcardTermExtractor(
        				commentPhrase,
        				queryOptions.getUnseparableCharacterSequences(),
        				queryOptions.getUserDefinedSeparatingCharacters(),
        				queryOptions.getLocale());
        
        List<String> termList =  termExtractor.getOrderedTerms();
        		
        Indexer indexer = queryOptions.getIndexer();
        
        QueryResult result = indexer.searchCommentPhrase(
        	queryOptions.getQueryId(),
        	queryOptions.getRelevantSourceDocumentIDs(), termList,
        	queryOptions.getLimit(),
        	queryOptions.getUnseparableCharacterSequences(),
        	queryOptions.getUserDefinedSeparatingCharacters(),
        	queryOptions.getLocale());
        
        Project repository = queryOptions.getRepository();
    	
    	
        for (QueryResultRow row  : result) {
        	SourceDocument sd = repository.getSourceDocument(row.getSourceDocumentId());
        	
        	row.setPhrase(sd.getContent(row.getRange()));
        }
        
        return result;
	}

}
