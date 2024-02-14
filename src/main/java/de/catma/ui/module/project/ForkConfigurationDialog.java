package de.catma.ui.module.project;

import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.project.ProjectView.DocumentGridColumn;
import de.catma.ui.module.project.ProjectView.TagsetGridColumn;
import de.catma.user.Member;

public class ForkConfigurationDialog extends AbstractOkCancelDialog<Set<String>> {
	
	

	private TreeGrid<Resource> documentGrid;
	private ActionGridComponent<TreeGrid<Resource>> documentGridComponent;
	private Grid<TagsetDefinition> tagsetGrid;
	private ActionGridComponent<Grid<TagsetDefinition>> tagsetGridComponent;

	private final Map<String, Member> membersByIdentifier;
	private final ErrorHandler errorHandler;
	private final Project project;
	private HorizontalFlexLayout resourcesContentLayout;
	
	public ForkConfigurationDialog(Project project, Map<String, Member> membersByIdentifier, SaveCancelListener<Set<String>> saveCancelListener) {
		super("Configure Project", saveCancelListener);
		this.project = project;
		this.errorHandler = (ErrorHandler) UI.getCurrent();
		this.membersByIdentifier = membersByIdentifier;
		createComponents();
		initData();
	}

	private void createComponents() {
		resourcesContentLayout = new HorizontalFlexLayout();

		documentGrid = TreeGridFactory.createDefaultTreeGrid();
		documentGrid.addStyleNames("flat-undecorated-icon-buttonrenderer");
		documentGrid.setHeaderVisible(false);
		documentGrid.setRowHeight(45);

		documentGrid.addColumn(Resource::getIcon, new HtmlRenderer())
				.setWidth(100);

		documentGrid.addColumn(buildResourceNameHtml::apply, new HtmlRenderer())
				.setCaption("Name");

		documentGrid.addColumn(buildResourceResponsibilityHtml::apply, new HtmlRenderer())
				.setId(DocumentGridColumn.RESPONSIBLE.name())
				.setCaption("Responsible")
				.setExpandRatio(1)
				.setHidden(false);

		documentGridComponent = new ActionGridComponent<>(
				new Label("Documents & Annotations"),
				documentGrid
		);
		documentGridComponent.addStyleName("project-view-action-grid");

		resourcesContentLayout.addComponent(documentGridComponent);

		tagsetGrid = new Grid<>();
		tagsetGrid.setHeaderVisible(false);
		tagsetGrid.setWidth("400px");

		tagsetGrid.addColumn(tagsetDefinition -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer())
				.setWidth(100);

		tagsetGrid.addColumn(TagsetDefinition::getName)
				.setId(TagsetGridColumn.NAME.name())
				.setCaption("Name")
				.setStyleGenerator(tagsetDefinition -> tagsetDefinition.isContribution() ? "project-view-tagset-with-contribution" : null);

		tagsetGrid.addColumn(
						tagsetDefinition -> tagsetDefinition.getResponsibleUser() == null ?
								"Not assigned" : membersByIdentifier.get(tagsetDefinition.getResponsibleUser())
				)
				.setId(TagsetGridColumn.RESPONSIBLE.name())
				.setCaption("Responsible")
				.setExpandRatio(1)
				.setHidden(true)
				.setHidable(true);

		tagsetGridComponent = new ActionGridComponent<>(
				new Label("Tagsets"),
				tagsetGrid
		);
		tagsetGridComponent.addStyleName("project-view-action-grid");

		resourcesContentLayout.addComponent(tagsetGridComponent);
	}

	@Override
	protected void addContent(ComponentContainer content) {
		content.addComponent(resourcesContentLayout);
	}
	
	private void initData() {
		try {
			TreeDataProvider<Resource> resourceDataProvider = buildResourceDataProvider();
			documentGrid.setDataProvider(resourceDataProvider);
			documentGrid.sort(DocumentGridColumn.NAME.name());
			documentGrid.expand(resourceDataProvider.getTreeData().getRootItems());

			ListDataProvider<TagsetDefinition> tagsetDataProvider = new ListDataProvider<>(project.getTagsets());
			tagsetGrid.setDataProvider(tagsetDataProvider);
			tagsetGrid.sort(TagsetGridColumn.NAME.name());

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
				.append(resource.getName())
				.append("</div>");

		if (resource.hasDetail()) {
			sb.append("<span class='documentsgrid__doc__author'>")
					.append(resource.getDetail())
					.append("</span>");
		}

		sb.append("</div>");

		return sb.toString();
	};

	private final Function<Resource, String> buildResourceResponsibilityHtml = (resource) -> {
		if (resource.getResponsibleUser() == null) {
			return "";
		}

		return String.format("<div class='documentsgrid__doc'>%s</div>", resource.getResponsibleUser());
	};

	
	@Override
	protected Set<String> getResult() {
		// TODO Auto-generated method stub
		return null;
	}

}
