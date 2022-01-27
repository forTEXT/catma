package de.catma.repository.git.serialization.models;

import java.util.SortedSet;
import java.util.TreeSet;

public class GitTagsetHeader extends GitHeaderBase {
	
	private TreeSet<String> deletedDefinitions;
	
	public GitTagsetHeader(
			String name, String description, String responsableUser, 
			String forkedFromCommitURL, TreeSet<String> deletedDefinitions) {
		super(name, description, responsableUser, forkedFromCommitURL);
		this.deletedDefinitions = deletedDefinitions;
	}
	
	public SortedSet<String> getDeletedDefinitions() {
		return deletedDefinitions;
	}
	
	public void setDeletedDefinitions(TreeSet<String> deletedDefinitions) {
		this.deletedDefinitions = deletedDefinitions;
	}
	
}
