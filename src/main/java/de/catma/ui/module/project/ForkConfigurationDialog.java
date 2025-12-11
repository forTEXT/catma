package de.catma.ui.module.project;

import java.io.IOException;
import java.text.Collator;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.data.SelectionModel.Multi;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.project.ProjectView.DocumentGridColumn;
import de.catma.ui.module.project.ProjectView.TagsetGridColumn;
import de.catma.ui.util.Cleaner;
import de.catma.user.Member;

public class ForkConfigurationDialog extends AbstractOkCancelDialog<Set<String>> {
	
	

	private TreeGrid<Resource> documentGrid;
	private ActionGridComponent<TreeGrid<Resource>> documentGridComponent;
	private Grid<TagsetDefinition> tagsetGrid;
	private ActionGridComponent<Grid<TagsetDefinition>> tagsetGridComponent;

	private final Map<String, Member> membersByIdentifier;
	private final ErrorHandler errorHandler;
	private final Project project;
	private HorizontalLayout resourcesContentLayout;
	
	public ForkConfigurationDialog(Project project, Map<String, Member> membersByIdentifier, SaveCancelListener<Set<String>> saveCancelListener) {
		super("Configure Copied Project", saveCancelListener);

		setShowMaximizeRestoreIcon(true);

		this.project = project;
		this.errorHandler = (ErrorHandler) UI.getCurrent();
		this.membersByIdentifier = membersByIdentifier;
		createComponents();
		initData();
	}

	private void createComponents() {
		resourcesContentLayout = new HorizontalLayout();
		resourcesContentLayout.setSizeFull();
		
		documentGrid = TreeGridFactory.createDefaultTreeGrid();
		documentGrid.addStyleNames("flat-undecorated-icon-buttonrenderer");
		documentGrid.setSelectionMode(SelectionMode.MULTI);
		// disabled, custom height needed to display additional document details but causes a styling issue, see buildResourceNameHtml
//		documentGrid.setRowHeight(45);
		documentGrid.setSizeFull();

		documentGrid.addColumn(Resource::getIcon, new HtmlRenderer())
				.setWidth(71); // we set an explicit width here because automatic sizing is not working properly in this case

		documentGrid.addColumn(buildResourceNameHtml::apply, new HtmlRenderer())
				.setId(DocumentGridColumn.NAME.name())
				.setCaption("Name")
				.setMinimumWidthFromContent(false)
				.setExpandRatio(1);

		documentGrid.addColumn(buildResourceResponsibilityHtml::apply, new HtmlRenderer())
				.setId(DocumentGridColumn.RESPONSIBLE.name())
				.setCaption("Responsible")
				.setMinimumWidthFromContent(false)
				.setMinimumWidth(110);

		documentGridComponent = new ActionGridComponent<>(
				new Label("Documents & Annotations"),
				documentGrid
		);
		documentGridComponent.setSelectionModeFixed(SelectionMode.MULTI);
		documentGridComponent.getActionGridBar().setMargin(new MarginInfo(false, false, false, true));
		documentGridComponent.getActionGridBar().setAddBtnVisible(false);
		documentGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
		// NB: if we add this listener before calling one of the ActionGridComponent.setSelectionMode functions then it will be removed, because the Grid's
		//     GridSelectionModel is replaced
		documentGrid.addSelectionListener(event -> handleResourceDeselection(event));

		IconButton btDeselectAllCollections = new IconButton(VaadinIcons.NOTEBOOK);
		btDeselectAllCollections.addClickListener(event -> setCollectionsSelected(false));
		btDeselectAllCollections.setDescription("Deselect all Collections");
		btDeselectAllCollections.addStyleName("fork-configuration-dialog__bt-deselect-all-collections");
		documentGridComponent.getActionGridBar().addButtonAfterSearchField(btDeselectAllCollections);

		IconButton btSelectAllCollections = new IconButton(VaadinIcons.NOTEBOOK);
		btSelectAllCollections.addStyleName(MaterialTheme.BUTTON_PRIMARY);
		btSelectAllCollections.setDescription("Select all Collections");
		btSelectAllCollections.addClickListener(event -> setCollectionsSelected(true));
		documentGridComponent.getActionGridBar().addButtonAfterSearchField(btSelectAllCollections);

		resourcesContentLayout.addComponent(documentGridComponent);
		resourcesContentLayout.setExpandRatio(documentGridComponent, 0.6f);

		tagsetGrid = new Grid<>();
		tagsetGrid.setSelectionMode(SelectionMode.MULTI);
		tagsetGrid.setSizeFull();
		
		tagsetGrid.addColumn(tagsetDefinition -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer());

		tagsetGrid.addColumn(TagsetDefinition::getName)
				.setId(TagsetGridColumn.NAME.name())
				.setCaption("Name")
				.setMinimumWidthFromContent(false)
				.setExpandRatio(1)
				.setStyleGenerator(tagsetDefinition -> tagsetDefinition.isContribution() ? "project-view-tagset-with-contribution" : null);

		tagsetGrid.addColumn(
						tagsetDefinition -> tagsetDefinition.getResponsibleUser() == null ?
								"" : membersByIdentifier.get(tagsetDefinition.getResponsibleUser())
				)
				.setId(TagsetGridColumn.RESPONSIBLE.name())
				.setCaption("Responsible")
				.setMinimumWidthFromContent(false)
				.setMinimumWidth(110);

		tagsetGridComponent = new ActionGridComponent<>(
				new Label("Tagsets"),
				tagsetGrid
		);
		tagsetGridComponent.addStyleName("fork-configuration-dialog__tagset-grid");
		tagsetGridComponent.setSelectionModeFixed(SelectionMode.MULTI);
		tagsetGridComponent.getActionGridBar().setMargin(new MarginInfo(false, false, false, true));
		tagsetGridComponent.getActionGridBar().setAddBtnVisible(false);
		tagsetGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
		// NB: if we add this listener before calling one of the ActionGridComponent.setSelectionMode functions then it will be removed, because the Grid's
		//     GridSelectionModel is replaced
		tagsetGrid.addSelectionListener(event -> handleTagsetDeselection(event));

		resourcesContentLayout.addComponent(tagsetGridComponent);
		resourcesContentLayout.setExpandRatio(tagsetGridComponent, 0.4f);
	}

