package de.catma.repository.git.graph.indexer;

import static de.catma.repository.git.graph.NodeType.Project;
import static de.catma.repository.git.graph.NodeType.ProjectRevision;
import static de.catma.repository.git.graph.NodeType.SourceDocument;
import static de.catma.repository.git.graph.NodeType.Term;
import static de.catma.repository.git.graph.NodeType.User;
import static de.catma.repository.git.graph.NodeType.nt;
import static de.catma.repository.git.graph.RelationType.hasDocument;
import static de.catma.repository.git.graph.RelationType.hasPosition;
import static de.catma.repository.git.graph.RelationType.hasProject;
import static de.catma.repository.git.graph.RelationType.hasRevision;
import static de.catma.repository.git.graph.RelationType.isPartOf;
import static de.catma.repository.git.graph.RelationType.rt;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;

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
import de.catma.repository.git.graph.NodeType;
import de.catma.repository.neo4j.SessionRunner;
import de.catma.repository.neo4j.StatementExcutor;
import de.catma.tag.Property;
import de.catma.tag.TagDefinitionPathInfo;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;

public class GraphProjectIndexer implements Indexer {

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
					+ createWhereCypherWithWildcardMatch(termList.size())
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

	private String createWhereCypherWithWildcardMatch(int termCount) {
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
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryResult searchProperty(List<String> userMarkupCollectionIdList, String propertyName,
			String propertyValue, String tagValue) throws IOException {
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
