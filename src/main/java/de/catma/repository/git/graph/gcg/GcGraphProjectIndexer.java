package de.catma.repository.git.graph.gcg;

import static de.catma.repository.git.graph.NodeType.AnnotationProperty;
import static de.catma.repository.git.graph.NodeType.MarkupCollection;
import static de.catma.repository.git.graph.NodeType.Position;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
import static de.catma.repository.git.graph.NodeType.Property;
import static de.catma.repository.git.graph.NodeType.SourceDocument;
import static de.catma.repository.git.graph.NodeType.Tag;
import static de.catma.repository.git.graph.NodeType.TagInstance;
import static de.catma.repository.git.graph.NodeType.Term;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasCollection;
import static de.catma.repository.git.graph.RelationType.hasDocument;
import static de.catma.repository.git.graph.RelationType.hasInstance;
import static de.catma.repository.git.graph.RelationType.hasParent;
import static de.catma.repository.git.graph.RelationType.hasPosition;
import static de.catma.repository.git.graph.RelationType.hasProperty;
import static de.catma.repository.git.graph.RelationType.isAdjacentTo;
import static de.catma.repository.git.graph.RelationType.isPartOf;
import static de.catma.repository.git.graph.RelationType.rt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.glassfish.jersey.internal.guava.Sets;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;

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
import de.catma.repository.git.graph.CommentProvider;
import de.catma.repository.git.graph.FileInfoProvider;
import de.catma.repository.git.graph.GraphProjectHandler.DocumentSupplier;
import de.catma.repository.git.graph.tp.InRangeFilter;
import de.catma.repository.git.graph.tp.PropertyValueFilter;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;

public class GcGraphProjectIndexer implements Indexer {
	@Deprecated
	private Graph graph;
	private final Logger logger = Logger.getLogger(GcGraphProjectIndexer.class.getName());
	private final CommentProvider commentProvider;
	private IDGenerator idGenerator;
	private FileInfoProvider fileInfoProvider;
	private DocumentSupplier documentSupplier;
	private Function<String, AnnotationCollection> collectionSupplier;
	private LoadingCache<String, Set<Term>> documentIndexCache;
	private Supplier<TagLibrary> tagLibrarySupplier;

