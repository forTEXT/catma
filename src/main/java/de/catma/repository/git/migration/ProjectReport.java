package de.catma.repository.git.migration;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Status;

import de.catma.project.CommitInfo;

public class ProjectReport {
	private final Logger logger = Logger.getLogger(ProjectReport.class.getName());
	
	private int totalUsers;
	
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
	
	private int documentsNeedMergeDevToMaster;
	private int collectionsNeedMergeDevToMaster;
	private int tagsetsNeedMergeDevToMaster;

	private int rootsNeedMergeMasterToOriginMaster;
	private int documentsNeedMergeMasterToOriginMaster;
	private int collectionsNeedMergeMasterToOriginMaster;
	private int tagsetsNeedMergeMasterToOriginMaster;
	
	private int rootsCanMergeMastertoOriginMaster;

	private int rootsConflictingMergeMastertoOriginMaster;

	private int	documentsCanMergeDevToMaster;
	private int collectionsCanMergeDevToMaster;
	private int tagsetsCanMergeDevToMaster;

	private int	documentsConflictingMergeDevToMaster;
	private int collectionsConflictingMergeDevToMaster;
	private int tagsetsConflictingMergeDevToMaster;

	private int	documentsCanMergeDevToOriginMaster;
	private int collectionsCanMergeDevToOriginMaster;
	private int tagsetsCanMergeDevToOriginMaster;

	private int	documentsConflictingMergeDevToOriginMaster;
	private int collectionsConflictingMergeDevToOriginMaster;
	private int tagsetsConflictingMergeDevToOriginMaster;

	
	public void addStatus(String projectId, String resource, Status status) {
		if (resource.startsWith("collection")) {
			totalCollections++;
			if (status.isClean()) {
				cleanCollections++;
			}
			else {
				dirtyCollections++;
			}
			
			if (status.hasUncommittedChanges()) {
				uncommittedCollections++;
				logger.info(String.format("%1$s has uncommitted changes!", resource));
			}
			
			if (!status.getConflicting().isEmpty()) {
				logger.info(String.format("%1$s has conflicts!", resource));
				conflictingCollections++;
			}
		}
		else if (resource.startsWith("tagsets")) {
			totalTagsets++;
			if (status.isClean()) {
				cleanTagsets++;
			}
			else {
				dirtyTagsets++;
			}
			
			if (status.hasUncommittedChanges()) {
				logger.info(String.format("%1$s has uncommitted changes!", resource));
				uncommittedTagsets++;
			}
			
			if (!status.getConflicting().isEmpty()) {
				logger.info(String.format("%1$s has conflicts!", resource));
				conflictingTagsets++;
			}
		}
		else if (resource.startsWith("documents")) {
			totalDocuments++;
			if (status.isClean()) {
				cleanDocuments++;
			}
			else {
				dirtyDocuments++;
			}
			
			if (status.hasUncommittedChanges()) {
				logger.info(String.format("%1$s has uncommitted changes!", resource));
				uncommittedDocuments++;
			}
			
			if (!status.getConflicting().isEmpty()) {
				logger.info(String.format("%1$s has conflicts!", resource));
				conflictingDocuments++;
			}
		}
		else if (resource.startsWith("CATMA") && !resource.contains("/")) {
			totalRoots++;
			if (status.isClean()) {
				cleanRoots++;
			}
			else {
				dirtyRoots++;
			}
			
			if (status.hasUncommittedChanges()) {
				logger.info(String.format("%1$s has uncommitted changes!", resource));
				uncommittedRoots++;
			}
			
			if (!status.getConflicting().isEmpty()) {
				logger.info(String.format("%1$s has conflicts!", resource));
				conflictingRoots++;
			}
		}
		else {
			logger.warning(String.format("Unexpected status resource type: %1$s", resource));
		}
	}


	public void addStaleProject(File projectDir) {
		staleProjects++;
	}
	
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("\n");
		builder.append("\ntotalUsers: ");
		builder.append(totalUsers);
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
		builder.append(uncommittedCollections+uncommittedDocuments+tagsetsNeedMergeMasterToOriginMaster);
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
		
