package de.catma.ui.module.annotate.annotationpanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.base.Objects;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializableComparator;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.grid.ScrollDestination;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.DescriptionGenerator;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.annotation.Annotation;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionManager;
import de.catma.document.source.SourceDocumentReference;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.project.Project.ProjectEvent;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.BeyondResponsibilityConfirmDialog;
import de.catma.ui.dialog.BeyondResponsibilityConfirmDialog.Action;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.module.annotate.annotationpanel.AnnotatedTextProvider.ContextSizeEditCommand;
import de.catma.ui.module.main.ErrorHandler;

public class AnnotationDetailsPanel extends VerticalLayout {
	private final static SerializableComparator<AnnotationTreeItem> ANNOTATION_TREE_ITEM_COMPARATOR_ASC = (t1, t2) -> t1.compareTo(t2);
	private final static SerializableComparator<AnnotationTreeItem> ANNOTATION_TREE_ITEM_BY_TAG_COMPARATOR_ASC = (t1, t2) -> t1.compareToByTag(t2);
	private final static SerializableComparator<AnnotationTreeItem> ANNOTATION_TREE_ITEM_BY_TAGPATH_COMPARATOR_ASC = (t1, t2) -> t1.compareToByTagPath(t2);
	private final static SerializableComparator<AnnotationTreeItem> ANNOTATION_TREE_ITEM_BY_AUTHOR_COMPARATOR_ASC = (t1, t2) -> t1.compareToByAuthor(t2);
	private final static SerializableComparator<AnnotationTreeItem> ANNOTATION_TREE_ITEM_BY_COLLECTION_COMPARATOR_ASC = (t1, t2) -> t1.compareToByCollection(t2);
	private final static SerializableComparator<AnnotationTreeItem> ANNOTATION_TREE_ITEM_BY_TAGSET_COMPARATOR_ASC = (t1, t2) -> t1.compareToByTagset(t2);
	private Project project;
	private AnnotationCollectionManager collectionManager;
	private TreeGrid<AnnotationTreeItem> annotationDetailsTree;
	private TreeData<AnnotationTreeItem> annotationDetailData;
	private TreeDataProvider<AnnotationTreeItem> annotationDetailsProvider;
	private Button btMinimize;
	private KwicProvider kwicProvider;
	private PropertyChangeListener annotationPropertiesChangedListener;
	private Consumer<String> annotationSelectionListener;
	private String lastSelectedAnnotationId = null;
	private IconButton btClearSelected;
	private PropertyChangeListener propertyDefinitionChangedListener;
	private PropertyChangeListener tagChangedListener;
	private Function<String, Boolean> isCurrentEditedCollection; //takes a collectionId and returns true if this is the collection currently being edited
	private Consumer<String> changeCollectionListener; //takes a target collectionId to switch to 
	private final ContextSizeEditCommand contextSizeEditCommand;
	private final Supplier<Integer> contextSizeSupplier;
	private final Collator collator;

	public AnnotationDetailsPanel(
		Project project, AnnotationCollectionManager collectionManager, 
		Consumer<String> annotationSelectionListener,
		Function<String, Boolean> isCurrentEditedCollection,
		Consumer<String> changeCollectionListener) {
		this.project = project;
		this.collectionManager = collectionManager;
		this.annotationSelectionListener = annotationSelectionListener;
		this.isCurrentEditedCollection = isCurrentEditedCollection;
		this.changeCollectionListener = changeCollectionListener;
		this.contextSizeEditCommand = new ContextSizeEditCommand((newContextSize) -> { 
			refreshAnnotationDetailDescriptions();
		});
		this.collator = Collator.getInstance(project.getTagManager().getTagLibrary().getLocale());
		contextSizeSupplier = () -> contextSizeEditCommand.getContextSize();
		initComponents();
		initActions();
		initListeners();
	}
	
	private void refreshAnnotationDetailDescriptions() {
		annotationDetailData.getRootItems().forEach(AnnotationTreeItem::refreshDescription);
		annotationDetailsProvider.refreshAll();
	}

