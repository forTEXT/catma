package de.catma.ui.module.analyze.visualization.kwic.annotation.edit;

import java.text.Collator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.GridSortOrder;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;

import de.catma.project.Project;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.DoubleTextInputDialog;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.dialog.wizard.ProgressStep;
import de.catma.ui.dialog.wizard.ProgressStepFactory;
import de.catma.ui.dialog.wizard.StepChangeListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.dialog.wizard.WizardStep;

public class PropertyActionSelectionStep extends VerticalLayout implements WizardStep {

	private ProgressStep progressStep;
	private Project project;
	private WizardContext context;
	private TreeGrid<ActionItem> actionGrid;
	private TreeData<ActionItem> actionData;
	private ActionGridComponent<TreeGrid<ActionItem>> actionGridComponent;
	private final Collator tagLibraryCollator;
	private TreeDataProvider<ActionItem> actionDataProvider;
	private StepChangeListener stepChangeListener;
	private Grid<TagDefinition> tagDefinitionGrid;
	private Map<String, TagsetDefinition> tagsetById;
	private Collection<TagDefinition> tags;

	
	@SuppressWarnings("unchecked")
	public PropertyActionSelectionStep(Project project, WizardContext context, ProgressStepFactory progressStepFactory) {
		this.project = project;
		this.context = context;
		this.progressStep = progressStepFactory.create(2, "Select property actions");
		this.tagLibraryCollator = Collator.getInstance(project.getTagManager().getTagLibrary().getLocale());
		
		Collection<String> propertyNames = (Collection<String>) context.get(EditAnnotationWizardContextKey.PROPERTY_NAMES);
		Collection<PropertyAction> preconfiguredActions = (Collection<PropertyAction>) context.get(EditAnnotationWizardContextKey.PROPERTY_ACTIONS);
		this.tags = (Collection<TagDefinition>) context.get(EditAnnotationWizardContextKey.TAGS);
		
		Collection<TagsetDefinition> affectedTagsets = (Collection<TagsetDefinition>) context.get(EditAnnotationWizardContextKey.TAGSETS);		
		this.tagsetById = affectedTagsets.stream().collect(Collectors.toMap(TagsetDefinition::getUuid, Function.identity()));

		
		initComponents();
		
		initData(propertyNames, preconfiguredActions);
	}

	private void initData(Collection<String> propertyNames, Collection<PropertyAction> preconfiguredActions) {
		Multimap<String, PropertyAction> actionsByName = ArrayListMultimap.create();
		preconfiguredActions.forEach(action -> actionsByName.put(action.propertyName(), action));
		
		actionData = new TreeData<ActionItem>();
		
		for (String propertyName : propertyNames.stream().sorted((n1, n2) -> tagLibraryCollator.compare(n1, n2)).toList()) {
			var nameItem = new PropertyNameItem(propertyName);
			actionData.addItem(null, nameItem);
			for (PropertyAction action : actionsByName.get(propertyName)) {
				actionData.addItem(nameItem, new PropertyActionItem(action));
			}
			
		}
		
		actionDataProvider = new TreeDataProvider<ActionItem>(actionData);
		
		actionGrid.setDataProvider(actionDataProvider);
		
		updateAffectedTags();
	}
	
	@Override
	public void enter(boolean back) {
		actionGrid.expand(actionData.getRootItems());
		stepChangeListener.stepChanged(this);
	}

	private void updateAffectedTags() {
		tagDefinitionGrid.setDataProvider(new ListDataProvider<TagDefinition>(
			actionData
			.getRootItems().stream()
			.flatMap(item -> actionData.getChildren(item).stream())
			.map(item -> ((PropertyActionItem)item).getPropertyAction())
			.flatMap(propertyAction -> 
				tags.stream()
				.filter(tag -> {
					PropertyDefinition propDef = tag.getPropertyDefinition(propertyAction.propertyName());
					
					switch (propertyAction.type()) {
					case ADD: return propDef != null;
					case REMOVE: return propDef != null && propDef.getPossibleValueList().contains(propertyAction.value());
					case REPLACE: return propDef != null && propDef.getPossibleValueList().contains(propertyAction.replaceValue());
					}
					
					return false;
				})
			)
			.collect(Collectors.toSet())));
	}


