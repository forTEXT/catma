package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.time.LocalDateTime;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.repository.Messages;
import de.catma.util.Pair;



public class ChooseAnnotationCollectionDialog extends Window {
	
	public static interface AnnotationCollectionListener {
		public void defaultCollectionCreated(UserMarkupCollection userMarkupCollection);
		public void openOrCreateCollection();
	}

	private Button btOpenOrCreateCollection;
	private Button btContinueWithout;
	private Repository repository;
	private SourceDocument sourceDocument;


	public ChooseAnnotationCollectionDialog( Repository repository,
			String sourceDocumentId, AnnotationCollectionListener annotationCollectionListener) {
		super("Choose one of the Options"); //$NON-NLS-1$
		this.repository = repository;
	
		sourceDocument = repository.getSourceDocument(sourceDocumentId);
		initComponents();
		initActions(annotationCollectionListener);

	}

	private void initActions(AnnotationCollectionListener annotationCollectionListener) {

		btOpenOrCreateCollection.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				try {
					annotationCollectionListener.openOrCreateCollection();
				} finally {
					UI.getCurrent().removeWindow(ChooseAnnotationCollectionDialog.this);
				}
			}
		});

		
		btContinueWithout.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				
				PropertyChangeListener userMarkupDocumentChangedListener = new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						if (evt.getOldValue() == null) {
							@SuppressWarnings("unchecked")
							Pair<UserMarkupCollectionReference, SourceDocument> result = (Pair<UserMarkupCollectionReference, SourceDocument>) evt
									.getNewValue();
							try {
								UserMarkupCollection umc = repository.getUserMarkupCollection(result.getFirst());							
								annotationCollectionListener.defaultCollectionCreated(umc); 							
							} catch (IOException e) {
								((CatmaApplication)UI.getCurrent()).showAndLogError(Messages.getString("CorpusContentSelectionDialog.errorCreatingCollection"), e); //$NON-NLS-1$
							}
						}
						repository.removePropertyChangeListener(
								Repository.RepositoryChangeEvent.userMarkupCollectionChanged, this);
					}
				};

						
				repository.addPropertyChangeListener(Repository.RepositoryChangeEvent.userMarkupCollectionChanged,
						userMarkupDocumentChangedListener);
				try {

					String collectionName = generateCollectionName();
					try {
						repository.createUserMarkupCollection(collectionName, sourceDocument);
					} catch (IOException e) {
						((CatmaApplication)UI.getCurrent()).showAndLogError(Messages.getString("CorpusContentSelectionDialog.errorCreatingCollection"), //$NON-NLS-1$
								e);
					}
				} finally {
					UI.getCurrent().removeWindow(ChooseAnnotationCollectionDialog.this);

				}

			}
		});

	}

	private String generateCollectionName() {
		String userName = repository.getUser().getName();
		String sourceDocumentName = sourceDocument.toString();
		LocalDateTime timePoint = LocalDateTime.now();
		String collectionName = sourceDocumentName + "_" + userName + "_" + timePoint; //$NON-NLS-1$ 
		return collectionName;
	}

	private void initComponents() {
		setWidth("300px"); //$NON-NLS-1$
		setHeight("150px"); //$NON-NLS-1$

		setModal(true);
		setClosable(true);
		setResizable(false);

		center();

		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSizeFull();
		content.setSpacing(true);

		setContent(content);
		btOpenOrCreateCollection = new Button(Messages.getString("ChooseAnnotationCollectionDialog.openOrCreateCollection")); //$NON-NLS-1$
		btContinueWithout = new Button(Messages.getString("ChooseAnnotationCollectionDialog.continueWithout")); //$NON-NLS-1$
		btContinueWithout.setWidth("90%"); //$NON-NLS-1$
		btOpenOrCreateCollection.setWidth("90%"); //$NON-NLS-1$
		content.addComponent(btOpenOrCreateCollection);
		content.addComponent(btContinueWithout);

		// content.setComponentAlignment(btOk, Alignment.MIDDLE_CENTER);
		btOpenOrCreateCollection.setClickShortcut(KeyCode.ENTER);
		btContinueWithout.setClickShortcut(KeyCode.ENTER);

	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
