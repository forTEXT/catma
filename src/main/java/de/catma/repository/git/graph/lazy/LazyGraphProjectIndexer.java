package de.catma.repository.git.graph.lazy;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import de.catma.backgroundservice.BackgroundService;
import de.catma.document.Range;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.*;
import de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.result.*;
import de.catma.repository.git.graph.interfaces.*;
import de.catma.tag.*;
import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiPredicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LazyGraphProjectIndexer implements Indexer {
	private final Logger logger = Logger.getLogger(LazyGraphProjectIndexer.class.getName());

	private final CommentsProvider commentsProvider;
	private final DocumentIndexProvider documentIndexProvider;
	private final CollectionProvider collectionProvider;
	private final TagLibraryProvider tagLibraryProvider;

	private final LoadingCache<String, Set<Term>> documentIndexCache;

	private final IDGenerator idGenerator = new IDGenerator();

	public LazyGraphProjectIndexer(
			CommentsProvider commentsProvider,
			DocumentProvider documentProvider,
			DocumentIndexProvider documentIndexProvider,
			CollectionProvider collectionProvider,
			TagLibraryProvider tagLibraryProvider
	) {
		this.documentIndexProvider = documentIndexProvider;
		this.commentsProvider = commentsProvider;
		this.collectionProvider = collectionProvider;
		this.tagLibraryProvider = tagLibraryProvider;

		this.documentIndexCache = CacheBuilder.newBuilder().maximumSize(10).build(
				new CacheLoader<String, Set<Term>>() {
					@Override
					public Set<Term> load(String key) throws Exception {
						return loadDocumentIndex(key);
					}
				}
		);
	}

	@SuppressWarnings({ "rawtypes" })
	private Set<Term> loadDocumentIndex(String documentId) throws Exception {
		Map documentIndexContent = documentIndexProvider.getDocumentIndex(documentId);

		Set<Term> terms = new HashSet<>();
		Map<Integer, Position> adjacencyMap = new HashMap<>();

		for (Object entry : documentIndexContent.entrySet()) {
			String literal = (String)((Map.Entry) entry).getKey();
			List positions = (List)((Map.Entry) entry).getValue();
			Term term = new Term(literal, positions.size());
			terms.add(term);

			for (Object positionEntry : positions) {
				int startOffset = ((Double) ((Map) positionEntry).get("startOffset")).intValue();
				int endOffset = ((Double) ((Map) positionEntry).get("endOffset")).intValue();
				int tokenOffset = ((Double) ((Map) positionEntry).get("tokenOffset")).intValue();

				Position position = new Position(startOffset, endOffset, tokenOffset, term);
				term.addPosition(position);
				adjacencyMap.put(tokenOffset, position);
			}
		}

		for (int i = 0; i < adjacencyMap.size(); i++) {
			Position position = adjacencyMap.get(i);

			if (i < adjacencyMap.size() - 1) {
				position.setForwardAdjacentPostion(adjacencyMap.get(i + 1));
			}

			if (i > 0) {
				position.setBackwardAdjacentPosition(adjacencyMap.get(i - 1));
			}
		}

		logger.info(String.format("Finished adding document with ID %s to the graph", documentId));

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

	private QueryResult searchPhrase(
			QueryId queryId,
			List<String> sourceDocumentIds,
			String searchPhrase,
			List<String> searchTerms,
			int limit,
			BiPredicate<String, String> termTestFunction
	) throws Exception {
		QueryResultRowArray result = new QueryResultRowArray();

		if (sourceDocumentIds.isEmpty() || searchTerms.isEmpty()) {
			return result;
		}

		for (String sourceDocumentId : sourceDocumentIds) {
			Set<Term> allDocumentTerms = documentIndexCache.get(sourceDocumentId);

			// in the case of a phrase query there can be multiple search terms which need to appear in order, but only one match per term
			// in the case of a wildcard query there will be only one search term, but potentially many matched terms
			// the loop below is therefore only relevant to wildcard queries (phrase queries start with the first search term and pass the rest to
			// Term.getPositions)
			Set<Term> matchedTerms = allDocumentTerms.parallelStream()
					.filter(term -> termTestFunction.test(term.getLiteral(), searchTerms.get(0)))
					.collect(Collectors.toSet());

			for (Term matchedTerm : matchedTerms) {
				List<List<Position>> positionLists = matchedTerm.getPositions(
						searchTerms.size() > 1 ? searchTerms.subList(1, searchTerms.size()) : Collections.emptyList(),
						termTestFunction
				);

				for (List<Position> positions : positionLists) {
					result.add(
							new QueryResultRow(
									queryId,
									sourceDocumentId,
									new Range(
											positions.get(0).getStartOffset(),
											positions.get(positions.size() - 1).getEndOffset()
									),
									searchPhrase
							)
					);
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
	public QueryResult searchTagDefinitionPath(QueryId queryId, List<String> collectionIds, String tagPathPattern) {
		QueryResultRowArray result = new QueryResultRowArray();

		// add default wildcard if no explicit root is defined
		if (!tagPathPattern.startsWith("/")) {
			tagPathPattern = "%" + tagPathPattern;
		}

		final boolean isWildcardQuery = tagPathPattern.trim().matches("^/?%+$");
		final String tagPathRegex = SQLWildcard2RegexConverter.convert(tagPathPattern);
		Map<String, String> validTagIdToTagPathMapping = new HashMap<>();
		Set<TagDefinition> validTagDefinitions = Sets.newHashSet();

		for (TagsetDefinition tagsetDefinition : tagLibraryProvider.getTagLibrary()) {
			for (TagDefinition tagDefinition : tagsetDefinition) {
				String path = tagsetDefinition.getTagPath(tagDefinition);
				if (isWildcardQuery || Pattern.matches(tagPathRegex, path)) {
					validTagIdToTagPathMapping.put(tagDefinition.getUuid(), path);
					validTagDefinitions.add(tagDefinition);
				}
			}
		}

		for (String collectionId : collectionIds) {
			AnnotationCollection collection = collectionProvider.getCollection(collectionId);

			for (TagDefinition tagDefinition : validTagDefinitions) {
				Multimap<String, TagReference> tagReferencesByTagInstanceId = collection.getTagReferencesByInstanceId(tagDefinition);

				for (String tagInstanceId : tagReferencesByTagInstanceId.keySet()) {
					result.add(
							new TagQueryResultRow(
									queryId,
									collection.getSourceDocumentId(),
									tagReferencesByTagInstanceId.get(tagInstanceId)
										.stream()
										.map(tr -> tr.getRange())
										.collect(Collectors.toList()),
									collectionId,
									tagDefinition.getUuid(),
									validTagIdToTagPathMapping.get(tagDefinition.getUuid()),
									"", // TODO: tagDefinitionVersion
									tagInstanceId
							)
					);
				}
			}
		}

		return result;
	}

	@Override
	public QueryResult searchProperty(
			QueryId queryId,
			List<String> collectionIds,
			String propertyNamePattern,
			String propertyValuePattern,
			String tagPathPattern
	) {
		QueryResultRowArray result = new QueryResultRowArray();

		PropertyNameFilter propertyNameFilter = new PropertyNameFilter(propertyNamePattern);
		PropertyValueFilter propertyValueFilter = new PropertyValueFilter(propertyValuePattern);

		// add default wildcard if no explicit root is defined
		if (tagPathPattern != null && !tagPathPattern.startsWith("/")) {
			tagPathPattern = "%" + tagPathPattern;
		}

		final boolean isWildcardQuery = tagPathPattern == null || tagPathPattern.trim().matches("^/?%+$");
		final String tagPathRegex = tagPathPattern == null ? null : SQLWildcard2RegexConverter.convert(tagPathPattern);
		Map<String, String> validTagIdToTagPathMapping = new HashMap<>();
		Set<TagDefinition> validTagDefinitions = Sets.newHashSet();

		for (TagsetDefinition tagsetDefinition : tagLibraryProvider.getTagLibrary()) {
			for (TagDefinition tagDefinition : tagsetDefinition) {
				String path = tagsetDefinition.getTagPath(tagDefinition);
				if (isWildcardQuery || Pattern.matches(tagPathRegex, path)) {
					validTagIdToTagPathMapping.put(tagDefinition.getUuid(), path);
					validTagDefinitions.add(tagDefinition);
				}
			}
		}

		for (String collectionId : collectionIds) {
			AnnotationCollection collection = collectionProvider.getCollection(collectionId);

			for (TagDefinition tagDefinition : validTagDefinitions) {
				Multimap<String, TagReference> tagReferencesByTagInstanceId = collection.getTagReferencesByInstanceId(tagDefinition);

				for (String tagInstanceId : tagReferencesByTagInstanceId.keySet()) {
					TagInstance tagInstance = tagReferencesByTagInstanceId.get(tagInstanceId).iterator().next().getTagInstance();
					List<Range> ranges = tagReferencesByTagInstanceId.get(tagInstanceId).stream()
							.map(TagReference::getRange)
							.collect(Collectors.toList());

					addTagQueryResultRowsForSystemProperties(
							queryId,
							result,
							tagDefinition,
							tagInstance,
							propertyNameFilter,
							propertyValueFilter,
							collection,
							validTagIdToTagPathMapping.get(tagDefinition.getUuid()),
							ranges
					);

					for (Property property : tagInstance.getUserDefinedProperties()) {
						if (propertyNameFilter.test(new Pair<>(property, tagDefinition))) {
							for (String value : property.getPropertyValueList()) {
								if (propertyValueFilter.testValue(value)) {
									result.add(
											new TagQueryResultRow(
													queryId,
													collection.getSourceDocumentId(),
													ranges,
													collectionId,
													tagDefinition.getUuid(),
													validTagIdToTagPathMapping.get(tagDefinition.getUuid()),
													"", // TODO: tagDefinitionVersion
													tagInstanceId,
													property.getPropertyDefinitionId(),
													tagDefinition.getPropertyDefinitionByUuid(
															property.getPropertyDefinitionId()
													).getName(),
													value
											)
									);
								}
							}
						}
					}
				}
			}
		}

		return result;
	}

	private void addTagQueryResultRowsForSystemProperties(
			QueryId queryId,
			QueryResultRowArray result,
			TagDefinition tagDefinition,
			TagInstance tagInstance,
			PropertyNameFilter propertyNameFilter,
			PropertyValueFilter propertyValueFilter,
			AnnotationCollection collection,
			String tagPath,
			List<Range> ranges
	) {
		addTagQueryResultRowForSystemProperty(
				queryId,
				result,
				SystemPropertyName.catma_markupauthor,
				tagInstance.getAuthor(),
				propertyNameFilter,
				propertyValueFilter,
				collection.getSourceDocumentId(),
				collection.getUuid(),
				tagDefinition.getUuid(),
				tagPath,
				tagInstance.getUuid(),
				ranges
		);
		addTagQueryResultRowForSystemProperty(
				queryId,
				result,
				SystemPropertyName.catma_markuptimestamp,
				tagInstance.getTimestamp(),
				propertyNameFilter,
				propertyValueFilter,
				collection.getSourceDocumentId(),
				collection.getUuid(),
				tagDefinition.getUuid(),
				tagPath,
				tagInstance.getUuid(),
				ranges
		);
		addTagQueryResultRowForSystemProperty(
				queryId,
				result,
				SystemPropertyName.catma_displaycolor,
				tagDefinition.getColor(),
				propertyNameFilter,
				propertyValueFilter,
				collection.getSourceDocumentId(),
				collection.getUuid(),
				tagDefinition.getUuid(),
				tagPath,
				tagInstance.getUuid(),
				ranges
		);
	}

	private void addTagQueryResultRowForSystemProperty(
			QueryId queryId, 
			QueryResultRowArray result, 
			SystemPropertyName systemPropertyName,
			String value,
			PropertyNameFilter propertyNameFilter,
			PropertyValueFilter propertyValueFilter,
			String documentId,
			String collectionId,
			String tagId,
			String tagPath,
			String tagInstanceId,
			List<Range> ranges
	) {
		if (propertyNameFilter.testPropertyName(systemPropertyName.name()) && propertyValueFilter.testValue(value)) {
			result.add(
					new TagQueryResultRow(
							queryId,
							documentId,
							ranges,
							collectionId,
							tagId,
							tagPath,
							"", // TODO: tagDefinitionVersion
							tagInstanceId,
							idGenerator.generate(systemPropertyName.name()),
							systemPropertyName.name(),
							value
					)
			);
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
	public QueryResult searchCommentPhrase(
			QueryId queryId,
			List<String> documentIds,
			List<String> terms,
			int limit,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters,
			Locale locale
	) throws Exception {
		List<Comment> comments = commentsProvider.getComments(documentIds);

		QueryResultRowArray result = new QueryResultRowArray();
		for (Comment comment : comments) {
			if (terms.size() == 1 && terms.get(0).equals("%")) {
				result.add(new CommentQueryResultRow(queryId, comment));
			}
			else {
				TermExtractor termExtractor = new TermExtractor(
						comment.getBody(),
						unseparableCharacterSequences,
						userDefinedSeparatingCharacters,
						locale
				);
				List<String> commentTerms = termExtractor.getTermsInOrder();

				if (matches(commentTerms, terms)) {
					result.add(new CommentQueryResultRow(queryId, comment));
				}
				else {
					for (Reply reply : comment.getReplies()) {
						TermExtractor replyTermExtractor = new TermExtractor(
								reply.getBody(),
								unseparableCharacterSequences,
								userDefinedSeparatingCharacters,
								locale
						);
						List<String> replyTerms = replyTermExtractor.getTermsInOrder();

						if (matches(replyTerms, terms)) {
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
