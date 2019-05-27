package de.catma.ui.component.actiongrid;

import com.vaadin.server.SerializablePredicate;

public interface SearchFilterProvider<T> {

	public SerializablePredicate<T> createSearchFilter(String searchInput);
}
