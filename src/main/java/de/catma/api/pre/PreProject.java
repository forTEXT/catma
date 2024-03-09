package de.catma.api.pre;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.graph.interfaces.GraphProjectHandler;
import de.catma.user.User;

public class PreProject {
	
	private final GitProjectHandler gitProjectHandler;
	private final GraphProjectHandler graphProjectHandler;
	private final ReadWriteLock accessLock = new ReentrantReadWriteLock();
	private final String namespace;
	private final String projectId;
	
	public PreProject(String namespace, String projectId, GitProjectHandler gitProjectHandler, GraphProjectHandler graphProjectHandler) {
		super();
		this.namespace = namespace;
		this.projectId = projectId;
		this.gitProjectHandler = gitProjectHandler;
		this.graphProjectHandler = graphProjectHandler;
	}

	public Collection<SourceDocumentReference> getSourceDocumentReferences() {
		Lock readLock = accessLock.readLock();
		try {
			readLock.lock();
			return this.graphProjectHandler.getSourceDocumentReferences();
		}
		finally {
			readLock.unlock();
		}
	}

	public SourceDocument getSourceDocument(String uuid) throws Exception {
		Lock readLock = accessLock.readLock();
		try {
			readLock.lock();
			return this.graphProjectHandler.getSourceDocument(uuid);
		}
		finally {
			readLock.unlock();
		}
	}

	public AnnotationCollection getAnnotationCollection(AnnotationCollectionReference annotationCollectionReference) throws Exception {
		Lock readLock = accessLock.readLock();
		try {
			readLock.lock();
			return this.graphProjectHandler.getAnnotationCollection(annotationCollectionReference);
		}
		finally {
			readLock.unlock();
		}
	}
	
	public String getProjectId() {
		Lock readLock = accessLock.readLock();
		try {
			readLock.lock();
			return projectId;
		}
		finally {
			readLock.unlock();
		}
	}
	
	public String getNamespace() {
		Lock readLock = accessLock.readLock();
		try {
			readLock.lock();
			return namespace;
		}
		finally {
			readLock.unlock();
		}	
	}
}
