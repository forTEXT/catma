package de.catma.indexer.db;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.jboss.logging.Logger;

import de.catma.core.document.Range;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.tag.TagLibrary;
import de.catma.indexer.Indexer;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class DBIndexer implements Indexer {
	
	private class TermPosition {
		private String documentId;
		private int characterStart;
		private int characterEnd;
		private int tokenOffset;
		private TermPosition(String documentId, int characterStart,
				int characterEnd, int tokenOffset) {
			super();
			this.documentId = documentId;
			this.characterStart = characterStart;
			this.characterEnd = characterEnd;
			this.tokenOffset = tokenOffset;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + characterEnd;
			result = prime * result + characterStart;
			result = prime * result
					+ ((documentId == null) ? 0 : documentId.hashCode());
			result = prime * result + tokenOffset;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TermPosition other = (TermPosition) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (characterEnd != other.characterEnd)
				return false;
			if (characterStart != other.characterStart)
				return false;
			if (documentId == null) {
				if (other.documentId != null)
					return false;
			} else if (!documentId.equals(other.documentId))
				return false;
			if (tokenOffset != other.tokenOffset)
				return false;
			return true;
		}
		private DBIndexer getOuterType() {
			return DBIndexer.this;
		}
		
		
	}
	
	private enum TableName {
		term,
		position,
		;
		
		public String withTableAlias(String tableAlias) {
			return this + " " + tableAlias;
		}
	}
	
	private enum TableColumnName {
		term_termID("termID"),
		term_documentID("documentID"),
		term_term("term"),
		term_frequency("frequency"),
		position_positionID("positionID"),
		position_termID("termID"),
		position_characterStart("characterStart"),
		position_characterEnd("characterEnd"),
		position_tokenOffset("tokenOffset"),
		;
		private String columnName;

		private TableColumnName(String columnName) {
			this.columnName = columnName;
		}
		
		public String withTableAlias(String tableAlias) {
			return tableAlias + "." + columnName;
		}
	}
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private SessionFactory sessionFactory; 
	private Configuration hibernateConfig;
	
	public DBIndexer() {
		hibernateConfig = new Configuration();
		hibernateConfig.configure(
				this.getClass().getPackage().getName().replace('.', '/') + "/hibernate.cfg.xml");
		ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
		serviceRegistryBuilder.applySettings(hibernateConfig.getProperties());
		ServiceRegistry serviceRegistry = 
				serviceRegistryBuilder.buildServiceRegistry();
		
		sessionFactory = hibernateConfig.buildSessionFactory(serviceRegistry);
	}
	

	public void index(SourceDocument sourceDocument,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale)
			throws Exception {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			
			
			TermExtractor termExtractor = 
					new TermExtractor(
						sourceDocument.getContent(), 
						unseparableCharacterSequences, 
						userDefinedSeparatingCharacters, 
						locale);
			
			Map<String, List<TermInfo>> terms = termExtractor.getTerms();
			
			for (Map.Entry<String, List<TermInfo>> entry : terms.entrySet()) {
				Term term = new Term(
						sourceDocument.getID(), 
						entry.getValue().size(), entry.getKey());
				session.save(term);
				
				for (TermInfo ti : entry.getValue()) {
					Position p = new Position(
						term.getTermId(),
						ti.getRange().getStartPoint(),
						ti.getRange().getEndPoint(),
						ti.getTokenOffset());
					session.save(p);
				}
			}
			
			session.getTransaction().commit();
			session.close();
		}
		catch (Exception e) {
			try {
				session.getTransaction().rollback();
			}
			catch (Throwable notOfInterest) {}
			session.close();
			throw e;
		}
	}

	public void index(List<TagReference> tagReferences,
			String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws Exception {
		// TODO Auto-generated method stub

	}

//	public QueryResult searchPhrase(List<String> documentIdList,
//			String phrase, List<String> termList) throws Exception {
//		
//		
//		Session session = sessionFactory.openSession();
//		StringBuilder builder = new StringBuilder();
//		
//		if ((documentIdList==null) || documentIdList.isEmpty()) {
//			addRangeSelectForDocument(null, builder, termList);
//		}
//		else {
//			String conc = "";
//			for (String documentId : documentIdList) {
//				builder.append(conc);
//				addRangeSelectForDocument(documentId, builder, termList);
//				conc = " UNION ";
//			}
//		}
//		
//		logger.info(
//			"excuting query for phrase " + phrase + ":\n" + builder.toString());
//		
//		SQLQuery q = 
//				session.createSQLQuery(builder.toString());
//	
//		@SuppressWarnings("rawtypes")
//		List result = q.list();
//		QueryResultRowArray queryResult = new QueryResultRowArray();
//		
//		for (Object resultRow : result) {
//			Object[] resultColumns = (Object[])resultRow;
//			
//			queryResult.add(new QueryResultRow(
//				resultColumns[0].toString(), 
//				new Range(
//					Integer.valueOf(resultColumns[1].toString()),
//					Integer.valueOf(resultColumns[2].toString())),
//				phrase));
//		}
//		
//		return queryResult;
//	}
	
	public QueryResult searchPhrase(List<String> documentIdList,
			String phrase, List<String> termList) throws Exception {
		
		
		Session session = sessionFactory.openSession();
		StringBuilder builder = new StringBuilder();
		
		if ((documentIdList==null) || documentIdList.isEmpty()) {
			addRangeSelectForTerm(null, builder, termList.get(0), -1);
		}
		else {
			String conc = "";
			for (String documentId : documentIdList) {
				builder.append(conc);
				addRangeSelectForTerm(documentId, builder, termList.get(0), -1);
				conc = " UNION ";
			}
		}
		
		logger.info(
			"excuting query for phrase " + phrase + ":\n" + builder.toString());
		
		SQLQuery q = 
				session.createSQLQuery(builder.toString());
	
		@SuppressWarnings("rawtypes")
		List result = q.list();
		
		
		QueryResultRowArray queryResult = new QueryResultRowArray();
		HashSet<TermPosition> termPositions = new HashSet<DBIndexer.TermPosition>();
		for (Object resultRow : result) {
			Object[] resultColumns = (Object[])resultRow;
			
			termPositions.add(new TermPosition(
				resultColumns[0].toString(),
				Integer.valueOf(resultColumns[1].toString()),
				Integer.valueOf(resultColumns[2].toString()), 
				Integer.valueOf(resultColumns[3].toString())));
			
		}
		for (int i=1; i<termList.size();i++) {
			filterWithTerm(termList.get(i), i, termPositions);
		}
		
		return queryResult;
	}
	
	private void filterWithTerm(String term, int tokenOffsetFromStartToken,
			HashSet<TermPosition> termPositions) {

		
	}


	private void addRangeSelectForTerm(String documentId, StringBuilder builder, String term, int tokenOffset) {
		builder.append("SELECT ");
		builder.append(TableColumnName.term_documentID.withTableAlias("t1"));
		builder.append(",");
		builder.append(TableColumnName.position_characterStart.withTableAlias("p1"));
		builder.append(",");
		builder.append(TableColumnName.position_characterEnd.withTableAlias("p1"));
		builder.append(",");
		builder.append(TableColumnName.position_tokenOffset.withTableAlias("p1"));
		builder.append(" FROM ");
		builder.append(TableName.position.withTableAlias("p1"));
		
		builder.append(" JOIN ");
		builder.append(TableName.term.withTableAlias("t1"));
		builder.append(" ON ");
		builder.append(TableColumnName.position_termID.withTableAlias("p1"));
		builder.append(" = ");
		builder.append(TableColumnName.term_termID.withTableAlias("t1"));
		if (term != null) {
			builder.append(" AND ");
			builder.append(TableColumnName.term_term.withTableAlias("t1"));
			builder.append("='");
			builder.append(term);
			builder.append("' ");
		}
		if (documentId != null) {
			builder.append(" AND ");
			builder.append(TableColumnName.term_documentID.withTableAlias("t1"));
			builder.append("='");
			builder.append(documentId);
			builder.append("'");
		}
		
		if (tokenOffset != -1) {
			builder.append( " WHERE ");
			builder.append(TableColumnName.position_tokenOffset.withTableAlias("p1"));
			builder.append(" = ");
			builder.append(tokenOffset);
		}
	}
	
	private void addRangeSelectForDocument(String documentId, StringBuilder builder, List<String> termList) {
		int lastIdx = termList.size();
		
		builder.append("SELECT ");
		builder.append(TableColumnName.term_documentID.withTableAlias("t1"));
		builder.append(",");
		builder.append(TableColumnName.position_characterStart.withTableAlias("p1"));
		builder.append(",");
		builder.append(TableColumnName.position_characterEnd.withTableAlias("p"+lastIdx));
		builder.append(" FROM ");
		builder.append(TableName.position.withTableAlias("p1"));
		
		builder.append(" JOIN ");
		builder.append(TableName.term.withTableAlias("t1"));
		builder.append(" ON ");
		builder.append(TableColumnName.position_termID.withTableAlias("p1"));
		builder.append(" = ");
		builder.append(TableColumnName.term_termID.withTableAlias("t1"));
		builder.append(" AND ");
		builder.append(TableColumnName.term_term.withTableAlias("t1"));
		builder.append("='");
		builder.append(termList.get(0));
		builder.append("' ");
		if (documentId != null) {
			builder.append(" AND ");
			builder.append(TableColumnName.term_documentID.withTableAlias("t1"));
			builder.append("='");
			builder.append(documentId);
			builder.append("'");
		}
		builder.append("JOIN ");
		builder.append(TableName.position.withTableAlias("p"+lastIdx));
		builder.append(" ON ");
		builder.append(TableColumnName.position_tokenOffset.withTableAlias("p"+lastIdx));
		builder.append("-");
		builder.append(lastIdx-1);
		builder.append(" = ");
		builder.append(TableColumnName.position_tokenOffset.withTableAlias("p1"));
		builder.append(" JOIN ");
		builder.append(TableName.term.withTableAlias("t"+lastIdx));
		builder.append(" ON ");
		builder.append(TableColumnName.term_termID.withTableAlias("t"+lastIdx));
		builder.append(" = ");
		builder.append(TableColumnName.position_termID.withTableAlias("p"+lastIdx));
		builder.append(" AND ");
		builder.append(TableColumnName.term_term.withTableAlias("t"+lastIdx));
		builder.append("= '");
		builder.append(termList.get(lastIdx-1));
		builder.append("'");
		builder.append(" WHERE ");
		builder.append(TableColumnName.term_documentID.withTableAlias("t1"));
		builder.append("=");
		builder.append(TableColumnName.term_documentID.withTableAlias("t"+lastIdx));
		
		if (termList.size() > 2) {
			for (int i=1; i<termList.size()-1; i++) {
				builder.append(" AND ");
				builder.append(TableColumnName.position_tokenOffset.withTableAlias("p"+i));
				builder.append(" IN (SELECT ");
				builder.append(TableColumnName.position_tokenOffset.withTableAlias("p"+(i+1)));
				builder.append("-1 FROM ");
				builder.append(TableName.position.withTableAlias("p"+(i+1)));
				builder.append(" JOIN ");
				builder.append(TableName.term.withTableAlias("t"+(i+1)));
				builder.append(" ON ");
				builder.append(TableColumnName.term_termID.withTableAlias("t"+(i+1)));
				builder.append(" = ");
				builder.append(TableColumnName.position_termID.withTableAlias("p"+(i+1)));
				builder.append(" AND ");
				builder.append(TableColumnName.term_term.withTableAlias("t"+(i+1)));
				builder.append("= '");
				builder.append(termList.get(i));
				builder.append("'");
				if (documentId != null) {
					builder.append(" AND ");
					builder.append(TableColumnName.term_documentID.withTableAlias("t"+(i+1)));
					builder.append("='");
					builder.append(documentId);
					builder.append("'");
				}
				builder.append(" WHERE ");
				builder.append(TableColumnName.term_documentID.withTableAlias("t1"));
				builder.append("=");
				builder.append(TableColumnName.term_documentID.withTableAlias("t"+(i+1)));
			}
						
			for (int i=2; i<termList.size(); i++) {
				builder.append(")");
			}
		}
		
	}

	public QueryResultRowArray searchTag(String tagPath, boolean isPrefixSearch)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() {
		sessionFactory.close();
	}

}
