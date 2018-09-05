package de.catma.repository.db;

import javax.sql.DataSource;

public enum CatmaDataSourceName {
	CATMADS,
	;
	
	private volatile DataSource dataSource;

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	
}
