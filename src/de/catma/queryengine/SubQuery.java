package de.catma.queryengine;

import de.catma.queryengine.result.QueryResult;

public class SubQuery extends Query {
	
	private Query subQuery;
	
	public SubQuery(Query subQuery) {
		this.subQuery = subQuery;
	}

	@Override
	protected QueryResult execute() throws Exception {
		return subQuery.getResult();
	}

	@Override
	public void setQueryOptions(QueryOptions queryOptions) {
		super.setQueryOptions(queryOptions);
		subQuery.setQueryOptions(queryOptions);
	}
}
