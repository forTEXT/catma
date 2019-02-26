package de.catma.repository.git.serialization.models;

import java.util.SortedSet;
import java.util.TreeSet;

public class GitTagsetHeader extends GitHeaderBase {
	
	private TreeSet<String> deletedTags;
	
	public GitTagsetHeader() {
		super();
		this.deletedTags = new TreeSet<>();
	}

	public GitTagsetHeader(String name, String description, TreeSet<String> deletedTags) {
		super(name, description);
		this.deletedTags = deletedTags;
	}
	
	public SortedSet<String> getDeletedTags() {
		return deletedTags;
	}
	
	public void setDeletedTags(TreeSet<String> deletedTags) {
		this.deletedTags = deletedTags;
	}
	
}
