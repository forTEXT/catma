package de.catma.repository.git.graph.lazy;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.glassfish.jersey.internal.guava.Sets;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.Range;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.Indexer;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.result.CommentQueryResultRow;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.repository.git.graph.interfaces.CommentProvider;
import de.catma.repository.git.graph.interfaces.DocumentIndexProvider;
import de.catma.repository.git.graph.interfaces.DocumentProvider;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class LazyGraphProjectIndexer implements Indexer {
	private final Logger logger = Logger.getLogger(LazyGraphProjectIndexer.class.getName());
	private final CommentProvider commentProvider;
	private Function<String, AnnotationCollection> collectionSupplier;
	private LoadingCache<String, Set<Term>> documentIndexCache;
	private Supplier<TagLibrary> tagLibrarySupplier;
	private IDGenerator idGenerator = new IDGenerator();
	private DocumentIndexProvider documentIndexProvider;

	public LazyGraphProjectIndexer(CommentProvider commentProvider,
			final DocumentProvider documentProvider,
			final DocumentIndexProvider documentIndexProvider,
			final Function<String, AnnotationCollection> collectionSupplier, 
			final Supplier<TagLibrary> tagLibrarySupplier) {
		super();
		this.documentIndexProvider = documentIndexProvider;
		this.commentProvider = commentProvider;
		this.collectionSupplier = collectionSupplier;
		this.tagLibrarySupplier = tagLibrarySupplier;
		
    	this.documentIndexCache = 
			CacheBuilder.newBuilder()
			.maximumSize(10)
			.build(new CacheLoader<String, Set<Term>>() {
				@Override
				public Set<Term> load(String key) throws Exception {
					return loadDocumentIndex(key);
				}
			});

	}
	
	@SuppressWarnings({ "rawtypes" })
	private Set<Term> loadDocumentIndex(String documentId) throws Exception {
		Set<Term> terms = new HashSet<>();
		Map documentIndexContent = this.documentIndexProvider.get(documentId);
		
		Map<Integer, Position> adjacencyMap = new HashMap<>();
		for (Object entry : documentIndexContent.entrySet()) {
			
			String literal = (String)((Map.Entry)entry).getKey();
			List positionList = (List)((Map.Entry)entry).getValue();
			Term term = new Term(literal, positionList.size());

			terms.add(term);
			for (Object posEntry : positionList) {
				int startOffset = ((Double)((Map)posEntry).get("startOffset")).intValue();
				int endOffset = ((Double)((Map)posEntry).get("endOffset")).intValue();
				int tokenOffset = ((Double)((Map)posEntry).get("tokenOffset")).intValue();
				Position pos = new Position(startOffset, endOffset, tokenOffset, term);

				// hasPosition
				term.addPosition(pos);
				adjacencyMap.put(tokenOffset, pos);
			}
		}
		for (int i=0; i<adjacencyMap.size(); i++) {
			// isAdjacentTo
			Position pos = adjacencyMap.get(i);
			if (i < adjacencyMap.size()-1) {
				pos.setForwardAdjacentPostion(adjacencyMap.get(i+1));
			}
			if (i > 0) {
				pos.setBackwardAdjacentPosition(adjacencyMap.get(i-1));
			}
		}
		
		
		
		logger.info("Finished adding Document to the graph");	
	
		return terms;
	}

	@Override
	public void index(SourceDocument sourceDocument, BackgroundService backgroundService) throws Exception {
		// noop
	}

	@Override
	public void index(List<TagReference> tagReferences, String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws IOException {
		// noop
	}

	@Override
	public void removeSourceDocument(String sourceDocumentID) throws IOException {
		// noop 
	}

	@Override
	public void removeUserMarkupCollection(String userMarkupCollectionID) throws IOException {
		//noop
	}

	@Override
	public void removeTagReferences(List<TagReference> tagReferences) throws IOException {
		//noop
	}

	@Override
	public QueryResult searchPhrase(QueryId queryId, List<String> documentIdList, String phrase, List<String> termList, int limit)
			throws Exception {

		return searchPhrase(queryId, documentIdList, phrase, termList, limit, (term1, term2) -> term1.equals(term2));
	}

	private QueryResult searchPhrase(QueryId queryId, List<String> documentIdList, String phrase, List<String> termList, int limit,
			BiPredicate<String, String> termTestFunction) throws Exception {
		
		
		QueryResultRowArray result = new QueryResultRowArray();
		
		if (termList.isEmpty() || documentIdList.isEmpty()) {
			return result;
		}
		
		for (String documentId : documentIdList) {
			Set<Term> terms = documentIndexCache.get(documentId);
			

			Term startTerm = terms
					.parallelStream()
					.filter(term -> termTestFunction.test(term.getLiteral(), termList.get(0)))
					.findAny()
					.orElse(null);
			
			if (startTerm != null) {
				List<List<Position>> positionLists = 
						startTerm.getPositions(
							termList.size()>1?
									termList.subList(
											1, 
											termList.size()):
									Collections.emptyList(),
							termTestFunction);
				
				for (List<Position> positions : positionLists) {
					result.add(
						new QueryResultRow(
								queryId,
								documentId,
								new Range(
									positions.get(0).getStartOffset(), 
									positions.get(positions.size()-1).getEndOffset()),
								phrase));
				}
			}
		}
		
		return result;
	}

	@Override
	public QueryResult searchWildcardPhrase(QueryId queryId, List<String> documentIdList, List<String> termList, int limit)
			throws Exception {
		
		return searchPhrase(
			queryId,
			documentIdList, 
			"", // phrase is added later in the processing
			termList, 
			limit, 
			(term1, term2) -> term1.matches(SQLWildcard2RegexConverter.convert(term2)));
	}

	@Override
	public QueryResult searchTagDefinitionPath(QueryId queryId, List<String> collectionIdList, String tagPath)
			throws Exception {
		QueryResultRowArray result = new QueryResultRowArray();
		
		if (!tagPath.startsWith("/")) {
			tagPath = "%"+tagPath;
		}
		final String tagPathRegex = SQLWildcard2RegexConverter.convert(tagPath);
		Map<String, String> validTagIdToTagPathMapping = new HashMap<>();
		Set<TagDefinition> validTagDefinitions = Sets.newHashSet();
		boolean allTags = tagPath.trim().replaceAll("/?%+", "").equals("");
		
		for (TagsetDefinition tagset : tagLibrarySupplier.get()) {
			for (TagDefinition tag : tagset) {
				String path = tagset.getTagPath(tag);
				if (allTags || Pattern.matches(tagPathRegex, path)) {
					validTagIdToTagPathMapping.put(tag.getUuid(), path);
					validTagDefinitions.add(tag);
				}	
			}
		}
		for (String collectionId : collectionIdList) {
			AnnotationCollection collection = collectionSupplier.apply(collectionId);
			
			for (TagDefinition tag : validTagDefinitions) {
				Multimap<String, TagReference> tagReferencesByInstanceId = 
						collection.getTagReferencesByInstanceId(tag);
				
				for (String tagInstanceId : tagReferencesByInstanceId.keySet()) {
					result.add(
						new TagQueryResultRow(
								queryId,
								collection.getSourceDocumentId(), 
								tagReferencesByInstanceId.get(tagInstanceId)
									.stream()
									.map(tr -> tr.getRange())
									.collect(Collectors.toList()), 
								collectionId, 
								tag.getUuid(),
								validTagIdToTagPathMapping.get(tag.getUuid()),
								"", //TODO: version
								tagInstanceId));
				}
				
			}
		}
		
		return result;
	}

	@Override
	public QueryResult searchProperty(QueryId queryId, List<String> collectionIdList, String propertyNamePattern,
			String propertyValuePattern, String tagPathPattern) throws Exception {
		
		QueryResultRowArray result = new QueryResultRowArray();

		PropertyNameFilter propertyNameFilter = new PropertyNameFilter(propertyNamePattern);
		PropertyValueFilter propertyValueFilter = new PropertyValueFilter(propertyValuePattern);
		
		// add default wildcard if no explicit root is defined
		if (tagPathPattern != null) {
			if (!tagPathPattern.startsWith("/")) {
				tagPathPattern = "%"+tagPathPattern;

			}
		}
		final String tagPathRegex = 
				tagPathPattern==null?null:SQLWildcard2RegexConverter.convert(tagPathPattern);
		
		Map<String, String> validTagIdToTagPathMapping = new HashMap<>();
		Set<TagDefinition> validTagDefinitions = Sets.newHashSet();
		boolean allTags = tagPathRegex==null || tagPathRegex.trim().replaceAll("/?%+", "").equals("");
		
		for (TagsetDefinition tagset : tagLibrarySupplier.get()) {
			for (TagDefinition tag : tagset) {
				String path = tagset.getTagPath(tag);
				if (allTags || Pattern.matches(tagPathRegex, path)) {
					validTagIdToTagPathMapping.put(tag.getUuid(), path);
					validTagDefinitions.add(tag);
				}	
			}
		}
		for (String collectionId : collectionIdList) {
			AnnotationCollection collection = collectionSupplier.apply(collectionId);
			
			for (TagDefinition tag : validTagDefinitions) {
				Multimap<String, TagReference> tagReferencesByInstanceId = 
						collection.getTagReferencesByInstanceId(tag);
				
				for (String tagInstanceId : tagReferencesByInstanceId.keySet()) {
					
					TagInstance ti = 
						tagReferencesByInstanceId.get(tagInstanceId).iterator().next().getTagInstance();
					
					List<Range> ranges = tagReferencesByInstanceId.get(tagInstanceId)
							.stream()
							.map(tr -> tr.getRange())
							.collect(Collectors.toList());


					addTagQueryResultRowForSystemProperty(
							queryId,
							result,
							SystemPropertyName.catma_markupauthor,
							ti.getAuthor(),
							propertyNameFilter,
							propertyValueFilter,
							collection.getSourceDocumentId(),
							collection.getUuid(),
							tag.getUuid(),
							validTagIdToTagPathMapping.get(tag.getUuid()),
							tagInstanceId,
							ranges);
					addTagQueryResultRowForSystemProperty(
							queryId,
							result,
							SystemPropertyName.catma_markuptimestamp,
							ti.getTimestamp(),
							propertyNameFilter,
							propertyValueFilter,
							collection.getSourceDocumentId(),
							collection.getUuid(),
							tag.getUuid(),
							validTagIdToTagPathMapping.get(tag.getUuid()),
							tagInstanceId,
							ranges);
					addTagQueryResultRowForSystemProperty(
							queryId,
							result,
							SystemPropertyName.catma_displaycolor,
							tag.getColor(),
							propertyNameFilter,
							propertyValueFilter,
							collection.getSourceDocumentId(),
							collection.getUuid(),
							tag.getUuid(),
							validTagIdToTagPathMapping.get(tag.getUuid()),
							tagInstanceId,
							ranges);

					for (Property prop : ti.getUserDefinedProperties()) {
						if (propertyNameFilter.test(new Pair<>(prop, tag))) {
							for (String value : prop.getPropertyValueList()) {
								if (propertyValueFilter.testValue(value)) {
									result.add(
										new TagQueryResultRow(
											queryId,
											collection.getSourceDocumentId(), 
											ranges, 
											collectionId, 
											tag.getUuid(),
											validTagIdToTagPathMapping.get(tag.getUuid()),
											"", //TODO: Version
											tagInstanceId,
											prop.getPropertyDefinitionId(),
											tag.getPropertyDefinitionByUuid(
													prop.getPropertyDefinitionId()).getName(),
											value));	
								}
							}
						}
					}
				}
			}
		}
		return result;
	}
	
	private void addTagQueryResultRowForSystemProperty(
			QueryId queryId, 
			QueryResultRowArray result, 
			SystemPropertyName systemPropertyName, String value, 
			PropertyNameFilter propertyNameFilter, PropertyValueFilter propertyValueFilter,
			String documentId, String collectionId, 
			String tagId, String tagPath, 
			String tagInstanceId, List<Range> rangeList) {
		
		if (propertyNameFilter.testPropertyName(systemPropertyName.name())
				&& propertyValueFilter.testValue(value)) {
			result.add(
				new TagQueryResultRow(
					queryId,
					documentId, 
					rangeList, 
					collectionId, 
					tagId,
					tagPath,
					"", //TODO: Version
					tagInstanceId,
					idGenerator.generate(systemPropertyName.name()),
					systemPropertyName.name(),
					value));
		}			
	}
	
	@Override
	public QueryResult searchFrequency(
			QueryId queryId, List<String> documentIdList, 
			CompareOperator comp1, int freq1,
			CompareOperator comp2, int freq2) throws IOException {
		try {
			QueryResultRowArray result = new QueryResultRowArray();
			
			for (String documentId : documentIdList) {
				Set<Term> terms = documentIndexCache.get(documentId);
				terms.stream()
					.filter(new FrequencyFilter(comp1, freq1, comp2, freq2))
					.flatMap(term -> term.getPositions().stream())
					.forEach(position -> {
						result.add(
							new QueryResultRow(
								queryId,
								documentId,
								new Range(
									position.getStartOffset(), 
									position.getEndOffset()),
								position.getTerm().getLiteral()));						
					});
			}
			return result;
		}
		catch (ExecutionException ee) {
			throw new IOException(ee);
		}
	}

	@Override
	public SpanContext getSpanContextFor(String sourceDocumentId, Range range, int spanContextSize,
			SpanDirection direction) throws IOException {
		
		SpanContext spanContext = new SpanContext(sourceDocumentId);

		try {
			SortedSet<Position> sortedPosition =
				new TreeSet<Position>(
					(pos1, pos2) -> Integer.compare(pos1.getTokenOffset(), pos2.getTokenOffset()));
			for (Term term : documentIndexCache.get(sourceDocumentId)) {
				sortedPosition.addAll(term.getPositions(range));
			}
			
			if (!sortedPosition.isEmpty()) {
				Position firstPos = sortedPosition.first();
				Position lastPos = sortedPosition.last();
				
				if (direction.equals(SpanDirection.BOTH) || direction.equals(SpanDirection.BACKWARD)) {
					Position backwardPos = firstPos.getBackwardAdjacentPosition();
					while (backwardPos != null && spanContext.getBackwardTokens().size() < spanContextSize) {
						spanContext.addBackwardToken(
								new TermInfo(
										backwardPos.getTerm().getLiteral(),
										backwardPos.getStartOffset(),
										backwardPos.getEndOffset(),
										backwardPos.getTokenOffset()
								));
						backwardPos = backwardPos.getBackwardAdjacentPosition();
					}
				}
				
				if (direction.equals(SpanDirection.BOTH) || direction.equals(SpanDirection.FORWARD)) {
					Position forwardPos = lastPos.getForwardAdjacentPostion();
					while (forwardPos != null && spanContext.getForwardTokens().size() < spanContextSize) {
						spanContext.addForwardToken(
								new TermInfo(
										forwardPos.getTerm().getLiteral(),
										forwardPos.getStartOffset(),
										forwardPos.getEndOffset(),
										forwardPos.getTokenOffset()
								));
						forwardPos = forwardPos.getForwardAdjacentPostion();
					}
				}
			}
			
			if (!spanContext.getBackwardTokens().isEmpty()) {
				TermInfo firstToken = spanContext.getBackwardTokens().get(0);
				TermInfo lastToken = spanContext.getBackwardTokens().get(spanContext.getBackwardTokens().size()-1);
				spanContext.setBackwardRange(
					new Range(firstToken.getRange().getStartPoint(), lastToken.getRange().getEndPoint()));
			}
			if (!spanContext.getForwardTokens().isEmpty()) {
				TermInfo firstToken = spanContext.getForwardTokens().get(0);
				TermInfo lastToken = spanContext.getForwardTokens().get(spanContext.getForwardTokens().size()-1);
				spanContext.setForwardRange(
					new Range(firstToken.getRange().getStartPoint(), lastToken.getRange().getEndPoint()));
			}

			
			return spanContext;
			
		} catch (ExecutionException e) {
			throw new IOException(e);
		}
		
	}

	
	@Override
	public QueryResult searchCollocation(
			QueryId queryId, QueryResult baseResult, QueryResult collocationConditionResult,
			int spanContextSize, SpanDirection direction) throws IOException {
	
		int baseResultSize = baseResult.size();
		int collocConditionResultSize = collocationConditionResult.size();
		
		boolean swapCollocationDirection = baseResultSize > collocConditionResultSize;
		
		//swap to reduce the amount of span context computation
		if (swapCollocationDirection) {
			QueryResult bufferResult = baseResult;
			baseResult = collocationConditionResult;
			collocationConditionResult = bufferResult;
		}
		
		Multimap<String, QueryResultRow> collocConditionResultBySourceDocumentId = 
				ArrayListMultimap.create();
		collocationConditionResult.forEach(
			row -> collocConditionResultBySourceDocumentId.put(row.getSourceDocumentId(), row));
		
		QueryResultRowArray matchingBaseRows = new QueryResultRowArray();
		QueryResultRowArray matchingCollocConditionRows = new QueryResultRowArray();

		for (QueryResultRow row : baseResult) {
			
			if (collocConditionResultBySourceDocumentId.containsKey(row.getSourceDocumentId())) {
				SpanContext spanContext = 
						getSpanContextFor(
								row.getSourceDocumentId(), row.getRange(), spanContextSize, direction);

				boolean baseMatch = matchingBaseRows.contains(row);
		
				for (QueryResultRow collocConditionRow : 
					collocConditionResultBySourceDocumentId.get(row.getSourceDocumentId()) ) {
					boolean collocMatch = matchingCollocConditionRows.contains(collocConditionRow);
					if (!baseMatch || !collocMatch) {
						if (spanContext.hasOverlappingRange(collocConditionRow.getRanges(), direction)) {
							if (!baseMatch) {
								matchingBaseRows.add(row);
								baseMatch = true;
							}
							if (!collocMatch) {
								matchingCollocConditionRows.add(collocConditionRow);
								collocMatch = true;
							}
						}

					}					 
				}
			}			
		}
		
		// swap back
		if (swapCollocationDirection) {
			QueryResultRowArray bufferResult = matchingBaseRows;
			matchingBaseRows = matchingCollocConditionRows;
			matchingCollocConditionRows = bufferResult;
		}
		
		return matchingBaseRows;
	}

	@Override
	public void close() {
		// noop
	}

	@Override
	public QueryResult searchTagDiff(
			QueryId queryId, List<String> relevantUserMarkupCollIDs, String propertyName, String tagPhrase)
			throws IOException {
		// TODO Auto-generated method stub
		return new QueryResultRowArray();
	}
	
	@Override
	public QueryResult searchCommentPhrase(QueryId queryId, List<String> documentIdList, List<String> termList,
			int limit, List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale) throws Exception {
		
		List<Comment> comments = commentProvider.getComments(documentIdList);
		
		QueryResultRowArray result = new QueryResultRowArray();
		
		for (Comment comment : comments) {
			
			if (termList.size() == 1 && termList.get(0).equals("%")) {
				result.add(new CommentQueryResultRow(queryId, comment));
			}
			else {
				TermExtractor termExtractor = 
					new TermExtractor(
						comment.getBody(), unseparableCharacterSequences, userDefinedSeparatingCharacters, locale);
				
				List<String> commentTerms = termExtractor.getTermsInOrder();
				
				if (matches(commentTerms, termList)) {
					result.add(new CommentQueryResultRow(queryId, comment));
				}
				else {
					for (Reply reply : comment.getReplies()) {
						TermExtractor replyTermExtractor = 
								new TermExtractor(
									reply.getBody(), 
									unseparableCharacterSequences, 
									userDefinedSeparatingCharacters, 
									locale);
						List<String> replyTerms = replyTermExtractor.getTermsInOrder();
						if (matches(replyTerms, termList)) {
							result.add(new CommentQueryResultRow(queryId, comment));
							break;
						}
					}
				}
			}				
		}
		
		
		
		return result;
	}
	
	private boolean matches(List<String> commentTerms, List<String> termList) {
		int startIdx = -1;
		
		String firstQueryTermRegEx = SQLWildcard2RegexConverter.convert(termList.get(0));
		
		for (int idx=0; idx<commentTerms.size(); idx++) {
			if (commentTerms.get(idx).matches(firstQueryTermRegEx)) {
				startIdx = idx;
				break;
			}
		}
		
		if ((startIdx != -1)  
				&& commentTerms.subList(startIdx, commentTerms.size()).size() >= termList.size()-1) {
			
			if (termList.size() > 1) {

				List<String> remainingTerms = termList.subList(1, termList.size());
				List<String> remainingCommentTerms = commentTerms.subList(startIdx+1, commentTerms.size());
				
				for (int i=0; i<remainingTerms.size(); i++) {
					if (!remainingCommentTerms.get(i).matches(SQLWildcard2RegexConverter.convert(remainingTerms.get(i)))) {
						return false;
					}
				}
				
				return true;
			}
			else {
				return true;
			}
		}
		else {
			return false;
		}
	}

}
