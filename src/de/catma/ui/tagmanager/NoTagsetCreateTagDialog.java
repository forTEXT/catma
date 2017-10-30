package de.catma.ui.tagmanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.repository.Repository;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.ui.CatmaApplication;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.repository.Messages;
import de.catma.ui.tagger.CurrentWritableUserMarkupCollectionProvider;
import de.catma.ui.tagger.TagsetSelectionDialog;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class NoTagsetCreateTagDialog extends Window {

	private Button btOpenTagset;
	private Button btAutoCreateTagset;
	private Button btOpenTagsetFromActiveAnnotationCollection;


	public NoTagsetCreateTagDialog(TagsetSelectionListener tagsetSelectionListener, Repository repository,
			CurrentWritableUserMarkupCollectionProvider collectionProvider) {
		super("Choose one of the options");
		initComponents();
		initActions(repository, tagsetSelectionListener, collectionProvider);
	}

	private void initActions(Repository repository, TagsetSelectionListener tagsetSelectionListener,
			CurrentWritableUserMarkupCollectionProvider collectionProvider) {

		btOpenTagset.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
			
				UI.getCurrent().removeWindow(NoTagsetCreateTagDialog.this);

				TagsetSelectionDialog tagsetSelectionDialog = new TagsetSelectionDialog(repository,
						tagsetSelectionListener);
				tagsetSelectionDialog.show();

			}
		});

		btAutoCreateTagset.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				TagManager tagManager = repository.getTagManager();

				PropertyChangeListener tagLibraryAddedListener = new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						repository.removePropertyChangeListener(Repository.RepositoryChangeEvent.tagLibraryChanged, this);

						if (evt.getOldValue() == null) {
													
							TagLibraryReference tagLibRef = (TagLibraryReference) evt.getNewValue();

							try {
								TagLibrary newTagLib = repository.getTagLibrary(tagLibRef);
						
								String tagsetName= generateTagsetName(repository);
								
								TagsetDefinition tagsetDefinition = new TagsetDefinition(null,
										new IDGenerator().generate(), tagsetName, new Version());
								tagManager.addTagsetDefinition(newTagLib, tagsetDefinition);
																							
							} catch (IOException e) {
								((CatmaApplication) UI.getCurrent()).showAndLogError(
										Messages.getString("TagLibraryPanel.errorCreatingTagLibrary"), e);
							}

						}
					}
				};

			
				PropertyChangeListener tagLibraryChangedListner = new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {

						tagManager.removePropertyChangeListener(TagManager.TagManagerEvent.tagsetDefinitionChanged,
								this);
						if (evt.getOldValue() == null) {
					
							Pair pair = (Pair) evt.getNewValue();
							final TagsetDefinition tagsetdef = (TagsetDefinition) pair.getSecond();
							CatmaApplication application = ((CatmaApplication) UI.getCurrent());
							application.addTagsetToActiveDocument(tagsetdef, tagsetSelectionListener);

						}

					}
				};

				repository.addPropertyChangeListener(Repository.RepositoryChangeEvent.tagLibraryChanged,
						tagLibraryAddedListener);
				tagManager.addPropertyChangeListener(TagManager.TagManagerEvent.tagsetDefinitionChanged,
						tagLibraryChangedListner);

				String tagLibName = generateTagLibraryName(repository);

				try {
					repository.createTagLibrary(tagLibName);											
				} catch (IOException e) {
					((CatmaApplication) UI.getCurrent())
							.showAndLogError(Messages.getString("TagLibraryPanel.errorCreatingTagLibrary"), e); //$NON-NLS-1$
				}

				UI.getCurrent().removeWindow(NoTagsetCreateTagDialog.this);
			}
		});

		btOpenTagsetFromActiveAnnotationCollection.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {

				UserMarkupCollection umc = collectionProvider.getTheCurrentWritableUserMarkupCollection();
				if (umc != null) {

					int numberOfTagsets = umc.getTagLibrary().getTagsetDefinitions().size();
					if (numberOfTagsets != 0) {

						if (numberOfTagsets == 1) {
							Collection<TagsetDefinition> allTagsetDefinitions = umc.getTagLibrary()
									.getTagsetDefinitions();
							Iterator<TagsetDefinition> iterator = allTagsetDefinitions.iterator();
							while (iterator.hasNext()) {
								TagsetDefinition tagsetDefinition = (TagsetDefinition) iterator.next();
								((CatmaApplication) UI.getCurrent()).addTagsetToActiveDocument(tagsetDefinition,
										tagsetSelectionListener);
							}

						} else {
							Collection<TagsetDefinition> allTagsetDefinitions = umc.getTagLibrary()
									.getTagsetDefinitions();
							Iterator<TagsetDefinition> iterator = allTagsetDefinitions.iterator();
							while (iterator.hasNext()) {
								TagsetDefinition tagsetDefinition = (TagsetDefinition) iterator.next();
								((CatmaApplication) UI.getCurrent()).addTagsetToActiveDocument(tagsetDefinition);
							}
							Notification.show(Messages.getString("CatmaApplication.info"), //$NON-NLS-1$
									"Select first one Tagset ", Notification.Type.HUMANIZED_MESSAGE);
						}

					} else {
						HTMLNotification.show(Messages.getString("CatmaApplication.error"), //$NON-NLS-1$
								"There is no Tagset for this Document", Type.HUMANIZED_MESSAGE);
					}
				} else {
					HTMLNotification.show(Messages.getString("CatmaApplication.error"), //$NON-NLS-1$
							"No Annotation Collection for this Document found", Type.HUMANIZED_MESSAGE);
				}
				UI.getCurrent().removeWindow(NoTagsetCreateTagDialog.this);
			}
		});

	}

	private String generateTagLibraryName(Repository repository) {
		String userName = repository.getUser().getName();
		LocalDateTime timePoint = LocalDateTime.now();
		String tagLibName = "Tag Library created for user " + userName + " at " + timePoint;
		return tagLibName;
	}

	private String generateTagsetName(Repository repository) {
		String userName = repository.getUser().getName();
		LocalDateTime timePoint = LocalDateTime.now();
		String tagSetName = "Tagset created for user " + userName + " at " + timePoint;
		return tagSetName;
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

		btOpenTagset = new Button("Open Tagsets");
		btOpenTagset.setWidth("90%");

		btAutoCreateTagset = new Button("Continue without");
		btAutoCreateTagset.setWidth("90%");

		btOpenTagsetFromActiveAnnotationCollection = new Button("Open Tagsets from the active Annotation Collection");
		btOpenTagsetFromActiveAnnotationCollection.setWidth("90%");

		content.addComponent(btOpenTagset);
		content.addComponent(btAutoCreateTagset);
		content.addComponent(btOpenTagsetFromActiveAnnotationCollection);

		btOpenTagset.setClickShortcut(KeyCode.ENTER);
		btAutoCreateTagset.setClickShortcut(KeyCode.ENTER);
		btOpenTagsetFromActiveAnnotationCollection.setClickShortcut(KeyCode.ENTER);

	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
