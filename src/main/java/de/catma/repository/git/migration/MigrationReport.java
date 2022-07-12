package de.catma.repository.git.migration;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Status;

import de.catma.project.CommitInfo;

public class MigrationReport {
	private final Logger logger = Logger.getLogger(MigrationReport.class.getName());
	
	private int totalRoots;	
	private int totalDocuments;
	private int totalCollections;
	private int totalTagsets;
	
	private int cleanRoots;
	private int cleanDocuments;
	private int cleanCollections;
	private int cleanTagsets;

	private int dirtyRoots;
	private int dirtyDocuments;
	private int dirtyCollections;
	private int dirtyTagsets;

	private int uncommittedRoots;
	private int uncommittedDocuments;
	private int uncommittedCollections;
	private int uncommittedTagsets;

	private int conflictingRoots;
	private int conflictingDocuments;
	private int conflictingCollections;
	private int conflictingTagsets;
	
	private int staleProjects;
	private int staleUsers;
	private int errorProjects;
	
	private int unmergedRoots;
	private int unmergedDocuments;
	private int unmergedCollections;
	private int unmergedTagsets;

	private int unpushedRoots;
	private int unpushedDocuments;
	private int unpushedCollections;
	private int unpushedTagsets;
	
	public void addStatus(String resource, Status status) {
		if (resource.startsWith("collection")) {
			totalCollections+=1;
			if (status.isClean()) {
				cleanCollections+=1;
			}
			else {
				dirtyCollections+=1;
			}
			
			if (status.hasUncommittedChanges()) {
				uncommittedCollections+=1;
				logger.info(String.format("%1$s has uncommitted changes!", resource));
			}
			
			if (!status.getConflicting().isEmpty()) {
				logger.info(String.format("%1$s has conflicts!", resource));
				conflictingCollections+=1;
			}
		}
		else if (resource.startsWith("tagsets")) {
			totalTagsets+=1;
			if (status.isClean()) {
				cleanTagsets+=1;
			}
			else {
				dirtyTagsets+=1;
			}
			
			if (status.hasUncommittedChanges()) {
				logger.info(String.format("%1$s has uncommitted changes!", resource));
				uncommittedTagsets+=1;
			}
			
			if (!status.getConflicting().isEmpty()) {
				logger.info(String.format("%1$s has conflicts!", resource));
				conflictingTagsets+=1;
			}
		}
		else if (resource.startsWith("documents")) {
			totalDocuments+=1;
			if (status.isClean()) {
				cleanDocuments+=1;
			}
			else {
				dirtyDocuments+=1;
			}
			
			if (status.hasUncommittedChanges()) {
				logger.info(String.format("%1$s has uncommitted changes!", resource));
				uncommittedDocuments+=1;
			}
			
			if (!status.getConflicting().isEmpty()) {
				logger.info(String.format("%1$s has conflicts!", resource));
				conflictingDocuments+=1;
			}
		}
		else if (resource.startsWith("CATMA") && !resource.contains("/")) {
			totalRoots+=1;
			if (status.isClean()) {
				cleanRoots+=1;
			}
			else {
				dirtyRoots+=1;
			}
			
			if (status.hasUncommittedChanges()) {
				logger.info(String.format("%1$s has uncommitted changes!", resource));
				uncommittedRoots+=1;
			}
			
			if (!status.getConflicting().isEmpty()) {
				logger.info(String.format("%1$s has conflicts!", resource));
				conflictingRoots+=1;
			}
		}
		else {
			logger.warning(String.format("Unexpected status resource type: %1$s", resource));
		}
	}


	public void addStaleProject(File projectDir) {
		staleProjects+=1;
	}
	
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("\n");
		builder.append("\ntotalRoots: ");
		builder.append(totalRoots);	
		builder.append("\ntotalResources: ");
		builder.append(totalDocuments+totalCollections+totalTagsets);
		builder.append("\ntotalDocuments: ");
		builder.append(totalDocuments);
		builder.append("\ntotalCollections: ");
		builder.append(totalCollections);
		builder.append("\ntotalTagsets: ");
		builder.append(totalTagsets);

		builder.append("\ncleanRoots: ");
		builder.append(cleanRoots);
		builder.append("\ncleanResources: ");
		builder.append(cleanDocuments+cleanCollections+cleanTagsets);
		builder.append("\ncleanDocuments: ");
		builder.append(cleanDocuments);
		builder.append("\ncleanCollections: ");
		builder.append(cleanCollections);
		builder.append("\ncleanTagsets: ");
		builder.append(cleanTagsets);

