package de.catma.indexer.db;
import java.util.List;
import java.util.logging.Logger;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import de.catma.document.Range;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;


public class PhraseSearcherSingleSQL {
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
	
	private SessionFactory sessionFactory;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	
	public PhraseSearcherSingleSQL(SessionFactory sessionFactory, Logger logger) {
		super();
		this.sessionFactory = sessionFactory;
		this.logger = logger;
	}


	public QueryResult searchPhrase(List<String> documentIdList,
			String phrase, List<String> termList) throws Exception {
		
		
		Session session = sessionFactory.openSession();
		StringBuilder builder = new StringBuilder();
		
		if ((documentIdList==null) || documentIdList.isEmpty()) {
			addRangeSelectForDocument(null, builder, termList);
		}
		else {
			String conc = "";
			for (String documentId : documentIdList) {
				builder.append(conc);
				addRangeSelectForDocument(documentId, builder, termList);
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
		
		for (Object resultRow : result) {
			Object[] resultColumns = (Object[])resultRow;
			
			queryResult.add(new QueryResultRow(
				resultColumns[0].toString(), 
				new Range(
					Integer.valueOf(resultColumns[1].toString()),
					Integer.valueOf(resultColumns[2].toString())),
				phrase));
		}
		
		return queryResult;
	}
	
	
	// way to slow for multiple term phrases :-(
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

}
