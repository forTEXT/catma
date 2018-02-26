package de.catma.ui.project;

import org.vaadin.addons.lazyquerycontainer.Query;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;
import org.vaadin.addons.lazyquerycontainer.QueryFactory;

public class ProjectQueryFactory implements QueryFactory {

	@Override
	public Query constructQuery(QueryDefinition queryDefinition) {
		try {
			return new ProjectQuery(((ProjectQueryDefinition)queryDefinition).getProjectManager());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
