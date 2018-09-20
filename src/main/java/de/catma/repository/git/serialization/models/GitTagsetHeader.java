package de.catma.repository.git.serialization.models;

import java.util.SortedSet;
import java.util.TreeSet;

public class GitTagsetHeader extends GitHeaderBase {
	
	private SortedSet<String> deletedTags;
	
	public GitTagsetHeader() {
		super();
		this.deletedTags = new TreeSet<>();
	}

	public GitTagsetHeader(String name, String description, SortedSet<String> deletedTags) {
		super(name, description);
		this.deletedTags = deletedTags;
	}
	
	public SortedSet<String> getDeletedTags() {
		return deletedTags;
	}
	
	public void setDeletedTags(SortedSet<String> deletedTags) {
		this.deletedTags = deletedTags;
	}
	
}
