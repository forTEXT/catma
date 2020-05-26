package de.catma.ui.module.project;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.project.ForkStatus;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.module.main.ErrorHandler;

public class ForkHandler {

	private final UI ui;
	private final Set<TagsetDefinition> tagsets;
	private final ProjectReference targetProject;
	private final ProgressListener progressListener;
	private final ExecutionListener<Void> executionListener;
	private String projectId;
	private ProjectManager projectManager;

	public ForkHandler(
			UI ui, 
			String projectId, ProjectManager projectManager, 
			Set<TagsetDefinition> tagsets, ProjectReference targetProject,
			ExecutionListener<Void> executionListener, ProgressListener progressListener) {
		this.ui = ui;
		this.projectId = projectId; 
		this.projectManager = projectManager;
		this.tagsets = tagsets;
		this.targetProject = targetProject;
		this.executionListener = executionListener;
		this.progressListener = progressListener;
	}

	public void fork() {
		BackgroundServiceProvider backgroundServiceProvider = (BackgroundServiceProvider)ui;
		BackgroundService backgroundService = backgroundServiceProvider.accuireBackgroundService();
		backgroundService.submit(
			new DefaultProgressCallable<Void>() {
				@Override
				public Void call() throws Exception {
					final AtomicBoolean cancel = new AtomicBoolean(false);
					for (TagsetDefinition tagset : tagsets) {
						
						getProgressListener().setProgress(
							"Forking Tagset %1$s into Project %2$s", 
							tagset.getName(), targetProject.getName());
					
						ui.accessSynchronously(() -> {
							try {
								ForkStatus forkStatus = 
										projectManager.forkTagset(tagset, projectId, targetProject);
								
								if (forkStatus.isResourceAlreadyExists()) {
									Notification.show(
										"Info", 
										String.format(
											"The Tagset %1$s is already present in the target Project and cannot be forked!", 
											tagset.getName()), 
										Type.ERROR_MESSAGE);
								}
								else if (forkStatus.isTargetHasConflicts()) {
									Notification.show(
										"Info",
										String.format(
											"The target Project %1$s has conflicts, please open the Project and resolve the conflicts first!", 
											targetProject.getName()),
										Type.ERROR_MESSAGE);
									cancel.set(true);
								}
								else if (forkStatus.isTargetNotClean()) {
									Notification.show(
										"Info",
										String.format(
											"The target Project %1$s has uncommited changes, please open the Project and commit all changes first!", 
											targetProject.getName()),
										Type.ERROR_MESSAGE);
									cancel.set(true);
								}
							} catch (Exception e) {
								((ErrorHandler)UI.getCurrent()).showAndLogError("Error forking Tagsets", e);
								cancel.set(true);
							}
						});
						
						if (cancel.get()) {
							return null;
						}
					}
					return null;
				}
			},
			executionListener,
			progressListener);
	}

}
