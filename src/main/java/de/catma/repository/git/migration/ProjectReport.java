package de.catma.repository.git.migration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
	

	private int totalUsers;
	
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

	private int documentsPushFailedfulC6MigrationToOriginC6Migration;
	private int collectionsPushFailedfulC6MigrationToOriginC6Migration;
	private int tagsetsPushFailedfulC6MigrationToOriginC6Migration;
	private int rootsPushFailedfulC6MigrationToOriginC6Migration;
	
	private String lastError;
	private LocalDateTime lastUnsynchronizedCommitTime;
	private LocalDateTime lastHeadCommitTime;
	
	public ProjectReport(String projectId) {
		this.projectId = projectId;
	}
	
	public ProjectReport(Collection<ProjectReport> reports) {
		for (ProjectReport report : reports) {
			totalUsers+=report.totalUsers;
			
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

			documentsPushFailedfulC6MigrationToOriginC6Migration+=report.documentsPushFailedfulC6MigrationToOriginC6Migration;
			collectionsPushFailedfulC6MigrationToOriginC6Migration+=report.collectionsPushFailedfulC6MigrationToOriginC6Migration;
			tagsetsPushFailedfulC6MigrationToOriginC6Migration+=report.tagsetsPushFailedfulC6MigrationToOriginC6Migration;
			rootsPushFailedfulC6MigrationToOriginC6Migration+=report.rootsPushFailedfulC6MigrationToOriginC6Migration;

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
	
	private static String formatColumnName(String name) {
		return "\n"+name.replaceAll("([A-Z])", " $1");
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (projectId != null) {
			builder.append("\n\nPROJECT REPORT ");
			builder.append(projectId);
			if (ownerEmail != null) {
				builder.append("\n\nOwner: ");
				builder.append(ownerEmail);
			}
			if (lastUnsynchronizedCommitTime != null) {
				builder.append("\nLast unsynchronized commit time: ");
				builder.append(lastUnsynchronizedCommitTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			}
			if (lastHeadCommitTime != null) {
				builder.append("\nLast HEAD commit time: ");
				builder.append(lastHeadCommitTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			}
			if (lastError != null) {
				builder.append("\nError");
				builder.append(lastError);
			}
			builder.append("\n\n");
		}
		builder.append(formatColumnName("totalUsers: "));
		builder.append(totalUsers);
		builder.append(formatColumnName("totalRoots: "));
		builder.append(totalRoots);	
		builder.append(formatColumnName("readyToMigrateRoots: "));
		builder.append(readyToMigrateRoots);
		builder.append(formatColumnName("totalResources: "));
		builder.append(totalDocuments+totalCollections+totalTagsets);
		builder.append(formatColumnName("totalDocuments: "));
		builder.append(totalDocuments);
		builder.append(formatColumnName("totalCollections: "));
		builder.append(totalCollections);
		builder.append(formatColumnName("totalTagsets: "));
		builder.append(totalTagsets);

		builder.append(formatColumnName("cleanRoots: "));
		builder.append(cleanRoots);
		builder.append(formatColumnName("cleanResources: "));
		builder.append(cleanDocuments+cleanCollections+cleanTagsets);
		builder.append(formatColumnName("cleanDocuments: "));
		builder.append(cleanDocuments);
		builder.append(formatColumnName("cleanCollections: "));
		builder.append(cleanCollections);
		builder.append(formatColumnName("cleanTagsets: "));
		builder.append(cleanTagsets);

		builder.append(formatColumnName("dirtyRoots: "));
		builder.append(dirtyRoots);
		builder.append(formatColumnName("dirtyResources: "));
		builder.append(dirtyDocuments+dirtyCollections+dirtyTagsets);
		builder.append(formatColumnName("dirtyDocuments: "));
		builder.append(dirtyDocuments);
		builder.append(formatColumnName("dirtyCollections: "));
		builder.append(dirtyCollections);
		builder.append(formatColumnName("dirtyTagsets: "));
		builder.append(dirtyTagsets);

		builder.append(formatColumnName("uncommittedRoots: "));
		builder.append(uncommittedRoots);
		builder.append(formatColumnName("uncommittedResources: "));
		builder.append(uncommittedCollections+uncommittedDocuments+tagsetsNeedMergeMasterToOriginMaster);
		builder.append(formatColumnName("uncommittedDocuments: "));
		builder.append(uncommittedDocuments);
		builder.append(formatColumnName("uncommittedCollections: "));
		builder.append(uncommittedCollections);
		builder.append(formatColumnName("uncommittedTagsets: "));
		builder.append(uncommittedTagsets);

		builder.append(formatColumnName("conflictingRoots: "));
		builder.append(conflictingRoots);
		builder.append(formatColumnName("conflictingResources: "));
		builder.append(conflictingCollections+conflictingDocuments+conflictingTagsets);
		builder.append(formatColumnName("conflictingDocuments: "));
		builder.append(conflictingDocuments);
		builder.append(formatColumnName("conflictingCollections: "));
		builder.append(conflictingCollections);
		builder.append(formatColumnName("conflictingTagsets: "));
		builder.append(conflictingTagsets);
		
		builder.append(formatColumnName("resourcesNeedMergeDevToMaster: "));
		builder.append(collectionsNeedMergeDevToMaster+documentsNeedMergeDevToMaster+tagsetsNeedMergeDevToMaster);
		builder.append(formatColumnName("documentsNeedMergeDevToMaster: "));
		builder.append(documentsNeedMergeDevToMaster);
		builder.append(formatColumnName("collectionsNeedMergeDevToMaster: "));
		builder.append(collectionsNeedMergeDevToMaster);
		builder.append(formatColumnName("tagsetsNeedMergeDevToMaster: "));
		builder.append(tagsetsNeedMergeDevToMaster);
		
		builder.append(formatColumnName("rootsNeedMergeMasterToOriginMaster: "));
		builder.append(rootsNeedMergeMasterToOriginMaster);
		builder.append(formatColumnName("resourcesNeedMergeMasterToOriginMaster: "));
		builder.append(collectionsNeedMergeMasterToOriginMaster+documentsNeedMergeMasterToOriginMaster+tagsetsNeedMergeMasterToOriginMaster);
		builder.append(formatColumnName("documentsNeedMergeMasterToOriginMaster: "));
		builder.append(documentsNeedMergeMasterToOriginMaster);
		builder.append(formatColumnName("collectionsNeedMergeMasterToOriginMaster: "));
		builder.append(collectionsNeedMergeMasterToOriginMaster);
		builder.append(formatColumnName("tagsetsNeedMergeMasterToOriginMaster: "));
		builder.append(tagsetsNeedMergeMasterToOriginMaster);
		
		builder.append(formatColumnName("rootsCanMergeMasterToOriginMaster: "));
		builder.append(rootsCanMergeMasterToOriginMaster);
		
		builder.append(formatColumnName("rootsConflictingMergeMasterToOriginMaster: "));
		builder.append(rootsConflictingMergeMasterToOriginMaster);

		builder.append(formatColumnName("resourcesCanMergeDevToMaster: "));
		builder.append(collectionsCanMergeDevToMaster+documentsCanMergeDevToMaster+tagsetsCanMergeDevToMaster);
		builder.append(formatColumnName("documentsCanMergeDevToMaster: "));
		builder.append(documentsCanMergeDevToMaster);
		builder.append(formatColumnName("collectionsCanMergeDevToMaster: "));
		builder.append(collectionsCanMergeDevToMaster);
		builder.append(formatColumnName("tagsetsCanMergeDevToMaster: "));
		builder.append(tagsetsCanMergeDevToMaster);


		builder.append(formatColumnName("resourcesConflictingMergeDevToMaster: "));
		builder.append(collectionsConflictingMergeDevToMaster+documentsConflictingMergeDevToMaster+tagsetsConflictingMergeDevToMaster);
		builder.append(formatColumnName("documentsConflictingMergeDevToMaster: "));
		builder.append(documentsConflictingMergeDevToMaster);
		builder.append(formatColumnName("collectionsConflictingMergeDevToMaster: "));
		builder.append(collectionsConflictingMergeDevToMaster);
		builder.append(formatColumnName("tagsetsConflictingMergeDevToMaster: "));
		builder.append(tagsetsConflictingMergeDevToMaster);

		builder.append(formatColumnName("resourcesCanMergeDevToOriginMaster: "));
		builder.append(collectionsCanMergeDevToOriginMaster+documentsCanMergeDevToOriginMaster+tagsetsCanMergeDevToOriginMaster);
		builder.append(formatColumnName("documentsCanMergeDevToOriginMaster: "));
		builder.append(documentsCanMergeDevToOriginMaster);
		builder.append(formatColumnName("collectionsCanMergeDevToOriginMaster: "));
		builder.append(collectionsCanMergeDevToOriginMaster);
		builder.append(formatColumnName("tagsetsCanMergeDevToOriginMaster: "));
		builder.append(tagsetsCanMergeDevToOriginMaster);
		
		builder.append(formatColumnName("resourcesConflictingMergeDevToOriginMaster: "));
		builder.append(collectionsConflictingMergeDevToOriginMaster+documentsConflictingMergeDevToOriginMaster+tagsetsConflictingMergeDevToOriginMaster);
		builder.append(formatColumnName("documentsConflictingMergeDevToOriginMaster: "));
		builder.append(documentsConflictingMergeDevToOriginMaster);
		builder.append(formatColumnName("collectionsConflictingMergeDevToOriginMaster: "));
		builder.append(collectionsConflictingMergeDevToOriginMaster);
		builder.append(formatColumnName("tagsetsConflictingMergeDevToOriginMaster: "));
		builder.append(tagsetsConflictingMergeDevToOriginMaster);
		
		builder.append(formatColumnName("staleProjects: "));
		builder.append(staleProjects);

		builder.append(formatColumnName("staleUsers: "));
		builder.append(staleUsers);

		builder.append(formatColumnName("errorProjects: "));
		builder.append(errorProjects);
		
		builder.append(formatColumnName("documentsMergeSuccessfulOriginC6MigrationToC6Migration: "));
		builder.append(documentsMergeSuccessfulOriginC6MigrationToC6Migration);
		builder.append(formatColumnName("collectionsMergeSuccessfulOriginC6MigrationToC6Migration: "));
		builder.append(collectionsMergeSuccessfulOriginC6MigrationToC6Migration);
		builder.append(formatColumnName("tagsetsMergeSuccessfulOriginC6MigrationToC6Migration: "));
		builder.append(tagsetsMergeSuccessfulOriginC6MigrationToC6Migration);
		builder.append(formatColumnName("rootsMergeSuccessfulOriginC6MigrationToC6Migration: "));
		builder.append(rootsMergeSuccessfulOriginC6MigrationToC6Migration);
        
		builder.append(formatColumnName("documentsMergeSuccessfulOriginMasterToC6Migration: "));
		builder.append(documentsMergeSuccessfulOriginMasterToC6Migration);
		builder.append(formatColumnName("collectionsMergeSuccessfulOriginMasterToC6Migration: "));
		builder.append(collectionsMergeSuccessfulOriginMasterToC6Migration);
		builder.append(formatColumnName("tagsetsMergeSuccessfulOriginMasterToC6Migration: "));
		builder.append(tagsetsMergeSuccessfulOriginMasterToC6Migration);
		builder.append(formatColumnName("rootsMergeSuccessfulOriginMasterToC6Migration: "));
		builder.append(rootsMergeSuccessfulOriginMasterToC6Migration);
		
		builder.append(formatColumnName("documentsMergeSuccessfulDevToC6Migration: "));
		builder.append(documentsMergeSuccessfulDevToC6Migration);
		builder.append(formatColumnName("collectionsMergeSuccessfulDevToC6Migration: "));
		builder.append(collectionsMergeSuccessfulDevToC6Migration);
		builder.append(formatColumnName("tagsetsMergeSuccessfulDevToC6Migration: "));
		builder.append(tagsetsMergeSuccessfulDevToC6Migration);
		
		builder.append(formatColumnName("rootsMergeSuccessfulMasterToC6Migration: "));
		builder.append(rootsMergeSuccessfulMasterToC6Migration);
        
		builder.append(formatColumnName("documentsPushSuccessfulC6MigrationToOriginC6Migration: "));
		builder.append(documentsPushSuccessfulC6MigrationToOriginC6Migration);
		builder.append(formatColumnName("collectionsPushSuccessfulC6MigrationToOriginC6Migration: "));
		builder.append(collectionsPushSuccessfulC6MigrationToOriginC6Migration);
		builder.append(formatColumnName("tagsetsPushSuccessfulC6MigrationToOriginC6Migration: "));
		builder.append(tagsetsPushSuccessfulC6MigrationToOriginC6Migration);
		builder.append(formatColumnName("rootsPushSuccessfulC6MigrationToOriginC6Migration: "));
		builder.append(rootsPushSuccessfulC6MigrationToOriginC6Migration);
        
		builder.append(formatColumnName("documentsMergeConflictingOriginC6MigrationToC6Migration: "));
		builder.append(documentsMergeConflictingOriginC6MigrationToC6Migration);
		builder.append(formatColumnName("collectionsMergeConflictingOriginC6MigrationToC6Migration: "));
		builder.append(collectionsMergeConflictingOriginC6MigrationToC6Migration);
		builder.append(formatColumnName("tagsetsMergeConflictingOriginC6MigrationToC6Migration: "));
		builder.append(tagsetsMergeConflictingOriginC6MigrationToC6Migration);
		builder.append(formatColumnName("rootsMergeConflictingOriginC6MigrationToC6Migration: "));
		builder.append(rootsMergeConflictingOriginC6MigrationToC6Migration);
        
		builder.append(formatColumnName("documentsMergeConflictingOriginMasterToC6Migration: "));
		builder.append(documentsMergeConflictingOriginMasterToC6Migration);
		builder.append(formatColumnName("collectionsMergeConflictingOriginMasterToC6Migration: "));
		builder.append(collectionsMergeConflictingOriginMasterToC6Migration);
		builder.append(formatColumnName("tagsetsMergeConflictingOriginMasterToC6Migration: "));
		builder.append(tagsetsMergeConflictingOriginMasterToC6Migration);
		builder.append(formatColumnName("rootsMergeConflictingOriginMasterToC6Migration: "));
		builder.append(rootsMergeConflictingOriginMasterToC6Migration);
		
		builder.append(formatColumnName("documentsMergeConflictingDevToC6Migration: "));
		builder.append(documentsMergeConflictingDevToC6Migration);
		builder.append(formatColumnName("collectionsMergeConflictingDevToC6Migration: "));
		builder.append(collectionsMergeConflictingDevToC6Migration);
		builder.append(formatColumnName("tagsetsMergeConflictingDevToC6Migration: "));
		builder.append(tagsetsMergeConflictingDevToC6Migration);
		
		builder.append(formatColumnName("rootsMergeConflictingMasterToC6Migration: "));
		builder.append(rootsMergeConflictingMasterToC6Migration);
        
		builder.append(formatColumnName("documentsPushFailedfulC6MigrationToOriginC6Migration: "));
		builder.append(documentsPushFailedfulC6MigrationToOriginC6Migration);
		builder.append(formatColumnName("collectionsPushFailedfulC6MigrationToOriginC6Migration: "));
		builder.append(collectionsPushFailedfulC6MigrationToOriginC6Migration);
		builder.append(formatColumnName("tagsetsPushFailedfulC6MigrationToOriginC6Migration: "));
		builder.append(tagsetsPushFailedfulC6MigrationToOriginC6Migration);
		builder.append(formatColumnName("rootsPushFailedfulC6MigrationToOriginC6Migration: "));
		builder.append(rootsPushFailedfulC6MigrationToOriginC6Migration);
		
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
			LocalDateTime commitTime = commit.getCommitTime().toInstant()
				      .atZone(ZoneId.systemDefault())
				      .toLocalDateTime();
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
			this.collectionsPushFailedfulC6MigrationToOriginC6Migration++;
		}
		else if (resource.startsWith("tagsets")) {
			this.tagsetsPushFailedfulC6MigrationToOriginC6Migration++;
		}
		else if (resource.startsWith("documents")) {
			this.documentsPushFailedfulC6MigrationToOriginC6Migration++;
		}
		else if (resource.startsWith("CATMA") && !resource.contains("/")) {
			this.rootsPushFailedfulC6MigrationToOriginC6Migration++;
		}
		else {
			logger.warning(String.format("Unexpected resource type failed to pushed %1$s %2$s->origin/%2$s", resource, migrationBranchName));
		}			
	}


	public void addUser(String username) {
		totalUsers++;
	}
	
	public void setOwnerEmail(String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	public void exportToCsv(CSVPrinter csvPrinter) throws IOException {
		csvPrinter.printRecord(
			projectId,
			ownerEmail,
			lastUnsynchronizedCommitTime==null?null:lastUnsynchronizedCommitTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
			lastHeadCommitTime==null?null:lastHeadCommitTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
			lastError,
			totalUsers,
			totalRoots,	
			readyToMigrateRoots,
			totalDocuments,
			totalCollections,
			totalTagsets,
			cleanRoots,
			cleanDocuments,
			cleanCollections,
			cleanTagsets,
			dirtyRoots,
			dirtyDocuments,
			dirtyCollections,
			dirtyTagsets,
			uncommittedRoots,
			uncommittedDocuments,
			uncommittedCollections,
			uncommittedTagsets,
			conflictingRoots,
			conflictingDocuments,
			conflictingCollections,
			conflictingTagsets,
			staleProjects,
			staleUsers,
			errorProjects,
			documentsNeedMergeDevToMaster,
			collectionsNeedMergeDevToMaster,
			tagsetsNeedMergeDevToMaster,
			rootsNeedMergeMasterToOriginMaster,
			documentsNeedMergeMasterToOriginMaster,
			collectionsNeedMergeMasterToOriginMaster,
			tagsetsNeedMergeMasterToOriginMaster,
			rootsCanMergeMasterToOriginMaster,
			rootsConflictingMergeMasterToOriginMaster,
			documentsCanMergeDevToMaster,
			collectionsCanMergeDevToMaster,
			tagsetsCanMergeDevToMaster,
			documentsConflictingMergeDevToMaster,
			collectionsConflictingMergeDevToMaster,
			tagsetsConflictingMergeDevToMaster,
			documentsCanMergeDevToOriginMaster,
			collectionsCanMergeDevToOriginMaster,
			tagsetsCanMergeDevToOriginMaster,
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

			documentsPushFailedfulC6MigrationToOriginC6Migration,
			collectionsPushFailedfulC6MigrationToOriginC6Migration,
			tagsetsPushFailedfulC6MigrationToOriginC6Migration,
			rootsPushFailedfulC6MigrationToOriginC6Migration
			
		);
	}

	public static void exportHeaderToCsv(CSVPrinter csvPrinter) throws IOException {
		csvPrinter.printRecord(
				formatColumnName("projectId"),
				formatColumnName("ownerEmail"),
				formatColumnName("lastUnsynchronizedCommitTime"),
				formatColumnName("lastHeadCommitTime"),
				formatColumnName("lastError"),
				formatColumnName("totalUsers"),
				formatColumnName("totalRoots"),	
				formatColumnName("readyToMigrateRoots"),
				formatColumnName("totalDocuments"),
				formatColumnName("totalCollections"),
				formatColumnName("totalTagsets"),
				formatColumnName("cleanRoots"),
				formatColumnName("cleanDocuments"),
				formatColumnName("cleanCollections"),
				formatColumnName("cleanTagsets"),
				formatColumnName("dirtyRoots"),
				formatColumnName("dirtyDocuments"),
				formatColumnName("dirtyCollections"),
				formatColumnName("dirtyTagsets"),
				formatColumnName("uncommittedRoots"),
				formatColumnName("uncommittedDocuments"),
				formatColumnName("uncommittedCollections"),
				formatColumnName("uncommittedTagsets"),
				formatColumnName("conflictingRoots"),
				formatColumnName("conflictingDocuments"),
				formatColumnName("conflictingCollections"),
				formatColumnName("conflictingTagsets"),
				formatColumnName("staleProjects"),
				formatColumnName("staleUsers"),
				formatColumnName("errorProjects"),
				formatColumnName("documentsNeedMergeDevToMaster"),
				formatColumnName("collectionsNeedMergeDevToMaster"),
				formatColumnName("tagsetsNeedMergeDevToMaster"),
				formatColumnName("rootsNeedMergeMasterToOriginMaster"),
				formatColumnName("documentsNeedMergeMasterToOriginMaster"),
				formatColumnName("collectionsNeedMergeMasterToOriginMaster"),
				formatColumnName("tagsetsNeedMergeMasterToOriginMaster"),
				formatColumnName("rootsCanMergeMastertoOriginMaster"),
				formatColumnName("rootsConflictingMergeMastertoOriginMaster"),
				formatColumnName("documentsCanMergeDevToMaster"),
				formatColumnName("collectionsCanMergeDevToMaster"),
				formatColumnName("tagsetsCanMergeDevToMaster"),
				formatColumnName("documentsConflictingMergeDevToMaster"),
				formatColumnName("collectionsConflictingMergeDevToMaster"),
				formatColumnName("tagsetsConflictingMergeDevToMaster"),
				formatColumnName("documentsCanMergeDevToOriginMaster"),
				formatColumnName("collectionsCanMergeDevToOriginMaster"),
				formatColumnName("tagsetsCanMergeDevToOriginMaster"),
				formatColumnName("documentsConflictingMergeDevToOriginMaster"),
				formatColumnName("collectionsConflictingMergeDevToOriginMaster"),
				formatColumnName("tagsetsConflictingMergeDevToOriginMaster"),
				formatColumnName("documentsMergeSuccessfulOriginC6MigrationToC6Migration"),
				formatColumnName("collectionsMergeSuccessfulOriginC6MigrationToC6Migration"),
				formatColumnName("tagsetsMergeSuccessfulOriginC6MigrationToC6Migration"),
				formatColumnName("rootsMergeSuccessfulOriginC6MigrationToC6Migration"),
				formatColumnName("documentsMergeSuccessfulOriginMasterToC6Migration"),
				formatColumnName("collectionsMergeSuccessfulOriginMasterToC6Migration"),
				formatColumnName("tagsetsMergeSuccessfulOriginMasterToC6Migration"),
				formatColumnName("rootsMergeSuccessfulOriginMasterToC6Migration"),
				formatColumnName("documentsMergeSuccessfulDevToC6Migration"),
				formatColumnName("collectionsMergeSuccessfulDevToC6Migration"),
				formatColumnName("tagsetsMergeSuccessfulDevToC6Migration"),
				formatColumnName("rootsMergeSuccessfulMasterToC6Migration"),
				formatColumnName("documentsPushSuccessfulC6MigrationToOriginC6Migration"),
				formatColumnName("collectionsPushSuccessfulC6MigrationToOriginC6Migration"),
				formatColumnName("tagsetsPushSuccessfulC6MigrationToOriginC6Migration"),
				formatColumnName("rootsPushSuccessfulC6MigrationToOriginC6Migration"),
				formatColumnName("documentsMergeConflictingOriginC6MigrationToC6Migration"),
				formatColumnName("collectionsMergeConflictingOriginC6MigrationToC6Migration"),
				formatColumnName("tagsetsMergeConflictingOriginC6MigrationToC6Migration"),
				formatColumnName("rootsMergeConflictingOriginC6MigrationToC6Migration"),
				formatColumnName("documentsMergeConflictingOriginMasterToC6Migration"),
				formatColumnName("collectionsMergeConflictingOriginMasterToC6Migration"),
				formatColumnName("tagsetsMergeConflictingOriginMasterToC6Migration"),
				formatColumnName("rootsMergeConflictingOriginMasterToC6Migration"),
				formatColumnName("documentsMergeConflictingDevToC6Migration"),
				formatColumnName("collectionsMergeConflictingDevToC6Migration"),
				formatColumnName("tagsetsMergeConflictingDevToC6Migration"),
				formatColumnName("rootsMergeConflictingMasterToC6Migration"),
				formatColumnName("documentsPushFailedC6MigrationToOriginC6Migration"),
				formatColumnName("collectionsPushFailed6MigrationToOriginC6Migration"),
				formatColumnName("tagsetsPushFailedC6MigrationToOriginC6Migration"),
				formatColumnName("rootsPushFailedC6MigrationToOriginC6Migration")
			);
		
	}

	public void addLastHeadCommit(CommitInfo commit) {
		LocalDateTime commitTime = commit.getCommitTime().toInstant()
			      .atZone(ZoneId.systemDefault())
			      .toLocalDateTime();
		if (this.lastHeadCommitTime == null || this.lastHeadCommitTime.isBefore(commitTime)) {
			this.lastHeadCommitTime = commitTime;
		}
	}
}
