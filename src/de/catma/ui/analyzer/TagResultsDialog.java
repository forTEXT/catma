package de.catma.ui.analyzer;

import java.io.IOException;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table.TableDragMode;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;

import de.catma.document.repository.Repository;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.menu.MainMenu;
import de.catma.ui.menu.MainMenu.MenuItemSelectedEvent;
import de.catma.ui.tagmanager.TagsetTree;

public class TagResultsDialog extends VerticalLayout {
	
	private final static String SORTCAP_PROP = "SORTCAP";
	
	private Window dialogWindow;
	
	private Repository repository;
	private TagLibrary tagLibrary;
	private HierarchicalContainer tagLibraryContainer;
	private Tree tagLibrariesTree;
	private TagsetTree tagsetTree;
	
	public TagResultsDialog(Repository repository) {
		super();
		
		this.repository = repository;
		
		initComponents();
		initListeners();
	}
	
	// TODO: factor out a TagLibrariesTree component, lots of stuff copied from TagLibraryPanel
	private void initComponents() {
		setSizeFull();
		
		Label lblInstructions = new Label(
				"Select a Tag Type Library and find the Tag that you want"
				+ " to apply to your selection in the KWIC view, then click"
				+ " and drag the Tag onto the KWIC view.");
		
		addComponent(lblInstructions);
		
		HorizontalLayout tagLibrariesTreeContainer = new HorizontalLayout();
		tagLibrariesTreeContainer.setWidth("100%");
		tagLibrariesTreeContainer.setMargin(new MarginInfo(true, false, true, false));

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
		
		tagLibrariesTree.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				handleTagLibrariesTreeItemClick(event);				
			}
		});
		
		tagLibrariesTreeContainer.addComponent(tagLibrariesTree);
//		tagLibrariesTreeContainer.setExpandRatio(tagLibrariesTree, 1.0f);
		
		addComponent(tagLibrariesTreeContainer);
		
		tagsetTree = new TagsetTree(repository.getTagManager(), null, false, false, false, false, false, null);
		tagsetTree.getTagTree().setDragMode(TableDragMode.ROW);
	
		addComponent(tagsetTree);
		setExpandRatio(tagsetTree, 1.0f);
		setMargin(true);
		
		dialogWindow = new Window("Tags");
		dialogWindow.setContent(this);
	}
	
	private void handleTagLibrariesTreeItemClick(ValueChangeEvent event) {
		TagLibraryReference tagLibraryReference = (TagLibraryReference)event.getProperty().getValue();
		
		if (tagLibrary == null || tagLibrary.getId() != tagLibraryReference.getId()) {
			try {
				tagLibrary = repository.getTagLibrary(tagLibraryReference);
				tagsetTree.setTagLibrary(tagLibrary);
				
			} catch (IOException e) {
				((CatmaApplication)UI.getCurrent()).showAndLogError(
						"Error opening the Tag Type Library!", e);
			}
		}		
	}
	
	private MainMenu.MenuItemSelectedListener menuItemSelectedListener = new MainMenu.MenuItemSelectedListener() {		
		@Override
		public void menuItemSelected(MenuItemSelectedEvent event) {
			
			Component component = event.getComponent();			
			if (component instanceof AnalyzerManagerView) {
				return;
			}
			
			dialogWindow.close();
		}
	};
	
	private void initListeners() {
		MainMenu menu = ((CatmaApplication)UI.getCurrent()).getMenu();
		menu.addMenuItemSelectedListener(menuItemSelectedListener);
		
		dialogWindow.addCloseListener(new Window.CloseListener() {
			
			@Override
			public void windowClose(CloseEvent e) {
				removeListeners();
			}
		});
	}
	
	private void removeListeners() {
		MainMenu menu = ((CatmaApplication)UI.getCurrent()).getMenu();
		menu.removeMenuItemSelectedListener(menuItemSelectedListener);
	}
	
	public void show(String dialogWidth) {
		dialogWindow.setWidth(dialogWidth);
		dialogWindow.setHeight("50%");
		dialogWindow.setPositionX(20);
		dialogWindow.setPositionY(80);
		UI.getCurrent().addWindow(dialogWindow);
	}
	
	public void show() {
		show("40%");
	}
}