		builder.append("\nresourcesNeedMergeDevToMaster: ");
		builder.append(collectionsNeedMergeDevToMaster+documentsNeedMergeDevToMaster+tagsetsNeedMergeDevToMaster);
		builder.append("\ndocumentsNeedMergeDevToMaster: ");
		builder.append(documentsNeedMergeDevToMaster);
		builder.append("\ncollectionsNeedMergeDevToMaster: ");
		builder.append(collectionsNeedMergeDevToMaster);
		builder.append("\ntagsetsNeedMergeDevToMaster: ");
		builder.append(tagsetsNeedMergeDevToMaster);
		
		builder.append("\nrootsNeedMergeMasterToOriginMaster: ");
		builder.append(rootsNeedMergeMasterToOriginMaster);
		builder.append("\nresourcesNeedMergeMasterToOriginMaster: ");
		builder.append(collectionsNeedMergeMasterToOriginMaster+documentsNeedMergeMasterToOriginMaster+tagsetsNeedMergeMasterToOriginMaster);
		builder.append("\ndocumentsNeedMergeMasterToOriginMaster: ");
		builder.append(documentsNeedMergeMasterToOriginMaster);
		builder.append("\ncollectionsNeedMergeMasterToOriginMaster: ");
		builder.append(collectionsNeedMergeMasterToOriginMaster);
		builder.append("\ntagsetsNeedMergeMasterToOriginMaster: ");
		builder.append(tagsetsNeedMergeMasterToOriginMaster);
		
		builder.append("\nrootsCanMergeMastertoOriginMaster: ");
		builder.append(rootsCanMergeMastertoOriginMaster);
		
		builder.append("\nrootsConflictingMergeMastertoOriginMaster: ");
		builder.append(rootsConflictingMergeMastertoOriginMaster);

		builder.append("\nresourcesCanMergeDevToMaster: ");
		builder.append(collectionsCanMergeDevToMaster+documentsCanMergeDevToMaster+tagsetsCanMergeDevToMaster);
		builder.append("\ndocumentsCanMergeDevToMaster: ");
		builder.append(documentsCanMergeDevToMaster);
		builder.append("\ncollectionsCanMergeDevToMaster: ");
		builder.append(collectionsCanMergeDevToMaster);
		builder.append("\ntagsetsCanMergeDevToMaster: ");
		builder.append(tagsetsCanMergeDevToMaster);


		builder.append("\nresourcesConflictingMergeDevToMaster: ");
		builder.append(collectionsConflictingMergeDevToMaster+documentsConflictingMergeDevToMaster+tagsetsConflictingMergeDevToMaster);
		builder.append("\ndocumentsConflictingMergeDevToMaster: ");
		builder.append(documentsConflictingMergeDevToMaster);
		builder.append("\ncollectionsConflictingMergeDevToMaster: ");
		builder.append(collectionsConflictingMergeDevToMaster);
		builder.append("\ntagsetsConflictingMergeDevToMaster: ");
		builder.append(tagsetsConflictingMergeDevToMaster);

		builder.append("\nresourcesCanMergeDevToOriginMaster: ");
		builder.append(collectionsCanMergeDevToOriginMaster+documentsCanMergeDevToOriginMaster+tagsetsCanMergeDevToOriginMaster);
		builder.append("\ndocumentsCanMergeDevToOriginMaster: ");
		builder.append(documentsCanMergeDevToOriginMaster);
		builder.append("\ncollectionsCanMergeDevToOriginMaster: ");
		builder.append(collectionsCanMergeDevToOriginMaster);
		builder.append("\ntagsetsCanMergeDevToOriginMaster: ");
		builder.append(tagsetsCanMergeDevToOriginMaster);
		
		builder.append("\nresourcesConflictingMergeDevToOriginMaster: ");
		builder.append(collectionsConflictingMergeDevToOriginMaster+documentsConflictingMergeDevToOriginMaster+tagsetsConflictingMergeDevToOriginMaster);
		builder.append("\ndocumentsConflictingMergeDevToOriginMaster: ");
		builder.append(documentsConflictingMergeDevToOriginMaster);
		builder.append("\ncollectionsConflictingMergeDevToOriginMaster: ");
		builder.append(collectionsConflictingMergeDevToOriginMaster);
		builder.append("\ntagsetsConflictingMergeDevToOriginMaster: ");
		builder.append(tagsetsConflictingMergeDevToOriginMaster);
		