	private void initActions() {
		annotationDetailsTree.addSelectionListener(
			selectionEvent -> handleSelectAnnotationRequest(selectionEvent));
		
		btClearSelected.addClickListener(clickEvent -> handleClearSelected());
	}

	private void handleClearSelected() {
		fireAnnotationSelectedEvent(""); //none
		annotationDetailData.clear();
		annotationDetailsProvider.refreshAll();
	}

	private void handleSelectAnnotationRequest(SelectionEvent<AnnotationTreeItem> selectionEvent) {
		if (selectionEvent.getAllSelectedItems().size() == 1) {
			selectionEvent.getFirstSelectedItem().ifPresent(
				annotationTreeItem -> fireAnnotationSelectedEvent(annotationTreeItem.getAnnotationId()));
		}
	}

	private void fireAnnotationSelectedEvent(String annotationId) {
		if (!Objects.equal(annotationId, lastSelectedAnnotationId)) {
			lastSelectedAnnotationId = annotationId;
			annotationSelectionListener.accept(annotationId);
		}
	}

	private void initListeners() {
		annotationPropertiesChangedListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				
				TagInstance tagInstance = (TagInstance) evt.getOldValue();
				
				findAnnotationDataItem(tagInstance.getUuid())
				.ifPresent(annotationDataItem -> {
					Annotation annotation = annotationDataItem.getAnnotation();
					annotationDetailData.removeItem(annotationDataItem);
					try {
						addAnnotation(annotation);
					} catch (Exception e) {
						((ErrorHandler) UI.getCurrent()).showAndLogError("Error adding annotation", e);
					}
				});
				
			}
		};
		project.addEventListener(
				ProjectEvent.propertyValueChanged,
				annotationPropertiesChangedListener);
		
		propertyDefinitionChangedListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				refreshAnnotations();
			}
		};
		
		project.getTagManager().addPropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged, 
				propertyDefinitionChangedListener);			
		
		tagChangedListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getOldValue() != null) { // update or deletion					
					refreshAnnotations();
				}
			}
		};
		
		project.getTagManager().addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged, 
				tagChangedListener);
		
	}

	private void refreshAnnotations() {
		Collection<Annotation> annotations = annotationDetailData.getRootItems()
				.stream()
				.map(annotationTreeItem -> ((AnnotationDataItem)annotationTreeItem).getAnnotation())
				.collect(Collectors.toList());
			
		annotationDetailData.clear();
		
		try {
			addAnnotations(annotations);
		} catch (IOException e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Error refreshing annotations", e);
		}
	}

	private Optional<AnnotationDataItem> findAnnotationDataItem(String annotationId) {

		for (AnnotationTreeItem annotationTreeItem : annotationDetailData.getRootItems()) {
			if (annotationTreeItem.getAnnotationId().equals(annotationId)) {
				return Optional.of((AnnotationDataItem)annotationTreeItem);
			}
		}
		return Optional.empty();
	}

	public void setDocument(SourceDocumentReference sdRef) throws Exception {
		this.kwicProvider = new KwicProvider(project.getSourceDocument(sdRef.getUuid()), sdRef);
		handleClearSelected();
	}
	
	public void refreshAnnotationDetailsProvider() {
		annotationDetailsProvider.refreshAll();
	}
	
	private void initComponents() {
		setSpacing(true);
		setMargin(false);
		
		setSizeFull();
		addStyleName("annotation-details-panel");
		
		HorizontalLayout headerPanel = new HorizontalLayout();
		headerPanel.setWidth("100%");
		addComponent(headerPanel);
		
		btClearSelected = new IconButton(VaadinIcons.ERASER);
		btClearSelected.setDescription("Clear the list of selected annotations");
		headerPanel.addComponent(btClearSelected);
		headerPanel.setComponentAlignment(btClearSelected, Alignment.TOP_RIGHT);
		headerPanel.setExpandRatio(btClearSelected, 1.0f);
		
		btMinimize = new IconButton(VaadinIcons.ANGLE_DOUBLE_DOWN);
		headerPanel.addComponent(btMinimize);
		headerPanel.setComponentAlignment(btMinimize, Alignment.TOP_RIGHT);
		
		annotationDetailData = new TreeData<>();
		annotationDetailsProvider = new TreeDataProvider<>(annotationDetailData);
		annotationDetailsTree = TreeGridFactory.createDefaultGrid(annotationDetailsProvider);
		annotationDetailsTree.setSizeFull();
		annotationDetailsTree.addStyleNames(
			"annotation-details-panel-annotation-details-grid", 
			"flat-undecorated-icon-buttonrenderer");

		annotationDetailsTree.addColumn(annotationTreeItem -> 
			annotationTreeItem.getDetail(),
			new HtmlRenderer())
		.setCaption("Annotation")
		.setWidth(200)
		.setSortable(true)
		.setComparator(ANNOTATION_TREE_ITEM_COMPARATOR_ASC);
		
		annotationDetailsTree.addColumn(
			annotationTreeItem -> annotationTreeItem.getTagName())
		.setCaption("Tag")
		.setSortable(true)
		.setComparator(ANNOTATION_TREE_ITEM_BY_TAG_COMPARATOR_ASC)
		.setWidth(200);
		
		annotationDetailsTree.addColumn(
				annotationTreeItem -> annotationTreeItem.getTagPath())
		.setCaption("Tag Path")
		.setHidable(true)
		.setHidden(true)
		.setSortable(true)
		.setComparator(ANNOTATION_TREE_ITEM_BY_TAGPATH_COMPARATOR_ASC)
		.setWidth(200);
		
		annotationDetailsTree.addColumn(
			annotationTreeItem -> annotationTreeItem.getAuthor())
		.setCaption("Author")
		.setSortable(true)
		.setComparator(ANNOTATION_TREE_ITEM_BY_AUTHOR_COMPARATOR_ASC)
		.setWidth(60);
		
		annotationDetailsTree.addColumn(
			annotationTreeItem -> annotationTreeItem.getCollectionName())
		.setCaption("Collection")
		.setSortable(true)
		.setComparator(ANNOTATION_TREE_ITEM_BY_COLLECTION_COMPARATOR_ASC)
		.setWidth(100);
		
		annotationDetailsTree.addColumn(
				annotationTreeItem -> annotationTreeItem.getTagsetName())
		.setCaption("Tagset")
		.setSortable(true)
		.setComparator(ANNOTATION_TREE_ITEM_BY_TAGSET_COMPARATOR_ASC)
		.setWidth(100);
		
		annotationDetailsTree.addColumn(
				annotationTreeItem -> annotationTreeItem.getAnnotationId())
		.setCaption("Annotation ID")
		.setHidable(true)
		.setHidden(true)
		.setSortable(false)
		.setWidth(100);
		
		ButtonRenderer<AnnotationTreeItem> editAnnotationRenderer = 
			new ButtonRenderer<AnnotationTreeItem>(clickEvent -> handleEditAnnotationRequest(clickEvent));
		editAnnotationRenderer.setHtmlContentAllowed(true);
		
		annotationDetailsTree.addColumn(
			annotationTreeItem -> annotationTreeItem.getEditIcon(),
			editAnnotationRenderer)
		.setWidth(60);

		ButtonRenderer<AnnotationTreeItem> deleteAnnotationRenderer = 
				new ButtonRenderer<AnnotationTreeItem>(clickEvent -> handleDeleteAnnotationRequest(clickEvent));
		deleteAnnotationRenderer.setHtmlContentAllowed(true);
		
		annotationDetailsTree.addColumn(
			annotationTreeItem -> annotationTreeItem.getDeleteIcon(),
			deleteAnnotationRenderer)
		.setWidth(60);
//		.setExpandRatio(1);
		
		annotationDetailsTree.setDescriptionGenerator(new DescriptionGenerator<AnnotationTreeItem>() {
			@Override
			public String apply(AnnotationTreeItem annotationTreeItem) {
				return annotationTreeItem.getDescription();
			}
		}, ContentMode.HTML);
		
		ActionGridComponent<TreeGrid<AnnotationTreeItem>> annotationDetailsGridComponent = 
				new ActionGridComponent<>(new Label("Selected Annotations"), annotationDetailsTree);
		annotationDetailsGridComponent.setMargin(false);
		annotationDetailsGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu().addItem(contextSizeEditCommand.getContextSizeMenuEntry(), contextSizeEditCommand);

		addComponent(annotationDetailsGridComponent);
		setExpandRatio(annotationDetailsGridComponent, 1.0f);
	}
	
	private void handleDeleteAnnotationRequest(RendererClickEvent<AnnotationTreeItem> clickEvent) {
		if (project.isReadOnly()) {
			Notification.show(
					"Info", 
					"This project is currently in read-only mode!",
					Type.WARNING_MESSAGE);
			return;
		}

		AnnotationDataItem item = (AnnotationDataItem) clickEvent.getItem();
		
		final Annotation annotation = item.getAnnotation();
		final AnnotationCollection collection = annotation.getUserMarkupCollection();
		
		BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
			!collection.isResponsible(project.getCurrentUser().getIdentifier()),
			true,
			new Action() {
				
				@Override
				public void execute() {
					if (!isCurrentEditedCollection.apply(collection.getUuid())) {
						changeCollectionListener.accept(collection.getUuid());
						annotationDetailsProvider.refreshAll();
					}
					else {
						ConfirmDialog.show(
								UI.getCurrent(), 
								"Info", 
								"Are you sure you want to delete this annotation?",
								"Delete", 
								"Cancel", dlg -> {
									if (dlg.isConfirmed()) {
										collectionManager.removeTagInstance(annotation.getTagInstance().getUuid());
									}
								}	
								);
					}
				}
			});
		
	}
	
	
	private void handleEditAnnotationRequest(RendererClickEvent<AnnotationTreeItem> clickEvent) {
		if (project.isReadOnly()) {
			Notification.show(
					"Info", 
					"This project is currently in read-only mode!",
					Type.WARNING_MESSAGE);
			return;
		}
		
		AnnotationDataItem item = (AnnotationDataItem) clickEvent.getItem();
		
		final Annotation annotation = item.getAnnotation();
		
		String tagId = annotation.getTagInstance().getTagDefinitionId();
		TagDefinition tag = project.getTagManager().getTagLibrary().getTagDefinition(tagId);
		if (tag.getUserDefinedPropertyDefinitions().isEmpty()) {
			Notification.show(
					"Info", 
					"There are no properties defined for the tag of this annotation!",
					Type.HUMANIZED_MESSAGE);
		}
		else {
			final AnnotationCollection collection = annotation.getUserMarkupCollection();
			
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
				!collection.isResponsible(project.getCurrentUser().getIdentifier()),
				true,
				new Action() {
					
					@Override
					public void execute() {
						
						EditAnnotationPropertiesDialog editAnnotationPropertiesDialog = 
							new EditAnnotationPropertiesDialog(project, annotation, 
									new SaveCancelListener<List<Property>>() {
							
							
							@Override
							public void savePressed(List<Property> result) {
								try {
									collectionManager.updateTagInstanceProperties(
										annotation.getUserMarkupCollection(), 
										annotation.getTagInstance(), result);
									
									
								} catch (Exception e) {
									((ErrorHandler) UI.getCurrent()).showAndLogError("Error updating annotation properties", e);
								}
							}
						});
						
						editAnnotationPropertiesDialog.show();
					}
				});
		}
	}

	public Registration addMinimizeButtonClickListener(ClickListener listener) {
		return btMinimize.addClickListener(listener);
	}

	public void addAnnotation(Annotation annotation) throws IOException {
		Optional<AnnotationDataItem> optionalItem = 
				findAnnotationDataItem(annotation.getTagInstance().getUuid());
		if (optionalItem.isPresent() && isAttached()) {
			annotationDetailsTree.collapse(annotationDetailData.getRootItems());
			annotationDetailsTree.select(optionalItem.get());
			annotationDetailsTree.expand(optionalItem.get());
			int itemIdx = annotationDetailData.getRootItems().indexOf(optionalItem.get());
			if (itemIdx >= 0) {
				annotationDetailsTree.scrollTo(
						itemIdx, 
						ScrollDestination.START);
			}
		}
		else {

			AnnotationTreeItem annotationDataItem = 
					new AnnotationDataItem(
						annotation, 
						project.getTagManager().getTagLibrary().getTagsetDefinition(
							annotation.getTagInstance().getTagsetId()), 
						kwicProvider,
						annotation.getUserMarkupCollection().isResponsible(project.getCurrentUser().getIdentifier()),
						() -> isCurrentEditedCollection.apply(annotation.getUserMarkupCollection().getUuid()),
						contextSizeSupplier,
						collator
						);
			
			annotationDetailsTree.collapse(annotationDetailData.getRootItems());
			annotationDetailData.addItem(null, annotationDataItem);
			

			String tagId = annotation.getTagInstance().getTagDefinitionId();
			TagDefinition tag = 
					project.getTagManager().getTagLibrary().getTagDefinition(tagId);
			if (tag != null) { // may be deleted already
				for (Property property : annotation.getTagInstance().getUserDefinedProperties()) {
					PropertyDefinition propertyDef = 
							tag.getPropertyDefinitionByUuid(property.getPropertyDefinitionId());
					if (propertyDef != null) { // may be deleted already
						AnnotationPropertyDataItem propertyDataItem = 
								new AnnotationPropertyDataItem(
										property, 
										() -> propertyDef.getName(),
										annotation.getTagInstance().getUuid(),
										annotation.getTagInstance().getAuthor(),
										propertyDef,
										tag,
										project.getTagManager().getTagLibrary().getTagsetDefinition(tag.getTagsetDefinitionUuid()),
										annotation.getUserMarkupCollection(),
										collator);
						annotationDetailData.addItem(annotationDataItem, propertyDataItem);
						for (String value : property.getPropertyValueList()) {
							AnnotationPropertyValueDataItem valueDataItem = 
									new AnnotationPropertyValueDataItem(
											value,
											annotation.getTagInstance().getUuid(),
											annotation.getTagInstance().getAuthor(),
											propertyDef,
											tag,
											project.getTagManager().getTagLibrary().getTagsetDefinition(tag.getTagsetDefinitionUuid()),
											annotation.getUserMarkupCollection(),
											collator);
							annotationDetailData.addItem(propertyDataItem, valueDataItem);
						}
					}
				}
			}
			
			annotationDetailsProvider.refreshAll();
			if (isAttached()) {
				List<AnnotationTreeItem> items = new ArrayList<>();
				items.add(annotationDataItem);
				items.addAll(annotationDetailData.getChildren(annotationDataItem));
				annotationDetailsTree.expand(items);
				annotationDetailsTree.select(annotationDataItem);
				int itemIdx = annotationDetailData.getRootItems().size()-1;
				if (itemIdx >= 0) {
					annotationDetailsTree.scrollTo(
						itemIdx, ScrollDestination.START);
				}
			}
		}
	}
	
	public void close() {
		annotationSelectionListener = null;
		if (project != null) {
			if (annotationPropertiesChangedListener != null) {
				project.removeEventListener(
					ProjectEvent.propertyValueChanged, annotationPropertiesChangedListener);
				annotationPropertiesChangedListener = null;
			}
			
			if (propertyDefinitionChangedListener != null) {
				project.getTagManager().removePropertyChangeListener(
						TagManagerEvent.userPropertyDefinitionChanged, 
						propertyDefinitionChangedListener);					
				propertyDefinitionChangedListener = null;
			}
			
			if (tagChangedListener != null) {
				project.getTagManager().removePropertyChangeListener(
						TagManagerEvent.tagDefinitionChanged, 
						tagChangedListener);			
				tagChangedListener = null;
			}
		}
	}

	public void removeAnnotations(Collection<String> annotationIds) {
		Set<AnnotationTreeItem> toBeDeletedItems = 
			annotationDetailData.getRootItems()
			.stream()
			.filter(item -> annotationIds.contains(item.getAnnotationId()))
			.collect(Collectors.toSet());
		
		toBeDeletedItems.forEach(item -> annotationDetailData.removeItem(item));
		
		annotationDetailsProvider.refreshAll();
	}

	public void addAnnotations(Collection<Annotation> annotations) throws IOException {

		if (annotations.size() > 1) {
			handleClearSelected();
		}
		
		for (Annotation annotation : annotations) {
			addAnnotation(annotation);
		}
		
	}

}
