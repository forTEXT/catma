package de.catma.ui.module.tags;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.provider.GridSortOrder;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;

import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.module.tags.BulkEditPropertyByNameDialog.Result;

public class BulkEditPropertyByNameDialog extends AbstractOkCancelDialog<Result> {
	
	record Result(Collection<PropertyNameItem> modifiedItems, Collection<PropertyNameItem> deletedItems) {};
	
	class PropertyNameItem {
		private String newName;
		private final String oldName;
		private final List<TagDefinition> tags;
		private List<String> possibleValues;
		
		public PropertyNameItem(String name, List<TagDefinition> tags, Collator tagsetCollator) {
			super();
			this.oldName = name;
			this.newName = name;
			this.tags = tags;
			
			this.possibleValues = tags.stream().flatMap(tag -> tag.getPropertyDefinition(name).getPossibleValueList().stream()).distinct().toList();
		}

		public List<String> getPossibleValueList() {
			return possibleValues;
		}
		public String getName() {
			return newName;
		}
		
		public void setName(String name) {
			this.newName = name;
		}
		
		public String getOldName() {
			return oldName;
		}

		public void setPossibleValueList(List<String> possibleValues) {
			this.possibleValues = possibleValues;
		}

		public List<TagDefinition> getTags() {
			return tags;
		}
		
		public boolean isModified() {
			return ! newName.equals(oldName) 
					|| ! tags.stream().flatMap(tag -> tag.getPropertyDefinition(oldName).getPossibleValueList().stream()).distinct().collect(Collectors.toSet())
						.equals(possibleValues.stream().collect(Collectors.toSet()));
		}
	}

	private HorizontalLayout propertyDefPanel;
	private Grid<TagDefinition> tagDefinitionGrid;
	private Grid<PropertyNameItem> propertyNameGrid;
	private ListDataProvider<PropertyNameItem> propertyNameDataProvider;
	private TextArea possibleValuesArea;
	private Label inputLabel;
	private final Collator tagsetCollator;
	private final List<PropertyNameItem> deletedItems;
	private final Map<String,TagsetDefinition> tagsetById; 

	public BulkEditPropertyByNameDialog(Set<TagsetDefinition> affectedTagsets, List<String> selectedPropertyDefNames, Collator tagLibaryCollator,
			SaveCancelListener<Result> saveCancelListener) {
		super("Edit Properties", saveCancelListener);
		this.tagsetCollator = tagLibaryCollator;
		this.deletedItems = new ArrayList<>();
		this.tagsetById = affectedTagsets.stream().collect(Collectors.toMap(TagsetDefinition::getUuid, Function.identity()));


		initComponents(affectedTagsets);
		initActions();
		initData(selectedPropertyDefNames);
	}

	private void initData(List<String> propertyDefNames) {
		ArrayList<PropertyNameItem> data = 
				propertyDefNames
				.stream()
				.map(name -> 
					new PropertyNameItem(
						name, 
						tagsetById.values().stream().flatMap(TagsetDefinition::stream).filter(tag -> tag.getPropertyDefinition(name)!= null).toList(),
						tagsetCollator))
				.sorted((i1,i2) -> tagsetCollator.compare(i1.getName(), i2.getName()))
				.collect(Collectors.toCollection(() -> new ArrayList<>()));
		
		
		propertyNameDataProvider = 
				new ListDataProvider<PropertyNameItem>(data);
		propertyNameGrid.setDataProvider(propertyNameDataProvider);
		
		tagDefinitionGrid.setDataProvider(new ListDataProvider<TagDefinition>(List.of()));
		propertyNameGrid.select(data.getFirst());
	}

	private void initActions() {
		ButtonRenderer<PropertyNameItem> deletePropertyDefRenderer =
				new ButtonRenderer<>(clickEvent -> handlePropertyDefDeleteRequest(clickEvent)); 
		deletePropertyDefRenderer.setHtmlContentAllowed(true);
		
		propertyNameGrid.addColumn(
				propertyNameItem -> VaadinIcons.TRASH.getHtml(), 
				deletePropertyDefRenderer);
		
		propertyNameGrid.addSelectionListener(
			selectionEvent -> handlePropertyNameSelection(selectionEvent));
		
		possibleValuesArea.addValueChangeListener(
			valueChangeEvent -> handlePossibleValuesChange(valueChangeEvent));
	}
	
	private void handlePropertyNameSelection(SelectionEvent<PropertyNameItem> selectionEvent) {
		selectionEvent.getFirstSelectedItem().ifPresent(
			propertyNameItem -> { 
				setPropertyValues(propertyNameItem.getPossibleValueList());
				setTags(propertyNameItem.getTags());
			});
	}
	
