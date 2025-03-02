package de.catma.repository.git.migration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;

import de.catma.project.CommitInfo;
import de.catma.properties.CATMAPropertyKey;

public class ProjectReport {
	private final Logger logger = Logger.getLogger(ProjectReport.class.getName());

	private static final String migrationBranchName = CATMAPropertyKey.V6_REPO_MIGRATION_BRANCH.getValue();

	private String projectId;
	private String ownerEmail;

	private int issuesCount;
	

	private int totalUsers;
	private int googleUsers;
	
	private int totalRoots;	
	private int totalDocuments;
	private int totalCollections;
	private int totalTagsets;
	
	private int readyToMigrateRoots;
	
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
	private int checkProjects;
	
	private int documentsNeedMergeDevToMaster;
	private int collectionsNeedMergeDevToMaster;
	private int tagsetsNeedMergeDevToMaster;

	private int rootsNeedMergeMasterToOriginMaster;
	private int documentsNeedMergeMasterToOriginMaster;
	private int collectionsNeedMergeMasterToOriginMaster;
	private int tagsetsNeedMergeMasterToOriginMaster;
	
	private int rootsCanMergeMasterToOriginMaster;

	private int rootsConflictingMergeMasterToOriginMaster;

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
	
	private int documentsMergeSuccessfulOriginC6MigrationToC6Migration;
	private int collectionsMergeSuccessfulOriginC6MigrationToC6Migration;
	private int tagsetsMergeSuccessfulOriginC6MigrationToC6Migration;
	private int rootsMergeSuccessfulOriginC6MigrationToC6Migration;

	private int documentsMergeSuccessfulOriginMasterToC6Migration;
	private int collectionsMergeSuccessfulOriginMasterToC6Migration;
	private int tagsetsMergeSuccessfulOriginMasterToC6Migration;
	private int rootsMergeSuccessfulOriginMasterToC6Migration;
	
	private int documentsMergeSuccessfulDevToC6Migration;
	private int collectionsMergeSuccessfulDevToC6Migration;
	private int tagsetsMergeSuccessfulDevToC6Migration;
	
	private int rootsMergeSuccessfulMasterToC6Migration;

	private int documentsPushSuccessfulC6MigrationToOriginC6Migration;
	private int collectionsPushSuccessfulC6MigrationToOriginC6Migration;
	private int tagsetsPushSuccessfulC6MigrationToOriginC6Migration;
	private int rootsPushSuccessfulC6MigrationToOriginC6Migration;

	private int documentsMergeConflictingOriginC6MigrationToC6Migration;
	private int collectionsMergeConflictingOriginC6MigrationToC6Migration;
	private int tagsetsMergeConflictingOriginC6MigrationToC6Migration;
	private int rootsMergeConflictingOriginC6MigrationToC6Migration;

	private int documentsMergeConflictingOriginMasterToC6Migration;
	private int collectionsMergeConflictingOriginMasterToC6Migration;
	private int tagsetsMergeConflictingOriginMasterToC6Migration;
	private int rootsMergeConflictingOriginMasterToC6Migration;
	
	private int documentsMergeConflictingDevToC6Migration;
	private int collectionsMergeConflictingDevToC6Migration;
	private int tagsetsMergeConflictingDevToC6Migration;
	
	private int rootsMergeConflictingMasterToC6Migration;

	private int documentsPushFailedC6MigrationToOriginC6Migration;
	private int collectionsPushFailedC6MigrationToOriginC6Migration;
	private int tagsetsPushFailedC6MigrationToOriginC6Migration;
	private int rootsPushFailedC6MigrationToOriginC6Migration;
	
	private LocalDateTime lastUnsynchronizedCommitTime;
	private LocalDateTime lastHeadCommitTime;
	private String lastError;
	private boolean requiresManualCheck;

	public ProjectReport(String projectId) {
		this.projectId = projectId;
	}
	
