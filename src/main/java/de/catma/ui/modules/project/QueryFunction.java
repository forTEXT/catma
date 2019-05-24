package de.catma.ui.modules.project;

import java.util.List;

import com.vaadin.data.provider.Query;

@FunctionalInterface
public interface QueryFunction<T> {

	List<T> apply(Query<T, String> t) throws Exception;
	
}