	public GcGraphProjectIndexer(FileInfoProvider fileInfoProvider, CommentProvider commentProvider,
			final DocumentSupplier documentSupplier, final Function<String, AnnotationCollection> collectionSupplier, 
			final Supplier<TagLibrary> tagLibrarySupplier) {
		super();
		this.fileInfoProvider = fileInfoProvider;
		this.commentProvider = commentProvider;
		this.documentSupplier = documentSupplier;
		this.collectionSupplier = collectionSupplier;
		this.tagLibrarySupplier = tagLibrarySupplier;
		
		this.idGenerator = new IDGenerator();
		
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
		
		Path tokensPath = fileInfoProvider.getTokenizedSourceDocumentPath(documentId);
		Map content = new Gson().fromJson(FileUtils.readFileToString(tokensPath.toFile(), "UTF-8"), Map.class);
		
		Map<Integer, Position> adjacencyMap = new HashMap<>();
		for (Object entry : content.entrySet()) {
			
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
		for (int i=0; i<adjacencyMap.size()-1; i++) {
			// isAdjacentTo
			adjacencyMap.get(i).setAdjacentPostion(adjacencyMap.get(i+1));
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

		GraphTraversalSource g = graph.traversal();
		
		// get all Tags referenced by the participating Collections
		GraphTraversal<Vertex, Vertex> traversal = g.V().hasLabel(nt(ProjectRevision))
		.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
		.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", P.within(collectionIdList))
		.outE(rt(hasInstance)).inV().hasLabel(nt(TagInstance)).inE(rt(hasInstance))
		.outV().hasLabel(nt(Tag));
		
		Set<Vertex> tagVs = traversal.toSet();
		if (!tagVs.isEmpty()) {
			// get all paths for the Tags
			List<org.apache.tinkerpop.gremlin.process.traversal.Path> tagPaths  = g.V(tagVs)
			.optional(__.repeat(__.out(rt(hasParent))).until(__.outE(rt(hasParent)).count().is(0)))
			.path()
			.toList();
			
			// collect all Tags matching the given pattern and map them by their tagId
			Map<String, String> validTagIdToTagPathMapping = new HashMap<>();
	
			for (org.apache.tinkerpop.gremlin.process.traversal.Path path : tagPaths) {
				Vertex tag = path.get(0);
				String tagId = (String) tag.properties("tagId").next().orElse(null);
		
				StringBuilder builder = new StringBuilder();
				String conc = "/";
				
				path.forEach(
					tagVertex -> {
						builder.insert(0,
							((Vertex)tagVertex).properties("name").next().orElse(null));
						builder.insert(0, conc);
					});
				
				String tagPathStr = builder.toString();
				
				if ((tagPathRegex==null) || Pattern.matches(tagPathRegex, tagPathStr)) {
					validTagIdToTagPathMapping.put(tagId, tagPathStr);
				}
			}
			
			// get all Annotations for the participating Collections and Tags with their matching Annotaiton Properties 
			List<Map<String,Object>> resultMap = g.V().hasLabel(nt(ProjectRevision))
			.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
			.as("doc-uuid")
			.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", P.within(collectionIdList))
			.as("collection-uuid")
			.outE(rt(hasInstance)).inV().hasLabel(nt(TagInstance))
			.as("anno")
			.optional(__.outE(rt(hasProperty)).inV().hasLabel(nt(AnnotationProperty)).filter(propertyValueFilter))
			.as("anno-property")
			.select("anno")
			.inE(rt(hasInstance)).outV().has(nt(Tag), "tagId", P.within(validTagIdToTagPathMapping.keySet()))
			.as("tag")
			.optional(__.outE(rt(hasProperty)).inV().hasLabel(nt(Property)).filter(propertyNameFilter))
			.as("property")
			.select("doc-uuid", "collection-uuid", "anno", "tag", "anno-property", "property")
			.by("documentId").by("collectionId").by().by().by().by()
			.toList();
		

			HashSet<String> systemPropertiesAddedTagInstanceIds = new HashSet<>();
			
			for (Map<String,Object> entry : resultMap) {
				String documentId = (String)entry.get("doc-uuid");
				String collectionId = (String)entry.get("collection-uuid");
				
				Vertex annoV = (Vertex) entry.get("anno");
				String tagInstanceId = (String)annoV.property("tagInstanceId").value();
				
				@SuppressWarnings("unchecked")
				List<Integer> ranges = (List<Integer>)annoV.property("ranges").value();
				
				List<Range> rangeList = new ArrayList<>();
				for (int i=0; i<ranges.size()-1; i+=2) {
					rangeList.add(new Range(ranges.get(i), ranges.get(i+1)));
				}
				
				String annoAuthor = (String)annoV.property("author").value();
				String annoTimestamp = (String)annoV.property("timestamp").value();
				
				Vertex tagV = (Vertex)entry.get("tag");
				
				String tagId = (String) tagV.property("tagId").value();
				String tagPath = validTagIdToTagPathMapping.get(tagId);
				TagDefinition tag = (TagDefinition) tagV.property("tag").value();
				String color = tag.getColor();
				
				Vertex propertyV = (Vertex)entry.get("property");
				if (propertyV.equals(tagV)) {
					propertyV = null; // no matching Properties for this Tag
				}
				
				Vertex annoPropertyV = (Vertex)entry.get("anno-property");
				if (annoPropertyV.equals(annoV)) {
					annoPropertyV = null; //no matching Annotation Property for this Annotation
				}
				
				// if we haven't added system properties for the current tagInstance yet
				// we try to add them now with respect to user defined name and value filters
				if (!systemPropertiesAddedTagInstanceIds.contains(tagInstanceId)) {
					// try to add rows for matching system properties
					addTagQueryResultRowForSystemProperty(
						queryId,
						result, 
						PropertyDefinition.SystemPropertyName.catma_markupauthor, 
						annoAuthor,
						propertyNameFilter,
						propertyValueFilter,
						documentId,
						collectionId,
						tagId,
						tagPath,
						tagInstanceId,
						rangeList);
					addTagQueryResultRowForSystemProperty(
						queryId,
						result, 
						PropertyDefinition.SystemPropertyName.catma_markuptimestamp, 
						annoTimestamp,
						propertyNameFilter,
						propertyValueFilter,
						documentId,
						collectionId,
						tagId,
						tagPath,
						tagInstanceId,
						rangeList);		
					addTagQueryResultRowForSystemProperty(
						queryId,
						result, 
						PropertyDefinition.SystemPropertyName.catma_displaycolor, 
						color,
						propertyNameFilter,
						propertyValueFilter,
						documentId,
						collectionId,
						tagId,
						tagPath,
						tagInstanceId,
						rangeList);
					systemPropertiesAddedTagInstanceIds.add(tagInstanceId);
				}			
				
				// add rows for user defined properties for each matching value
				if ((propertyV != null) && (annoPropertyV != null)) {
					@SuppressWarnings("unchecked")
					List<String> propertyValues = (List<String>) annoPropertyV.property("values").value();
					String annoPropertyDefinitionId = (String)annoPropertyV.property("uuid").value();
					String propertyName = (String)propertyV.property("name").value();
					String propertyDefinitionId = (String)propertyV.property("uuid").value();
					
					if (annoPropertyDefinitionId.equals(propertyDefinitionId)) {
						for (String propValue : propertyValues) {
							if (propertyValueFilter.testValue(propValue)) {
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
										annoPropertyDefinitionId,
										propertyName,
										propValue));
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
		
		GraphTraversalSource g = graph.traversal();
		
		List<Vertex> positionVs = g.V().hasLabel(nt(ProjectRevision))
		.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", sourceDocumentId)
		.inE(rt(isPartOf))
		.outV().hasLabel(nt(Term))
		.outE(rt(hasPosition)).inV().hasLabel(nt(Position))
		.filter(new InRangeFilter(range))
		.order().by("tokenOffset", Order.asc)
		.toList();
		
		SpanContext spanContext = new SpanContext(sourceDocumentId);
		if (!positionVs.isEmpty()) {
			Vertex firstPositionV = positionVs.get(0);
			Vertex lastPositionV = positionVs.get(positionVs.size()-1);
			
			if (direction.equals(SpanDirection.BOTH) || direction.equals(SpanDirection.BACKWARD)) {
				GraphTraversal<Vertex, org.apache.tinkerpop.gremlin.process.traversal.Path> backwardAdjacencyTraversal = 
					g.V(firstPositionV).repeat(__.in(rt(isAdjacentTo))).times(spanContextSize).path();
				
				if (backwardAdjacencyTraversal.hasNext()) {
					org.apache.tinkerpop.gremlin.process.traversal.Path backwardAdjacencyPath = backwardAdjacencyTraversal.next();
					Iterator<Object> backwardAdjacencyPathIterator = backwardAdjacencyPath.iterator();
					//skip first
					backwardAdjacencyPathIterator.next();
					while(backwardAdjacencyPathIterator.hasNext()) {
						Vertex positionVertex = (Vertex)backwardAdjacencyPathIterator.next();
						
						Vertex termV = 
							g.V(positionVertex).inE(rt(hasPosition)).outV().hasLabel(nt(Term)).next();
						
						String term = (String)termV.property("literal").value();
						int tokenOffset = (int)positionVertex.property("tokenOffset").value();
						int startOffset = (int)positionVertex.property("startOffset").value();
						int endOffset = (int)positionVertex.property("endOffset").value();
						
						spanContext.addBackwardToken(new TermInfo(term, startOffset, endOffset, tokenOffset));
					}
				}
			}
			
			if (direction.equals(SpanDirection.BOTH) || direction.equals(SpanDirection.FORWARD)) {
				GraphTraversal<Vertex, org.apache.tinkerpop.gremlin.process.traversal.Path> forwardAdjacencyTraversal = 
					g.V(lastPositionV).repeat(__.out(rt(isAdjacentTo))).times(spanContextSize).path();
				
				if (forwardAdjacencyTraversal.hasNext()) {
					org.apache.tinkerpop.gremlin.process.traversal.Path forwardAdjacencyPath = forwardAdjacencyTraversal.next();
					Iterator<Object> forwardAdjacencyPathIterator = forwardAdjacencyPath.iterator();
					//skip first
					forwardAdjacencyPathIterator.next();
					while(forwardAdjacencyPathIterator.hasNext()) {
						Vertex positionVertex = (Vertex)forwardAdjacencyPathIterator.next();
						
						Vertex termV = 
							g.V(positionVertex).inE(rt(hasPosition)).outV().hasLabel(nt(Term)).next();
						
						String term = (String)termV.property("literal").value();
						int tokenOffset = (int)positionVertex.property("tokenOffset").value();
						int startOffset = (int)positionVertex.property("startOffset").value();
						int endOffset = (int)positionVertex.property("endOffset").value();
						
						spanContext.addForwardToken(new TermInfo(term, startOffset, endOffset, tokenOffset));
					}
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
	}
	

	@Override
	public List<TermInfo> getTermInfosFor(String sourceDocumentId, Range range) throws IOException {
		throw new UnsupportedOperationException("there hasn't been a use case for this so far");
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
