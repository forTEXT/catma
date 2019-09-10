package de.catma.ui.module.project;

import java.util.List;

import com.vaadin.data.provider.Query;

@FunctionalInterface
public interface QueryFunction<T> {

	List<T> apply(Query<T, String> t) throws Exception;
	
}
