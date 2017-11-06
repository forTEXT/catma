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

public class CreateTagsetOptionsSelectionDialog extends Window {

	private Button btOpenTagset;
	private Button btAutoCreateTagset;
	private Button btOpenTagsetFromActiveAnnotationCollection;


	public CreateTagsetOptionsSelectionDialog(TagsetSelectionListener tagsetSelectionListener, Repository repository,
			CurrentWritableUserMarkupCollectionProvider collectionProvider) {
		super(Messages.getString("CreateTagsetOptionsSelectionDialog.chooseOnOfTheOptions")); //$NON-NLS-1$
		initComponents();
		initActions(repository, tagsetSelectionListener, collectionProvider);
	}

	private void initActions(Repository repository, TagsetSelectionListener tagsetSelectionListener,
			CurrentWritableUserMarkupCollectionProvider collectionProvider) {

		btOpenTagset.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
			
				UI.getCurrent().removeWindow(CreateTagsetOptionsSelectionDialog.this);

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
										Messages.getString("TagLibraryPanel.errorCreatingTagLibrary"), e); //$NON-NLS-1$
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

				UI.getCurrent().removeWindow(CreateTagsetOptionsSelectionDialog.this);
			}
		});

		btOpenTagsetFromActiveAnnotationCollection.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				try {
					UserMarkupCollection umc = collectionProvider.getCurrentWritableUserMarkupCollection();
					if (umc != null) {
	
						int numberOfTagsets = umc.getTagLibrary().getTagsetDefinitions().size();
						if (numberOfTagsets != 0) {
	
							if (numberOfTagsets == 1) {
								Collection<TagsetDefinition> allTagsetDefinitions = umc.getTagLibrary()
										.getTagsetDefinitions();
								Iterator<TagsetDefinition> iterator = allTagsetDefinitions.iterator();
								while (iterator.hasNext()) {
									TagsetDefinition tagsetDefinition = (TagsetDefinition) iterator.next();
									TagLibrary tagLibrary = repository.getTagLibraryFor(tagsetDefinition.getUuid(), null);
									if (tagLibrary != null) {
										((CatmaApplication) UI.getCurrent()).openTagLibrary(repository, tagLibrary, false);
										((CatmaApplication) UI.getCurrent()).addTagsetToActiveDocument(tagLibrary.getTagsetDefinition(tagsetDefinition.getUuid()),
												tagsetSelectionListener);
									}
								}
	
							} else {
								Collection<TagsetDefinition> allTagsetDefinitions = umc.getTagLibrary()
										.getTagsetDefinitions();
								Iterator<TagsetDefinition> iterator = allTagsetDefinitions.iterator();
								while (iterator.hasNext()) {
									TagsetDefinition tagsetDefinition = (TagsetDefinition) iterator.next();
									TagLibrary tagLibrary = repository.getTagLibraryFor(tagsetDefinition.getUuid(), null);
									if (tagLibrary != null) {
										((CatmaApplication) UI.getCurrent()).openTagLibrary(repository, tagLibrary, false);
										((CatmaApplication) UI.getCurrent()).addTagsetToActiveDocument(tagLibrary.getTagsetDefinition(tagsetDefinition.getUuid()));
									}
								}
								Notification.show("",  //$NON-NLS-1$
										Messages.getString("CreateTagsetOptionsSelectionDialog.selectFirstOneTagset"), Notification.Type.HUMANIZED_MESSAGE);  //$NON-NLS-1$
							}
	
						} else {
							HTMLNotification.show("",  //$NON-NLS-1$
									Messages.getString("CreateTagsetOptionsSelectionDialog.thereIsNoTagsetForThisDocument"), Type.HUMANIZED_MESSAGE);  //$NON-NLS-1$
						}
					} else {
						HTMLNotification.show("",  //$NON-NLS-1$
								Messages.getString("CreateTagsetOptionsSelectionDialog.noAnnotationCollectionforThisDocumentFound"), Type.HUMANIZED_MESSAGE);   //$NON-NLS-1$
					}
					UI.getCurrent().removeWindow(CreateTagsetOptionsSelectionDialog.this);
				}
				catch (IOException e) {
					((CatmaApplication) UI.getCurrent())
					.showAndLogError(Messages.getString("TagLibraryPanel.errorOpeningTagLibrary"), e); //$NON-NLS-1$
				}
			}
		});

	}

	private String generateTagLibraryName(Repository repository) {
		String userName = repository.getUser().getName();
		LocalDateTime timePoint = LocalDateTime.now();
		String tagLibName = "Tag Library created for user " + userName + " at " + timePoint; //$NON-NLS-1$ //$NON-NLS-2$
		return tagLibName;
	}

	private String generateTagsetName(Repository repository) {
		String userName = repository.getUser().getName();
		LocalDateTime timePoint = LocalDateTime.now();
		String tagSetName = "Tagset created for user " + userName + " at " + timePoint; //$NON-NLS-1$ //$NON-NLS-2$
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

		btOpenTagset = new Button(Messages.getString("CreateTagsetOptionsSelectionDialog.openTagsets"));  //$NON-NLS-1$
		btOpenTagset.setWidth("90%"); //$NON-NLS-1$

		btAutoCreateTagset = new Button(Messages.getString("CreateTagsetOptionsSelectionDialog.continueWithout"));  //$NON-NLS-1$
		btAutoCreateTagset.setWidth("90%"); //$NON-NLS-1$

		btOpenTagsetFromActiveAnnotationCollection = new Button(Messages.getString("CreateTagsetOptionsSelectionDialog.openTagsetFromTheActiveAnnotationCollection"));  //$NON-NLS-1$
		btOpenTagsetFromActiveAnnotationCollection.setWidth("90%"); //$NON-NLS-1$

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