		builder.append("\ndirtyRoots: ");
		builder.append(dirtyRoots);
		builder.append("\ndirtyResources: ");
		builder.append(dirtyDocuments+dirtyCollections+dirtyTagsets);
		builder.append("\ndirtyDocuments: ");
		builder.append(dirtyDocuments);
		builder.append("\ndirtyCollections: ");
		builder.append(dirtyCollections);
		builder.append("\ndirtyTagsets: ");
		builder.append(dirtyTagsets);

		builder.append("\nuncommittedRoots: ");
		builder.append(uncommittedRoots);
		builder.append("\nuncommittedResources: ");
		builder.append(uncommittedCollections+uncommittedDocuments+uncommittedTagsets);
		builder.append("\nuncommittedDocuments: ");
		builder.append(uncommittedDocuments);
		builder.append("\nuncommittedCollections: ");
		builder.append(uncommittedCollections);
		builder.append("\nuncommittedTagsets: ");
		builder.append(uncommittedTagsets);

		builder.append("\nconflictingRoots: ");
		builder.append(conflictingRoots);
		builder.append("\nconflictingResources: ");
		builder.append(conflictingCollections+conflictingDocuments+conflictingTagsets);
		builder.append("\nconflictingDocuments: ");
		builder.append(conflictingDocuments);
		builder.append("\nconflictingCollections: ");
		builder.append(conflictingCollections);
		builder.append("\nconflictingTagsets: ");
		builder.append(conflictingTagsets);
		
		builder.append("\nunmergedRoots: ");
		builder.append(unmergedRoots);
		builder.append("\nunmergedResources: ");
		builder.append(unmergedCollections+unmergedDocuments+unmergedTagsets);
		builder.append("\nunmergedDocuments: ");
		builder.append(unmergedDocuments);
		builder.append("\nunmergedCollections: ");
		builder.append(unmergedCollections);
		builder.append("\nunmergedTagsets: ");
		builder.append(unmergedTagsets);
		
		builder.append("\nunpushedRoots: ");
		builder.append(unpushedRoots);
		builder.append("\nunpushedResources: ");
		builder.append(unpushedCollections+unpushedDocuments+unpushedTagsets);
		builder.append("\nunpushedDocuments: ");
		builder.append(unpushedDocuments);
		builder.append("\nunpushedCollections: ");
		builder.append(unpushedCollections);
		builder.append("\nunpushedTagsets: ");
		builder.append(unpushedTagsets);
		
		builder.append("\nstaleProjects: ");
		builder.append(staleProjects);

		builder.append("\nstaleUsers: ");
		builder.append(staleUsers);

		builder.append("\nerrorProjcts: ");
		builder.append(errorProjects);
		
		return builder.toString();
	}


	public void addUnmergedChanges(String resource, List<CommitInfo> unmergedChanges) {
		if (!unmergedChanges.isEmpty()) {
			logger.info(String.format("%1$s has unmerged changes!", resource));

			if (resource.startsWith("collections")) {
				this.unmergedCollections+=1;
			}
			else if (resource.startsWith("tagsets")) {
				this.unmergedTagsets+=1;
			}
			else if (resource.startsWith("documents")) {
				this.unmergedDocuments+=1;
			}
			else if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.unmergedRoots+=1;
			}		
			else {
				logger.warning(String.format("Unexpected unmerged resource type: %1$s", resource));
			}
		}
	}


	public void addUnpushedChanges(String resource, List<CommitInfo> unpushedChanges) {
		if (!unpushedChanges.isEmpty()) {
			logger.info(String.format("%1$s has unpushed changes!", resource));

			if (resource.startsWith("collections")) {
				this.unpushedCollections+=1;
			}
			else if (resource.startsWith("tagsets")) {
				this.uncommittedTagsets+=1;
			}
			else if (resource.startsWith("documents")) {
				this.unpushedDocuments+=1;
			}
			else if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.unpushedRoots+=1;
			}		
			else {
				logger.warning(String.format("Unexpected unpushed resource type: %1$s", resource));
			}	
		}
	}


	public void addError(String projectId) {
		errorProjects+=1;
	}


	public void addStaleUser(String username) {
		logger.info(String.format("Found stale user %1$s", username));
		this.staleUsers+=1;
	}
}