	public ProjectReport(Collection<ProjectReport> reports) {
		for (ProjectReport report : reports) {
			issuesCount+=report.issuesCount;

			totalUsers+=report.totalUsers;
			googleUsers+=report.googleUsers;
			
			totalRoots+=report.totalRoots;	
			readyToMigrateRoots+=report.readyToMigrateRoots;
			totalDocuments+=report.totalDocuments;
			totalCollections+=report.totalCollections;
			totalTagsets+=report.totalTagsets;
			
			cleanRoots+=report.cleanRoots;
			cleanDocuments+=report.cleanDocuments;
			cleanCollections+=report.cleanCollections;
			cleanTagsets+=report.cleanTagsets;

			dirtyRoots+=report.dirtyRoots;
			dirtyDocuments+=report.dirtyDocuments;
			dirtyCollections+=report.dirtyCollections;
			dirtyTagsets+=report.dirtyTagsets;

			uncommittedRoots+=report.uncommittedRoots;
			uncommittedDocuments+=report.uncommittedDocuments;
			uncommittedCollections+=report.uncommittedCollections;
			uncommittedTagsets+=report.uncommittedTagsets;

			conflictingRoots+=report.conflictingRoots;
			conflictingDocuments+=report.conflictingDocuments;
			conflictingCollections+=report.conflictingCollections;
			conflictingTagsets+=report.conflictingTagsets;
			
			staleProjects+=report.staleProjects;
			staleUsers+=report.staleUsers;
			errorProjects+=report.errorProjects;
			checkProjects+=report.checkProjects;
			
			documentsNeedMergeDevToMaster+=report.documentsNeedMergeDevToMaster;
			collectionsNeedMergeDevToMaster+=report.collectionsNeedMergeDevToMaster;
			tagsetsNeedMergeDevToMaster+=report.tagsetsNeedMergeDevToMaster;

			rootsNeedMergeMasterToOriginMaster+=report.rootsNeedMergeMasterToOriginMaster;
			documentsNeedMergeMasterToOriginMaster+=report.documentsNeedMergeMasterToOriginMaster;
			collectionsNeedMergeMasterToOriginMaster+=report.collectionsNeedMergeMasterToOriginMaster;
			tagsetsNeedMergeMasterToOriginMaster+=report.tagsetsNeedMergeMasterToOriginMaster;
			
			rootsCanMergeMasterToOriginMaster+=report.rootsCanMergeMasterToOriginMaster;

			rootsConflictingMergeMasterToOriginMaster+=report.rootsConflictingMergeMasterToOriginMaster;

			documentsCanMergeDevToMaster+=report.documentsCanMergeDevToMaster;
			collectionsCanMergeDevToMaster+=report.collectionsCanMergeDevToMaster;
			tagsetsCanMergeDevToMaster+=report.tagsetsCanMergeDevToMaster;

			documentsConflictingMergeDevToMaster+=report.documentsConflictingMergeDevToMaster;
			collectionsConflictingMergeDevToMaster+=report.collectionsConflictingMergeDevToMaster;
			tagsetsConflictingMergeDevToMaster+=report.tagsetsConflictingMergeDevToMaster;

			documentsCanMergeDevToOriginMaster+=report.documentsCanMergeDevToOriginMaster;
			collectionsCanMergeDevToOriginMaster+=report.collectionsCanMergeDevToOriginMaster;
			tagsetsCanMergeDevToOriginMaster+=report.tagsetsCanMergeDevToOriginMaster;

			documentsConflictingMergeDevToOriginMaster+=report.documentsConflictingMergeDevToOriginMaster;
			collectionsConflictingMergeDevToOriginMaster+=report.collectionsConflictingMergeDevToOriginMaster;
			tagsetsConflictingMergeDevToOriginMaster+=report.tagsetsConflictingMergeDevToOriginMaster;		
			
			documentsMergeSuccessfulOriginC6MigrationToC6Migration+=report.documentsMergeSuccessfulOriginC6MigrationToC6Migration;
			collectionsMergeSuccessfulOriginC6MigrationToC6Migration+=report.collectionsMergeSuccessfulOriginC6MigrationToC6Migration;
			tagsetsMergeSuccessfulOriginC6MigrationToC6Migration+=report.tagsetsMergeSuccessfulOriginC6MigrationToC6Migration;
			rootsMergeSuccessfulOriginC6MigrationToC6Migration+=report.rootsMergeSuccessfulOriginC6MigrationToC6Migration;

			documentsMergeSuccessfulOriginMasterToC6Migration+=report.documentsMergeSuccessfulOriginMasterToC6Migration;
			collectionsMergeSuccessfulOriginMasterToC6Migration+=report.collectionsMergeSuccessfulOriginMasterToC6Migration;
			tagsetsMergeSuccessfulOriginMasterToC6Migration+=report.tagsetsMergeSuccessfulOriginMasterToC6Migration;
			rootsMergeSuccessfulOriginMasterToC6Migration+=report.rootsMergeSuccessfulOriginMasterToC6Migration;
			
			documentsMergeSuccessfulDevToC6Migration+=report.documentsMergeSuccessfulDevToC6Migration;
			collectionsMergeSuccessfulDevToC6Migration+=report.collectionsMergeSuccessfulDevToC6Migration;
			tagsetsMergeSuccessfulDevToC6Migration+=report.tagsetsMergeSuccessfulDevToC6Migration;
			
			rootsMergeSuccessfulMasterToC6Migration+=report.rootsMergeSuccessfulMasterToC6Migration;

			documentsPushSuccessfulC6MigrationToOriginC6Migration+=documentsPushSuccessfulC6MigrationToOriginC6Migration;
			collectionsPushSuccessfulC6MigrationToOriginC6Migration+=collectionsPushSuccessfulC6MigrationToOriginC6Migration;
			tagsetsPushSuccessfulC6MigrationToOriginC6Migration+=tagsetsPushSuccessfulC6MigrationToOriginC6Migration;
			rootsPushSuccessfulC6MigrationToOriginC6Migration+=rootsPushSuccessfulC6MigrationToOriginC6Migration;

			documentsMergeConflictingOriginC6MigrationToC6Migration+=documentsMergeConflictingOriginC6MigrationToC6Migration;
			collectionsMergeConflictingOriginC6MigrationToC6Migration+=collectionsMergeConflictingOriginC6MigrationToC6Migration;
			tagsetsMergeConflictingOriginC6MigrationToC6Migration+=tagsetsMergeConflictingOriginC6MigrationToC6Migration;
			rootsMergeConflictingOriginC6MigrationToC6Migration+=report.rootsMergeConflictingOriginC6MigrationToC6Migration;

			documentsMergeConflictingOriginMasterToC6Migration+=report.documentsMergeConflictingOriginMasterToC6Migration;
			collectionsMergeConflictingOriginMasterToC6Migration+=report.collectionsMergeConflictingOriginMasterToC6Migration;
			tagsetsMergeConflictingOriginMasterToC6Migration+=report.tagsetsMergeConflictingOriginMasterToC6Migration;
			rootsMergeConflictingOriginMasterToC6Migration+=report.rootsMergeConflictingOriginMasterToC6Migration;
			
			documentsMergeConflictingDevToC6Migration+=report.documentsMergeConflictingDevToC6Migration;
			collectionsMergeConflictingDevToC6Migration+=report.collectionsMergeConflictingDevToC6Migration;
			tagsetsMergeConflictingDevToC6Migration+=report.tagsetsMergeConflictingDevToC6Migration;
			
			rootsMergeConflictingMasterToC6Migration+=report.rootsMergeConflictingMasterToC6Migration;

			documentsPushFailedC6MigrationToOriginC6Migration+=report.documentsPushFailedC6MigrationToOriginC6Migration;
			collectionsPushFailedC6MigrationToOriginC6Migration+=report.collectionsPushFailedC6MigrationToOriginC6Migration;
			tagsetsPushFailedC6MigrationToOriginC6Migration+=report.tagsetsPushFailedC6MigrationToOriginC6Migration;
			rootsPushFailedC6MigrationToOriginC6Migration+=report.rootsPushFailedC6MigrationToOriginC6Migration;

		}
	}

