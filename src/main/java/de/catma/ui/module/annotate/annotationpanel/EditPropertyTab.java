package de.catma.ui.module.annotate.annotationpanel;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import de.catma.tag.Property;
import de.catma.ui.component.IconButton;

public class EditPropertyTab extends HorizontalLayout {

	private Property property;
	private Grid<String> valueGrid;
	private IconButton btAdd;
	private boolean changed = false;
	private TextArea adhocValueArea;
	private ListDataProvider<String> valueProvider;
	private List<String> proposedValueList;

	public EditPropertyTab(Property property, List<String> proposedValueList) {
		super();
		this.property = property;
		this.proposedValueList = proposedValueList;
		initComponents();
		initActions();
	}

	private void initActions() {
		valueGrid.addSelectionListener(selectionEvent -> changed = true);
		btAdd.addClickListener(clickEvent -> handleAddValueEvent());
	}

	private void handleAddValueEvent() {
		String value = adhocValueArea.getValue();
		if (value != null) {
			value = value.trim();
		}
		
		if (!value.isEmpty()) {
			if (!valueProvider.getItems().contains(value)) {
				valueProvider.getItems().add(value);
				valueProvider.refreshAll();
			}
			else {
				Notification.show(
					"Info", "This value is already available!", Type.HUMANIZED_MESSAGE);
			}
			valueGrid.select(value);
			adhocValueArea.setValue("");
		}
	}

	private void initComponents() {
		setSizeFull();
		setSpacing(true);
		setMargin(true);
		
		ArrayList<String> valueList = new ArrayList<>(proposedValueList);
		for (String value : property.getPropertyValueList()) {
			if (!valueList.contains(value)) {
				valueList.add(value);
			}
		}
		valueProvider = new ListDataProvider<>(valueList);
		valueGrid = new Grid<>("Assigned values", valueProvider);
		valueGrid.setSelectionMode(SelectionMode.MULTI);
		valueGrid.setSizeFull();
		
		valueGrid.asMultiSelect().selectItems(property.getPropertyValueList().toArray(new String[] {}));
		valueGrid.addColumn(value -> value.toString()).setCaption("Value");
		addComponent(valueGrid);
		
		VerticalLayout customValuePanel = new VerticalLayout();
		customValuePanel.setSizeFull();
		
		adhocValueArea = new TextArea("Add a custom value");
		adhocValueArea.setSizeFull();
		customValuePanel.addComponent(adhocValueArea);
		customValuePanel.setExpandRatio(adhocValueArea, 1.0f);
		
		btAdd = new IconButton(VaadinIcons.PLUS);
		btAdd.setDescription("Add another custom value");
		customValuePanel.addComponent(btAdd);
		customValuePanel.setComponentAlignment(btAdd, Alignment.TOP_RIGHT);
		
		addComponent(customValuePanel);
	}
	
	private boolean hasAdhocValue() {
		return (adhocValueArea.getValue() != null) 
				&& !adhocValueArea.getValue().isEmpty()
				&& !valueGrid.getSelectedItems().contains(adhocValueArea.getValue());
	}
	
	public List<String> getPropertyValues() {
		List<String> selectedValues = new ArrayList<>(valueGrid.getSelectedItems());
		
		if (hasAdhocValue()) {
			selectedValues.add(adhocValueArea.getValue());
			changed = true;
		}
		
		return selectedValues;
	}
	
	public boolean isChanged() {
		return changed || hasAdhocValue();
	}
	
	public Property getProperty() {
		return property;
	}
	
	public void addSelectionListener(SelectionListener<String> selectionListener) {
		valueGrid.addSelectionListener(selectionListener);
	}
}
