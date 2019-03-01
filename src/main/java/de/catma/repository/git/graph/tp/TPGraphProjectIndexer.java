package de.catma.repository.git.graph.tp;

import static de.catma.repository.git.graph.NodeType.MarkupCollection;
import static de.catma.repository.git.graph.NodeType.Position;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
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
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.Property;
import de.catma.tag.TagDefinitionPathInfo;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

public class TPGraphProjectIndexer implements Indexer {
	
	private Graph graph;

	public TPGraphProjectIndexer(Graph graph) {
		super();
		this.graph = graph;
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
	public QueryResult searchPhrase(List<String> documentIdList, String phrase, List<String> termList, int limit)
			throws Exception {

		return searchPhrase(documentIdList, phrase, termList, limit, (term1, term2) -> term1.equals(term2));
	}

	private QueryResult searchPhrase(List<String> documentIdList, String phrase, List<String> termList, int limit,
			BiPredicate<String, String> termTestFunction) {
		
		GraphTraversalSource g = graph.traversal();

		GraphTraversal<Vertex, Vertex> currentTraversal = 
			g.V().hasLabel(nt(ProjectRevision))
			.outE(rt(hasDocument)).inV().has(nt(SourceDocument), "documentId", P.within(documentIdList)).as("doc");
		
		currentTraversal = currentTraversal
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
	public QueryResult searchWildcardPhrase(List<String> documentIdList, List<String> termList, int limit)
			throws Exception {
		
		return searchPhrase(
			documentIdList, 
			"", // phrase is added later in the processing
			termList, 
			limit, 
			(term1, term2) -> term1.matches(SQLWildcard2RegexConverter.convert(term2)));
	}

	@Override
	public QueryResult searchTagDefinitionPath(List<String> userMarkupCollectionIdList, String tagDefinitionPath)
			throws Exception {
		if (!tagDefinitionPath.startsWith("/")) {
			tagDefinitionPath = "%"+tagDefinitionPath;
		}
		final String tagDefinitionPathRegex = SQLWildcard2RegexConverter.convert(tagDefinitionPath);

		GraphTraversalSource g = graph.traversal();
		
		Set<Vertex> tagVs = g.V().hasLabel(nt(ProjectRevision))
		.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
		.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", P.within(userMarkupCollectionIdList))
		.outE(rt(hasInstance)).inV().hasLabel(nt(TagInstance)).inE(rt(hasInstance))
		.outV().hasLabel(nt(Tag))
		.toSet();
		
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
			
			if (Pattern.matches(tagDefinitionPathRegex, tagPathStr)) {
				validTagIdToTagPathMapping.put(tagId, tagPathStr);
			}
		}
		
		List<Map<String,Object>> resultMap = g.V().hasLabel(nt(ProjectRevision))
		.outE(rt(hasDocument)).inV().hasLabel(nt(SourceDocument))
		.as("doc")
		.outE(rt(hasCollection)).inV().has(nt(MarkupCollection), "collectionId", P.within(userMarkupCollectionIdList))
		.as("collection")
		.outE(rt(hasInstance)).inV().hasLabel(nt(TagInstance))
		.as("anno", "ranges")
		.inE(rt(hasInstance)).outV().has(nt(Tag), "tagId", P.within(validTagIdToTagPathMapping.keySet()))
		.as("tag")
		.select("doc", "collection", "anno", "ranges", "tag")
		.by("documentId").by("collectionId").by("tagInstanceId").by("ranges").by("tagId")
		.toList();
		
		QueryResultRowArray result = new QueryResultRowArray();
		
		for (Map<String,Object> entry : resultMap) {
			List<Integer> ranges = (List<Integer>)entry.get("ranges");
			List<Range> rangeList = new ArrayList<>();
			for (int i=0; i<ranges.size()-1; i+=2) {
				rangeList.add(new Range(ranges.get(i), ranges.get(i+1)));
			}
			result.add(
				new TagQueryResultRow(
					(String)entry.get("doc"), 
					rangeList, 
					(String)entry.get("collection"), 
					(String)entry.get("tag"),
					validTagIdToTagPathMapping.get((String)entry.get("tag")),
					"",
					(String)entry.get("anno")));
		}
		
		
		return result;
	}

	@Override
	public QueryResult searchProperty(List<String> userMarkupCollectionIdList, String propertyName,
			String propertyValue, String tagValue) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchFreqency(List<String> documentIdList, CompareOperator comp1, int freq1,
			CompareOperator comp2, int freq2) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SpanContext getSpanContextFor(String sourceDocumentId, Range range, int spanContextSize,
			SpanDirection direction) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchCollocation(QueryResult baseResult, QueryResult collocationConditionResult,
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
	public QueryResult searchTagDiff(List<String> relevantUserMarkupCollIDs, String propertyName, String tagPhrase)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
