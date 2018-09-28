package de.catma.repository.git.graph.indexer;

import static de.catma.repository.git.graph.NodeType.AnnotationProperty;
import static de.catma.repository.git.graph.NodeType.MarkupCollection;
import static de.catma.repository.git.graph.NodeType.Project;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
import static de.catma.repository.git.graph.NodeType.Property;
import static de.catma.repository.git.graph.NodeType.SourceDocument;
import static de.catma.repository.git.graph.NodeType.Tag;
import static de.catma.repository.git.graph.NodeType.TagInstance;
import static de.catma.repository.git.graph.NodeType.Tagset;
import static de.catma.repository.git.graph.NodeType.Term;
import static de.catma.repository.git.graph.NodeType.User;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasCollection;
import static de.catma.repository.git.graph.RelationType.hasDocument;
import static de.catma.repository.git.graph.RelationType.hasInstance;
import static de.catma.repository.git.graph.RelationType.hasParent;
import static de.catma.repository.git.graph.RelationType.hasPosition;
import static de.catma.repository.git.graph.RelationType.hasProject;
import static de.catma.repository.git.graph.RelationType.hasProperty;
import static de.catma.repository.git.graph.RelationType.hasRevision;
import static de.catma.repository.git.graph.RelationType.hasTag;
import static de.catma.repository.git.graph.RelationType.hasTagset;
import static de.catma.repository.git.graph.RelationType.isPartOf;
import static de.catma.repository.git.graph.RelationType.rt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.neo4j.driver.internal.value.NullValue;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Path.Segment;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.Range;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.Indexer;
import de.catma.indexer.SQLWildcard2RegexConverter;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TagsetDefinitionUpdateLog;
import de.catma.indexer.TermInfo;
import de.catma.project.ProjectReference;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.repository.git.graph.NodeType;
import de.catma.repository.git.graph.RelationType;
import de.catma.repository.neo4j.SessionRunner;
import de.catma.repository.neo4j.StatementExcutor;
import de.catma.tag.Property;
import de.catma.tag.TagDefinitionPathInfo;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;

public class GraphProjectIndexer implements Indexer {
	
	private static class TagPathElement {
		private String elementDefinition;
		private String pattern;
		private String parameterName;
		
		public TagPathElement(String elementDefinition, String pattern, String parameterName) {
			super();
			this.elementDefinition = elementDefinition;
			this.pattern = pattern;
			this.parameterName = parameterName;
		}

		String getElementDefinition() {
			return elementDefinition;
		}

		String getPattern() {
			return pattern;
		}

		String getParameterName() {
			return parameterName;
		}
		
		
		
	}

	private User user;
	private ProjectReference projectReference;
	private Supplier<String> rootRevisionHashSupplier;

	public GraphProjectIndexer(User user, ProjectReference projectReference,
			Supplier<String> rootRevisionHashSupplier) {
		super();
		this.user = user;
		this.projectReference = projectReference;
		this.rootRevisionHashSupplier = rootRevisionHashSupplier;
	}

	@Override
	@Deprecated
	public void updateIndex(TagInstance tagInstance, Collection<Property> properties) throws IOException {}

	@Override
	@Deprecated
	public void removeUserMarkupCollections(Collection<String> usermarkupCollectionIDs) throws IOException {}

	@Override
	@Deprecated
	public void index(SourceDocument sourceDocument, BackgroundService backgroundService) throws Exception {}

