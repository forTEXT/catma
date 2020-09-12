/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.module.annotate;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.ui.Component;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.source.SourceDocument;
import de.catma.project.Project;
import de.catma.project.event.ChangeType;
import de.catma.project.event.DocumentChangeEvent;
import de.catma.ui.component.tabbedview.TabbedView;
import de.catma.ui.module.annotate.TaggerView.AfterDocumentLoadedOperation;

public class TaggerManagerView extends TabbedView {
	
	private int nextTaggerID = 1;
	private final EventBus eventBus;
	
	public TaggerManagerView(EventBus eventBus, Project project) {
		super(() -> new TaggerView(
				0, null, project,
				eventBus, null));
		this.eventBus = eventBus;
		this.eventBus.register(this);
	}
	
    @Subscribe
    public void handleDocumentChanged(DocumentChangeEvent documentChangeEvent) {

		if (documentChangeEvent.getChangeType().equals(ChangeType.DELETED)) {
				TaggerView taggerView = 
						getTaggerView(documentChangeEvent.getDocument());
			if (taggerView != null) {
				onTabClose(taggerView);
			}
		}
		else if (documentChangeEvent.getChangeType().equals(ChangeType.UPDATED)) {
			SourceDocument document = documentChangeEvent.getDocument();
			TaggerView taggerView = getTaggerView(document);
			if (taggerView != null) {
				taggerView.setSourceDocument(document, null);
			}
		}    
    }
    
	public TaggerView openSourceDocument(
			final SourceDocument sourceDocument, Project repository,
			AfterDocumentLoadedOperation afterDocumentLoadedOperation) {

		TaggerView taggerView = getTaggerView(sourceDocument);
		if (taggerView != null) {
			setSelectedTab(taggerView);
			if (taggerView.getSourceDocument() == null) {
				taggerView.setSourceDocument(sourceDocument, afterDocumentLoadedOperation);
			}
			else {
				afterDocumentLoadedOperation.afterDocumentLoaded(taggerView);
			}
		}
		else {
			taggerView = new TaggerView(
					nextTaggerID++, sourceDocument, repository,
					eventBus, afterDocumentLoadedOperation);
			addClosableTab(taggerView, sourceDocument.toString());
			setSelectedTab(taggerView);
		}
		
		return taggerView;
	}
	
	
	private TaggerView getTaggerView(SourceDocument sourceDocument) {
		for (Component tabContent : this.getTabSheet()) {
			TaggerView taggerView = (TaggerView)tabContent;
			if (taggerView.getSourceDocument() == null || taggerView.getSourceDocument().getUuid().equals(
					sourceDocument.getUuid())) {
				return taggerView;
			}
		}
		
		return null;
	}

	public void openUserMarkupCollection(TaggerView taggerView,
			AnnotationCollection userMarkupCollection) {
		taggerView.openUserMarkupCollection(userMarkupCollection);
	}
	
	@Override
	public void closeClosables() {
		eventBus.unregister(this);
		super.closeClosables();
	}
}
