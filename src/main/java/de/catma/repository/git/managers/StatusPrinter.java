package de.catma.repository.git.managers;

import java.util.Map.Entry;

import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.IndexDiff.StageState;

public class StatusPrinter {

	public static void print(String resourceDescription, Status status, StringBuilder builder) {
		builder.append("Git Status report for Resource: " + resourceDescription);
		builder.append("Clean :" + status.isClean());
		builder.append("Has uncommitted changes: " + status.hasUncommittedChanges());
		if (status.hasUncommittedChanges()) {
			builder.append(status.getUncommittedChanges());
		}
		builder.append("Added: " + status.getAdded());
		builder.append("Changed: " + status.getChanged());
		builder.append("Removed: " + status.getRemoved());
		builder.append("Missing: " + status.getMissing());
		builder.append("Modified: " + status.getModified());
		builder.append("Untracked: " +  status.getUntracked());
		builder.append("Untracted Folders: " + status.getUntrackedFolders());
		
		builder.append("Conflicting: " + status.getConflicting());
		for (Entry<String, StageState> entry : status.getConflictingStageState().entrySet()) {
			builder.append("Conflict: " + entry.getKey() + " " + entry.getValue());
		}
		
		builder.append("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
	}
	
}
