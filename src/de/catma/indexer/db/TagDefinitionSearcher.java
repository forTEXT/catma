/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.indexer.db;

import static de.catma.repository.db.jooqgen.catmaindex.Tables.PROPERTY;
import static de.catma.repository.db.jooqgen.catmaindex.Tables.TAGREFERENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEF_POSSIBLEVALUE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGSETDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.impl.DSL;

import com.google.common.base.Function;

import de.catma.document.Range;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.repository.db.CatmaDataSourceName;
import de.catma.repository.db.jooq.ResultUtil;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinitionPathInfo;
import de.catma.util.IDGenerator;

public class TagDefinitionSearcher {

	private static class GroupByTagInstanceIdFunction implements
			Function<Record, String> {
		private IDGenerator idGenerator;
		
		public GroupByTagInstanceIdFunction(IDGenerator idGenerator) {
			this.idGenerator = idGenerator;
		}

		public String apply(Record r) {
			return idGenerator.uuidBytesToCatmaID(r.getValue(TAGREFERENCE.TAGINSTANCEID));
		}
	}
	
	private static class GroupByTagInstanceIdPropertyDefIdFunction implements
		Function<Record, String> {
		private IDGenerator idGenerator;
		
		public GroupByTagInstanceIdPropertyDefIdFunction(IDGenerator idGenerator) {
			this.idGenerator = idGenerator;
		}
		public String apply(Record r) {
			return idGenerator.uuidBytesToCatmaID(r.getValue(TAGREFERENCE.TAGINSTANCEID))
					+ "_"
					+idGenerator.uuidBytesToCatmaID(r.getValue(PROPERTY.PROPERTYDEFINITIONID));
		}
	}

	private IDGenerator idGenerator;
	private GroupByTagInstanceIdFunction groupByTagInstanceIdFunction;
	private GroupByTagInstanceIdPropertyDefIdFunction groupByTagInstanceIdPropertyDefIdFunction;

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private DataSource dataSource;

	public TagDefinitionSearcher() throws NamingException {
		this.idGenerator = new IDGenerator();
		this.groupByTagInstanceIdFunction = new GroupByTagInstanceIdFunction(idGenerator);
		this.groupByTagInstanceIdPropertyDefIdFunction = new GroupByTagInstanceIdPropertyDefIdFunction(idGenerator);
		Context  context = new InitialContext();
		this.dataSource = (DataSource) context.lookup(CatmaDataSourceName.CATMADS.name());
	}

	public QueryResult search(
			List<String> userMarkupCollectionIdList, String tagDefinitionPath) {
		return searchInUserMarkupCollection(userMarkupCollectionIdList, tagDefinitionPath);
	}
	

	private QueryResult searchInUserMarkupCollection(
			List<String> userMarkupCollectionIdList, 
			String tagDefinitionPath) {
		
		if (!tagDefinitionPath.startsWith("/")) {
			tagDefinitionPath = "%" + tagDefinitionPath;
		}
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
		Map<String, List<Record>> recordsGroupedByInstanceUUID = ResultUtil.asGroups(
				groupByTagInstanceIdFunction, 
				db
				.select()
				.from(TAGREFERENCE)
				.where(TAGREFERENCE.TAGDEFINITIONPATH
						.likeIgnoreCase(tagDefinitionPath))
				.and(TAGREFERENCE.USERMARKUPCOLLECTIONID
						.in(userMarkupCollectionIdList))
				.fetch());
		
		return createTagQueryResult(recordsGroupedByInstanceUUID, tagDefinitionPath, false);
		
	}

	private QueryResult createTagQueryResult(
			Map<String, List<Record>> recordsGroupedByInstanceUUID, 
			String resultGroupKey, boolean keepProperties) {
		TagQueryResult result = new TagQueryResult(resultGroupKey);
		
		for (Map.Entry<String, List<Record>> entry : 
				recordsGroupedByInstanceUUID.entrySet()) {
			
			Record masterRecord = null;
			
			SortedSet<Range> ranges = new TreeSet<Range>();
			for (Record r : entry.getValue()) {
				ranges.add(
					new Range(
						r.getValue(TAGREFERENCE.CHARACTERSTART), 
						r.getValue(TAGREFERENCE.CHARACTEREND)));
				if (masterRecord == null) {
					masterRecord = r;
				}
			}
			// the merge is relevant for refinement matching (see TagMatchMode)
			List<Range> mergedRanges = Range.mergeRanges(ranges);

			if (keepProperties) {
				result.add(
					new TagQueryResultRow(
						masterRecord.getValue(TAGREFERENCE.DOCUMENTID),
						mergedRanges, 
						masterRecord.getValue(TAGREFERENCE.USERMARKUPCOLLECTIONID),
						idGenerator.uuidBytesToCatmaID(
								masterRecord.getValue(TAGREFERENCE.TAGDEFINITIONID)),
						masterRecord.getValue(TAGREFERENCE.TAGDEFINITIONPATH),
						masterRecord.getValue(TAGREFERENCE.TAGDEFINITIONVERSION),
						idGenerator.uuidBytesToCatmaID(
								masterRecord.getValue(TAGREFERENCE.TAGINSTANCEID)),
						idGenerator.uuidBytesToCatmaID(
								masterRecord.getValue(PROPERTY.PROPERTYDEFINITIONID)),
						masterRecord.getValue(PROPERTY.NAME),
						masterRecord.getValue(PROPERTY.VALUE)
					));
			}
			else {
				result.add(
					new TagQueryResultRow(
						masterRecord.getValue(TAGREFERENCE.DOCUMENTID),
						mergedRanges, 
						masterRecord.getValue(TAGREFERENCE.USERMARKUPCOLLECTIONID),
						idGenerator.uuidBytesToCatmaID(
								masterRecord.getValue(TAGREFERENCE.TAGDEFINITIONID)),
						masterRecord.getValue(TAGREFERENCE.TAGDEFINITIONPATH),
						masterRecord.getValue(TAGREFERENCE.TAGDEFINITIONVERSION),
						idGenerator.uuidBytesToCatmaID(
								masterRecord.getValue(TAGREFERENCE.TAGINSTANCEID))
					));
			}
		}
		return result;
	}

