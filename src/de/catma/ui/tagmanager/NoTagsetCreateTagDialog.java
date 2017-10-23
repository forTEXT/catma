package de.catma.ui.tagmanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.CatmaApplication;
import de.catma.ui.repository.Messages;
import de.catma.ui.tagger.TagsetSelectionDialog;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class NoTagsetCreateTagDialog extends Window {

	private Button btOpenTagset;
	private Button btAutoCreateTagset;
	private Button btOpenTagsetFromActiveAnnotationCollection;
	private Logger logger = Logger.getLogger(this.getClass().getName());

	public NoTagsetCreateTagDialog(TagsetSelectionListener tagsetSelectionListener, Repository repository) {
		super("Choose one of the Options");
		initComponents();
		initActions(repository, tagsetSelectionListener);
	}

	private void initActions(Repository repository, TagsetSelectionListener tagsetSelectionListener) {
		Object selectedParent = null;

		btOpenTagset.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				// zuerst this.DialogFenster entfernen---> dann nächstes
				// Dialogfenster aufmachen !
				UI.getCurrent().removeWindow(NoTagsetCreateTagDialog.this);

				TagsetSelectionDialog tagsetSelectionDialog = new TagsetSelectionDialog(repository,
						tagsetSelectionListener);
				tagsetSelectionDialog.show();

			}
		});

		btAutoCreateTagset.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				TagManager tagManager = repository.getTagManager();

				// 1= Listener fuer neue Library
				PropertyChangeListener tagLibraryAddedListner = new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						repository.addPropertyChangeListener(Repository.RepositoryChangeEvent.tagLibraryChanged, this);

						if (evt.getOldValue() == null) {// is null wenn was geadded wurde
												
							TagLibraryReference tagLibRef = (TagLibraryReference) evt.getNewValue();

							try {
								TagLibrary newTagLib = repository.getTagLibrary(tagLibRef);
								// logger.info("TagLibrary called: "+ tagLibRef + " has been created");

								TagsetDefinition tagsetDefinition = new TagsetDefinition(null,
										new IDGenerator().generate(), "Start-Tagset", new Version());
								tagManager.addTagsetDefinition(newTagLib, tagsetDefinition);// hier springt 2 Listener an
																						

							} catch (IOException e) {
								((CatmaApplication) UI.getCurrent()).showAndLogError(
										Messages.getString("TagLibraryPanel.errorCreatingTagLibrary"), e);
							}

						}
					}
				};

				// 2= Listener fuer Tagset adden zur Taglibrary
				PropertyChangeListener tagLibraryChangedListner = new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						// springt an wenn neuer Tagset angelegt wurde, damit  jetzt das CreateTag Dialog erzeugen
						
						tagManager.removePropertyChangeListener(TagManager.TagManagerEvent.tagsetDefinitionChanged,
								this);
						if (evt.getOldValue() == null) {
							// logger.info("TagSet called: "+ evt.getNewValue() + " has been created");
							
							Pair pair = (Pair) evt.getNewValue();
							final TagsetDefinition tagsetdef = (TagsetDefinition) pair.getSecond();
							CatmaApplication application = ((CatmaApplication) UI.getCurrent());
							application.addTagsetToActiveDocument(tagsetdef, tagsetSelectionListener);

						}

					}
				};

				repository.addPropertyChangeListener(Repository.RepositoryChangeEvent.tagLibraryChanged,
						tagLibraryAddedListner);
				tagManager.addPropertyChangeListener(TagManager.TagManagerEvent.tagsetDefinitionChanged,
						tagLibraryChangedListner);

				String tagLibName = generateTagLibraryName(repository);

				try {
					repository.createTagLibrary(tagLibName);// hier springt 1 Listener an
															

				} catch (IOException e) {
					((CatmaApplication) UI.getCurrent())
							.showAndLogError(Messages.getString("TagLibraryPanel.errorCreatingTagLibrary"), e); //$NON-NLS-1$
				}

				UI.getCurrent().removeWindow(NoTagsetCreateTagDialog.this);


			}
		});

		btOpenTagsetFromActiveAnnotationCollection.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				// hier aus ActiveAnnotations die Library und den Tagset  ermitteln( falls eine Annotation auf ist) und öffnen

			}
		});

	}

	private String generateTagLibraryName(Repository repository) {
		String userName = repository.getUser().getName();
		// String sourceDocumentName = sourceDocument.toString();
		LocalDateTime timePoint = LocalDateTime.now();
		String TagLibName = "Tag Library created at " + timePoint + " for user " + userName;
		return TagLibName;
	}

	private String generateTagsetName() {
		return "Start Tagset";
	}

	private void initComponents() {
		setWidth("370px"); //$NON-NLS-1$
		setHeight("180px"); //$NON-NLS-1$

		setModal(true);
		setClosable(true);
		setResizable(false);

		center();

		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSizeFull();
		content.setSpacing(true);

		setContent(content);

		btOpenTagset = new Button("Open Tagset");
		btOpenTagset.setWidth("90%");

		btAutoCreateTagset = new Button("Continue without");
		btAutoCreateTagset.setWidth("90%");

		btOpenTagsetFromActiveAnnotationCollection = new Button("Open Tagset from the active Annotation Collection");
		btOpenTagsetFromActiveAnnotationCollection.setWidth("90%");

		content.addComponent(btOpenTagset);
		content.addComponent(btAutoCreateTagset);
		content.addComponent(btOpenTagsetFromActiveAnnotationCollection);

		// content.setComponentAlignment(btOk, Alignment.MIDDLE_CENTER);
		btOpenTagset.setClickShortcut(KeyCode.ENTER);
		btAutoCreateTagset.setClickShortcut(KeyCode.ENTER);
		btOpenTagsetFromActiveAnnotationCollection.setClickShortcut(KeyCode.ENTER);

	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
