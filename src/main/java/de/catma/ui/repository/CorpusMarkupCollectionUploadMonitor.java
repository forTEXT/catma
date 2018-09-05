package de.catma.ui.repository;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import de.catma.document.Corpus;
import de.catma.document.repository.Repository;

public class CorpusMarkupCollectionUploadMonitor implements Runnable {
	
	private List<Corpus> corpora;
	private Repository repository;
	
	public CorpusMarkupCollectionUploadMonitor(Repository repository) {
		super();
		this.repository = repository;
		this.corpora = new ArrayList<>();
	}

	@Override
	public void run() {
		UI.getCurrent().access(new Runnable() {
			@Override
			public void run() {
				for (Corpus corpus : corpora) {
					int newUserMarkupCollectionRefCount = 
						repository.getNewUserMarkupCollectionRefs(corpus);
	
					if (newUserMarkupCollectionRefCount > 0) {
						
						Notification.show(
							Messages.getString("CorpusMarkupCollectionUploadMonitor.infoTitle"),  //$NON-NLS-1$
							MessageFormat.format(Messages.getString("CorpusMarkupCollectionUploadMonitor.newAnnotationCollectionFeedback"), newUserMarkupCollectionRefCount),  //$NON-NLS-1$
							Type.TRAY_NOTIFICATION);
	
					}
				}
			}
		});
	}

	
	public void addCorpus(Corpus corpus) {
		this.corpora.add(corpus);
	}
	
	public void removeCorpus(Corpus corpus) {
		this.corpora.remove(corpus);
	}
}