	private void handleTagsetDeselection(SelectionEvent<TagsetDefinition> event) {
		Set<String> selectedTagIds = 
				event.getAllSelectedItems().stream().flatMap(tagset -> tagset.getTagDefinitionIds().stream()).collect(Collectors.toSet());
		
		List<Resource> toBeDeselected = documentGrid
		.getSelectedItems().stream()
		.filter(resource -> resource instanceof CollectionResource)
		.filter(resource -> {
		
			try {
				return project.getAnnotationCollection(((CollectionResource)resource).getCollectionReference())
						.getTagDefinitionIds()
						.stream()
						.filter(tagId -> !selectedTagIds.contains(tagId))
						.findAny()
						.isPresent();				
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		})
		.toList();
		if (!toBeDeselected.isEmpty()) {
			Notification.show(
					"Info",
					"We also deselected the collections that use tags from the deselected tagsets!",
					Type.HUMANIZED_MESSAGE
			);
			toBeDeselected.stream().forEach(documentGrid::deselect);
		}
	}

	private void handleResourceDeselection(SelectionEvent<Resource> event) {
		if (event.isUserOriginated()) {
			TreeDataProvider<Resource> resourceDataProvider = (TreeDataProvider<Resource>) documentGrid.getDataProvider();
			TreeData<Resource> treeData = resourceDataProvider.getTreeData();
			
			List<Resource> toBeDeselected = resourceDataProvider.getTreeData()
			.getRootItems().stream()
			.flatMap(item -> treeData.getChildren(item).stream())
			.filter(resource -> !event.getAllSelectedItems().contains(treeData.getParent(resource)) && event.getAllSelectedItems().contains(resource))
			.toList();
			
			if (!toBeDeselected.isEmpty()) {
				Notification.show(
						"Info",
						"We also deselected the collections corresponding with the deselected documents!",
						Type.HUMANIZED_MESSAGE
				);
				toBeDeselected.stream().forEach(documentGrid::deselect);
			}
		}
		
		
	}
	
	@Override
	public void attach() {
		super.attach();
		TreeDataProvider<Resource> resourceDataProvider = (TreeDataProvider<Resource>) documentGrid.getDataProvider();
		documentGrid.expand(resourceDataProvider.getTreeData().getRootItems());
	}

	private void setCollectionsSelected(boolean selected) {
		TreeDataProvider<Resource> resourceDataProvider = (TreeDataProvider<Resource>) documentGrid.getDataProvider();

		Consumer<Resource> selectorFunction = selected?(documentGrid::select):documentGrid::deselect;
		
		resourceDataProvider.getTreeData()
			.getRootItems().stream()
			.flatMap(item -> resourceDataProvider.getTreeData().getChildren(item).stream())
			.forEach(selectorFunction);
	}

	@Override
	protected void addContent(ComponentContainer content) {
		Label infoLabel = new Label("Please select the resources you want to <strong>keep</strong> in your newly copied project:", ContentMode.HTML);
		content.addComponent(infoLabel);
		content.addComponent(resourcesContentLayout);
		if (content instanceof AbstractOrderedLayout) {
			((AbstractOrderedLayout) content).setExpandRatio(resourcesContentLayout, 1.0f);
		}
	}

	@Override
	protected void layoutWindow() {
		setWidth("80%");
		setHeight("80%");
	}
	
	private void initData() {
		try {
			TreeDataProvider<Resource> resourceDataProvider = buildResourceDataProvider();
			documentGrid.setDataProvider(resourceDataProvider);
			documentGrid.sort(DocumentGridColumn.NAME.name());
			((Multi)documentGrid.getSelectionModel()).selectAll();
			
			ListDataProvider<TagsetDefinition> tagsetDataProvider = new ListDataProvider<>(project.getTagsets());
			tagsetGrid.setDataProvider(tagsetDataProvider);
			tagsetGrid.sort(TagsetGridColumn.NAME.name());
			((Multi)tagsetGrid.getSelectionModel()).selectAll();
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Failed to initialize data", e);
		}
	}
	
	private TreeDataProvider<Resource> buildResourceDataProvider() throws Exception {
		if (project == null) {
			return new TreeDataProvider<>(new TreeData<>());
		}

		Collection<SourceDocumentReference> sourceDocumentRefs = project.getSourceDocumentReferences();

		TreeData<Resource> treeData = new TreeData<>();

		for (SourceDocumentReference sourceDocumentRef : sourceDocumentRefs) {
			DocumentResource documentResource = new DocumentResource(
					sourceDocumentRef,
					project.getId(),
					sourceDocumentRef.getResponsibleUser() != null ?
							membersByIdentifier.get(sourceDocumentRef.getResponsibleUser()) : null
			);
			treeData.addItem(null, documentResource);

			List<AnnotationCollectionReference> annotationCollectionRefs = sourceDocumentRef.getUserMarkupCollectionRefs();

			List<Resource> collectionResources = annotationCollectionRefs.stream()
					.map(annotationCollectionRef -> new CollectionResource(
							annotationCollectionRef,
							project.getId(),
							annotationCollectionRef.getResponsibleUser() != null ?
									membersByIdentifier.get(annotationCollectionRef.getResponsibleUser()) : null
					))
					.collect(Collectors.toList());

			if (!collectionResources.isEmpty()) {
				treeData.addItems(
						documentResource,
						collectionResources
				);
			}
		}

		// do a locale-specific sort, assuming that all documents share the same locale
		// TODO: this probably belongs in initData or its own function which is called from there
		Optional<SourceDocumentReference> optionalFirstDocument = sourceDocumentRefs.stream().findFirst();
		Locale locale = optionalFirstDocument.isPresent() ?
				optionalFirstDocument.get().getSourceDocumentInfo().getIndexInfoSet().getLocale() : Locale.getDefault();

		Collator collator = Collator.getInstance(locale);
		collator.setStrength(Collator.PRIMARY);

		documentGrid.getColumn(DocumentGridColumn.NAME.name()).setComparator(
				(r1, r2) -> collator.compare(r1.getName(), r2.getName())
		);
		tagsetGrid.getColumn(TagsetGridColumn.NAME.name()).setComparator(
				(t1, t2) -> collator.compare(t1.getName(), t2.getName())
		);

		return new TreeDataProvider<>(treeData);
	}

	private final Function<Resource, String> buildResourceNameHtml = (resource) -> {
		StringBuilder sb = new StringBuilder()
				.append("<div class='documentsgrid__doc'>")
				.append("<div class='documentsgrid__doc__title")
				.append(resource.isContribution() ? " documentsgrid__doc__contrib'>" : "'>")
				.append(Cleaner.clean(resource.getName()))
				.append("</div>");

		// disabled due to styling issue and not really adding value, also see createComponents and `div.documentsgrid__doc` in CSS
		// if re-enabling we need to set `height: 100%` on `.catma .v-treegrid:not(.borderless) .v-treegrid-header::after` so that the box-shadow does not
		// disappear from the header (but this hasn't been tested properly)
//		if (resource.hasDetail()) {
//			sb.append("<span class='documentsgrid__doc__author'>")
//					.append(resource.getDetail())
//					.append("</span>");
//		}

		sb.append("</div>");

		return sb.toString();
	};

	private final Function<Resource, String> buildResourceResponsibilityHtml = (resource) -> {
		if (resource.getResponsibleUser() == null) {
			return "";
		}

		return String.format("<div class='documentsgrid__doc'>%s</div>", Cleaner.clean(resource.getResponsibleUser()));
	};

	
	@Override
	protected Set<String> getResult() {
		HashSet<String> selectedResourceIds = 
				documentGrid.getSelectedItems().stream()
				.map(Resource::getResourceId)
				.collect(Collectors.toCollection(() -> new HashSet<String>()));
		selectedResourceIds.addAll(
				tagsetGrid.getSelectedItems().stream()
				.map(TagsetDefinition::getUuid)
				.collect(Collectors.toSet()));
		return selectedResourceIds;
	}

}
