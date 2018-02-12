package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.repository.Repository;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleValueDialog;
import de.catma.ui.tagmanager.TagsetSelectionListener;
import de.catma.ui.tagmanager.TagsetTree;

public class TagsetSelectionDialog extends Window {
	
	private final static String TAGLIB_CAPTION_PROPERTY = "TAGLIBPROPERTY"; //$NON-NLS-1$
	
	private Repository repository;
	private TagLibrary tagLibrary;
	private HierarchicalContainer tagLibraryContainer;
	private Table tagLibrariesTable;
	private TagsetTree tagsetTree;
	private Button btCreateTagLibrary;
	
	private PropertyChangeListener tagLibraryChangedListener;
	private TagsetSelectionListener tagsetSelectionListener;
	
	public TagsetSelectionDialog(Repository repository) {
		this(repository ,null);
	}
	
	public TagsetSelectionDialog(Repository repository,TagsetSelectionListener tagsetSelectionListener) {
		super(Messages.getString("TagsetSelectionDialog.openTagset")); //$NON-NLS-1$
		
		this.repository = repository;
		this.tagsetSelectionListener = tagsetSelectionListener;
		
		
		initComponents();
		initActions();
		initListeners();
	}

	private void initComponents() {
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		content.setSpacing(true);
		content.setMargin(true);
		

		HorizontalLayout tagLibrariesPanel = new HorizontalLayout();
		tagLibrariesPanel.setSpacing(true);
		tagLibrariesPanel.setSizeFull();
		tagLibrariesPanel.setMargin(new MarginInfo(false, false, true, false));
		tagLibraryContainer = new HierarchicalContainer();
		tagLibraryContainer.addContainerProperty(TAGLIB_CAPTION_PROPERTY, String.class, null);		

		tagLibrariesTable = new Table(Messages.getString("TagsetSelectionDialog.selectTagLibrary")); //$NON-NLS-1$
		tagLibrariesTable.setSizeFull();
		tagLibrariesTable.setContainerDataSource(tagLibraryContainer);
		tagLibrariesTable.setNullSelectionAllowed(false);
		
		tagLibrariesTable.addContainerProperty(TAGLIB_CAPTION_PROPERTY, String.class, null);
		tagLibrariesTable.setColumnHeader(TAGLIB_CAPTION_PROPERTY, Messages.getString("TagsetSelectionDialog.TagLibraries")); //$NON-NLS-1$
		
		for (TagLibraryReference tlr : repository.getTagLibraryReferences()) {
			addTagLibraryReferenceToTree(tlr);
		}
		
		tagLibraryContainer.sort(new Object[] {TAGLIB_CAPTION_PROPERTY}, new boolean[] { true });
		
		tagLibrariesPanel.addComponent(tagLibrariesTable);
		tagLibrariesPanel.setExpandRatio(tagLibrariesTable, 1.0f);
		
		btCreateTagLibrary = new Button(Messages.getString("TagsetSelectionDialog.createTagLibrary")); //$NON-NLS-1$
		btCreateTagLibrary.addStyleName("secondary-button"); //$NON-NLS-1$
		btCreateTagLibrary.addStyleName("tagsetselection-dialog-create-tag-libary"); //$NON-NLS-1$
		tagLibrariesPanel.addComponent(btCreateTagLibrary);
		
		content.addComponent(tagLibrariesPanel);
		content.setExpandRatio(tagLibrariesPanel, 0.5f);
		
		tagsetTree = new TagsetTree(
			repository.getTagManager(), null, false, true, false, false, true, null,tagsetSelectionListener,repository);
		tagsetTree.setCaption(Messages.getString("TagsetSelectionDialog.selectTagset")); //$NON-NLS-1$

		content.addComponent(tagsetTree);
		content.setExpandRatio(tagsetTree, 0.5f);
		
		
		setContent(content);
	}
	
	private void initActions() {
		tagLibrariesTable.addValueChangeListener(event->{handleTagLibrariesTreeItemClick(event);});
		

		btCreateTagLibrary.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				final String nameProperty = "name"; //$NON-NLS-1$
				SingleValueDialog singleValueDialog = new SingleValueDialog();
						
				singleValueDialog.getSingleValue(
						Messages.getString("TagsetSelectionDialog.createNewTagLibrary"), //$NON-NLS-1$
						Messages.getString("TagsetSelectionDialog.enterNameObligation"), //$NON-NLS-1$
						new SaveCancelListener<PropertysetItem>() {
					public void cancelPressed() {}
					public void savePressed(
							PropertysetItem propertysetItem) {
						Property<?> property = 
								propertysetItem.getItemProperty(
										nameProperty);
						String name = (String)property.getValue();
						try {
							repository.createTagLibrary(name);
						} catch (IOException e) {
							((CatmaApplication)UI.getCurrent()).showAndLogError(
								Messages.getString("TagsetSelectionDialog.errorCreatingTagLibrary"), e); //$NON-NLS-1$
						}
					}
				}, nameProperty);
			}
		});
	}
	
	private void initListeners() {
		tagsetTree.addBtLoadIntoDocumentListener( new ClickListener() {

			public void buttonClick(ClickEvent event) {
				close();
			}
		});
		
		tagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) { //insert
					TagLibraryReference tagLibraryRef = 
							(TagLibraryReference)evt.getNewValue();
					addTagLibraryReferenceToTree(tagLibraryRef);
					tagLibraryContainer.sort(
						new Object[] {TAGLIB_CAPTION_PROPERTY}, new boolean[] { true });

				}
			}
		};
		
		this.repository.addPropertyChangeListener(
			Repository.RepositoryChangeEvent.tagLibraryChanged, 
			tagLibraryChangedListener
		);
		
		addCloseListener(new Window.CloseListener() {
			
			@Override
			public void windowClose(CloseEvent e) {
				TagsetSelectionDialog.this.repository.removePropertyChangeListener(
						Repository.RepositoryChangeEvent.tagLibraryChanged, 
						tagLibraryChangedListener
					);
				tagsetTree.close(false);
			}
		});
	}
	
	private void addTagLibraryReferenceToTree(TagLibraryReference tlr) {
		tagLibrariesTable.addItem(new Object[] {(tlr.toString()==null)?Messages.getString("TagsetSelectionDialog.notAvailable"):tlr.toString()}, tlr); //$NON-NLS-1$
	}

	private void handleTagLibrariesTreeItemClick(ValueChangeEvent event) {
		TagLibraryReference tagLibraryReference = ((TagLibraryReference)event.getProperty().getValue());
		
		if (tagLibrary == null || tagLibrary.getId() != tagLibraryReference.getId()) {
			try {
				tagLibrary = repository.getTagLibrary(tagLibraryReference);

				((CatmaApplication)UI.getCurrent()).openTagLibrary(repository, tagLibrary, false);
				tagsetTree.setTagLibrary(tagLibrary);
				
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
						Messages.getString("TagsetSelectionDialog.errorOpeningTagLibrary"), e); //$NON-NLS-1$
			}
		}		
	}
	
	public void show() {
		setWidth("40%"); //$NON-NLS-1$
		setHeight("65%"); //$NON-NLS-1$
		setModal(true);
		center();
		UI.getCurrent().addWindow(this);
	}
}