	public void addStatus(String resource, Status status) {
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

	public void addReadyToMigrateRoot(String projectId) {
		this.readyToMigrateRoots++;
	}

	public void addStaleProject(File projectDir) {
		staleProjects++;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (projectId != null) {
			builder.append("\n\nProject Scan Report: ");
			builder.append(projectId);
			builder.append("\n\n");
			if (ownerEmail != null) {
				builder.append("Owner: ");
				builder.append(ownerEmail);
				builder.append("\n");
			}
			if (lastUnsynchronizedCommitTime != null) {
				builder.append("Last Unsynchronized Commit Time: ");
				builder.append(lastUnsynchronizedCommitTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
				builder.append("\n");
			}
			if (lastHeadCommitTime != null) {
				builder.append("Last HEAD Commit Time: ");
				builder.append(lastHeadCommitTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
				builder.append("\n");
			}
			if (lastError != null) {
				builder.append("Last Error: ");
				builder.append(lastError);
				builder.append("\n");
			}

			builder.append("Requires Manual Check: ");
			builder.append(requiresManualCheck);
			builder.append("\n");

			builder.append("\n");
		}

		builder.append("Issues Count: ");
		builder.append(issuesCount);
		builder.append("\n\n");

		builder.append("Total Users: ");
		builder.append(totalUsers);
		builder.append("\n");
		builder.append("Google Users: ");
		builder.append(googleUsers);
		builder.append("\n\n");

		builder.append("Total Roots: ");
		builder.append(totalRoots);
		builder.append("\n");
		builder.append("Ready to Migrate Roots: ");
		builder.append(readyToMigrateRoots);
		builder.append("\n\n");

		builder.append("Total Resources: ");
		builder.append(totalDocuments+totalCollections+totalTagsets);
		builder.append("\n");
		builder.append("Total Documents: ");
		builder.append(totalDocuments);
		builder.append("\n");
		builder.append("Total Collections: ");
		builder.append(totalCollections);
		builder.append("\n");
		builder.append("Total Tagsets: ");
		builder.append(totalTagsets);
		builder.append("\n\n");

		builder.append("Clean Roots: ");
		builder.append(cleanRoots);
		builder.append("\n");
		builder.append("Clean Resources: ");
		builder.append(cleanDocuments+cleanCollections+cleanTagsets);
		builder.append("\n");
		builder.append("Clean Documents: ");
		builder.append(cleanDocuments);
		builder.append("\n");
		builder.append("Clean Collections: ");
		builder.append(cleanCollections);
		builder.append("\n");
		builder.append("Clean Tagsets: ");
		builder.append(cleanTagsets);
		builder.append("\n\n");

		builder.append("Dirty Roots: ");
		builder.append(dirtyRoots);
		builder.append("\n");
		builder.append("Dirty Resources: ");
		builder.append(dirtyDocuments+dirtyCollections+dirtyTagsets);
		builder.append("\n");
		builder.append("Dirty Documents: ");
		builder.append(dirtyDocuments);
		builder.append("\n");
		builder.append("Dirty Collections: ");
		builder.append(dirtyCollections);
		builder.append("\n");
		builder.append("Dirty Tagsets: ");
		builder.append(dirtyTagsets);
		builder.append("\n\n");

		builder.append("Uncommitted Roots: ");
		builder.append(uncommittedRoots);
		builder.append("\n");
		builder.append("Uncommitted Resources: ");
		builder.append(uncommittedCollections+uncommittedDocuments+tagsetsNeedMergeMasterToOriginMaster);
		builder.append("\n");
		builder.append("Uncommitted Documents: ");
		builder.append(uncommittedDocuments);
		builder.append("\n");
		builder.append("Uncommitted Collections: ");
		builder.append(uncommittedCollections);
		builder.append("\n");
		builder.append("Uncommitted Tagsets: ");
		builder.append(uncommittedTagsets);
		builder.append("\n\n");

		builder.append("Conflicting Roots: ");
		builder.append(conflictingRoots);
		builder.append("\n");
		builder.append("Conflicting Resources: ");
		builder.append(conflictingCollections+conflictingDocuments+conflictingTagsets);
		builder.append("\n");
		builder.append("Conflicting Documents: ");
		builder.append(conflictingDocuments);
		builder.append("\n");
		builder.append("Conflicting Collections: ");
		builder.append(conflictingCollections);
		builder.append("\n");
		builder.append("Conflicting Tagsets: ");
		builder.append(conflictingTagsets);
		builder.append("\n\n");

		builder.append("Stale Projects: ");
		builder.append(staleProjects);
		builder.append("\n");
		builder.append("Stale Users: ");
		builder.append(staleUsers);
		builder.append("\n\n");

		builder.append("Projects with Errors: ");
		builder.append(errorProjects);
		builder.append("\n");
		builder.append("Projects Requiring Manual Check: ");
		builder.append(checkProjects);
		builder.append("\n\n");

		builder.append("Resources Needing Merge, dev->master: ");
		builder.append(collectionsNeedMergeDevToMaster+documentsNeedMergeDevToMaster+tagsetsNeedMergeDevToMaster);
		builder.append("\n");
		builder.append("Documents Needing Merge, dev->master: ");
		builder.append(documentsNeedMergeDevToMaster);
		builder.append("\n");
		builder.append("Collections Needing Merge, dev->master: ");
		builder.append(collectionsNeedMergeDevToMaster);
		builder.append("\n");
		builder.append("Tagsets Needing Merge, dev->master: ");
		builder.append(tagsetsNeedMergeDevToMaster);
		builder.append("\n\n");

		builder.append("Roots Needing Merge, master->origin/master: ");
		builder.append(rootsNeedMergeMasterToOriginMaster);
		builder.append("\n");
		builder.append("Resources Needing Merge, master->origin/master: ");
		builder.append(collectionsNeedMergeMasterToOriginMaster+documentsNeedMergeMasterToOriginMaster+tagsetsNeedMergeMasterToOriginMaster);
		builder.append("\n");
		builder.append("Documents Needing Merge, master->origin/master: ");
		builder.append(documentsNeedMergeMasterToOriginMaster);
		builder.append("\n");
		builder.append("Collections Needing Merge, master->origin/master: ");
		builder.append(collectionsNeedMergeMasterToOriginMaster);
		builder.append("\n");
		builder.append("Tagsets Needing Merge, master->origin/master: ");
		builder.append(tagsetsNeedMergeMasterToOriginMaster);
		builder.append("\n\n");

		builder.append("Roots Able to be Merged, master->origin/master: ");
		builder.append(rootsCanMergeMasterToOriginMaster);
		builder.append("\n\n");

		builder.append("Roots with Conflicted Merge, master->origin/master: ");
		builder.append(rootsConflictingMergeMasterToOriginMaster);
		builder.append("\n\n");

		builder.append("Resources Able to be Merged, dev->master: ");
		builder.append(collectionsCanMergeDevToMaster+documentsCanMergeDevToMaster+tagsetsCanMergeDevToMaster);
		builder.append("\n");
		builder.append("Documents Able to be Merged, dev->master: ");
		builder.append(documentsCanMergeDevToMaster);
		builder.append("\n");
		builder.append("Collections Able to be Merged, dev->master: ");
		builder.append(collectionsCanMergeDevToMaster);
		builder.append("\n");
		builder.append("Tagsets Able to be Merged, dev->master: ");
		builder.append(tagsetsCanMergeDevToMaster);
		builder.append("\n\n");

		builder.append("Resources with Conflicted Merge, dev->master: ");
		builder.append(collectionsConflictingMergeDevToMaster+documentsConflictingMergeDevToMaster+tagsetsConflictingMergeDevToMaster);
		builder.append("\n");
		builder.append("Documents with Conflicted Merge, dev->master: ");
		builder.append(documentsConflictingMergeDevToMaster);
		builder.append("\n");
		builder.append("Collections with Conflicted Merge, dev->master: ");
		builder.append(collectionsConflictingMergeDevToMaster);
		builder.append("\n");
		builder.append("Tagsets with Conflicted Merge, dev->master: ");
		builder.append(tagsetsConflictingMergeDevToMaster);
		builder.append("\n\n");

		builder.append("Resources Able to be Merged, dev->origin/master: ");
		builder.append(collectionsCanMergeDevToOriginMaster+documentsCanMergeDevToOriginMaster+tagsetsCanMergeDevToOriginMaster);
		builder.append("\n");
		builder.append("Documents Able to be Merged, dev->origin/master: ");
		builder.append(documentsCanMergeDevToOriginMaster);
		builder.append("\n");
		builder.append("Collections Able to be Merged, dev->origin/master: ");
		builder.append(collectionsCanMergeDevToOriginMaster);
		builder.append("\n");
		builder.append("Tagsets Able to be Merged, dev->origin/master: ");
		builder.append(tagsetsCanMergeDevToOriginMaster);
		builder.append("\n\n");

		builder.append("Resources with Conflicted Merge, dev->origin/master: ");
		builder.append(collectionsConflictingMergeDevToOriginMaster+documentsConflictingMergeDevToOriginMaster+tagsetsConflictingMergeDevToOriginMaster);
		builder.append("\n");
		builder.append("Documents with Conflicted Merge, dev->origin/master: ");
		builder.append(documentsConflictingMergeDevToOriginMaster);
		builder.append("\n");
		builder.append("Collections with Conflicted Merge, dev->origin/master: ");
		builder.append(collectionsConflictingMergeDevToOriginMaster);
		builder.append("\n");
		builder.append("Tagsets with Conflicted Merge, dev->origin/master: ");
		builder.append(tagsetsConflictingMergeDevToOriginMaster);
		builder.append("\n\n");

		builder.append(String.format("Documents Merged Successfully, origin/%1$s->%1$s: ", migrationBranchName));
		builder.append(documentsMergeSuccessfulOriginC6MigrationToC6Migration);
		builder.append("\n");
		builder.append(String.format("Collections Merged Successfully, origin/%1$s->%1$s: ", migrationBranchName));
		builder.append(collectionsMergeSuccessfulOriginC6MigrationToC6Migration);
		builder.append("\n");
		builder.append(String.format("Tagsets Merged Successfully, origin/%1$s->%1$s: ", migrationBranchName));
		builder.append(tagsetsMergeSuccessfulOriginC6MigrationToC6Migration);
		builder.append("\n");
		builder.append(String.format("Roots Merged Successfully, origin/%1$s->%1$s: ", migrationBranchName));
		builder.append(rootsMergeSuccessfulOriginC6MigrationToC6Migration);
		builder.append("\n\n");

		builder.append(String.format("Documents Merged Successfully, origin/master->%s: ", migrationBranchName));
		builder.append(documentsMergeSuccessfulOriginMasterToC6Migration);
		builder.append("\n");
		builder.append(String.format("Collections Merged Successfully, origin/master->%s: ", migrationBranchName));
		builder.append(collectionsMergeSuccessfulOriginMasterToC6Migration);
		builder.append("\n");
		builder.append(String.format("Tagsets Merged Successfully, origin/master->%s: ", migrationBranchName));
		builder.append(tagsetsMergeSuccessfulOriginMasterToC6Migration);
		builder.append("\n");
		builder.append(String.format("Roots Merged Successfully, origin/master->%s: ", migrationBranchName));
		builder.append(rootsMergeSuccessfulOriginMasterToC6Migration);
		builder.append("\n\n");

		builder.append(String.format("Documents Merged Successfully, dev->%s: ", migrationBranchName));
		builder.append(documentsMergeSuccessfulDevToC6Migration);
		builder.append("\n");
		builder.append(String.format("Collections Merged Successfully, dev->%s: ", migrationBranchName));
		builder.append(collectionsMergeSuccessfulDevToC6Migration);
		builder.append("\n");
		builder.append(String.format("Tagsets Merged Successfully, dev->%s: ", migrationBranchName));
		builder.append(tagsetsMergeSuccessfulDevToC6Migration);
		builder.append("\n\n");

		builder.append(String.format("Roots Merged Successfully, master->%s: ", migrationBranchName));
		builder.append(rootsMergeSuccessfulMasterToC6Migration);
		builder.append("\n\n");

		builder.append(String.format("Documents Pushed Successfully, %1$s->origin/%1$s: ", migrationBranchName));
		builder.append(documentsPushSuccessfulC6MigrationToOriginC6Migration);
		builder.append("\n");
		builder.append(String.format("Collections Pushed Successfully, %1$s->origin/%1$s: ", migrationBranchName));
		builder.append(collectionsPushSuccessfulC6MigrationToOriginC6Migration);
		builder.append("\n");
		builder.append(String.format("Tagsets Pushed Successfully, %1$s->origin/%1$s: ", migrationBranchName));
		builder.append(tagsetsPushSuccessfulC6MigrationToOriginC6Migration);
		builder.append("\n");
		builder.append(String.format("Roots Pushed Successfully, %1$s->origin/%1$s: ", migrationBranchName));
		builder.append(rootsPushSuccessfulC6MigrationToOriginC6Migration);
		builder.append("\n\n");

		builder.append(String.format("Documents with Conflicted Merge, origin/%1$s->%1$s: ", migrationBranchName));
		builder.append(documentsMergeConflictingOriginC6MigrationToC6Migration);
		builder.append("\n");
		builder.append(String.format("Collections with Conflicted Merge, origin/%1$s->%1$s: ", migrationBranchName));
		builder.append(collectionsMergeConflictingOriginC6MigrationToC6Migration);
		builder.append("\n");
		builder.append(String.format("Tagsets with Conflicted Merge, origin/%1$s->%1$s: ", migrationBranchName));
		builder.append(tagsetsMergeConflictingOriginC6MigrationToC6Migration);
		builder.append("\n");
		builder.append(String.format("Roots with Conflicted Merge, origin/%1$s->%1$s: ", migrationBranchName));
		builder.append(rootsMergeConflictingOriginC6MigrationToC6Migration);
		builder.append("\n\n");

		builder.append(String.format("Documents with Conflicted Merge, origin/master->%s: ", migrationBranchName));
		builder.append(documentsMergeConflictingOriginMasterToC6Migration);
		builder.append("\n");
		builder.append(String.format("Collections with Conflicted Merge, origin/master->%s: ", migrationBranchName));
		builder.append(collectionsMergeConflictingOriginMasterToC6Migration);
		builder.append("\n");
		builder.append(String.format("Tagsets with Conflicted Merge, origin/master->%s: ", migrationBranchName));
		builder.append(tagsetsMergeConflictingOriginMasterToC6Migration);
		builder.append("\n");
		builder.append(String.format("Roots with Conflicted Merge, origin/master->%s: ", migrationBranchName));
		builder.append(rootsMergeConflictingOriginMasterToC6Migration);
		builder.append("\n\n");

		builder.append(String.format("Documents with Conflicted Merge, dev->%s: ", migrationBranchName));
		builder.append(documentsMergeConflictingDevToC6Migration);
		builder.append("\n");
		builder.append(String.format("Collections with Conflicted Merge, dev->%s: ", migrationBranchName));
		builder.append(collectionsMergeConflictingDevToC6Migration);
		builder.append("\n");
		builder.append(String.format("Tagsets with Conflicted Merge, dev->%s: ", migrationBranchName));
		builder.append(tagsetsMergeConflictingDevToC6Migration);
		builder.append("\n\n");

		builder.append(String.format("Roots with Conflicted Merge, master->%s: ", migrationBranchName));
		builder.append(rootsMergeConflictingMasterToC6Migration);
		builder.append("\n\n");

		builder.append(String.format("Documents that Failed to Push, %1$s->origin/%1$s: ", migrationBranchName));
		builder.append(documentsPushFailedC6MigrationToOriginC6Migration);
		builder.append("\n");
		builder.append(String.format("Collections that Failed to Push, %1$s->origin/%1$s: ", migrationBranchName));
		builder.append(collectionsPushFailedC6MigrationToOriginC6Migration);
		builder.append("\n");
		builder.append(String.format("Tagsets that Failed to Push, %1$s->origin/%1$s: ", migrationBranchName));
		builder.append(tagsetsPushFailedC6MigrationToOriginC6Migration);
		builder.append("\n");
		builder.append(String.format("Roots that Failed to Push, %1$s->origin/%1$s: ", migrationBranchName));
		builder.append(rootsPushFailedC6MigrationToOriginC6Migration);
		builder.append("\n");

		return builder.toString();
	}


	public void addNeedMergeDevToMaster(String resource, List<CommitInfo> commits) {
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
			
			setLastUnsynchronizedCommitTime(commits);
		}
	}
	
	
	private void setLastUnsynchronizedCommitTime(List<CommitInfo> commits) {
		for (CommitInfo commit : commits) {
			LocalDateTime commitTime = commit.getCommittedDate();
			if (this.lastUnsynchronizedCommitTime == null || this.lastUnsynchronizedCommitTime.isBefore(commitTime)) {
				this.lastUnsynchronizedCommitTime = commitTime;
			}
		}
	}

	
	public void addNeedMergeMasterToOriginMaster(String resource, List<CommitInfo> commits) {
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
			
			setLastUnsynchronizedCommitTime(commits);
		}
	}


	public void addError(Exception e) {
		errorProjects++;
		lastError = e.getClass().getName() + ": " + e.getMessage();
		setRequiresManualCheck();
	}


	public void addStaleUser(String username) {
		logger.info(String.format("Found stale user \"%s\"", username));
		this.staleUsers++;
	}


	public void addCanMergeMasterToOriginMaster(String resource, boolean canMerge) {
		if (canMerge) {
			logger.info(String.format("%1$s can be merged master->origin/master!", resource));

			if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsCanMergeMasterToOriginMaster++;
			}		
			else {
				logger.warning(String.format("Unexpected resource type can merge master->origin/master: %1$s", resource));
			}				
		}
		else {
			logger.info(String.format("%1$s would conflict when being merged master->origin/master!", resource));
			setRequiresManualCheck();

			if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsConflictingMergeMasterToOriginMaster++;
			}		
			else {
				logger.warning(String.format("Unexpected resource type conflicting merge master->origin/master: %1$s", resource));
			}				
		}		
	}
	
	public void addCanMergeDevToMaster(String resource, boolean canMerge) {
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
			setRequiresManualCheck();

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
	
	public void addCanMergeDevToOriginMaster(String resource, boolean canMerge) {
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
			setRequiresManualCheck();

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
	
	public void addMergeResultDevToC6Migration(String resource, MergeResult mergeResult) {
		if (mergeResult.getMergeStatus().isSuccessful()) {
			logger.info(String.format("Successfully merged %s dev->%s", resource, migrationBranchName));

			if (resource.startsWith("collections")) {
				this.collectionsMergeSuccessfulDevToC6Migration++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsMergeSuccessfulDevToC6Migration++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsMergeSuccessfulDevToC6Migration++;
			}
			else {
				logger.warning(String.format("Unexpected resource type successfully merged %s dev->%s", resource, migrationBranchName));
			}			
		}
		else {
			logger.info(String.format("Conflicting merge %s dev->%s", resource, migrationBranchName));
			setRequiresManualCheck();

			if (resource.startsWith("collections")) {
				this.collectionsMergeConflictingDevToC6Migration++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsMergeConflictingDevToC6Migration++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsMergeConflictingDevToC6Migration++;
			}
			else {
				logger.warning(String.format("Unexpected resource type conflicting merge %s dev->%s", resource, migrationBranchName));
			}			
		}
	}
	
	public void addMergeResulMasterToC6Migration(String resource, MergeResult mergeResult) {
		if (mergeResult.getMergeStatus().isSuccessful()) {
			logger.info(String.format("Successfully merged %s master->%s", resource, migrationBranchName));

			if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsMergeSuccessfulMasterToC6Migration++;
			}
			else {
				logger.warning(String.format("Unexpected resource type successfully merged %s master->%s", resource, migrationBranchName));
			}			
		}
		else {
			logger.info(String.format("Conflicting merge %s master->%s", resource, migrationBranchName));
			setRequiresManualCheck();

			if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsMergeConflictingMasterToC6Migration++;
			}
			else {
				logger.warning(String.format("Unexpected resource type conflicting merge %s master->%s", resource, migrationBranchName));
			}			
		}
	}

	public void addMergeResultOriginMasterToC6Migration(String resource, MergeResult mergeResult) {
		if (mergeResult.getMergeStatus().isSuccessful()) {
			logger.info(String.format("Successfully merged %s origin/master->%s", resource, migrationBranchName));

			if (resource.startsWith("collections")) {
				this.collectionsMergeSuccessfulOriginMasterToC6Migration++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsMergeSuccessfulOriginMasterToC6Migration++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsMergeSuccessfulOriginMasterToC6Migration++;
			}
			else if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsMergeSuccessfulOriginMasterToC6Migration++;
			}
			else {
				logger.warning(String.format("Unexpected resource type successfully merged %s origin/master->%s", resource, migrationBranchName));
			}			
		}
		else {
			logger.info(String.format("Conflicting merge %s origin/master->%s", resource, migrationBranchName));
			setRequiresManualCheck();

			if (resource.startsWith("collections")) {
				this.collectionsMergeConflictingOriginMasterToC6Migration++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsMergeConflictingOriginMasterToC6Migration++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsMergeConflictingOriginMasterToC6Migration++;
			}
			else if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsMergeConflictingOriginMasterToC6Migration++;
			}
			else {
				logger.warning(String.format("Unexpected resource type conflicting merge %s origin/master->%s", resource, migrationBranchName));
			}			
		}
	}

	public void addMergeResultOriginC6MigrationToC6Migration(String resource, MergeResult mergeResult) {
		if (mergeResult.getMergeStatus().isSuccessful()) {
			logger.info(String.format("Successfully merged %1$s origin/%2$s->%2$s", resource, migrationBranchName));

			if (resource.startsWith("collections")) {
				this.collectionsMergeSuccessfulOriginC6MigrationToC6Migration++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsMergeSuccessfulOriginC6MigrationToC6Migration++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsMergeSuccessfulOriginC6MigrationToC6Migration++;
			}
			else if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsMergeSuccessfulOriginC6MigrationToC6Migration++;
			}
			else {
				logger.warning(String.format("Unexpected resource type successfully merged %1$s origin/%2$s->%2$s", resource, migrationBranchName));
			}			
		}
		else {
			logger.info(String.format("Conflicting merge %1$s origin/%2$s->%2$s", resource, migrationBranchName));
			setRequiresManualCheck();

			if (resource.startsWith("collections")) {
				this.collectionsMergeConflictingOriginC6MigrationToC6Migration++;
			}
			else if (resource.startsWith("tagsets")) {
				this.tagsetsMergeConflictingOriginC6MigrationToC6Migration++;
			}
			else if (resource.startsWith("documents")) {
				this.documentsMergeConflictingOriginC6MigrationToC6Migration++;
			}
			else if (resource.startsWith("CATMA") && !resource.contains("/")) {
				this.rootsMergeConflictingOriginC6MigrationToC6Migration++;
			}
			else {
				logger.warning(String.format("Unexpected resource type conflicting merge %1$s origin/%2$s->%2$s", resource, migrationBranchName));
			}			
		}
	}

	public void addPushC6MigrationToOriginC6MigrationSuccessful(String resource) {
		logger.info(String.format("Successfully pushed %1$s %2$s->origin/%2$s", resource, migrationBranchName));
		if (resource.startsWith("collections")) {
			this.collectionsPushSuccessfulC6MigrationToOriginC6Migration++;
		}
		else if (resource.startsWith("tagsets")) {
			this.tagsetsPushSuccessfulC6MigrationToOriginC6Migration++;
		}
		else if (resource.startsWith("documents")) {
			this.documentsPushSuccessfulC6MigrationToOriginC6Migration++;
		}
		else if (resource.startsWith("CATMA") && !resource.contains("/")) {
			this.rootsPushSuccessfulC6MigrationToOriginC6Migration++;
		}
		else {
			logger.warning(String.format("Unexpected resource type successfully pushed %1$s %2$s->origin/%2$s", resource, migrationBranchName));
		}			
	}
	
	public void addPushC6MigrationToOriginC6MigrationFailed(String resource) {
		logger.info(String.format("Failed to push %1$s %2$s->origin/%2$s", resource, migrationBranchName));
		if (resource.startsWith("collections")) {
			this.collectionsPushFailedC6MigrationToOriginC6Migration++;
		}
		else if (resource.startsWith("tagsets")) {
			this.tagsetsPushFailedC6MigrationToOriginC6Migration++;
		}
		else if (resource.startsWith("documents")) {
			this.documentsPushFailedC6MigrationToOriginC6Migration++;
		}
		else if (resource.startsWith("CATMA") && !resource.contains("/")) {
			this.rootsPushFailedC6MigrationToOriginC6Migration++;
		}
		else {
			logger.warning(String.format("Unexpected resource type failed to push %1$s %2$s->origin/%2$s", resource, migrationBranchName));
		}			
	}


	public void addUser(String username) {
		totalUsers++;

		if (username.contains("google_com")) {
			googleUsers++;
		}
	}
	
	public void setOwnerEmail(String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	public void setIssuesCount(int issuesCount) {
		this.issuesCount = issuesCount;
	}

	public boolean getRequiresManualCheck() {
		return requiresManualCheck;
	}

	public void setRequiresManualCheck() {
		if (!requiresManualCheck) {
			requiresManualCheck = true;
			checkProjects++;
		}
	}

	public void exportToCsv(CSVPrinter csvPrinter) throws IOException {
		csvPrinter.printRecord(
			projectId,
			ownerEmail,
			lastUnsynchronizedCommitTime==null?null:lastUnsynchronizedCommitTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
			lastHeadCommitTime==null?null:lastHeadCommitTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
			lastError,
			issuesCount,
			totalUsers,
			googleUsers,
			totalRoots,	
			readyToMigrateRoots,
			totalDocuments+totalCollections+totalTagsets,
			totalDocuments,
			totalCollections,
			totalTagsets,
			cleanRoots,
			cleanDocuments+cleanCollections+cleanTagsets,
			cleanDocuments,
			cleanCollections,
			cleanTagsets,
			dirtyRoots,
			dirtyDocuments+dirtyCollections+dirtyTagsets,
			dirtyDocuments,
			dirtyCollections,
			dirtyTagsets,
			uncommittedRoots,
			uncommittedCollections+uncommittedDocuments+tagsetsNeedMergeMasterToOriginMaster,
			uncommittedDocuments,
			uncommittedCollections,
			uncommittedTagsets,
			conflictingRoots,
			conflictingCollections+conflictingDocuments+conflictingTagsets,
			conflictingDocuments,
			conflictingCollections,
			conflictingTagsets,
			staleProjects,
			staleUsers,
			errorProjects,
			checkProjects,
			collectionsNeedMergeDevToMaster+documentsNeedMergeDevToMaster+tagsetsNeedMergeDevToMaster,
			documentsNeedMergeDevToMaster,
			collectionsNeedMergeDevToMaster,
			tagsetsNeedMergeDevToMaster,
			rootsNeedMergeMasterToOriginMaster,
			collectionsNeedMergeMasterToOriginMaster+documentsNeedMergeMasterToOriginMaster+tagsetsNeedMergeMasterToOriginMaster,
			documentsNeedMergeMasterToOriginMaster,
			collectionsNeedMergeMasterToOriginMaster,
			tagsetsNeedMergeMasterToOriginMaster,
			rootsCanMergeMasterToOriginMaster,
			rootsConflictingMergeMasterToOriginMaster,
			collectionsCanMergeDevToMaster+documentsCanMergeDevToMaster+tagsetsCanMergeDevToMaster,
			documentsCanMergeDevToMaster,
			collectionsCanMergeDevToMaster,
			tagsetsCanMergeDevToMaster,
			collectionsConflictingMergeDevToMaster+documentsConflictingMergeDevToMaster+tagsetsConflictingMergeDevToMaster,
			documentsConflictingMergeDevToMaster,
			collectionsConflictingMergeDevToMaster,
			tagsetsConflictingMergeDevToMaster,
			collectionsCanMergeDevToOriginMaster+documentsCanMergeDevToOriginMaster+tagsetsCanMergeDevToOriginMaster,
			documentsCanMergeDevToOriginMaster,
			collectionsCanMergeDevToOriginMaster,
			tagsetsCanMergeDevToOriginMaster,
			collectionsConflictingMergeDevToOriginMaster+documentsConflictingMergeDevToOriginMaster+tagsetsConflictingMergeDevToOriginMaster,
			documentsConflictingMergeDevToOriginMaster,
			collectionsConflictingMergeDevToOriginMaster,
			tagsetsConflictingMergeDevToOriginMaster,
			documentsMergeSuccessfulOriginC6MigrationToC6Migration,
			collectionsMergeSuccessfulOriginC6MigrationToC6Migration,
			tagsetsMergeSuccessfulOriginC6MigrationToC6Migration,
			rootsMergeSuccessfulOriginC6MigrationToC6Migration,

			documentsMergeSuccessfulOriginMasterToC6Migration,
			collectionsMergeSuccessfulOriginMasterToC6Migration,
			tagsetsMergeSuccessfulOriginMasterToC6Migration,
			rootsMergeSuccessfulOriginMasterToC6Migration,
			
			documentsMergeSuccessfulDevToC6Migration,
			collectionsMergeSuccessfulDevToC6Migration,
			tagsetsMergeSuccessfulDevToC6Migration,
			
			rootsMergeSuccessfulMasterToC6Migration,

			documentsPushSuccessfulC6MigrationToOriginC6Migration,
			collectionsPushSuccessfulC6MigrationToOriginC6Migration,
			tagsetsPushSuccessfulC6MigrationToOriginC6Migration,
			rootsPushSuccessfulC6MigrationToOriginC6Migration,

			documentsMergeConflictingOriginC6MigrationToC6Migration,
			collectionsMergeConflictingOriginC6MigrationToC6Migration,
			tagsetsMergeConflictingOriginC6MigrationToC6Migration,
			rootsMergeConflictingOriginC6MigrationToC6Migration,

			documentsMergeConflictingOriginMasterToC6Migration,
			collectionsMergeConflictingOriginMasterToC6Migration,
			tagsetsMergeConflictingOriginMasterToC6Migration,
			rootsMergeConflictingOriginMasterToC6Migration,
			
			documentsMergeConflictingDevToC6Migration,
			collectionsMergeConflictingDevToC6Migration,
			tagsetsMergeConflictingDevToC6Migration,
			
			rootsMergeConflictingMasterToC6Migration,

			documentsPushFailedC6MigrationToOriginC6Migration,
			collectionsPushFailedC6MigrationToOriginC6Migration,
			tagsetsPushFailedC6MigrationToOriginC6Migration,
			rootsPushFailedC6MigrationToOriginC6Migration
			
		);
	}

	public static void exportHeaderToCsv(CSVPrinter csvPrinter) throws IOException {
		csvPrinter.printRecord(
				"Project ID",
				"Owner",
				"Last Unsynchronized Commit Time",
				"Last HEAD Commit Time",
				"Last Error",
				"Issues Count",
				"Total Users",
				"Google Users",
				"Total Roots",
				"Ready to Migrate Roots",
				"Total Resources",
				"Total Documents",
				"Total Collections",
				"Total Tagsets",
				"Clean Roots",
				"Clean Resources",
				"Clean Documents",
				"Clean Collections",
				"Clean Tagsets",
				"Dirty Roots",
				"Dirty Resources",
				"Dirty Documents",
				"Dirty Collections",
				"Dirty Tagsets",
				"Uncommitted Roots",
				"Uncommitted Resources",
				"Uncommitted Documents",
				"Uncommitted Collections",
				"Uncommitted Tagsets",
				"Conflicting Roots",
				"Conflicting Resources",
				"Conflicting Documents",
				"Conflicting Collections",
				"Conflicting Tagsets",
				"Stale Projects",
				"Stale Users",
				"Projects with Errors",
				"Projects Requiring Manual Check",
				"Resources Needing Merge, dev->master",
				"Documents Needing Merge, dev->master",
				"Collections Needing Merge, dev->master",
				"Tagsets Needing Merge, dev->master",
				"Roots Needing Merge, master->origin/master",
				"Resources Needing Merge, master->origin/master",
				"Documents Needing Merge, master->origin/master",
				"Collections Needing Merge, master->origin/master",
				"Tagsets Needing Merge, master->origin/master",
				"Roots Able to be Merged, master->origin/master",
				"Roots with Conflicted Merge, master->origin/master",
				"Resources Able to be Merged, dev->master",
				"Documents Able to be Merged, dev->master",
				"Collections Able to be Merged, dev->master",
				"Tagsets Able to be Merged, dev->master",
				"Resources with Conflicted Merge, dev->master",
				"Documents with Conflicted Merge, dev->master",
				"Collections with Conflicted Merge, dev->master",
				"Tagsets with Conflicted Merge, dev->master",
				"Resources Able to be Merged, dev->origin/master",
				"Documents Able to be Merged, dev->origin/master",
				"Collections Able to be Merged, dev->origin/master",
				"Tagsets Able to be Merged, dev->origin/master",
				"Resources with Conflicted Merge, dev->origin/master",
				"Documents with Conflicted Merge, dev->origin/master",
				"Collections with Conflicted Merge, dev->origin/master",
				"Tagsets with Conflicted Merge, dev->origin/master",
				String.format("Documents Merged Successfully, origin/%1$s->%1$s", migrationBranchName),
				String.format("Collections Merged Successfully, origin/%1$s->%1$s", migrationBranchName),
				String.format("Tagsets Merged Successfully, origin/%1$s->%1$s", migrationBranchName),
				String.format("Roots Merged Successfully, origin/%1$s->%1$s", migrationBranchName),
				String.format("Documents Merged Successfully, origin/master->%s", migrationBranchName),
				String.format("Collections Merged Successfully, origin/master->%s", migrationBranchName),
				String.format("Tagsets Merged Successfully, origin/master->%s", migrationBranchName),
				String.format("Roots Merged Successfully, origin/master->%s", migrationBranchName),
				String.format("Documents Merged Successfully, dev->%s", migrationBranchName),
				String.format("Collections Merged Successfully, dev->%s", migrationBranchName),
				String.format("Tagsets Merged Successfully, dev->%s", migrationBranchName),
				String.format("Roots Merged Successfully, master->%s", migrationBranchName),
				String.format("Documents Pushed Successfully, %1$s->origin/%1$s", migrationBranchName),
				String.format("Collections Pushed Successfully, %1$s->origin/%1$s", migrationBranchName),
				String.format("Tagsets Pushed Successfully, %1$s->origin/%1$s", migrationBranchName),
				String.format("Roots Pushed Successfully, %1$s->origin/%1$s", migrationBranchName),
				String.format("Documents with Conflicted Merge, origin/%1$s->%1$s", migrationBranchName),
				String.format("Collections with Conflicted Merge, origin/%1$s->%1$s", migrationBranchName),
				String.format("Tagsets with Conflicted Merge, origin/%1$s->%1$s", migrationBranchName),
				String.format("Roots with Conflicted Merge, origin/%1$s->%1$s", migrationBranchName),
				String.format("Documents with Conflicted Merge, origin/master->%s", migrationBranchName),
				String.format("Collections with Conflicted Merge, origin/master->%s", migrationBranchName),
				String.format("Tagsets with Conflicted Merge, origin/master->%s", migrationBranchName),
				String.format("Roots with Conflicted Merge, origin/master->%s", migrationBranchName),
				String.format("Documents with Conflicted Merge, dev->%s", migrationBranchName),
				String.format("Collections with Conflicted Merge, dev->%s", migrationBranchName),
				String.format("Tagsets with Conflicted Merge, dev->%s", migrationBranchName),
				String.format("Roots with Conflicted Merge, master->%s", migrationBranchName),
				String.format("Documents that Failed to Push, %1$s->origin/%1$s", migrationBranchName),
				String.format("Collections that Failed to Push, %1$s->origin/%1$s", migrationBranchName),
				String.format("Tagsets that Failed to Push, %1$s->origin/%1$s", migrationBranchName),
				String.format("Roots that Failed to Push, %1$s->origin/%1$s", migrationBranchName)
			);
		
	}

	public void addLastHeadCommit(CommitInfo commit) {
		LocalDateTime commitTime = commit.getCommittedDate();
		if (this.lastHeadCommitTime == null || this.lastHeadCommitTime.isBefore(commitTime)) {
			this.lastHeadCommitTime = commitTime;
		}
	}
}
