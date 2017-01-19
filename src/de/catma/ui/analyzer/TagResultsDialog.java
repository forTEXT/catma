package de.catma.ui.analyzer;

import java.io.IOException;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table.TableDragMode;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.document.repository.Repository;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.ui.CatmaApplication;
import de.catma.ui.menu.MainMenu;
import de.catma.ui.menu.MainMenu.MenuItemSelectedEvent;
import de.catma.ui.tagmanager.TagsetTree;

public class TagResultsDialog extends Window {
	
	private final static String SORTCAP_PROP = "SORTCAP";
	
	private Repository repository;
	private TagLibrary tagLibrary;
	private HierarchicalContainer tagLibraryContainer;
	private Tree tagLibraryTree;
	private TagsetTree tagsetTree;

	public TagResultsDialog(Repository repository, 
			Object lastTagResultsDialogTagLibrarySelection,
			Object lastTagResultsDialogTagsetSelection) {
		super("Tags");
		
		this.repository = repository;
		initComponents();
		initListeners();
		
		if (lastTagResultsDialogTagLibrarySelection != null &&
				tagLibraryTree.containsId(lastTagResultsDialogTagLibrarySelection)) {
			tagLibraryTree.setValue(lastTagResultsDialogTagLibrarySelection);
		}
		else if (!tagLibraryTree.getItemIds().isEmpty()) {
			tagLibraryTree.setValue(tagLibraryTree.getItemIds().iterator().next());
		}
		
		if (lastTagResultsDialogTagsetSelection != null && 
				tagsetTree.getTagTree().containsId(lastTagResultsDialogTagsetSelection)) {
			tagsetTree.getTagTree().setValue(lastTagResultsDialogTagsetSelection);
			Object parent = tagsetTree.getTagTree().getParent(lastTagResultsDialogTagsetSelection);
			while (parent != null) {
				tagsetTree.getTagTree().setCollapsed(parent, false);
				parent = tagsetTree.getTagTree().getParent(parent);
			}
			tagsetTree.getTagTree().setCurrentPageFirstItemId(lastTagResultsDialogTagsetSelection);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void initComponents() {
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		content.setSpacing(true);
		
		Label lblInstructions = new Label(
				"Select a Tag Type Library and find the Tag that you want"
				+ " to apply to your selection in the KWIC view, then click"
				+ " and drag the Tag onto the KWIC view.");
		
		content.addComponent(lblInstructions);
		
		HorizontalSplitPanel tagLibraryPanel = new HorizontalSplitPanel();
		content.addComponent(tagLibraryPanel);
		content.setExpandRatio(tagLibraryPanel, 1.0f);
		tagLibraryPanel.setSizeFull();
		
		tagLibraryContainer = new HierarchicalContainer();
		tagLibraryContainer.addContainerProperty(SORTCAP_PROP, String.class, null);		

		tagLibraryTree = new Tree();
		tagLibraryTree.setContainerDataSource(tagLibraryContainer);
		tagLibraryTree.setWidth("100%");
		tagLibraryTree.setCaption("Tag Type Libraries");
		tagLibraryTree.addStyleName("bold-label-caption");
		tagLibraryTree.setImmediate(true);
		tagLibraryTree.setItemCaptionMode(ItemCaptionMode.ID);
		
		for (TagLibraryReference tlr : repository.getTagLibraryReferences()) {
			tagLibraryTree.addItem(tlr);
			tagLibraryTree.getItem(tlr).getItemProperty(SORTCAP_PROP).setValue(
					(tlr.toString()==null)?"":tlr.toString());
			tagLibraryTree.setChildrenAllowed(tlr, false);
		}
		tagLibraryContainer.sort(new Object[] {SORTCAP_PROP}, new boolean[] { true });
		
		tagLibraryTree.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				handleTagLibrariesTreeItemClick(event);				
			}
		});
		
		tagLibraryPanel.addComponent(tagLibraryTree);
		

		tagsetTree = new TagsetTree(
				repository.getTagManager(), null, false, false, false, false, false, null);
		tagsetTree.getTagTree().setDragMode(TableDragMode.ROW);
		
		tagLibraryPanel.addComponent(tagsetTree);
		
		content.setMargin(true);
		
		setContent(content);
	}
	
	private void handleTagLibrariesTreeItemClick(ValueChangeEvent event) {
		TagLibraryReference tagLibraryReference = (TagLibraryReference)event.getProperty().getValue();
		if (tagLibraryReference != null) {
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
		else {
			tagsetTree.setTagLibrary(null);
			tagLibrary = null;
		}
	}
	
	private MainMenu.MenuItemSelectedListener menuItemSelectedListener = new MainMenu.MenuItemSelectedListener() {		
		@Override
		public void menuItemSelected(MenuItemSelectedEvent event) {
			
			Component component = event.getComponent();			
			if (component instanceof AnalyzerManagerView) {
				return;
			}
			
			close();
		}
	};
	
	private void initListeners() {
		MainMenu menu = ((CatmaApplication)UI.getCurrent()).getMenu();
		menu.addMenuItemSelectedListener(menuItemSelectedListener);
		
		addCloseListener(new Window.CloseListener() {
			
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
	
	public void show(Float height, Float width, Unit lastTagResultsDialogUnit) {
		setPositionX(20);
		setPositionY(80);
		if (height != null) {
			setHeight(height, lastTagResultsDialogUnit);
		}
		else {
			setHeight("50%");
		}
		
		if (width != null) {
			setWidth(width, lastTagResultsDialogUnit);
		}
		else {
			setWidth("40%");
		}
		
		UI.getCurrent().addWindow(this);
	}
	
	public Object getCurrenTagLibraryTreeSelection() {
		return tagLibraryTree.getValue();
	}
	
	public Object getCurrentTagsetTreeSelection() {
		return tagsetTree.getTagTree().getValue();
	}

}