	private void setTags(List<TagDefinition> tags) {
		tagDefinitionGrid.setDataProvider(new ListDataProvider<TagDefinition>(tags));
	}

	private void handlePossibleValuesChange(ValueChangeEvent<String> valueChangeEvent) {
		String values = valueChangeEvent.getValue();
		
		propertyNameGrid.getSelectedItems().stream().findFirst().ifPresent(
				propertyNameItem -> setPossibleValues(
						propertyNameItem, Arrays.asList(values.split(Pattern.quote(",")))));

	}
	
	private void setPossibleValues(PropertyNameItem propertyNameItem, List<String> possibleValues) {
		possibleValuesArea.setEnabled(true);
		propertyNameItem.setPossibleValueList(
			possibleValues
			.stream()
			.map(value -> value.trim())
			.filter(value -> !value.isEmpty())
			.collect(Collectors.toList()));
	}
	
	private void setPropertyValues(List<String> possibleValueList) {
		possibleValuesArea.setValue(possibleValueList.stream().sorted(tagsetCollator).collect(Collectors.joining(",")));
	}
	
	private void handlePropertyDefDeleteRequest(RendererClickEvent<PropertyNameItem> clickEvent) {
		PropertyNameItem item = clickEvent.getItem();
		propertyNameDataProvider.getItems().remove(item);
		propertyNameDataProvider.refreshAll();
		deletedItems.add(item);
	}

	
	private void initComponents(Set<TagsetDefinition> tagsets) {
		inputLabel = new Label(
				String.format(
						"Edit the properties across all tags in tagset%s %s",
						tagsets.size()==1?"":"s",
						tagsets.stream()
							.sorted((t1,t2) -> tagsetCollator.compare(t1.getName(), t2.getName()))
							.map(tagset -> String.format("\"%s\"", tagset.getName()))
							.collect(Collectors.joining(","))));
		
		propertyDefPanel = new HorizontalLayout();
		propertyDefPanel.setMargin(new MarginInfo(false, true));
		propertyDefPanel.setSpacing(true);
		propertyDefPanel.setSizeFull();

		propertyNameGrid = new Grid<PropertyNameItem>("Property names");
		propertyNameGrid.addStyleName("flat-undecorated-icon-buttonrenderer");
		TextField propertyNameField = new TextField();
		
		propertyNameGrid.addColumn(
			propertyNameItem -> propertyNameItem.getName())
		.setEditorComponent(
			    propertyNameField, PropertyNameItem::setName)
		.setEditable(true);

		propertyNameGrid.getEditor().setEnabled(true);
		propertyNameGrid.setHeaderVisible(false);
		propertyNameGrid.setWidth("99%");
		propertyNameGrid.setHeight("100%");
				
		propertyDefPanel.addComponent(propertyNameGrid);
		
		possibleValuesArea = new TextArea("Proposed Values");
		possibleValuesArea.setPlaceholder("You can add multiple comma separated values: value1, value2, ...");
		possibleValuesArea.setSizeFull();
		possibleValuesArea.setEnabled(false);
		
		propertyDefPanel.addComponent(possibleValuesArea);
		
		tagDefinitionGrid = new Grid<TagDefinition>("Affected Tags");
		tagDefinitionGrid.addStyleName("flat-undecorated-icon-buttonrenderer");
		tagDefinitionGrid.setHeight("100%");
		tagDefinitionGrid.setWidth("99%");
		var tagPathColumn = tagDefinitionGrid
			.addColumn(tag -> tagsetById.get(tag.getTagsetDefinitionUuid()).getTagPath(tag))
			.setSortable(true)
			.setComparator((tp1, tp2) -> tagsetCollator.compare(tagsetById.get(tp1.getTagsetDefinitionUuid()).getTagPath(tp1), tagsetById.get(tp2.getTagsetDefinitionUuid()).getTagPath(tp2)))
			.setCaption("Tag path");
		tagDefinitionGrid.setSortOrder(List.of(new GridSortOrder<>(tagPathColumn, SortDirection.ASCENDING)));
		propertyDefPanel.addComponent(tagDefinitionGrid);

	}


	@Override
	protected Result getResult() {
		return new Result(
				propertyNameDataProvider.getItems().stream().filter(PropertyNameItem::isModified).toList(), 
				deletedItems);
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(inputLabel);
		content.addComponent(propertyDefPanel);
		
		if (content instanceof VerticalLayout) {
			((VerticalLayout) content).setExpandRatio(propertyDefPanel, 1.0f);
		}
	}
	
	@Override
	protected void layoutWindow() {
		setHeight("80%");
		setWidth("70%");
	}
}