	public QueryResult searchProperties(
			List<String> userMarkupCollectionIdList, 
			Set<String> propertyDefinitionIDs,
			String propertyName,
			String propertyValue, String tagDefinitionPath) {
		
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
		Select<Record> selectQuery = db
		.select()
		.from(TAGREFERENCE)
		.join(PROPERTY)
			.on(PROPERTY.TAGINSTANCEID.eq(TAGREFERENCE.TAGINSTANCEID))
			.and(PROPERTY.NAME.likeIgnoreCase(propertyName));
		
		if ((propertyValue != null) && (!propertyValue.isEmpty())) {
			
			selectQuery = ((SelectOnConditionStep<Record>)selectQuery).and(PROPERTY.VALUE.eq(propertyValue));
		}		
		
		selectQuery = ((SelectOnConditionStep<Record>)selectQuery).where(
				TAGREFERENCE.USERMARKUPCOLLECTIONID.in(userMarkupCollectionIdList));
		
		
		if ((tagDefinitionPath != null) && (!tagDefinitionPath.isEmpty())) {
			if (!tagDefinitionPath.startsWith("/")) {
				tagDefinitionPath = "%" + tagDefinitionPath;
			}
			selectQuery = ((SelectConditionStep<Record>)selectQuery).and(
					TAGREFERENCE.TAGDEFINITIONPATH.likeIgnoreCase(tagDefinitionPath));
		}
		
		Map<String, List<Record>> recordsGroupedByInstanceUUID =
				ResultUtil.asGroups(
					groupByTagInstanceIdPropertyDefIdFunction,
					selectQuery.fetch());
		
		return createTagQueryResult(
			recordsGroupedByInstanceUUID, 
			propertyName + 
					(((propertyValue==null)||propertyValue.isEmpty())?"":
						(":"+propertyValue)), true);
	}
	
	public List<TagDefinitionPathInfo> getTagDefinitionPathInfos(List<String> userMarkupCollectionIDs) {
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
		
		return db
		.selectDistinct(
				DSL.field("ciTagRef.tagDefintionPath"), 
				TAGSETDEFINITION.NAME, 
				PROPERTYDEF_POSSIBLEVALUE.VALUE)
		.from(TAGDEFINITION)
		.join(TAGSETDEFINITION)
			.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
		.join(USERMARKUPCOLLECTION)
			.on(USERMARKUPCOLLECTION.TAGLIBRARYID.eq(TAGSETDEFINITION.TAGLIBRARYID))
		.join(PROPERTYDEFINITION)
			.on(PROPERTYDEFINITION.TAGDEFINITIONID.eq(TAGDEFINITION.TAGDEFINITIONID))
			.and(PROPERTYDEFINITION.NAME.eq(PropertyDefinition.SystemPropertyName.catma_displaycolor.name()))
		.join(PROPERTYDEF_POSSIBLEVALUE)
			.on(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.eq(PROPERTYDEFINITION.PROPERTYDEFINITIONID))
		.join(db
				.selectDistinct(
					TAGREFERENCE.USERMARKUPCOLLECTIONID.as("userMarkupCollectionID"), 
					TAGREFERENCE.TAGDEFINITIONID.as("tagDefinitionID"),
					TAGREFERENCE.TAGDEFINITIONPATH.as("tagDefintionPath"))
				.from(TAGREFERENCE)
				.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.in(userMarkupCollectionIDs)).asTable("ciTagRef"))
			.on(DSL.field("ciTagRef.userMarkupCollectionID").eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
			.and(DSL.field("ciTagRef.tagDefinitionID").eq(TAGDEFINITION.UUID))
		.fetch()
		.map(new TagDefinitionPathInfoMapper());
	}
}
