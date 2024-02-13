package de.catma.ui.module.dashboard;

import java.util.Comparator;

public class SortItem<T> {
	private Comparator<T> sortComparator;
	private String sortCaption;
	public SortItem(Comparator<T> sortComparator, String sortCaption) {
		super();
		this.sortComparator = sortComparator;
		this.sortCaption = sortCaption;
	}

	@Override
	public String toString() {
		return sortCaption;
	}
	
	public Comparator<T> getSortComparator() {
		return sortComparator;
	}
}