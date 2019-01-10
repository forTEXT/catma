package de.catma.ui.tagger.annotationpanel;

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
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;

import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;

public class AddTagDialog extends AbstractOkCancelDialog<TagDefinition> {

	private Optional<TagsetDefinition> preSelectedTagset;
	private Collection<TagsetDefinition> availableTagsets;
	private ComboBox<TagsetDefinition> cbTagsets;
	private HorizontalLayout tagPanel;
	private HorizontalLayout propertyDefNamePanel;
	private HorizontalLayout propertyDefPanel;
	private Grid<PropertyDefinition> propertyDefinitionGrid;
	private TextArea possibleValuesArea;
	private Button btAddProperty;
	private TextField tfPropertyDefName;
	private IDGenerator idGenerator = new IDGenerator();
	private ListDataProvider<PropertyDefinition> propertyDefDataProvider;
	
	public AddTagDialog(
			Collection<TagsetDefinition> availableTagsets, 
			Optional<TagsetDefinition> preSelectedTagset, 
			SaveCancelListener<TagDefinition> saveCancelListener) {
		super("Add Tag", saveCancelListener);
		this.availableTagsets = availableTagsets;
		this.preSelectedTagset = preSelectedTagset;
		initComponents(availableTagsets, preSelectedTagset);
		initActions();
	}
	
	private void initActions() {
		ButtonRenderer<PropertyDefinition> deletePropertyDefRenderer =
				new ButtonRenderer<>(clickEvent -> handlePropertyDefDeleteRequest(clickEvent)); 
		deletePropertyDefRenderer.setHtmlContentAllowed(true);
		
		propertyDefinitionGrid.addColumn(
				propertyDef -> VaadinIcons.TRASH.getHtml(), 
				deletePropertyDefRenderer);
		
		btAddProperty.addClickListener(
			clickEvent -> handleAddPropertyDefinition(tfPropertyDefName.getValue()));

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

	private void handleAddPropertyDefinition(String name) {
		if ((name != null) && !name.trim().isEmpty()) {
			tfPropertyDefName.clear();
			
			PropertyDefinition propertyDefinition = 
				new PropertyDefinition(
					idGenerator.generate(), name, Collections.emptyList());
			
			propertyDefDataProvider.getItems().add(propertyDefinition);
			propertyDefDataProvider.refreshAll();
		}
	}

	private void handlePropertyDefDeleteRequest(RendererClickEvent<PropertyDefinition> clickEvent) {
		// TODO Auto-generated method stub
	}

	private void initComponents(Collection<TagsetDefinition> availableTagsets,
			Optional<TagsetDefinition> preSelectedTagset) {
		cbTagsets = new ComboBox<TagsetDefinition>("Tagset", availableTagsets);
		cbTagsets.setItemCaptionGenerator(tagset -> tagset.getName());
		cbTagsets.setWidth("100%");
		cbTagsets.setDescription("The Tagset that will be the container of the new Tag.");
		cbTagsets.setEmptySelectionAllowed(false);
		preSelectedTagset.ifPresent(tagset -> cbTagsets.setValue(tagset));
		
		
		tagPanel = new HorizontalLayout();
		tagPanel.setSpacing(true);
		tagPanel.setWidth("100%");
		tagPanel.setMargin(new MarginInfo(false, true));
		
		TextField tfName= new TextField();
		tfName.setPlaceholder("Tag Name");
		tagPanel.addComponent(tfName);
		
		int[] randomRGBColor = ColorConverter.getRandomColor();
		
		ColorPicker colorPicker = new ColorPicker(
				"Tag color", 
				new Color(randomRGBColor[0], randomRGBColor[1], randomRGBColor[2]));
		colorPicker.addStyleName(MaterialTheme.BUTTON_FLAT);
		colorPicker.addStyleName("inputfield-color-picker");
		
		colorPicker.setModal(true);
		
		tagPanel.addComponent(colorPicker);
		
		propertyDefNamePanel = new HorizontalLayout();
		propertyDefNamePanel.setSpacing(true);
		propertyDefNamePanel.setMargin(new MarginInfo(false, true));

		tfPropertyDefName = new TextField("Add Property");
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
		propertyDefinitionGrid.addColumn(propertyDef -> propertyDef.getName());
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

	@Override
	protected boolean isEnterClickShortcut() {
		return false;
	}
	
	@Override
	protected void layoutWindow() {
		setHeight("80%");
		setWidth("60%");
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(cbTagsets);
		content.addComponent(
			new Label(
				"Enter name, color and properties (optional) of the Tag you want to add"));
		content.addComponent(tagPanel);
		content.addComponent(propertyDefNamePanel);
		content.addComponent(propertyDefPanel);
		if (content instanceof VerticalLayout) {
			((VerticalLayout) content).setExpandRatio(propertyDefPanel, 1.0f);
		}

	}

	
	@Override
	protected String getOkCaption() {
		return "Add Tag";
	}
	
	@Override
	protected TagDefinition getResult() {
		// TODO Auto-generated method stub
		return null;
	}

}
