package de.catma.ui.module.tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;

import de.catma.tag.PropertyDefinition;
import de.catma.ui.FocusHandler;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.util.IDGenerator;

public class AddEditPropertyDialog extends AbstractOkCancelDialog<List<PropertyDefinition>> {

	private HorizontalLayout propertyDefNamePanel;
	private TextField tfPropertyDefName;
	private Button btAddProperty;
	private HorizontalLayout propertyDefPanel;
	private Grid<PropertyDefinition> propertyDefinitionGrid;
	private ListDataProvider<PropertyDefinition> propertyDefDataProvider;
	private TextArea possibleValuesArea;
	private IDGenerator idGenerator = new IDGenerator();
	private Label inputLabel;

	public AddEditPropertyDialog(boolean bulkEdit, List<PropertyDefinition> commonPropertyDefs, 
			SaveCancelListener<List<PropertyDefinition>> saveCancelListener) {
		super("Edit " + (bulkEdit?"common ":"") +"Properties", saveCancelListener);
		initComponents(bulkEdit, commonPropertyDefs);
		initActions();
	}

	private void initActions() {
		ButtonRenderer<PropertyDefinition> deletePropertyDefRenderer =
				new ButtonRenderer<>(clickEvent -> handlePropertyDefDeleteRequest(clickEvent)); 
		deletePropertyDefRenderer.setHtmlContentAllowed(true);
		
		propertyDefinitionGrid.addColumn(
				propertyDef -> VaadinIcons.TRASH.getHtml(), 
				deletePropertyDefRenderer);
		
		tfPropertyDefName.addFocusListener(focusEvent -> btAddProperty.setClickShortcut(KeyCode.ENTER));
		tfPropertyDefName.addBlurListener(BlurEvent -> btAddProperty.removeClickShortcut());
		
		btAddProperty.addClickListener(
			clickEvent -> handleAddPropertyDefinition(tfPropertyDefName.getValue()));

		propertyDefinitionGrid.addSelectionListener(
			selectionEvent -> handlePropertyDefSelection(selectionEvent));
		
		possibleValuesArea.addValueChangeListener(
			valueChangeEvent -> handlePossibleValuesChange(valueChangeEvent));
	}
	
	private void handlePropertyDefSelection(SelectionEvent<PropertyDefinition> selectionEvent) {
		selectionEvent.getFirstSelectedItem().ifPresent(
			propertyDef -> setPropertyValues(propertyDef.getPossibleValueList()));
	}
	
	private void handlePossibleValuesChange(ValueChangeEvent<String> valueChangeEvent) {
		String values = valueChangeEvent.getValue();
		
		propertyDefinitionGrid.getSelectedItems().stream().findFirst().ifPresent(
				propertyDef -> setPossibleValues(
						propertyDef, Arrays.asList(values.split(Pattern.quote(",")))));

	}
	
	private void setPossibleValues(PropertyDefinition propertyDef, List<String> possibleValues) {
		propertyDef.setPossibleValueList(
			possibleValues
			.stream()
			.map(value -> value.trim())
			.filter(value -> !value.isEmpty())
			.collect(Collectors.toList()));
	}
	
	private void setPropertyValues(List<String> possibleValueList) {
		possibleValuesArea.setValue(possibleValueList.stream().collect(Collectors.joining(",")));
	}
	
	private void handlePropertyDefDeleteRequest(RendererClickEvent<PropertyDefinition> clickEvent) {
		propertyDefDataProvider.getItems().remove(clickEvent.getItem());
		propertyDefDataProvider.refreshAll();
	}
	
	private void handleAddPropertyDefinition(String name) {
		
		if ((name != null) && !name.trim().isEmpty()) {
			tfPropertyDefName.clear();
			
			PropertyDefinition propertyDefinition = 
				new PropertyDefinition(
					idGenerator.generate(), name, Collections.emptyList());
			
			propertyDefDataProvider.getItems().add(propertyDefinition);
			propertyDefDataProvider.refreshAll();
			propertyDefinitionGrid.select(propertyDefinition);
		}
	}
	
	private void initComponents(boolean bulkEdit, List<PropertyDefinition> commonPropertyDefs) {
		inputLabel = new Label(
				bulkEdit?"Edit the common Properties of the selected Tags":
					"Edit the Properties of the selected Tag");
		
		propertyDefNamePanel = new HorizontalLayout();
		propertyDefNamePanel.setSpacing(true);
		propertyDefNamePanel.setMargin(new MarginInfo(true, true, false, true));
		
		tfPropertyDefName = new TextField("Add Properties");
		tfPropertyDefName.setPlaceholder("Property Name");
		propertyDefNamePanel.addComponent(tfPropertyDefName);
		
		btAddProperty = new Button("Add Property");
		btAddProperty.addStyleName(MaterialTheme.BUTTON_FLAT);
		propertyDefNamePanel.addComponent(btAddProperty);
		propertyDefNamePanel.setComponentAlignment(btAddProperty, Alignment.BOTTOM_LEFT);	
		
		propertyDefPanel = new HorizontalLayout();
		propertyDefPanel.setMargin(new MarginInfo(false, true));
		propertyDefPanel.setSpacing(true);
		propertyDefPanel.setSizeFull();

		propertyDefinitionGrid = new Grid<PropertyDefinition>("Assigned Properties");
		propertyDefinitionGrid.addStyleName("flat-undecorated-icon-buttonrenderer");
		TextField propertyNameField = new TextField();
		
		propertyDefinitionGrid.addColumn(
			propertyDef -> propertyDef.getName())
		.setEditorComponent(
			    propertyNameField, PropertyDefinition::setName)
		.setEditable(!bulkEdit);

		propertyDefinitionGrid.getEditor().setEnabled(!bulkEdit);
		propertyDefinitionGrid.setHeaderVisible(false);
		propertyDefinitionGrid.setWidth("99%");
		propertyDefinitionGrid.setHeight("100%");
		
		propertyDefDataProvider = 
				new ListDataProvider<PropertyDefinition>(new ArrayList<>(commonPropertyDefs));
		propertyDefinitionGrid.setDataProvider(propertyDefDataProvider);
		
		propertyDefPanel.addComponent(propertyDefinitionGrid);
		
		possibleValuesArea = new TextArea("Proposed Values");
		possibleValuesArea.setSizeFull();
		
		propertyDefPanel.addComponent(possibleValuesArea);
	}


	@Override
	protected List<PropertyDefinition> getResult() {

		List<PropertyDefinition> propertyDefs = new ArrayList<>();
		
		for (PropertyDefinition propertyDefinition : propertyDefDataProvider.getItems()) {
			propertyDefs.add(propertyDefinition);
		}		
		
		return propertyDefs;
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(inputLabel);
		content.addComponent(propertyDefNamePanel);
		content.addComponent(propertyDefPanel);
		if (content instanceof VerticalLayout) {
			((VerticalLayout) content).setExpandRatio(propertyDefNamePanel, 0.3f);
		}
		
		if (content instanceof VerticalLayout) {
			((VerticalLayout) content).setExpandRatio(propertyDefPanel, 1.0f);
		}

		((FocusHandler)UI.getCurrent()).focusDeferred(tfPropertyDefName);
	}
}