	private void initComponents() {
		setSizeFull();
    	actionGrid = TreeGridFactory.createDefaultTreeGrid();
    	actionGrid.setSizeFull();
    	
        actionGrid.addStyleNames(
				"flat-undecorated-icon-buttonrenderer");
        
        actionGrid.addColumn(actionItem -> actionItem.getPropertyName())
        .setCaption("Property name");
        
        actionGrid.addColumn(actionItem -> actionItem.getActionDescription())
        .setCaption("Action description").setWidth(500);

		ButtonRenderer<ActionItem> removeItemRenderer = 
				new ButtonRenderer<ActionItem>(clickEvent -> handleRemoveItemRequest(clickEvent));
		removeItemRenderer.setHtmlContentAllowed(true);
		
		actionGrid.addColumn(
			actionItem -> actionItem.getRemoveItemIcon(),
			removeItemRenderer).setExpandRatio(1).setDescriptionGenerator(item -> "Remove this action");

		ButtonRenderer<ActionItem> addValueActionRenderer = 
				new ButtonRenderer<ActionItem>(clickEvent -> handleAddValueActionRequest(clickEvent));
		addValueActionRenderer.setHtmlContentAllowed(true);
		
		actionGrid.addColumn(
			actionItem -> actionItem.getAddValueIcon(),
			addValueActionRenderer)
		.setDescriptionGenerator(item -> "Add a value from annotations with this property");

		ButtonRenderer<ActionItem> replaceValueActionRenderer = 
				new ButtonRenderer<ActionItem>(clickEvent -> handleReplaceValueActionRequest(clickEvent));
		replaceValueActionRenderer.setHtmlContentAllowed(true);

		actionGrid.addColumn(
				actionItem -> actionItem.getReplaceValueIcon(),
				replaceValueActionRenderer);

		ButtonRenderer<ActionItem> removeValueActionRenderer = 
				new ButtonRenderer<ActionItem>(clickEvent -> handleRemoveValueActionRequest(clickEvent));
		removeValueActionRenderer.setHtmlContentAllowed(true);
		actionGrid.addColumn(
			actionItem -> actionItem.getDeleteValueIcon(),
			removeValueActionRenderer
		).setDescriptionGenerator(item -> "Remove a value from annotations with this property");

        Label actionLabel = new Label("Configure actions for each Property. You can add, remove or replace values");

        
        actionGridComponent = new ActionGridComponent<TreeGrid<ActionItem>>(
                actionLabel,
                actionGrid
        );
        actionGridComponent.setSizeFull();
        addComponent(actionGridComponent);
        
		tagDefinitionGrid = new Grid<TagDefinition>("Affected Annotations");
		tagDefinitionGrid.addStyleName("flat-undecorated-icon-buttonrenderer");
		tagDefinitionGrid.addStyleName("property-action-selection-step-affected-tags-grid");
		tagDefinitionGrid.setSizeFull();

		var tagPathColumn = tagDefinitionGrid
			.addColumn(tag -> tagsetById.get(tag.getTagsetDefinitionUuid()).getTagPath(tag))
			.setSortable(true)
			.setComparator((tp1, tp2) -> tagLibraryCollator.compare(tagsetById.get(tp1.getTagsetDefinitionUuid()).getTagPath(tp1), tagsetById.get(tp2.getTagsetDefinitionUuid()).getTagPath(tp2)))
			.setCaption("Tag path");
		tagDefinitionGrid.setSortOrder(List.of(new GridSortOrder<>(tagPathColumn, SortDirection.ASCENDING)));
		
		addComponent(tagDefinitionGrid);

	}

	private void handleRemoveItemRequest(RendererClickEvent<ActionItem> clickEvent) {
		actionData.removeItem(clickEvent.getItem());
		actionDataProvider.refreshAll();
		updateAffectedTags();
		stepChangeListener.stepChanged(this);
	}

	private void handleRemoveValueActionRequest(RendererClickEvent<ActionItem> clickEvent) {
		SingleTextInputDialog dlg = 
				new SingleTextInputDialog(
						"Value removal", 
						"Please enter the value to be removed", 
						value -> {						
							var item = new PropertyActionItem(
									new PropertyAction(
											clickEvent.getItem().getPropertyName(), 
											PropertyActionType.REMOVE, 
											value, 
											null));
							actionData.addItem(
									clickEvent.getItem(), 
									item);
							actionDataProvider.refreshAll();
							updateAffectedTags();
							stepChangeListener.stepChanged(this);
						});
		dlg.show();
	}

	private void handleReplaceValueActionRequest(RendererClickEvent<ActionItem> clickEvent) {
		DoubleTextInputDialog dlg = 
				new DoubleTextInputDialog("Value replacement", "Please enter the value to be replaced", "and the replacement value", valuePair -> {
					String value = valuePair.getFirst();
					String replacement= valuePair.getSecond();
					if (value != null && !value.trim().isEmpty() && replacement != null && !replacement.trim().isEmpty()) {						
						var item = new PropertyActionItem(
								new PropertyAction(
										clickEvent.getItem().getPropertyName(),
										PropertyActionType.REPLACE,
										value,
										replacement));
						actionData.addItem(clickEvent.getItem(), item);
						actionDataProvider.refreshAll();
						updateAffectedTags();
						stepChangeListener.stepChanged(this);
					}
					else {
						Notification.show("Info", "The value and its replacement must not be empty", Type.HUMANIZED_MESSAGE);
					}
				});
		dlg.show();
	}

	private void handleAddValueActionRequest(RendererClickEvent<ActionItem> clickEvent) {
		SingleTextInputDialog dlg = 
				new SingleTextInputDialog(
						"Value addition", 
						"Please enter the value to be added", 
						value -> {	
							var item = new PropertyActionItem(
									new PropertyAction(
											clickEvent.getItem().getPropertyName(), 
											PropertyActionType.ADD, 
											value, 
											null));
							actionData.addItem(
									clickEvent.getItem(), 
									item);
							actionDataProvider.refreshAll();
							updateAffectedTags();
							stepChangeListener.stepChanged(this);
						});
		dlg.show();
	}

	@Override
	public ProgressStep getProgressStep() {
		return this.progressStep;
	}

	@Override
	public WizardStep getNextStep() {
		return null; // no next step
	}

	@Override
	public boolean isValid() {
		return actionData
				.getRootItems().stream()
				.flatMap(item -> actionData.getChildren(item).stream()).findAny().isPresent();

	}

	@Override
	public void setStepChangeListener(StepChangeListener stepChangeListener) {
		this.stepChangeListener = stepChangeListener;
	}
	
	@Override
	public boolean canNext() {
		return false;
	}
	
	@Override
	public boolean canFinish() {
		return true;
	}
	
	@Override
	public void exit(boolean back) {
		if (back) {
			context.put(EditAnnotationWizardContextKey.PROPERTY_ACTIONS, Collections.EMPTY_LIST);
		}
		else {
			context.put(EditAnnotationWizardContextKey.PROPERTY_ACTIONS, actionData
				.getRootItems().stream()
				.flatMap(item -> actionData.getChildren(item).stream())
				.toList());
		}
	}
}
