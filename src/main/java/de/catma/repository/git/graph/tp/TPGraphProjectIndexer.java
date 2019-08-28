package de.catma.repository.git.graph.tp;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.Range;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.Indexer;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TagsetDefinitionUpdateLog;
import de.catma.indexer.TermInfo;
import de.catma.indexer.wildcard2regex.SQLWildcard2RegexConverter;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagDefinitionPathInfo;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;

public class TPGraphProjectIndexer implements Indexer {
	
	private Graph graph;
	private IDGenerator idGenerator;

	public TPGraphProjectIndexer(Graph graph) {
		super();
		this.graph = graph;
		this.idGenerator = new IDGenerator();
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
	public void reindex(TagsetDefinition tagsetDefinition, TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog,
			UserMarkupCollection userMarkupCollection) throws IOException {
		//noop
	}

	@Override
	public QueryResult searchPhrase(QueryId queryId, List<String> documentIdList, String phrase, List<String> termList, int limit)
			throws Exception {

		return searchPhrase(queryId, documentIdList, phrase, termList, limit, (term1, term2) -> term1.equals(term2));
	}

	private QueryResult searchPhrase(QueryId queryId, List<String> documentIdList, String phrase, List<String> termList, int limit,
			BiPredicate<String, String> termTestFunction) {
		
		GraphTraversalSource g = graph.traversal();

		GraphTraversal<Vertex, Vertex> currentTraversal = 
			g.V().hasLabel(nt(ProjectRevision))
			.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", P.within(documentIdList)).as("doc")
			.inE(rt(isPartOf))
			.outV().has(nt(Term), "literal", P.test(termTestFunction, termList.get(0)))
			.outE(rt(hasPosition)).inV().hasLabel(nt(Position)).as("startPos", "currentPos");
		
		if (termList.size() > 1) {
			for (String term : termList.subList(1, termList.size())) {
				currentTraversal = currentTraversal
					.outE(rt(isAdjacentTo)).inV().hasLabel(nt(Position)).as("currentPos")
					.inE(rt(hasPosition)).outV().has(nt(Term), "literal", P.test(termTestFunction, term)).select("currentPos");
			}
		}
		
		if (limit > 0) {
			currentTraversal = currentTraversal.limit(limit);
		}

		return new QueryResultRowArray(
			currentTraversal
			.select("doc", "startPos", "currentPos")
			.by("documentId").by("startOffset").by("endOffset")
			.map(resultMap -> 
				new QueryResultRow(
					queryId,
					(String)resultMap.get().get("doc"),
					new Range(
						(Integer)resultMap.get().get("startPos"), 
						(Integer)resultMap.get().get("currentPos")),
					phrase)
			)
			.toList()
			);
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

		GraphTraversalSource g = graph.traversal();
		
		Set<Vertex> tagVs = g.V().hasLabel(nt(ProjectRevision))
		.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
		.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", P.within(collectionIdList))
		.outE(rt(hasInstance)).inV().hasLabel(nt(TagInstance)).inE(rt(hasInstance))
		.outV().hasLabel(nt(Tag))
		.toSet();
		
		if (!tagVs.isEmpty()) {
			List<Path> tagPaths  = g.V(tagVs)
			.optional(__.repeat(__.out(rt(hasParent))).until(__.outE(rt(hasParent)).count().is(0)))
			.path()
			.toList();
			
			Map<String, String> validTagIdToTagPathMapping = new HashMap<>();
	
			for (Path path : tagPaths) {
				Vertex tag = path.get(0);
				String tagId = (String) tag.properties("tagId").next().orElse(null);
		
				StringBuilder builder = new StringBuilder();
				String conc = "/";
				
				path.forEach(
					tagVertex -> {
						builder.append(conc);
						builder.append(
							((Vertex)tagVertex).properties("name").next().orElse(null));
					});
				
				String tagPathStr = builder.toString();
				
				if (Pattern.matches(tagPathRegex, tagPathStr)) {
					validTagIdToTagPathMapping.put(tagId, tagPathStr);
				}
			}
			
			List<Map<String,Object>> resultMap = g.V().hasLabel(nt(ProjectRevision))
			.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
			.as("doc")
			.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", P.within(collectionIdList))
			.as("collection")
			.outE(rt(hasInstance)).inV().hasLabel(nt(TagInstance))
			.as("anno", "ranges")
			.inE(rt(hasInstance)).outV().has(nt(Tag), "tagId", P.within(validTagIdToTagPathMapping.keySet()))
			.as("tag")
			.select("doc", "collection", "anno", "ranges", "tag")
			.by("documentId").by("collectionId").by("tagInstanceId").by("ranges").by("tagId")
			.toList();
			
			for (Map<String,Object> entry : resultMap) {
				@SuppressWarnings("unchecked")
				List<Integer> ranges = (List<Integer>)entry.get("ranges");
				List<Range> rangeList = new ArrayList<>();
				for (int i=0; i<ranges.size()-1; i+=2) {
					rangeList.add(new Range(ranges.get(i), ranges.get(i+1)));
				}
				result.add(
					new TagQueryResultRow(
						queryId,
						(String)entry.get("doc"), 
						rangeList, 
						(String)entry.get("collection"), 
						(String)entry.get("tag"),
						validTagIdToTagPathMapping.get((String)entry.get("tag")),
						"", //TODO: version
						(String)entry.get("anno")));
			}
		}		
		
		return result;
	}

	@Override
	public QueryResult searchProperty(QueryId queryId, List<String> collectionIdList, String propertyNamePattern,
			String propertyValuePattern, String tagPathPattern) throws Exception {
		
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
		
		// get all paths for the Tags
		List<Path> tagPaths  = g.V(tagVs)
		.optional(__.repeat(__.out(rt(hasParent))).until(__.outE(rt(hasParent)).count().is(0)))
		.path()
		.toList();
		
		// collect all Tags matching the given pattern and map them by their tagId
		Map<String, String> validTagIdToTagPathMapping = new HashMap<>();

		for (Path path : tagPaths) {
			Vertex tag = path.get(0);
			String tagId = (String) tag.properties("tagId").next().orElse(null);
	
			StringBuilder builder = new StringBuilder();
			String conc = "/";
			
			path.forEach(
				tagVertex -> {
					builder.append(conc);
					builder.append(
						((Vertex)tagVertex).properties("name").next().orElse(null));
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
	
		QueryResultRowArray result = new QueryResultRowArray();
		
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
	public QueryResult searchFreqency(QueryId queryId, List<String> documentIdList, CompareOperator comp1, int freq1,
			CompareOperator comp2, int freq2) throws IOException {
		GraphTraversalSource g = graph.traversal();

		return new QueryResultRowArray(
			g.V().hasLabel(nt(ProjectRevision))
			.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", P.within(documentIdList))
			.as("doc")
			.inE(rt(isPartOf))
			.outV().hasLabel(nt(Term)).filter(new FrequencyFilter(comp1, freq1, comp2, freq2))
			.as("literal")
			.outE(rt(hasPosition)).inV().hasLabel(nt(Position)).as("startPos", "endPos")
			.select("doc", "literal", "startPos", "endPos")
			.by("documentId").by("literal").by("startOffset").by("endOffset")
			.map(resultMap -> 
				new QueryResultRow(
					queryId,
					(String)resultMap.get().get("doc"),
					new Range(
						(Integer)resultMap.get().get("startPos"), 
						(Integer)resultMap.get().get("endPos")),
					(String)resultMap.get().get("literal"))
			)
			.toList());
	}

	@Override
	public SpanContext getSpanContextFor(String sourceDocumentId, Range range, int spanContextSize,
			SpanDirection direction) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchCollocation(QueryId queryId, QueryResult baseResult, QueryResult collocationConditionResult,
			int spanContextSize, SpanDirection direction) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TermInfo> getTermInfosFor(String sourceDocumentId, Range range) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateIndex(TagInstance tagInstance, Collection<Property> properties) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeUserMarkupCollections(Collection<String> usermarkupCollectionIDs) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<TagDefinitionPathInfo> getTagDefinitionPathInfos(List<String> userMarkupCollectionIDs)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchTagDiff(QueryId queryId, List<String> relevantUserMarkupCollIDs, String propertyName, String tagPhrase)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