	@Override
	@Deprecated
	public void index(List<TagReference> tagReferences, String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws IOException {}

	@Override
	@Deprecated
	public void removeSourceDocument(String sourceDocumentID) throws IOException {}

	@Override
	@Deprecated
	public void removeUserMarkupCollection(String userMarkupCollectionID) throws IOException {}

	@Override
	@Deprecated
	public void removeTagReferences(List<TagReference> tagReferences) throws IOException {}

	@Override
	@Deprecated
	public void reindex(TagsetDefinition tagsetDefinition, TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog,
			UserMarkupCollection userMarkupCollection) throws IOException {}

	@Override
	public QueryResult searchPhrase(List<String> documentIdList, String phrase, List<String> termList, int limit)
			throws Exception {
		final QueryResultRowArray result = new QueryResultRowArray();

		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					" MATCH (:"+nt(User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+ "(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(s:"+nt(SourceDocument)+")"
					+ "<-[:"+rt(isPartOf)+"]-(t0:"+nt(Term)+"{literal:{pLiteral0}})-[:"+rt(hasPosition)+"]->"
					+ "(p0:"+nt(NodeType.Position)+")"
					+ createTermPositionPathCypher(termList.size()-1, true)
					+ " WHERE s.sourceDocumentId IN {pDocumentIdList} "
					+ " RETURN s.sourceDocumentId, p0.startOffset AS startOffset"
					+ ", p"+(termList.size()-1)+".endOffset AS endOffset ",	
					Values.parameters(createParametersWithTermList(
							termList.size()>1?termList.subList(1, termList.size()):Collections.emptyList(),
							false,
							"pUserId", user.getIdentifier(),
							"pProjectId", projectReference.getProjectId(),
							"pRootRevisionHash", rootRevisionHashSupplier.get(),
							"pDocumentIdList", documentIdList,
							"pLiteral0", termList.get(0)
						))
				);
				
				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					
					String sourceDocumentId = record.get("s.sourceDocumentId").asString();
					int startOffset = record.get("startOffset").asInt();
					int endOffset = record.get("endOffset").asInt();
					
					QueryResultRow row = new QueryResultRow(sourceDocumentId, new Range(startOffset, endOffset), phrase);
					result.add(row);
				}
			}
		});
		
		return result;
	}

	private Object[] createParametersWithTermList(List<String> termList, boolean convertWildcard, Object...keysAndValues) {

		Object[] parameters = new Object[(termList.size()*2)+keysAndValues.length];
		
		for (int keyAndValueIdx=0; keyAndValueIdx<=keysAndValues.length-1; keyAndValueIdx++) {
			parameters[keyAndValueIdx] = keysAndValues[keyAndValueIdx];
		}

		int idx = keysAndValues.length;
		int termIdx = 1;

		for (String term : termList) {
			parameters[idx] = "pLiteral"+termIdx;
			idx++;
			parameters[idx] = convertWildcard?SQLWildcard2RegexConverter.convert(term):term; 
			idx++;
			
			termIdx++;
		}
		
		return parameters;
	}

	private String createTermPositionPathCypher(int termCount, boolean includeLiteralMatching) {
		if (termCount == 0) {
			return "";
		}
		
		StringBuilder builder = new StringBuilder();
		
		//	-[:isAdjacentTo]->(p1:Position) ...
		for (int argIdx = 1; argIdx<=termCount; argIdx++) {
			builder.append("-[:isAdjacentTo]->(p");
			builder.append(argIdx);
			builder.append(":Position)");
		}
		
		// ,(p1)<-[:hasPosition]-(t1:Term{literal:{pLiteral1}}) ...
		for (int argIdx = 1; argIdx<=termCount; argIdx++) {
			builder.append(",(p");
			builder.append(argIdx);
			builder.append(")<-[:hasPosition]-(t");
			builder.append(argIdx);
			builder.append(":Term");
			if (includeLiteralMatching) {
				builder.append("{literal:{pLiteral");
				builder.append(argIdx);
				builder.append("}}");
			}
			builder.append(")");
		}

		return builder.toString();
	}
	
	@Override
	public QueryResult searchWildcardPhrase(List<String> documentIdList, List<String> termList, int limit)
			throws Exception {
		final QueryResultRowArray result = new QueryResultRowArray();

		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					" MATCH (:"+nt(User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+ "(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasDocument)+"]->"
					+ "(s:"+nt(SourceDocument)+")"
					+ "<-[:"+rt(isPartOf)+"]-(t0:"+nt(Term)+")-[:"+rt(hasPosition)+"]->"
					+ "(p0:"+nt(NodeType.Position)+")"
					+ createTermPositionPathCypher(termList.size()-1, false)
					+ " WHERE s.sourceDocumentId IN {pDocumentIdList} "
					+ createWhereCypherWithTermWildcardMatch(termList.size())
					+ " RETURN s.sourceDocumentId, p0.startOffset AS startOffset"
					+ ", p"+(termList.size()-1)+".endOffset AS endOffset ",	
					Values.parameters(createParametersWithTermList(
							termList.size()>1?termList.subList(1, termList.size()):Collections.emptyList(),
							true,
							"pUserId", user.getIdentifier(),
							"pProjectId", projectReference.getProjectId(),
							"pRootRevisionHash", rootRevisionHashSupplier.get(),
							"pDocumentIdList", documentIdList,
							"pLiteral0", SQLWildcard2RegexConverter.convert(termList.get(0))
						))
				);
				
				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					
					String sourceDocumentId = record.get("s.sourceDocumentId").asString();
					int startOffset = record.get("startOffset").asInt();
					int endOffset = record.get("endOffset").asInt();
					
					QueryResultRow row = new QueryResultRow(sourceDocumentId, new Range(startOffset, endOffset));
					result.add(row);
				}
			}
		});
		
		return result;
	}

	private String createWhereCypherWithTermWildcardMatch(int termCount) {
		StringBuilder builder = new StringBuilder();
		
		for (int termIdx = 0; termIdx<termCount; termIdx++) {
			builder.append(" AND t");
			builder.append(termIdx);
			builder.append(".literal =~ {pLiteral");
			builder.append(termIdx);
			builder.append("} ");
		}
		
		return builder.toString();
	}

	@Override
	public QueryResult searchTagDefinitionPath(List<String> userMarkupCollectionIdList, String tagDefinitionPath)
			throws Exception {
		if (!tagDefinitionPath.startsWith("/")) {
			tagDefinitionPath = "%"+tagDefinitionPath;
		}
		final QueryResultRowArray result = new QueryResultRowArray();
		final String tagDefinitionPathRegex = SQLWildcard2RegexConverter.convert(tagDefinitionPath);
		
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					" MATCH (:"+nt(User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+ "(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+ "(:"+nt(Tagset)+")-[:"+rt(hasTag)+"]->(t:"+nt(Tag)+")"
					+ "-[:"+rt(hasInstance)+"]->(:"+nt(TagInstance)+")"
					+ "<-[:"+rt(hasInstance)+"]-(c:"+nt(MarkupCollection)+") " 
					+ "WHERE c.collectionId IN {pCollectionIdList} "
					+ "WITH DISTINCT t "
					+ "OPTIONAL MATCH tagPath=("
						+ "(t)-[:"+rt(hasParent)+"*..]->(tp:"+nt(Tag)+")"
					+ ") "
					+ "WHERE NOT EXISTS ((tp)-[:"+rt(hasParent)+"]->(:"+nt(Tag)+")) "
					+ "RETURN t, tagPath",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHashSupplier.get(),
						"pCollectionIdList", userMarkupCollectionIdList
					)
				);
				
				Map<String, String> validTagIdToTagPathMapping = new HashMap<>();
				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					Node tagNode = record.get("t").asNode();
					String tagId = tagNode.get("tagId").asString();
					Path tagPath = record.get("tagPath").equals(NullValue.NULL)?null:record.get("tagPath").asPath();

					StringBuilder pathBuilder = new StringBuilder();
					if (tagPath != null) {
						Stack<Segment> segmentStack = new Stack<>();
						for (Segment segment : tagPath) {
							segmentStack.push(segment);
						}
						while (!segmentStack.isEmpty()) {
							Segment segment = segmentStack.pop();
							pathBuilder.append("/");
							Node end = segment.end();
							String tagName = end.get("name").asString();
							pathBuilder.append(tagName);
						}
					}
					pathBuilder.append("/");
					pathBuilder.append(tagNode.get("name").asString());
					
					String tagPathStr = pathBuilder.toString();
					
					
					if (Pattern.matches(tagDefinitionPathRegex, tagPathStr)) {
						validTagIdToTagPathMapping.put(tagId, tagPathStr);
						System.out.println(tagPathStr);
					}
				}
				
				statementResult = session.run(
					" MATCH (:"+nt(User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+ "(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+ "(:"+nt(Tagset)+")-[:"+rt(hasTag)+"]->(t:"+nt(Tag)+")"
					+ "-[:"+rt(hasInstance)+"]->(a:"+nt(TagInstance)+")"
					+ "<-[:"+rt(hasInstance)+"]-(c:"+nt(MarkupCollection)+") " 
					+ "<-[:"+rt(hasCollection)+"]-(s:"+nt(SourceDocument)+")"
					+ "WHERE t.tagId IN {pTagIdList} "
					+ "AND c.collectionId IN {pCollectionIdList} "
					+ "RETURN s.sourceDocumentId, c.collectionId, t.tagId, a.tagInstanceId, a.ranges ",	
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHashSupplier.get(),
						"pTagIdList", validTagIdToTagPathMapping.keySet(),
						"pCollectionIdList", userMarkupCollectionIdList
					)
				);
				
				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					
					String sourceDocumentId = record.get("s.sourceDocumentId").asString();
					String collectionId = record.get("c.collectionId").asString();
					String tagId = record.get("t.tagId").asString();
					String tagInstanceId = record.get("a.tagInstanceId").asString();
					List<Integer> ranges = record.get("a.ranges").asList(val -> val.asInt());

					List<Range> rangeList = new ArrayList<>();
					for (int i=0; i<ranges.size()-1; i+=2) {
						rangeList.add(new Range(ranges.get(i), ranges.get(i+1)));
					}
					
					result.add(
						new TagQueryResultRow(
							sourceDocumentId, 
							rangeList, 
							collectionId, 
							tagId, 
							validTagIdToTagPathMapping.get(tagId), 
							null, //TODO tagVersion obsolete
							tagInstanceId));					
				}
			}
		});
		
		return result;
	}

	@Override
	public QueryResult searchProperty(List<String> userMarkupCollectionIdList, String propertyName,
			String propertyValue, String tagDefinitionPath) throws Exception {
		
		
		final QueryResultRowArray result = new QueryResultRowArray();
		final String tagDefinitionPathRegex = 
			tagDefinitionPath!=null?SQLWildcard2RegexConverter.convert(tagDefinitionPath):null;
		final boolean valueContainsWildcard = (propertyValue!=null) 
				&& SQLWildcard2RegexConverter.containsWildcard(propertyValue);
		StatementExcutor.execute(new SessionRunner() {
			@Override
			public void run(Session session) throws Exception {
				StatementResult statementResult = session.run(
					" MATCH (:"+nt(User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+ "(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+ "(:"+nt(Tagset)+")-[:"+rt(hasTag)+"]->(t:"+nt(Tag)+")"
					+ "-[:"+rt(hasInstance)+"]->(:"+nt(TagInstance)+")"
					+ "<-[:"+rt(hasInstance)+"]-(c:"+nt(MarkupCollection)+") " 
					+ "WHERE c.collectionId IN {pCollectionIdList} "
					+ "WITH DISTINCT t "
					+ "OPTIONAL MATCH tagPath=("
						+ "(t)-[:"+rt(hasParent)+"*..]->(tp:"+nt(Tag)+")"
					+ ") "
					+ "WHERE NOT EXISTS ((tp)-[:"+rt(hasParent)+"]->(:"+nt(Tag)+")) "
					+ "RETURN t, tagPath",
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHashSupplier.get(),
						"pCollectionIdList", userMarkupCollectionIdList
					)
				);
				
				Map<String, String> validTagIdToTagPathMapping = new HashMap<>();
				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					Node tagNode = record.get("t").asNode();
					String tagId = tagNode.get("tagId").asString();
					Path tagPath = record.get("tagPath").equals(NullValue.NULL)?null:record.get("tagPath").asPath();

					StringBuilder pathBuilder = new StringBuilder();
					if (tagPath != null) {
						Stack<Segment> segmentStack = new Stack<>();
						for (Segment segment : tagPath) {
							segmentStack.push(segment);
						}
						while (!segmentStack.isEmpty()) {
							Segment segment = segmentStack.pop();
							pathBuilder.append("/");
							Node end = segment.end();
							String tagName = end.get("name").asString();
							pathBuilder.append(tagName);
						}
					}
					pathBuilder.append("/");
					pathBuilder.append(tagNode.get("name").asString());
					
					String tagPathStr = pathBuilder.toString();
					
					
					if ((tagDefinitionPathRegex == null) 
							|| Pattern.matches(tagDefinitionPathRegex, tagPathStr)) {
						validTagIdToTagPathMapping.put(tagId, tagPathStr);
					}
				}
				
				statementResult = session.run(
					" MATCH (:"+nt(User)+"{userId:{pUserId}})-[:"+rt(hasProject)+"]->"
					+ "(:"+nt(Project)+"{projectId:{pProjectId}})-[:"+rt(hasRevision)+"]->"
					+ "(:"+nt(ProjectRevision)+"{revisionHash:{pRootRevisionHash}})-[:"+rt(hasTagset)+"]->"
					+ "(:"+nt(Tagset)+")-[:"+rt(hasTag)+"]->(t:"+nt(Tag)+")"
					+ "-[:"+rt(hasInstance)+"]->(a:"+nt(TagInstance)+")"
					+ "<-[:"+rt(hasInstance)+"]-(c:"+nt(MarkupCollection)+") " 
					+ "<-[:"+rt(hasCollection)+"]-(s:"+nt(SourceDocument)+"), "
					+ "(t)-[:"+rt(hasProperty)+"]->(p:"+nt(Property)+"), "
					+ "(a)-[:"+rt(hasProperty)+"]->(ap:"+nt(AnnotationProperty)+") "
					+ "WHERE t.tagId IN {pTagIdList} "
					+ "AND c.collectionId IN {pCollectionIdList} "
					+ (propertyName!=null?" AND p.name =~ {pPropertyName} ":"")
					+ ((propertyValue!=null && !valueContainsWildcard)?" AND {pPropertyValue} IN ap.values ":"")
					+ "RETURN s.sourceDocumentId, c.collectionId, t.tagId, "
					+ "a.tagInstanceId, a.ranges, p.name, ap.uuid, ap.values ",	
					Values.parameters(
						"pUserId", user.getIdentifier(),
						"pProjectId", projectReference.getProjectId(),
						"pRootRevisionHash", rootRevisionHashSupplier.get(),
						"pTagIdList", validTagIdToTagPathMapping.keySet(),
						"pCollectionIdList", userMarkupCollectionIdList,
						"pPropertyName", SQLWildcard2RegexConverter.convert(propertyName),
						"pPropertyValue", propertyValue
					)
				);
				
				
				while (statementResult.hasNext()) {
					Record record = statementResult.next();
					
					String sourceDocumentId = record.get("s.sourceDocumentId").asString();
					String collectionId = record.get("c.collectionId").asString();
					String tagId = record.get("t.tagId").asString();
					String tagInstanceId = record.get("a.tagInstanceId").asString();
					List<Integer> ranges = record.get("a.ranges").asList(val -> val.asInt());
					String propertyId = record.get("ap.uuid").asString();
					String propertyName = record.get("p.name").asString();
					List<String> propertyValues = record.get("ap.values").asList(value -> value.asString());
					
					if (!valueContainsWildcard 
						|| matchWildcardValue(
							propertyValues, SQLWildcard2RegexConverter.convert(propertyValue))) {
						
						List<Range> rangeList = new ArrayList<>();
						for (int i=0; i<ranges.size()-1; i+=2) {
							rangeList.add(new Range(ranges.get(i), ranges.get(i+1)));
						}
						for (String propertyValue : propertyValues) {
							result.add(
								new TagQueryResultRow(
									sourceDocumentId, 
									rangeList, 
									collectionId, 
									tagId, 
									validTagIdToTagPathMapping.get(tagId), 
									null, //TODO tagVersion obsolete
									tagInstanceId,
									propertyId,
									propertyName,
									propertyValue));
						}
					}
				}
			}
		});
		
		return result;
	}

	private boolean matchWildcardValue(List<String> propertyValues, String wildcardPropertyValue) {
		for (String propertyValue : propertyValues) {
			if (propertyValue.matches(wildcardPropertyValue)) {
				return true;
			}
		}
		
		return false;
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
