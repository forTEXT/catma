package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;

import de.catma.document.repository.Repository;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleValueDialog;
import de.catma.ui.tagmanager.TagsetTree;

public class TagsetSelectionDialog extends VerticalLayout {
	
	private final static String SORTCAP_PROP = "SORTCAP";
	
	private Window dialogWindow;
	
	private Repository repository;
	private TagLibrary tagLibrary;
	private HierarchicalContainer tagLibraryContainer;
	private Tree tagLibrariesTree;
	private TagsetTree tagsetTree;
	private Button btCreateTagLibrary;
	
	PropertyChangeListener tagLibraryChangedListener;
	
	public TagsetSelectionDialog(Repository repository) {
		super();
		
		this.repository = repository;
		
		initComponents();
		initActions();
		initListeners();
	}

	// TODO: factor out a TagLibrariesTree component, lots of stuff copied from TagLibraryPanel
	private void initComponents() {
		HorizontalLayout tagLibrariesTreeContainer = new HorizontalLayout();
		tagLibrariesTreeContainer.setWidth("100%");
		tagLibrariesTreeContainer.setMargin(new MarginInfo(false, true, true, false));

		tagLibraryContainer = new HierarchicalContainer();
		tagLibraryContainer.addContainerProperty(SORTCAP_PROP, String.class, null);		

		tagLibrariesTree = new Tree();
		tagLibrariesTree.setContainerDataSource(tagLibraryContainer);
		
		tagLibrariesTree.setCaption("Tag Type Libraries");
		tagLibrariesTree.addStyleName("bold-label-caption");
		tagLibrariesTree.setImmediate(true);
		tagLibrariesTree.setItemCaptionMode(ItemCaptionMode.ID);
		
		for (TagLibraryReference tlr : repository.getTagLibraryReferences()) {
			tagLibrariesTree.addItem(tlr);
			tagLibrariesTree.getItem(tlr).getItemProperty(SORTCAP_PROP).setValue(
					(tlr.toString()==null)?"":tlr.toString());
			tagLibrariesTree.setChildrenAllowed(tlr, false);
		}
		tagLibraryContainer.sort(new Object[] {SORTCAP_PROP}, new boolean[] { true });
		
		tagLibrariesTree.addItemClickListener(new ItemClickListener() {
			
			public void itemClick(ItemClickEvent event) {
				handleTagLibrariesTreeItemClick(event);				
			}
		});
		
		tagLibrariesTreeContainer.addComponent(tagLibrariesTree);
		tagLibrariesTreeContainer.setExpandRatio(tagLibrariesTree, 1.0f);
		
		btCreateTagLibrary = new Button("Create Tag Type Library");
		btCreateTagLibrary.addStyleName("secondary-button");
		tagLibrariesTreeContainer.addComponent(btCreateTagLibrary);
		
		addComponent(tagLibrariesTreeContainer);

		tagsetTree = new TagsetTree(
			repository.getTagManager(), null, false, true, false, false, true, null);
		addComponent(tagsetTree);
		
		setMargin(true);
		
		dialogWindow = new Window("Open Tagset");
		dialogWindow.setContent(this);
	}
	
	private void initActions() {
		btCreateTagLibrary.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				final String nameProperty = "name";
				SingleValueDialog singleValueDialog = new SingleValueDialog();
						
				singleValueDialog.getSingleValue(
						"Create a new Tag Type Library",
						"You have to enter a name!",
						new SaveCancelListener<PropertysetItem>() {
					public void cancelPressed() {}
					public void savePressed(
							PropertysetItem propertysetItem) {
						Property property = 
								propertysetItem.getItemProperty(
										nameProperty);
						String name = (String)property.getValue();
						try {
							repository.createTagLibrary(name);
						} catch (IOException e) {
							((CatmaApplication)UI.getCurrent()).showAndLogError(
								"Error creating the Tag Type Library!", e);
						}
					}
				}, nameProperty);
			}
		});
	}
	
	private void initListeners() {
		tagsetTree.addBtLoadIntoDocumentListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				dialogWindow.close();
			}
		});
		
		tagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() == null) { //insert
					TagLibraryReference tagLibraryRef = 
							(TagLibraryReference)evt.getNewValue();
					addTagLibraryReferenceToTree(tagLibraryRef);
					tagLibraryContainer.sort(
						new Object[] {SORTCAP_PROP}, new boolean[] { true });

				}
			}
		};
		
		this.repository.addPropertyChangeListener(
			Repository.RepositoryChangeEvent.tagLibraryChanged, 
			tagLibraryChangedListener
		);
		
		dialogWindow.addCloseListener(new Window.CloseListener() {
			
			@Override
			public void windowClose(CloseEvent e) {
				removeListeners();
				tagsetTree.close(false);
			}
		});
	}
	
	private void addTagLibraryReferenceToTree(TagLibraryReference tlr) {
		tagLibrariesTree.addItem(tlr);
		tagLibrariesTree.getItem(tlr).getItemProperty(SORTCAP_PROP).setValue(
				(tlr.toString()==null)?"":tlr.toString());
		tagLibrariesTree.setChildrenAllowed(tlr, false);
	}

	private void handleTagLibrariesTreeItemClick(ItemClickEvent event) {
		TagLibraryReference tagLibraryReference = ((TagLibraryReference)event.getItemId());
		
		if (tagLibrary == null || tagLibrary.getId() != tagLibraryReference.getId()) {
			try {
				tagLibrary = repository.getTagLibrary(tagLibraryReference);

				((CatmaApplication)UI.getCurrent()).openTagLibrary(repository, tagLibrary, false);
				tagsetTree.setTagLibrary(tagLibrary);
				
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
						"Error opening the Tag Type Library!", e);
			}
		}		
	}
	
	public void show(String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		dialogWindow.setStyleName("open-tag-set");
		dialogWindow.setModal(true);
		UI.getCurrent().addWindow(dialogWindow);
		dialogWindow.center();
	}
	
	public void show() {
		show("40%");
	}
	
	// seems to get hit twice??
	private void removeListeners() {
		this.repository.removePropertyChangeListener(
			Repository.RepositoryChangeEvent.tagLibraryChanged, 
			tagLibraryChangedListener
		);
	}
}
