package de.catma.indexer.db;

//TODO: change to Entity.class.getSimpleName()
public enum DBEntityName {
	DBTerm("term"),
	DBPosition("position"),
	DBTagReference("tagreference"),
	;
	
	private String tableName;

	private DBEntityName(String tableName) {
		this.tableName = tableName;
	}
	
	public String getTableName() {
		return tableName;
	}
}