		builder.append("\nstaleProjects: ");
		builder.append(staleProjects);

		builder.append("\nstaleUsers: ");
		builder.append(staleUsers);

		builder.append("\nerrorProjcts: ");
		builder.append(errorProjects);
		
		return builder.toString();
	}


	public void addNeedMergeDevToMaster(String projectId, String resource, List<CommitInfo> commits) {
		if (!commits.isEmpty()) {
			logger.info(String.format("%1$s has changes to be merged dev->master!", resource));

			if (resource.startsWith("collections")) {
				this.collectionsNeedMergeDevToMaster++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsNeedMergeDevToMaster++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsNeedMergeDevToMaster++;
			}
			else {
				logger.warning(String.format("Unexpected unmerged resource type: %1$s", resource));
			}
		}
	}


	public void addNeedMergeMasterToOriginMaster(String projectId, String resource, List<CommitInfo> commits) {
		if (!commits.isEmpty()) {
			logger.info(String.format("%1$s has changes to be merged master->origin/master!", resource));

			if (resource.startsWith("collections")) {
				this.collectionsNeedMergeMasterToOriginMaster++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsNeedMergeMasterToOriginMaster++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsNeedMergeMasterToOriginMaster++;
			}
			else if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsNeedMergeMasterToOriginMaster++;
			}		
			else {
				logger.warning(String.format("Unexpected resource type needs merge master->origin/master: %1$s", resource));
			}	
		}
	}


	public void addError(String projectId) {
		errorProjects++;
	}


	public void addStaleUser(String projectId, String username) {
		logger.info(String.format("Found stale user %1$s", username));
		this.staleUsers++;
	}


	public void addCanMergeMasterToOriginMaster(String projectId, String resource, boolean canMerge) {
		
		if (canMerge) {
			logger.info(String.format("%1$s can be merged master->origin/master!", resource));

			if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsCanMergeMastertoOriginMaster++;
			}		
			else {
				logger.warning(String.format("Unexpected resource type can merge master->origin/master: %1$s", resource));
			}				
		}
		else {
			logger.info(String.format("%1$s would conflict when being merged master->origin/master!", resource));

			if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsConflictingMergeMastertoOriginMaster++;
			}		
			else {
				logger.warning(String.format("Unexpected resource type conflicting merge master->origin/master: %1$s", resource));
			}				
		}		
	}
	
	public void addCanMergeDevToMaster(String projectId, String resource, boolean canMerge) {
		
		if (canMerge) {
			logger.info(String.format("%1$s can be merged dev->master!", resource));

			if (resource.startsWith("collections")) {
				this.collectionsCanMergeDevToMaster++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsCanMergeDevToMaster++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsCanMergeDevToMaster++;
			}
			else {
				logger.warning(String.format("Unexpected resource type can merge dev->master: %1$s", resource));
			}				
		}
		else {
			logger.info(String.format("%1$s would conflict when being merged dev->master!", resource));

			if (resource.startsWith("collections")) {
				this.collectionsConflictingMergeDevToMaster++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsConflictingMergeDevToMaster++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsConflictingMergeDevToMaster++;
			}	
			else {
				logger.warning(String.format("Unexpected resource type conflicting merge dev->master: %1$s", resource));
			}				
		}		
	}
	
	public void addCanMergeDevToOriginMaster(String projectId, String resource, boolean canMerge) {
		
		if (canMerge) {
			logger.info(String.format("%1$s can be merged dev->origin/master!", resource));

			if (resource.startsWith("collections")) {
				this.collectionsCanMergeDevToOriginMaster++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsCanMergeDevToOriginMaster++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsCanMergeDevToOriginMaster++;
			}
			else {
				logger.warning(String.format("Unexpected resource type can merge dev->origin/master: %1$s", resource));
			}				
		}
		else {
			logger.info(String.format("%1$s would conflict when being merged dev->origin/master!", resource));

			if (resource.startsWith("collections")) {
				this.collectionsConflictingMergeDevToOriginMaster++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsConflictingMergeDevToOriginMaster++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsConflictingMergeDevToOriginMaster++;
			}	
			else {
				logger.warning(String.format("Unexpected resource type conflicting merge dev->origin/master: %1$s", resource));
			}				
		}		
	}


	public void addUser(String projectId, String username) {
		totalUsers++;
	}
}
