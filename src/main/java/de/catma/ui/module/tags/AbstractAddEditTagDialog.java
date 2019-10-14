package de.catma.ui.module.tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ColorPicker;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.FocusHandler;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;

public abstract class AbstractAddEditTagDialog<T> extends AbstractOkCancelDialog<T> {

	protected ComboBox<TagsetDefinition> cbTagsets;
	private HorizontalLayout tagPanel;
	private HorizontalLayout propertyDefNamePanel;
	private HorizontalLayout propertyDefPanel;
	protected Grid<PropertyDefinition> propertyDefinitionGrid;
	private TextArea possibleValuesArea;
	private Button btAddProperty;
	private TextField tfPropertyDefName;
	protected IDGenerator idGenerator = new IDGenerator();
	protected ListDataProvider<PropertyDefinition> propertyDefDataProvider;
	protected ColorPicker colorPicker;
	protected TextField tfName;

	protected AbstractAddEditTagDialog(
			String dialogCaption, SaveCancelListener<T> saveCancelListener) {
		super(dialogCaption, saveCancelListener);
	}

	
	protected void initActions() {
		ButtonRenderer<PropertyDefinition> deletePropertyDefRenderer =
				new ButtonRenderer<>(clickEvent -> handleDeletePropertyDefRequest(clickEvent)); 
		deletePropertyDefRenderer.setHtmlContentAllowed(true);
		
		propertyDefinitionGrid.addColumn(
				propertyDef -> VaadinIcons.TRASH.getHtml(), 
				deletePropertyDefRenderer);
		
		tfPropertyDefName.addFocusListener(focusEvent -> btAddProperty.setClickShortcut(KeyCode.ENTER));
		tfPropertyDefName.addBlurListener(BlurEvent -> btAddProperty.removeClickShortcut());
		
		btAddProperty.addClickListener(
			clickEvent -> handleAddPropertyDefRequest(tfPropertyDefName.getValue()));

		propertyDefinitionGrid.addSelectionListener(
			selectionEvent -> handlePropertyDefSelection(selectionEvent));
		
		possibleValuesArea.addValueChangeListener(
			valueChangeEvent -> handlePossibleValuesChange(valueChangeEvent));
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

	private void handlePropertyDefSelection(SelectionEvent<PropertyDefinition> selectionEvent) {
		selectionEvent.getFirstSelectedItem().ifPresent(propertyDef -> setPropertyValues(propertyDef.getPossibleValueList()));
	}

	private void setPropertyValues(List<String> possibleValueList) {
		possibleValuesArea.setValue(possibleValueList.stream().collect(Collectors.joining(",")));
	}

	private void handleAddPropertyDefRequest(String name) {
		if (propertyDefDataProvider.getItems().isEmpty()) {
			setPropertyDefinitionsVisible();
		}
		
		if ((name != null) && !name.trim().isEmpty()) {
			tfPropertyDefName.clear();
			
			PropertyDefinition propertyDefinition = 
				new PropertyDefinition(
					idGenerator.generate(), name, Collections.emptyList());
			
			propertyDefDataProvider.getItems().add(propertyDefinition);
			propertyDefDataProvider.refreshAll();
			propertyDefinitionGrid.select(propertyDefinition);
			propertyDefinitionAdded(propertyDefinition);
		}
	}

	private void handleDeletePropertyDefRequest(RendererClickEvent<PropertyDefinition> clickEvent) {
		propertyDefDataProvider.getItems().remove(clickEvent.getItem());
		propertyDefDataProvider.refreshAll();
		propertyDefinitionRemoved(clickEvent.getItem());
	}
	
	protected void propertyDefinitionRemoved(PropertyDefinition propertyDefinition) {
		//noop
	}

	protected void propertyDefinitionAdded(PropertyDefinition propertyDefinition) {
		//noop
	}
	
	protected void initComponents(boolean allowPropertyDefEditing) {
		initComponents(Collections.emptyList(), Optional.empty(), allowPropertyDefEditing);
	}
	
	protected void initComponents(Collection<TagsetDefinition> availableTagsets,
			Optional<TagsetDefinition> preSelectedTagset, boolean allowPropertyDefEditing) {
		
		if (isWithTagsetSelection()) {
			cbTagsets = new ComboBox<TagsetDefinition>("Tagset", availableTagsets);
			cbTagsets.setItemCaptionGenerator(tagset -> tagset.getName());
			cbTagsets.setWidth("100%");
			cbTagsets.setDescription("The Tagset that will be the container of the new Tag.");
			cbTagsets.setEmptySelectionAllowed(false);
			preSelectedTagset.ifPresent(tagset -> cbTagsets.setValue(tagset));
		}		
		
		tagPanel = new HorizontalLayout();
		tagPanel.setSpacing(true);
		tagPanel.setWidth("100%");
		tagPanel.setMargin(new MarginInfo(false, true));
		
		tfName= new TextField();
		tfName.setPlaceholder("Tag Name");
		tagPanel.addComponent(tfName);
		
		int[] randomRGBColor = ColorConverter.getRandomColor();
		
		colorPicker = new ColorPicker(
				"Tag color", 
				new Color(randomRGBColor[0], randomRGBColor[1], randomRGBColor[2]));
		colorPicker.addStyleName(MaterialTheme.BUTTON_FLAT);
		colorPicker.addStyleName("inputfield-color-picker");
		
		colorPicker.setModal(true);
		
		tagPanel.addComponent(colorPicker);
		
		propertyDefNamePanel = new HorizontalLayout();
		propertyDefNamePanel.setSpacing(true);
		propertyDefNamePanel.setMargin(new MarginInfo(true, true, false, true));
		
		tfPropertyDefName = new TextField("Add Properties");
		tfPropertyDefName.setPlaceholder("Enter a Property name");
		tfPropertyDefName.setWidth("250px");
		propertyDefNamePanel.addComponent(tfPropertyDefName);
		
		btAddProperty = new Button("Add Property");
		btAddProperty.addStyleName(MaterialTheme.BUTTON_FLAT);
		propertyDefNamePanel.addComponent(btAddProperty);
		propertyDefNamePanel.setComponentAlignment(btAddProperty, Alignment.BOTTOM_LEFT);	
		
		propertyDefPanel = new HorizontalLayout();
		propertyDefPanel.setMargin(new MarginInfo(false, true));
		propertyDefPanel.setSpacing(true);
		propertyDefPanel.setSizeFull();
		propertyDefPanel.setVisible(false);

		TextField propertyNameField = new TextField();

		propertyDefinitionGrid = new Grid<PropertyDefinition>("Assigned Properties");
		propertyDefinitionGrid.addStyleName("flat-undecorated-icon-buttonrenderer");
		
		propertyDefinitionGrid.addColumn(
				propertyDef -> propertyDef.getName())
			.setEditorComponent(
				    propertyNameField, PropertyDefinition::setName)
			.setEditable(allowPropertyDefEditing);
		
		propertyDefinitionGrid.getEditor().setEnabled(allowPropertyDefEditing);
		
		propertyDefinitionGrid.setHeaderVisible(false);
		propertyDefinitionGrid.setWidth("99%");
		propertyDefinitionGrid.setHeight("100%");
		
		propertyDefDataProvider = 
				new ListDataProvider<PropertyDefinition>(new ArrayList<>());
		propertyDefinitionGrid.setDataProvider(propertyDefDataProvider);
		
		propertyDefPanel.addComponent(propertyDefinitionGrid);
		
		possibleValuesArea = new TextArea("Proposed Values");
		possibleValuesArea.setSizeFull();
		
		propertyDefPanel.addComponent(possibleValuesArea);
	}
	
	protected void setPropertyDefinitionsVisible() {
		propertyDefPanel.setVisible(true);
		if (propertyDefNamePanel.getParent() instanceof VerticalLayout) {
			((VerticalLayout) propertyDefNamePanel.getParent()).setExpandRatio(propertyDefNamePanel, 0f);
		}
				
		setHeight("80%");
		center();
		
	}

	@Override
	protected boolean isEnterClickShortcut() {
		return false;
	}
	
	@Override
	protected void layoutWindow() {
		setHeight("80%");
		setWidth("60%");
	}
	
	protected abstract boolean isWithTagsetSelection();

	@Override
	protected void addContent(ComponentContainer content) {
		if (isWithTagsetSelection()) {
			content.addComponent(cbTagsets);
		}
		content.addComponent(
			new Label(
				"Enter name, color and Properties (optional) of the Tag you want to add"));
		content.addComponent(tagPanel);
		content.addComponent(propertyDefNamePanel);
		content.addComponent(propertyDefPanel);
		if (content instanceof VerticalLayout) {
			((VerticalLayout) content).setExpandRatio(propertyDefNamePanel, 0.3f);
		}
		
		if (content instanceof VerticalLayout) {
			((VerticalLayout) content).setExpandRatio(propertyDefPanel, 1.0f);
		}

		if (!isWithTagsetSelection() || cbTagsets.getValue() != null) {
			((FocusHandler)UI.getCurrent()).focusDeferred(tfName);
		}
		else {
			((FocusHandler)UI.getCurrent()).focusDeferred(cbTagsets);
		}
	}

	@Override
	protected void handleOkPressed() {
		String name = tfName.getValue();
		
		if ((name == null) || name.isEmpty()) {
			Notification.show("Info", "Please enter the Tag's name!", Type.ERROR_MESSAGE);
		}
		else if (colorPicker.getValue() == null) {
			Notification.show("Info", "Please choose the Tag's color!", Type.ERROR_MESSAGE);
		}
		else if (isWithTagsetSelection() && cbTagsets.getValue() == null) {
			Notification.show("Info", "Please choose the Tag's Tagset!", Type.ERROR_MESSAGE);
		}
		else {
			super.handleOkPressed();
		}
	}
	
	@Override
	protected String getOkCaption() {
		return "Add Tag";
	}

}
