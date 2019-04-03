package de.catma.repository.git.managers;

import java.io.PrintStream;
import java.util.Map.Entry;

import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.IndexDiff.StageState;

public class StatusPrinter {

	public static void print(String resourceDescription, Status status, PrintStream stream) {
		stream.println("Git Status report for Resource: " + resourceDescription);
		stream.println("Clean :" + status.isClean());
		stream.println("Has uncommitted changes: " + status.hasUncommittedChanges());
		if (status.hasUncommittedChanges()) {
			stream.println(status.getUncommittedChanges());
		}
		stream.println("Added: " + status.getAdded());
		stream.println("Changed: " + status.getChanged());
		stream.println("Removed: " + status.getRemoved());
		stream.println("Missing: " + status.getMissing());
		stream.println("Modified: " + status.getModified());
		stream.println("Untracked: " +  status.getUntracked());
		stream.println("Untracted Folders: " + status.getUntrackedFolders());
		
		stream.println("Conflicting: " + status.getConflicting());
		for (Entry<String, StageState> entry : status.getConflictingStageState().entrySet()) {
			stream.println("Conflict: " + entry.getKey() + " " + entry.getValue());
		}
		
		stream.println("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
	}
	
}
