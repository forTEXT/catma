package de.catma.ui.module.project;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.sliderpanel.SliderPanel;
import org.vaadin.sliderpanel.SliderPanelBuilder;
import org.vaadin.sliderpanel.client.SliderMode;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Page;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Notification;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.corpus.Corpus;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.SourceDocumentReference;
import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.document.source.contenthandler.TikaContentHandler;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.indexer.IndexedProject;
import de.catma.project.OpenProjectListener;
import de.catma.project.Project;
import de.catma.project.Project.ProjectEvent;
import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.project.event.ChangeType;
import de.catma.project.event.CollectionChangeEvent;
import de.catma.project.event.DocumentChangeEvent;
import de.catma.project.event.ProjectReadyEvent;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.serialization.TagsetDefinitionImportStatus;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.TagsetMetadata;
import de.catma.ui.CatmaApplication;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.dialog.BeyondResponsibilityConfirmDialog;
import de.catma.ui.dialog.BeyondResponsibilityConfirmDialog.Action;
import de.catma.ui.dialog.GenericUploadDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.events.HeaderContextChangeEvent;
import de.catma.ui.events.MembersChangedEvent;
import de.catma.ui.events.ProjectsChangedEvent;
import de.catma.ui.events.routing.RouteToAnalyzeEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.events.routing.RouteToTagsEvent;
import de.catma.ui.layout.FlexLayout.FlexWrap;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.dashboard.ForkProjectDialog;
import de.catma.ui.module.main.CanReloadAll;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.project.InviteMembersWithGroupDialog.MemberData;
import de.catma.ui.module.project.documentwizard.DocumentWizard;
import de.catma.ui.module.project.documentwizard.TagsetImport;
import de.catma.ui.module.project.documentwizard.TagsetImportState;
import de.catma.ui.module.project.documentwizard.UploadFile;
import de.catma.user.Group;
import de.catma.user.Member;
import de.catma.user.SharedGroup;
import de.catma.user.SharedGroupMember;
import de.catma.user.signup.SignupTokenManager;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

/**
 * Renders a single project with all of its resources and members
 */
public class ProjectView extends HugeCard implements CanReloadAll {
	private final Logger logger = Logger.getLogger(ProjectView.class.getName());

	private final ProjectsManager projectsManager;
	private final EventBus eventBus;

	private final ErrorHandler errorHandler;
	private final TagManager tagManager;
	private final ProgressListener progressListener;

	private Tika tika;

	private PropertyChangeListener projectExceptionListener;
	private PropertyChangeListener tagsetChangeListener;

	private ProjectReference projectReference;
	private Project project;
	private Map<String, Member> membersByIdentifier;

	private ProgressBar progressBar;
	private SliderPanel drawer;

	// documents & annotations components
	enum DocumentGridColumn {
		NAME,
		RESPONSIBLE,
	}
	private TreeGrid<Resource> documentGrid;
	private ActionGridComponent<TreeGrid<Resource>> documentGridComponent;
	private MenuItem miEditDocumentOrCollection;
	private MenuItem miDeleteDocumentOrCollection;
	private MenuItem miImportCollection;
	private MenuItem miToggleResponsibilityFilter;

	// tagsets components
	enum TagsetGridColumn {
		NAME,
		RESPONSIBLE,
	}
	private Grid<TagsetDefinition> tagsetGrid;
	private ListDataProvider<TagsetDefinition> tagsetDataProvider;
	private ActionGridComponent<Grid<TagsetDefinition>> tagsetGridComponent;
	private MenuItem miEditTagset;
	private MenuItem miDeleteTaget;
	private MenuItem miImportTagset;

	// team components
	private VerticalFlexLayout teamLayout;
	private TreeGrid<ProjectParticipant> memberGrid;
	private ActionGridComponent<TreeGrid<ProjectParticipant>> memberGridComponent;

	// project components
	private LocalTime lastSynchronization;
	private Button btnSynchronize;
	private IconButton btnToggleViewSynchronizedOrLatestContributions;
	private MenuItem miCommit;
	private MenuItem miCopyProject;

	private MenuItem miShareResources;
	private ProjectResourceExportApiDialog projectResourceExportApiDialog;

	private ProjectEventPanel projectEventPanel;

	public ProjectView(ProjectsManager projectsManager, EventBus eventBus) {
		super("Project");

		this.projectsManager = projectsManager;
		this.eventBus = eventBus;

		final UI ui = UI.getCurrent();
		this.errorHandler = (ErrorHandler) ui;
		this.tagManager = new TagManager(new TagLibrary());

		this.progressListener = new ProgressListener() {
			@Override
			public void setProgress(String value, Object... args) {
				ui.accessSynchronously(() -> {
					if (args != null) {
						progressBar.setCaption(String.format(value, args));
					}
					else {
						progressBar.setCaption(value);
					}
					ui.push();
				});
			}
		};

		try {
			File tikaConfigFile = new File(VaadinSession.getCurrent().getService().getBaseDirectory(), "tika-config.xml");
			TikaConfig tikaConfig = new TikaConfig(tikaConfigFile.getAbsolutePath());
			this.tika = new Tika(tikaConfig);
		}
		catch (Exception e) {
			this.errorHandler.showAndLogError("Failed to initialize Tika", e);
		}

		initProjectListeners();
		initComponents();
		initActions();

		eventBus.register(this);
	}


	// event listeners
	private void initProjectListeners() {
		projectExceptionListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Exception e = (Exception) evt.getNewValue();
				errorHandler.showAndLogError("Error in project", e);
			}
		};

		tagsetChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				handleTagsetChange(evt);
			}
		};
	}

	private void handleTagsetChange(PropertyChangeEvent evt) {
		// TODO: stop calling refreshAll and work directly with the data source to add/remove items
		//       also see handleDocumentChanged, handleCollectionChanged & toggleResponsibilityFilter

		Object oldValue = evt.getOldValue();
		Object newValue = evt.getNewValue();

		if (oldValue == null) { // creation
			tagsetDataProvider.refreshAll();
		}
		else if (newValue == null) { // removal
			tagsetDataProvider.refreshAll();
		}
		else { // metadata update
			TagsetDefinition tagsetDefinition = (TagsetDefinition) newValue;
			tagsetDataProvider.refreshItem(tagsetDefinition);
		}
	}

	@Subscribe
	public void handleDocumentChanged(DocumentChangeEvent documentChangeEvent) {
		// TODO: stop calling initData and work directly with the data source to add/update/remove items (will require some changes to the Resource types)
		//       also see handleCollectionChanged, handleTagsetChange & toggleResponsibilityFilter
		initData();
	}

	@Subscribe
	public void handleCollectionChanged(CollectionChangeEvent collectionChangeEvent) {
		// TODO: stop calling initData and work directly with the data source to add/update/remove items (will require some changes to the Resource types)
		//       also see handleDocumentChanged, handleTagsetChange & toggleResponsibilityFilter

		if (!collectionChangeEvent.getChangeType().equals(ChangeType.CREATED)) {
			// ChangeType.UPDATED or DELETED
			initData();
			return;
		}

		// ChangeType.CREATED
		SourceDocumentReference sourceDocumentRef = collectionChangeEvent.getDocument();
		AnnotationCollectionReference annotationCollectionRef = collectionChangeEvent.getCollectionReference();

		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> resourceDataProvider = (TreeDataProvider<Resource>) documentGrid.getDataProvider();

		DocumentResource documentResource = new DocumentResource(
				sourceDocumentRef,
				project.getId(),
				sourceDocumentRef.getResponsibleUser() == null ? null : membersByIdentifier.get(sourceDocumentRef.getResponsibleUser())
		);

		CollectionResource collectionResource = new CollectionResource(
				annotationCollectionRef,
				project.getId(),
				project.getCurrentUser()
		);

		resourceDataProvider.getTreeData().addItem(documentResource, collectionResource);
		resourceDataProvider.refreshAll();

		if (isAttached()) {
			documentGrid.expand(documentResource);

			Notification.show(
					"Info",
					String.format("Collection \"%s\" has been created", annotationCollectionRef.toString()),
					Notification.Type.TRAY_NOTIFICATION
			);
		}
	}

	@Subscribe
	public void handleMembersChanged(MembersChangedEvent membersChangedEvent) {
		try {
			
			Set<Member> members = project.getProjectMembers();
			initMemberData(members);
		}
		catch (IOException e) {
			errorHandler.showAndLogError("Failed to load project members", e);
		}
	}


	private void initMemberData(Set<Member> members) {
		// add direct members and group non direct members by their shared group
		Map<Long, GroupParticipant> groupParticipantByGroupId = new HashMap<>();
		TreeData<ProjectParticipant> memberData = new TreeData<ProjectParticipant>();
		
		for (Member member : members) {
			if (member instanceof SharedGroupMember) {
				SharedGroupMember sharedGroupMember = (SharedGroupMember)member;
				Long groupId = sharedGroupMember.getSharedGroup().groupId();
				GroupParticipant groupParticipant = groupParticipantByGroupId.get(groupId);
				if (groupParticipant == null) {
					groupParticipant = new GroupParticipant(sharedGroupMember.getSharedGroup());
					memberData.addItem(null, groupParticipant);
					groupParticipantByGroupId.put(groupId, groupParticipant);
				}
				memberData.addItem(groupParticipant, new ProjectMemberParticipant(sharedGroupMember, false));					
			}
			else {
				memberData.addItem(null, new ProjectMemberParticipant(member, true));
			}
		}
		
		TreeDataProvider<ProjectParticipant> memberDataProvider = new TreeDataProvider<ProjectParticipant>(memberData);
		
		memberGrid.setDataProvider(memberDataProvider);
		setToggleViewButtonStateBasedOnMemberCount();
	}


	// initialization
	private void initComponents() {
		progressBar = new ProgressBar();
		progressBar.setIndeterminate(false);
		progressBar.setVisible(false);
		addComponent(progressBar);
		setComponentAlignment(progressBar, Alignment.TOP_CENTER);
		
		HorizontalLayout content = new HorizontalLayout();
		addComponent(content);
		setExpandRatio(content, 1.f);
		
		projectEventPanel = new ProjectEventPanel(eventBus);
		drawer = new SliderPanelBuilder(projectEventPanel)
				.mode(SliderMode.LEFT).expanded(false).build();
		content.addComponent(drawer);
		
		HorizontalFlexLayout mainLayout = new HorizontalFlexLayout();
		mainLayout.setFlexWrap(FlexWrap.WRAP);
		mainLayout.addStyleName("project-view-main-panel");
		content.addComponent(mainLayout);
		content.setExpandRatio(mainLayout, 1.0f);
		
		VerticalFlexLayout resourcesLayout = new VerticalFlexLayout();
		resourcesLayout.setSizeUndefined(); // don't set width 100%
		resourcesLayout.addComponent(new Label("Resources"));

		resourcesLayout.addComponent(initResourcesContent());

		mainLayout.addComponent(resourcesLayout);

		teamLayout = new VerticalFlexLayout();
		teamLayout.setSizeUndefined(); // don't set width 100%
		teamLayout.setVisible(false);
		teamLayout.addComponent(new Label("Team"));

		teamLayout.addComponent(initTeamContent());

		mainLayout.addComponent(teamLayout);

//		addComponent(mainLayout);
//		setExpandRatio(mainLayout, 1.f);

		btnToggleViewSynchronizedOrLatestContributions = new IconButton(VaadinIcons.DESKTOP);
		btnToggleViewSynchronizedOrLatestContributions.setData(false); // default is synchronized view, false = synchronized, true = latest contributions
		btnToggleViewSynchronizedOrLatestContributions.setCaption("Switch View");
		btnToggleViewSynchronizedOrLatestContributions.setDescription("Switch between 'Synchronized' and 'Latest Contributions' views");
		getHugeCardBar().addComponentBeforeMoreOptions(btnToggleViewSynchronizedOrLatestContributions);

		btnSynchronize = new IconButton(VaadinIcons.EXCHANGE);
		btnSynchronize.setCaption("Sync");
		btnSynchronize.setDescription("Synchronize with the Team");
		getHugeCardBar().addComponentBeforeMoreOptions(btnSynchronize);
	}

	private final Function<Resource, String> buildResourceNameHtml = (resource) -> {
		StringBuilder sb = new StringBuilder()
				.append("<div class='documentsgrid__doc'>")
				.append("<div class='documentsgrid__doc__title")
				.append(resource.isContribution() ? " documentsgrid__doc__contrib'>" : "'>")
				.append(resource.getName())
				.append("</div>");

		// disabled due to styling issue and not really adding value, also see initResourcesContent and `div.documentsgrid__doc` in CSS
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

		return String.format("<div class='documentsgrid__doc'>%s</div>", resource.getResponsibleUser());
	};

	private Component initResourcesContent() {
		HorizontalFlexLayout resourcesContentLayout = new HorizontalFlexLayout();

		documentGrid = TreeGridFactory.createDefaultTreeGrid();
		documentGrid.addStyleNames("flat-undecorated-icon-buttonrenderer");
		// disabled, custom height needed to display additional document details but causes a styling issue, see buildResourceNameHtml
//		documentGrid.setRowHeight(45);

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
		documentGridComponent.addStyleName("project-view-action-grid");

		resourcesContentLayout.addComponent(documentGridComponent);

		tagsetGrid = new Grid<>();
		tagsetGrid.setWidth("400px");

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
		tagsetGridComponent.addStyleName("project-view-action-grid");

		resourcesContentLayout.addComponent(tagsetGridComponent);

		return resourcesContentLayout;
	}

	private Component initTeamContent() {
		HorizontalFlexLayout teamContentLayout = new HorizontalFlexLayout();

		memberGrid = TreeGridFactory.createDefaultTreeGrid();
		memberGrid.setWidth("400px");

		memberGrid.addColumn(ProjectParticipant::getIcon, new HtmlRenderer());

		memberGrid.addColumn(ProjectParticipant::getName)
				.setCaption("Name")
				.setMinimumWidthFromContent(false)
				.setExpandRatio(1)
				.setComparator((r1, r2) -> String.CASE_INSENSITIVE_ORDER.compare(r1.getName(), r2.getName()))
				.setDescriptionGenerator(ProjectParticipant::getDescription);

		memberGrid.addColumn(ProjectParticipant::getRole)
				.setCaption("Role");

		memberGridComponent = new ActionGridComponent<>(
				new Label("Members"),
				memberGrid
		);
		memberGridComponent.addStyleName("project-view-action-grid");

		teamContentLayout.addComponent(memberGridComponent);

		return teamContentLayout;
	}

	private void initActions() {
		// documents & annotations actions
		documentGridComponent.setSearchFilterProvider(searchInput -> createDocumentGridComponentSearchFilterProvider(searchInput));
		documentGrid.addItemClickListener(itemClickEvent -> handleResourceItemClick(itemClickEvent));

		ContextMenu documentGridComponentAddContextMenu = documentGridComponent.getActionGridBar().getBtnAddContextMenu();
		documentGridComponentAddContextMenu.addItem("Add Document", menuItem -> handleAddDocumentRequest());
		documentGridComponentAddContextMenu.addItem("Add Annotation Collection", menuItem -> handleAddCollectionRequest());

		ContextMenu documentGridComponentMoreOptionsContextMenu = documentGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

		miEditDocumentOrCollection = documentGridComponentMoreOptionsContextMenu.addItem(
				"Edit Documents / Collections", menuItem -> handleEditResources()
		);
		miDeleteDocumentOrCollection = documentGridComponentMoreOptionsContextMenu.addItem(
				"Delete Documents / Collections", menuItem -> handleDeleteResources(documentGrid)
		);
		documentGridComponentMoreOptionsContextMenu.addItem(
				"Analyze Documents / Collections", menuItem -> handleAnalyzeResources(documentGrid)
		);
		documentGridComponentMoreOptionsContextMenu.addSeparator();

		miImportCollection = documentGridComponentMoreOptionsContextMenu.addItem(
				"Import a Collection", menuItem -> handleImportCollectionRequest()
		);
		MenuItem miExportDocumentsAndCollections = documentGridComponentMoreOptionsContextMenu.addItem(
				"Export Documents & Collections"
		);
		documentGridComponentMoreOptionsContextMenu.addSeparator();

		StreamResource documentsAndCollectionsExportStreamResource = new StreamResource(
				new CollectionXMLExportStreamSource(
						()-> getSelectedDocuments(),
						() -> documentGrid.getSelectedItems().stream().filter(resource -> resource.isCollection())
								.map(resource -> ((CollectionResource) resource).getCollectionReference())
								.collect(Collectors.toList()),
						() -> project
				),
				"CATMA-Corpus-Export-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ".tar.gz"
		);
		documentsAndCollectionsExportStreamResource.setCacheTime(0);
		documentsAndCollectionsExportStreamResource.setMIMEType("application/gzip");

		FileDownloader documentsAndCollectionsExportFileDownloader = new FileDownloader(documentsAndCollectionsExportStreamResource);
		documentsAndCollectionsExportFileDownloader.extend(miExportDocumentsAndCollections);

		documentGridComponentMoreOptionsContextMenu.addItem(
				"Select Filtered Entries", menuItem -> handleSelectFilteredDocuments()
		);
		miToggleResponsibilityFilter = documentGridComponentMoreOptionsContextMenu.addItem(
				"Hide Others' Responsibilities", menuItem -> toggleResponsibilityFilter()
		);
		miToggleResponsibilityFilter.setCheckable(true);
		miToggleResponsibilityFilter.setChecked(false);

		// tagsets actions
		tagsetGridComponent.setSearchFilterProvider(searchInput -> createTagsetGridComponentSearchFilterProvider(searchInput));
		tagsetGrid.addItemClickListener(itemClickEvent -> handleTagsetClick(itemClickEvent));

		tagsetGridComponent.getActionGridBar().addBtnAddClickListener(clickEvent -> handleAddTagsetRequest());

		ContextMenu tagsetGridComponentMoreOptionsContextMenu = tagsetGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

		miEditTagset = tagsetGridComponentMoreOptionsContextMenu.addItem(
				"Edit Tagset", menuItem -> handleEditTagsetRequest()
		);
		miDeleteTaget = tagsetGridComponentMoreOptionsContextMenu.addItem(
				"Delete Tagset", menuItem -> handleDeleteTagsetRequest()
		);
		tagsetGridComponentMoreOptionsContextMenu.addSeparator();

		miImportTagset = tagsetGridComponentMoreOptionsContextMenu.addItem(
				"Import Tagsets", menuItem -> handleImportTagsetsRequest()
		);
		MenuItem miExportTagsets = tagsetGridComponentMoreOptionsContextMenu.addItem("Export Tagsets");

		MenuItem miExportTagsetsAsXml = miExportTagsets.addItem("as XML");

		StreamResource tagsetsXmlExportStreamResource = new StreamResource(
				new TagsetXMLExportStreamSource(
						() -> tagsetGrid.getSelectedItems(),
						() -> project
				),
				"CATMA-Tagsets-Export-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ".xml"
		);
		tagsetsXmlExportStreamResource.setCacheTime(0);
		tagsetsXmlExportStreamResource.setMIMEType("text/xml");

		FileDownloader tagsetsXmlExportFileDownloader = new FileDownloader(tagsetsXmlExportStreamResource);
		tagsetsXmlExportFileDownloader.extend(miExportTagsetsAsXml);

		MenuItem miExportTagsetsAsCsv = miExportTagsets.addItem("as CSV");

		StreamResource tagsetsCsvExportStreamResource = new StreamResource(
				new TagsetCSVExportStreamSource(
						() -> tagsetGrid.getSelectedItems(),
						() -> project
				),
				"CATMA-Tagsets-Export-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ".csv"
		);
		tagsetsCsvExportStreamResource.setCacheTime(0);
		tagsetsCsvExportStreamResource.setMIMEType("text/comma-separated-values");

		FileDownloader tagsetsCsvExportFileDownloader =	new FileDownloader(tagsetsCsvExportStreamResource);
		tagsetsCsvExportFileDownloader.extend(miExportTagsetsAsCsv);

		// members actions
		ContextMenu memberGridComponentAddContextMenu = memberGridComponent.getActionGridBar().getBtnAddContextMenu();
		memberGridComponentAddContextMenu.addItem("Add a User Group", menuItem -> handleAddGroupRequest());
		memberGridComponentAddContextMenu.addItem("Invite Someone to the Project by Email", menuItem -> handleInviteUserRequest());

		ContextMenu memberGridComponentMoreOptionsContextMenu = memberGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

		memberGridComponentMoreOptionsContextMenu.addItem("Edit Members", (selectedItem) -> handleEditMembers());
		memberGridComponentMoreOptionsContextMenu.addItem("Remove Members", (selectedItem) -> handleRemoveMembers());
		memberGridComponentMoreOptionsContextMenu.addSeparator();

		memberGridComponentMoreOptionsContextMenu.addItem("Add Someone Directly by Username", (selectedItem) -> handleAddMemberByNameRequest());
		memberGridComponentMoreOptionsContextMenu.addItem("Invite Someone to the Project by a Shared Code", (selectedItem) -> handleProjectInvitationByCodeRequest());

		// global project actions
		btnSynchronize.addClickListener(clickEvent -> handleSynchronize());
		btnToggleViewSynchronizedOrLatestContributions.addClickListener(clickEvent -> handleToggleViewSynchronizedOrLatestContributions());

		ContextMenu hugeCardMoreOptionsContextMenu = getMoreOptionsContextMenu();
		miCommit = hugeCardMoreOptionsContextMenu.addItem(
				"Commit All Changes", menuItem -> handleCommitRequest()
		);
		
		miCopyProject = hugeCardMoreOptionsContextMenu.addItem(
				"Create a Copy of this Project", menuItem -> handleCopyProjectRequest()
		);
		
		
		hugeCardMoreOptionsContextMenu.addSeparator();
		miShareResources = hugeCardMoreOptionsContextMenu.addItem(
				"Share Project Resources (Experimental API)", menuItem -> handleShareProjectResources()
		);
	}

	private void handleCopyProjectRequest() {
		ForkProjectDialog dialog = new ForkProjectDialog(projectsManager, projectReference,  new SaveCancelListener<ProjectReference>() {
			
			@Override
			public void savePressed(ProjectReference forkedProjectReference) {
				handleForkedProject(forkedProjectReference, 2, 0);
			}
		});
		dialog.show();
	}


	private void handleForkedProject(final ProjectReference forkedProjectReference, final long delay, final long accumulatedWaitingTime) {
		setEnabled(false);
		setProgressBarVisible(true);
		
		progressBar.setCaption(String.format("Copying in progress...%s The new project will open automatically once it becomes available.", accumulatedWaitingTime>0?(accumulatedWaitingTime+"s"):""));
		
		final UI currentUI = UI.getCurrent();
		currentUI.push();
		
		((BackgroundServiceProvider)UI.getCurrent()).acquireBackgroundService().schedule(() -> {
			currentUI.access(() -> {
				try {
				
					if (projectsManager.isProjectImportFinished(forkedProjectReference)) {
						setProgressBarVisible(false);
						setEnabled(true);
						projectsManager.updateProjectMetadata(forkedProjectReference);
						eventBus.post(new ProjectsChangedEvent());
						eventBus.post(new RouteToDashboardEvent());
						eventBus.post(new RouteToProjectEvent(forkedProjectReference, true));
					}
					else {
						handleForkedProject(forkedProjectReference, 5, accumulatedWaitingTime+delay);
					}
				}
				catch (IOException e) {
					setProgressBarVisible(false);
					setEnabled(true);
					errorHandler.showAndLogError(String.format("Error copying old project '%s' into new project '%s'", projectReference, forkedProjectReference), e);
				}
			});
		}, delay, TimeUnit.SECONDS);
	}


	private SerializablePredicate<Object> createDocumentGridComponentSearchFilterProvider(String searchInput) {
		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> resourceDataProvider = (TreeDataProvider<Resource>) documentGrid.getDataProvider();
		TreeData<Resource> resourceData = resourceDataProvider.getTreeData();

		return new SerializablePredicate<Object>() {
			@Override
			public boolean test(Object obj) {
				boolean isMatch = obj.toString().toLowerCase().contains(searchInput.toLowerCase());

				if (obj instanceof CollectionResource) {
					return isMatch;
				}
				else { // it's a document
					if (isMatch) {
						return true;
					}
					else { // check child collections
						return resourceData.getChildren((Resource) obj).stream().anyMatch(
								child -> child.toString().toLowerCase().contains(searchInput.toLowerCase())
						);
					}
				}
			}
		};
	}

	private SerializablePredicate<Object> createTagsetGridComponentSearchFilterProvider(String searchInput) {
		return new SerializablePredicate<Object>() {
			@Override
			public boolean test(Object obj) {
				if (obj instanceof TagsetDefinition) {
					String name = ((TagsetDefinition) obj).getName();
					return name != null && name.toLowerCase().contains(searchInput.toLowerCase());
				}
				return false;
			}
		};
	}


	// open project / load data
	public void openProject(ProjectReference projectReference, boolean needsForkConfiguration) {
		setEnabled(false);
		setProgressBarVisible(true);

		this.projectReference = projectReference;
		eventBus.post(new HeaderContextChangeEvent(projectReference.getName()));

		final UI ui = UI.getCurrent();

		projectsManager.openProject(projectReference, tagManager, new OpenProjectListener() {
			@Override
			public void progress(String msg, Object... params) {
				ui.access(() -> {
					if (params != null) {
						progressBar.setCaption(String.format(msg, params));
					}
					else {
						progressBar.setCaption(msg);
					}
					ui.push();
				});
			}

			@Override
			public void ready(Project project) {
				ProjectView.this.project = project;

				ProjectView.this.project.addEventListener(
						ProjectEvent.exceptionOccurred,
						projectExceptionListener
				);

				ProjectView.this.project.getTagManager().addPropertyChangeListener(
						TagManagerEvent.tagsetDefinitionChanged,
						tagsetChangeListener
				);

				setProgressBarVisible(false);
				reloadAll();
				setEnabled(true);
				if (needsForkConfiguration) {
					showForkConfigurationDialog();
				}
			}

			@Override
			public void failure(Throwable t) {
				setProgressBarVisible(false);
				setEnabled(true);
				errorHandler.showAndLogError("Failed to open project", t);
			}
		});
	}

	private void showForkConfigurationDialog() {
		new ForkConfigurationDialog(project, membersByIdentifier, new SaveCancelListener<Set<String>>() {
			
			@Override
			public void savePressed(final Set<String> selectedResourceIdsToKeep) {
				setEnabled(false);
				setProgressBarVisible(true);

				final UI currentUI = UI.getCurrent();

				
				((BackgroundServiceProvider)UI.getCurrent()).acquireBackgroundService().submit(
						new DefaultProgressCallable<Void>() {
							@Override
							public Void call() throws Exception {
								getProgressListener().setProgress("Starting to remove unwanted resources...");
								
								currentUI.access(() -> {
									
									try {
										Set<SourceDocumentReference> docsToBeDeleted = new HashSet<>();
										Set<AnnotationCollectionReference> collectionsToBeDeleted = new HashSet<>();
										Set<TagsetDefinition> tagsetsToBeDeleted = new HashSet<>();
										
										for (SourceDocumentReference docRef : project.getSourceDocumentReferences().stream().toList()) {
											if (!selectedResourceIdsToKeep.contains(docRef.getUuid())) {
												getProgressListener().setProgress("Removing document \"%s\" and its collections...", docRef.toString());
												docsToBeDeleted.add(docRef);
												collectionsToBeDeleted.addAll(docRef.getUserMarkupCollectionRefs());
											}
											else {
												for (AnnotationCollectionReference collRef : docRef.getUserMarkupCollectionRefs().stream().toList()) {
													if (!selectedResourceIdsToKeep.contains(collRef.getId())) {
														getProgressListener().setProgress("Removing collection \"%s\" ...", collRef.toString());
														collectionsToBeDeleted.add(collRef);
													}
												}
											}
										}
										
										for (TagsetDefinition tagset : project.getTagsets().stream().toList()) {
											if (!selectedResourceIdsToKeep.contains(tagset.getUuid())) {
												getProgressListener().setProgress("Removing tagset \"%s\" ...", tagset.getName());
												tagsetsToBeDeleted.add(tagset);
											}
										}
										
										project.removeResources(docsToBeDeleted, collectionsToBeDeleted, tagsetsToBeDeleted, getProgressListener());
										
									} catch (Exception e) {
										setEnabled(true);
										setProgressBarVisible(false);
										errorHandler.showAndLogError(String.format("Failed to remove resources from project \"%s\"", project.getName()), e);
									}
								});
								return null;
							}
							
						}, 
						new ExecutionListener<Void>() {
							@Override
							public void done(Void result) {
								setEnabled(true);
								setProgressBarVisible(false);
								Notification.show("Info", "We will now synchronize your project, after which you can start to work with it!", Type.HUMANIZED_MESSAGE);
								handleSynchronize();
							}
							
							@Override
							public void error(Throwable t) {
								errorHandler.showAndLogError(String.format("Failed to remove resources from project \"%s\"", project.getName()), t);
							}
						},
						progressListener);
				
			}
		}).show();
	}


	@Override
	public void reloadAll() {
		initData();

		boolean isMembersEditAllowed = projectsManager.isAuthorizedOnProject(projectReference, RBACPermission.PROJECT_MEMBERS_EDIT);
		teamLayout.setVisible(isMembersEditAllowed);

		setControlsStateBasedOnProjectReadOnlyState();
		setToggleViewButtonStateBasedOnMemberCount();

		eventBus.post(new ProjectReadyEvent(project));
	}

	private void setControlsStateBasedOnProjectReadOnlyState() {
		boolean controlsEnabled = !project.isReadOnly();

		documentGridComponent.getActionGridBar().setAddBtnEnabled(controlsEnabled);
		miEditDocumentOrCollection.setEnabled(controlsEnabled);
		miDeleteDocumentOrCollection.setEnabled(controlsEnabled);
		miImportCollection.setEnabled(controlsEnabled);

		tagsetGridComponent.getActionGridBar().setAddBtnEnabled(controlsEnabled);
		miEditTagset.setEnabled(controlsEnabled);
		miDeleteTaget.setEnabled(controlsEnabled);
		miImportTagset.setEnabled(controlsEnabled);

		btnSynchronize.setEnabled(controlsEnabled);

		miCommit.setEnabled(controlsEnabled);
		miShareResources.setEnabled(controlsEnabled);
	}

	private void setToggleViewButtonStateBasedOnMemberCount() {
		@SuppressWarnings("unchecked")
		TreeDataProvider<ProjectParticipant> memberDataProvider = (TreeDataProvider<ProjectParticipant>) memberGrid.getDataProvider();
		btnToggleViewSynchronizedOrLatestContributions.setEnabled(memberDataProvider.getTreeData().getRootItems().size() > 1); // owner + (group | direct other users)
	}

	private void initData() {
		try {
			Set<Member> projectMembers = project.getProjectMembers();

			membersByIdentifier = projectMembers.stream()
					.collect(Collectors.toMap(Member::getIdentifier, Function.identity()));

			TreeDataProvider<Resource> resourceDataProvider = buildResourceDataProvider();
			documentGrid.setDataProvider(resourceDataProvider);
			documentGrid.sort(DocumentGridColumn.NAME.name());
			documentGrid.expand(resourceDataProvider.getTreeData().getRootItems());

			tagsetDataProvider = new ListDataProvider<>(project.getTagsets());
			tagsetGrid.setDataProvider(tagsetDataProvider);
			tagsetGrid.sort(TagsetGridColumn.NAME.name());

			initMemberData(projectMembers);
			
			projectEventPanel.setProject(project);
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
					.filter(annotationCollectionRef ->
									!miToggleResponsibilityFilter.isChecked() ||
											annotationCollectionRef.isResponsible(project.getCurrentUser().getIdentifier())
					)
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


	// documents & annotations actions
	private Set<SourceDocumentReference> getSelectedDocuments() {
		Set<Resource> selectedResources = documentGrid.getSelectedItems();

		Set<SourceDocumentReference> selectedSourceDocumentRefs = new HashSet<>();

		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> resourceDataProvider = (TreeDataProvider<Resource>) documentGrid.getDataProvider();

		for (Resource resource : selectedResources) {
			Resource root = resourceDataProvider.getTreeData().getParent(resource);

			if (root == null) {
				root = resource;
			}

			DocumentResource documentResource = (DocumentResource) root;
			selectedSourceDocumentRefs.add(documentResource.getSourceDocumentRef());
		}

		return selectedSourceDocumentRefs;
	}

	private void handleResourceItemClick(ItemClick<Resource> itemClickEvent) {
		if (!itemClickEvent.getMouseEventDetails().isDoubleClick()) {
			return;
		}

		Resource resource = itemClickEvent.getItem();

		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> resourceDataProvider = (TreeDataProvider<Resource>) documentGrid.getDataProvider();

		Resource root = resourceDataProvider.getTreeData().getParent(resource);
		Resource child = null;

		if (root == null) {
			root = resource;
		}
		else {
			child = resource;
		}

		if (root != null) {
			SourceDocumentReference sourceDocumentRef = ((DocumentResource) root).getSourceDocumentRef();
			AnnotationCollectionReference annotationCollectionRef = child == null ? null : ((CollectionResource) child).getCollectionReference();

			eventBus.post(new RouteToAnnotateEvent(project, sourceDocumentRef, annotationCollectionRef));
		}
	}

	private void handleAddDocumentRequest() {
		WizardContext wizardContext = new WizardContext();
		wizardContext.put(DocumentWizard.WizardContextKey.PROJECT, project);

		DocumentWizard documentWizard = new DocumentWizard(
				wizardContext,
				new SaveCancelListener<WizardContext>() {
					@Override
					public void savePressed(WizardContext result) {
						handleSaveDocumentWizardContext(result);
					}
				},
				tika
		);

		documentWizard.show();
	}

	private void handleSaveDocumentWizardContext(final WizardContext result) {
		setEnabled(false);
		setProgressBarVisible(true);

		final UI ui = UI.getCurrent();

		BackgroundServiceProvider backgroundServiceProvider = (BackgroundServiceProvider) ui;
		BackgroundService backgroundService = backgroundServiceProvider.acquireBackgroundService();

		backgroundService.submit(
				new DefaultProgressCallable<Void>() {
					@SuppressWarnings("unchecked")
					@Override
					public Void call() throws Exception {
						Collection<TagsetImport> tagsetImports = (Collection<TagsetImport>) result.get(DocumentWizard.WizardContextKey.TAGSET_IMPORT_LIST);
						Collection<UploadFile> uploadFiles = (Collection<UploadFile>) result.get(DocumentWizard.WizardContextKey.UPLOAD_FILE_LIST);

						if (tagsetImports == null) {
							tagsetImports = Collections.emptyList();
						}

						// ignoring tagsets
						// uploaded files may contain tagsets and annotations (TEI-XML)
						// filter out those annotations that use tags that belong to tagsets which were not selected for import
						tagsetImports.stream()
								.filter(ti -> ti.getImportState().equals(TagsetImportState.WILL_BE_IGNORED))
								.map(TagsetImport::getExtractedTagset)
								.forEach(ignoredTagsetDefinition -> uploadFiles.stream()
										.filter(uploadFile -> uploadFile.getIntrinsicMarkupCollection() != null)
										.forEach(uploadFile -> {
											AnnotationCollection intrinsicAnnotationCollection = uploadFile.getIntrinsicMarkupCollection();
											intrinsicAnnotationCollection.removeTagReferences(
													intrinsicAnnotationCollection.getTagReferences(ignoredTagsetDefinition)
											);
										})
								);

						getProgressListener().setProgress("Importing tagsets");

						// creating tagsets
						tagsetImports.stream()
								.filter(ti -> ti.getImportState().equals(TagsetImportState.WILL_BE_CREATED))
								.forEach(tagsetImport -> {
									getProgressListener().setProgress("Importing tagset \"%s\"", tagsetImport.getTargetTagset().getName());

									ui.accessSynchronously(() -> {
										if (project.getTagManager().getTagLibrary().getTagsetDefinition(tagsetImport.getTargetTagset().getUuid()) != null) {
											// tagset already exists in project, so it will be a merge (handled below)
											tagsetImport.setImportState(TagsetImportState.WILL_BE_MERGED);
										}
										else {
											TagsetDefinition extractedTagset = tagsetImport.getExtractedTagset();

											try {
												project.importTagsets(Collections.singletonList(
														new TagsetDefinitionImportStatus(
																extractedTagset,
																project.getTagManager().getTagLibrary().getTagsetDefinition(
																		extractedTagset.getUuid()
																) != null
														)
												));
											}
											catch (Exception e) {
												logger.log(
														Level.SEVERE,
														String.format(
																"Failed to import tagset \"%s\" with ID %s",
																extractedTagset.getName(),
																extractedTagset.getUuid()
														),
														e
												);

												Notification.show(
														"Error",
														String.format(
																"Failed to import tagset \"%s\"! This tagset will be skipped.\n" +
																		"The underlying error message was:\n%s",
																extractedTagset.getName(),
																e.getMessage()
														),
														Notification.Type.ERROR_MESSAGE
												);
											}
										}

										ui.push();
									});
								});

						// merging tagsets
						tagsetImports.stream()
								.filter(ti -> ti.getImportState().equals(TagsetImportState.WILL_BE_MERGED))
								.forEach(tagsetImport -> {
									getProgressListener().setProgress("Merging tagset \"%s\"", tagsetImport.getTargetTagset().getName());

									ui.accessSynchronously(() -> {
										TagsetDefinition targetTagset = project.getTagManager().getTagLibrary().getTagsetDefinition(
												tagsetImport.getTargetTagset().getUuid()
										);

										IDGenerator idGenerator = new IDGenerator();

										for (TagDefinition incomingTagDefinition : tagsetImport.getExtractedTagset()) {
											Optional<TagDefinition> optionalExistingTagDefinition = targetTagset.getTagDefinitionsByName(
													incomingTagDefinition.getName()
											).findFirst();

											if (!optionalExistingTagDefinition.isPresent()) {
												// tag doesn't exist in target tagset, add it
												incomingTagDefinition.setTagsetDefinitionUuid(targetTagset.getUuid());
												project.getTagManager().addTagDefinition(targetTagset, incomingTagDefinition);
												continue;
											}

											// otherwise, tag *does* exist in target tagset...
											TagDefinition existingTagDefinition = optionalExistingTagDefinition.get();

											// add any missing properties to the existing tag...
											incomingTagDefinition.getUserDefinedPropertyDefinitions().forEach(incomingPropertyDefinition -> {
												if (existingTagDefinition.getPropertyDefinition(incomingPropertyDefinition.getName()) == null) {
													project.getTagManager().addUserDefinedPropertyDefinition(
															existingTagDefinition, new PropertyDefinition(incomingPropertyDefinition)
													);
												}
											});

											// then, import tag instances (** but assign new IDs)
											uploadFiles.stream()
													.filter(uploadFile -> uploadFile.getIntrinsicMarkupCollection() != null)
													.forEach(uploadFile -> {
														AnnotationCollection intrinsicAnnotationCollection = uploadFile.getIntrinsicMarkupCollection();
														List<TagReference> incomingTagReferences = intrinsicAnnotationCollection.getTagReferences(
																incomingTagDefinition
														);
														intrinsicAnnotationCollection.removeTagReferences(incomingTagReferences);

														Multimap<TagInstance, TagReference> incomingTagReferencesByTagInstance = ArrayListMultimap.create();
														incomingTagReferences.forEach(incomingTagReference -> incomingTagReferencesByTagInstance.put(
																incomingTagReference.getTagInstance(), incomingTagReference
														));

														for (TagInstance incomingTagInstance : incomingTagReferencesByTagInstance.keySet()) {
															TagInstance newTagInstance = new TagInstance(
																	idGenerator.generate(), // ** generate new ID
																	existingTagDefinition.getUuid(),
																	incomingTagInstance.getAuthor(),
																	incomingTagInstance.getTimestamp(),
																	existingTagDefinition.getUserDefinedPropertyDefinitions(),
																	targetTagset.getUuid()
															);

															// for existing properties, we keep the existing property definition
															// but take the incoming property values
															for (Property incomingProperty : incomingTagInstance.getUserDefinedProperties()) {
																PropertyDefinition incomingPropertyDefinition =
																		incomingTagDefinition.getPropertyDefinitionByUuid(
																				incomingProperty.getPropertyDefinitionId()
																		);

																PropertyDefinition existingPropertyDefinition =
																		existingTagDefinition.getPropertyDefinition(incomingPropertyDefinition.getName());

																newTagInstance.addUserDefinedProperty(
																		new Property(
																				existingPropertyDefinition.getUuid(),
																				incomingProperty.getPropertyValueList()
																		)
																);
															}

															// re-write tag references
															ArrayList<TagReference> newTagReferences = new ArrayList<>();

															incomingTagReferencesByTagInstance.get(incomingTagInstance).forEach(
																	incomingTagReference -> newTagReferences.add(
																			new TagReference(
																					incomingTagReference.getAnnotationCollectionId(),
																					newTagInstance,
																					incomingTagReference.getSourceDocumentId(),
																					incomingTagReference.getRange()
																			)
																	)
															);

															intrinsicAnnotationCollection.addTagReferences(newTagReferences);
														}
													});
										}

										ui.push();
									});
								});

						// creating documents and collections
						boolean useApostropheAsSeparator = (boolean) result.get(DocumentWizard.WizardContextKey.APOSTROPHE_AS_SEPARATOR);
						String collectionNamePattern = (String) result.get(DocumentWizard.WizardContextKey.COLLECTION_NAME_PATTERN);
						boolean simpleXml = (boolean) result.get(DocumentWizard.WizardContextKey.SIMPLE_XML);

						for (UploadFile uploadFile : uploadFiles) {
							getProgressListener().setProgress("Importing document \"%s\"", uploadFile.getTitle());

							ui.accessSynchronously(() -> {
								addUploadFile(uploadFile, useApostropheAsSeparator, collectionNamePattern, simpleXml);
								ui.push();
							});
						}

						return null;
					}
				},
				new ExecutionListener<Void>() {
					@Override
					public void done(Void result) {
						setProgressBarVisible(false);
						setEnabled(true);
					}

					@Override
					public void error(Throwable t) {
						setProgressBarVisible(false);
						setEnabled(true);
						errorHandler.showAndLogError("Failed to add documents", t);
					}
				},
				progressListener
		);
	}

	private void addUploadFile(UploadFile uploadFile, boolean useApostropheAsSeparator, String collectionNamePattern, boolean simpleXml) {
		SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(
				uploadFile.getIndexInfoSet(useApostropheAsSeparator),
				uploadFile.getContentInfoSet(),
				uploadFile.getTechInfoSet()
		);

		SourceContentHandler sourceContentHandler =
				sourceDocumentInfo.getTechInfoSet().getMimeType().equals(FileType.XML2.getMimeType())
						? new XML2ContentHandler(simpleXml) : new TikaContentHandler(tika);
		sourceContentHandler.setSourceDocumentInfo(sourceDocumentInfo);

		SourceDocument sourceDocument = new SourceDocument(uploadFile.getUuid(), sourceContentHandler);

		try {
			String documentContent = sourceDocument.getContent();

			sourceDocumentInfo.getTechInfoSet().setFileOSType(FileOSType.getFileOSType(documentContent));

			CRC32 checksum = new CRC32();
			checksum.update(documentContent.getBytes());
			sourceDocumentInfo.getTechInfoSet().setChecksum(checksum.getValue());

			project.addSourceDocument(sourceDocument);

			AnnotationCollection intrinsicAnnotationCollection = uploadFile.getIntrinsicMarkupCollection();
			if (intrinsicAnnotationCollection != null) {
				project.importAnnotationCollection(Collections.emptyList(), intrinsicAnnotationCollection);
			}

			if (!StringUtils.isBlank(collectionNamePattern)) {
				String collectionName = collectionNamePattern.replace("{{Title}}", uploadFile.getTitle());
				project.createAnnotationCollection(collectionName, project.getSourceDocumentReference(sourceDocument.getUuid()));
			}
		}
		catch (Exception e) {
			logger.log(
					Level.SEVERE,
					String.format("Failed to load document content from file %s", uploadFile.getTempFilename().toString()),
					e
			);

			Notification.show(
					"Error",
					String.format(
							"Failed to load document content from file \"%s\"! This document will be skipped.\n" +
									"The underlying error message was:\n%s",
							uploadFile.getTitle(),
							e.getMessage()
					),
					Notification.Type.ERROR_MESSAGE
			);
		}
	}

	private void handleAddCollectionRequest() {
		Set<SourceDocumentReference> selectedSourceDocumentRefs = getSelectedDocuments();

		if (selectedSourceDocumentRefs.isEmpty()) {
			Notification.show("Info", "Please select one or more documents first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}

		try {
			SingleTextInputDialog collectionNameDialog = new SingleTextInputDialog(
					"Create Annotation Collection(s)",
					"Please enter the collection name:",
					new SaveCancelListener<String>() {
						@Override
						public void savePressed(String result) {
							try {
								for (SourceDocumentReference sourceDocumentRef : selectedSourceDocumentRefs) {
									project.createAnnotationCollection(result, sourceDocumentRef);
								}
							}
							catch (Exception e) {
								errorHandler.showAndLogError(
										String.format("Failed to create collection \"%s\"", result),
										e
								);
							}
						}
					}
			);

			collectionNameDialog.show();
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Failed to create collection", e);
		}
	}

	private void handleEditResources() {
		final Set<Resource> selectedResources = documentGrid.getSelectedItems();

		if (selectedResources.isEmpty()) {
			Notification.show("Info", "Please select a resource first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}

		// TODO: this silently ignores all but the first selected resource - disallow multi-select?
		// only one resource can be edited at a time
		if (selectedResources.size() > 1) {
			documentGridComponent.setSelectionMode(SelectionMode.SINGLE);
		}
		// take the first selected resource
		final Resource resourceToEdit = selectedResources.iterator().next();

		if (resourceToEdit.isCollection()) {
			handleEditCollection(resourceToEdit);
		}
		else { // document
			handleEditDocument(resourceToEdit);
		}
	}

	private void handleEditDocument(Resource documentToEdit) {
		final SourceDocumentReference documentRef = ((DocumentResource) documentToEdit).getSourceDocumentRef();
		boolean isBeyondCurrentUsersResponsibility = !documentRef.isResponsible(project.getCurrentUser().getIdentifier());

		try {
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					isBeyondCurrentUsersResponsibility,
					project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.DOCUMENT_DELETE_OR_EDIT),
					new Action() {
						@Override
						public void execute() {
							EditResourceDialog editDocumentDialog = new EditResourceDialog(
									"Edit Document Metadata",
									documentRef.getSourceDocumentInfo().getContentInfoSet(),
									documentRef.getSourceDocumentInfo().getTechInfoSet().getResponsibleUser(),
									membersByIdentifier.values(),
									new SaveCancelListener<Pair<String, ContentInfoSet>>() {
										@Override
										public void savePressed(Pair<String, ContentInfoSet> result) {
											try {
												String updatedResponsibleUser = result.getFirst();
												documentRef.setResponsibleUser(updatedResponsibleUser);
												// the ContentInfoSet is updated directly by EditResourceDialog

												project.updateSourceDocumentMetadata(documentRef);
											}
											catch (IOException e) {
												errorHandler.showAndLogError(
														String.format(
																"Failed to update document \"%s\"",
																documentRef.getSourceDocumentInfo().getContentInfoSet().getTitle()
														),
														e
												);

												// this is only called because the ContentInfoSet is updated directly by EditResourceDialog
												// and we don't currently have a better way to restore the old values; TODO: improve
												initData();
											}
										}
									}
							);

							editDocumentDialog.show();
						}
					}
			);
		}
		catch (IOException e) {
			errorHandler.showAndLogError(
					String.format("Failed to update document \"%s\"", documentRef.getSourceDocumentInfo().getContentInfoSet().getTitle()),
					e
			);
		}
	}

	private void handleEditCollection(Resource collectionToEdit) {
		final AnnotationCollectionReference collectionRef = ((CollectionResource) collectionToEdit).getCollectionReference();
		boolean isBeyondCurrentUsersResponsibility = !collectionRef.isResponsible(project.getCurrentUser().getIdentifier());

		try {
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					isBeyondCurrentUsersResponsibility,
					project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.COLLECTION_DELETE_OR_EDIT),
					new Action() {
						@Override
						public void execute() {
							EditResourceDialog editCollectionDialog = new EditResourceDialog(
									"Edit Collection Metadata",
									collectionRef.getContentInfoSet(),
									collectionRef.getResponsibleUser(),
									membersByIdentifier.values(),
									new SaveCancelListener<Pair<String, ContentInfoSet>>() {
										@Override
										public void savePressed(Pair<String, ContentInfoSet> result) {
											try {
												String updatedResponsibleUser = result.getFirst();
												collectionRef.setResponsibleUser(updatedResponsibleUser);
												// the ContentInfoSet is updated directly by EditResourceDialog

												project.updateAnnotationCollectionMetadata(collectionRef);
											}
											catch (IOException e) {
												errorHandler.showAndLogError(
														String.format("Failed to update collection \"%s\"", collectionRef.getName()),
														e
												);

												// this is only called because the ContentInfoSet is updated directly by EditResourceDialog
												// and we don't currently have a better way to restore the old values; TODO: improve
												initData();
											}
										}
									}
							);

							editCollectionDialog.show();
						}
					}
			);
		}
		catch (IOException e) {
			errorHandler.showAndLogError(
					String.format("Failed to update collection \"%s\"", collectionRef.getName()),
					e
			);
		}
	}

	private void handleDeleteResources(TreeGrid<Resource> resourceGrid) {
		final Set<Resource> selectedResources = resourceGrid.getSelectedItems();

		if (selectedResources.isEmpty()) {
			Notification.show("Info", "Please select one or more resources first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}

		boolean isBeyondCurrentUsersResponsibility = selectedResources.stream().anyMatch(
				resource -> !resource.isResponsible(project.getCurrentUser().getIdentifier())
		);

		List<String> selectedResourceNames = selectedResources.stream()
				.map(Resource::getName)
				.sorted()
				.collect(Collectors.toList());
		String quotedCommaSeparatedResourceNames = String.format("\"%s\"", String.join("\", \"", selectedResourceNames));

		try {
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					isBeyondCurrentUsersResponsibility,
					project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.DOCUMENT_DELETE_OR_EDIT),
					new Action() {
						@Override
						public void execute() {
							ConfirmDialog.show(
									UI.getCurrent(),
									"Warning",
									String.format(
											"Are you sure you want to delete the resource(s) %s and all associated annotations?",
											quotedCommaSeparatedResourceNames
									),
									"Delete",
									"Cancel",
									dlg -> {
										if (dlg.isConfirmed()) {
											try {
												// this sort ensures that we delete collections before we delete documents
												List<Resource> sortedSelectedResources = selectedResources.stream()
														.sorted(new Comparator<Resource>() {
															@Override
															public int compare(Resource r1, Resource r2) {
																if (r1.isCollection() && !r2.isCollection()) {
																	return -1;
																}
																else if (!r1.isCollection() && r2.isCollection()) {
																	return 1;
																}
																else {
																	return r1.getResourceId().compareTo(r2.getResourceId());
																}
															}
														})
														.collect(Collectors.toList());

												for (Resource resource : sortedSelectedResources) {
													resource.deleteFrom(project);
												}
											}
											catch (Exception e) {
												errorHandler.showAndLogError(
														String.format("Failed to delete resource(s) %s", quotedCommaSeparatedResourceNames),
														e
												);
											}
										}
									}
							);
						}
					}
			);
		}
		catch (IOException e) {
			errorHandler.showAndLogError(
					String.format("Failed to delete resource(s) %s", quotedCommaSeparatedResourceNames),
					e
			);
		}
	}

	private void handleAnalyzeResources(TreeGrid<Resource> resourceGrid) {
		final Set<Resource> selectedResources = resourceGrid.getSelectedItems();

		if (selectedResources.isEmpty()) {
			Notification.show("Info", "Please select one or more resources first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}

		try {
			Corpus corpusToBeAnalyzed = new Corpus();

			for (Resource resource : selectedResources) {
				if (resource.isCollection()) {
					CollectionResource collectionResource = (CollectionResource) resource;
					corpusToBeAnalyzed.addUserMarkupCollectionReference(collectionResource.getCollectionReference());

					DocumentResource parentDocumentResource = (DocumentResource) resourceGrid.getTreeData().getParent(collectionResource);
					if (!corpusToBeAnalyzed.getSourceDocuments().contains(parentDocumentResource.getSourceDocumentRef())) {
						corpusToBeAnalyzed.addSourceDocument(parentDocumentResource.getSourceDocumentRef());
					}
				}
				else { // it's a document
					DocumentResource documentResource = (DocumentResource) resource;
					corpusToBeAnalyzed.addSourceDocument(documentResource.getSourceDocumentRef());
				}
			}

			eventBus.post(new RouteToAnalyzeEvent((IndexedProject) project, corpusToBeAnalyzed));
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Failed to create corpus for analysis", e);
		}
	}

	private void handleImportCollectionRequest() {
		try {
			if (!project.hasUncommittedChanges() && !project.hasUntrackedChanges()) {
				importCollection();
			}
			else {
				SingleTextInputDialog dlg = new SingleTextInputDialog(
						"Commit All Changes",
						"You have changes that need to be committed first, please enter a short description for this commit:",
						commitMsg -> {
							try {
								project.commitAndPushChanges(commitMsg);
								importCollection();
							}
							catch (IOException e) {
								errorHandler.showAndLogError("Failed to import collection", e);
							}
						}
				);
				dlg.show();
			}
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Failed to import collection", e);
		}
	}

	private void importCollection() {
		Set<SourceDocumentReference> selectedSourceDocumentRefs = getSelectedDocuments();

		if (selectedSourceDocumentRefs.size() != 1) {
			Notification.show("Info", "Please select the corresponding document first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}

		final SourceDocumentReference selectedSourceDocumentRef = selectedSourceDocumentRefs.iterator().next();

		GenericUploadDialog uploadDialog = new GenericUploadDialog(
				String.format("Upload a collection for \"%s\":", selectedSourceDocumentRef.toString()),
				new SaveCancelListener<byte[]>() {
					public void savePressed(byte[] result) {
						InputStream inputStream = new ByteArrayInputStream(result);

						try {
							if (BOMFilterInputStream.hasBOM(result)) {
								inputStream = new BOMFilterInputStream(inputStream, StandardCharsets.UTF_8);
							}

							Pair<AnnotationCollection, List<TagsetDefinitionImportStatus>> loadResult =
									project.prepareAnnotationCollectionForImport(inputStream, selectedSourceDocumentRef);

							final AnnotationCollection annotationCollection = loadResult.getFirst();
							List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses = loadResult.getSecond();

							CollectionImportDialog importDialog = new CollectionImportDialog(
									tagsetDefinitionImportStatuses,
									new SaveCancelListener<List<TagsetDefinitionImportStatus>>() {
										@Override
										public void savePressed(List<TagsetDefinitionImportStatus> result) {
											try {
												project.importAnnotationCollection(result, annotationCollection);
											}
											catch (IOException e) {
												errorHandler.showAndLogError("Failed to import collection", e);
											}
										}
									}
							);

							importDialog.show();
						}
						catch (IOException e) {
							errorHandler.showAndLogError("Failed to import collection", e);
						}
						finally {
							CloseSafe.close(inputStream);
						}
					}
				}
		);

		uploadDialog.show();
	}

	private void handleSelectFilteredDocuments() {
		documentGridComponent.setSelectionMode(SelectionMode.MULTI);

		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> dataProvider = (TreeDataProvider<Resource>) documentGrid.getDataProvider();
		dataProvider.fetch(new HierarchicalQuery<>(dataProvider.getFilter(), null))
				.forEach(resource -> {
					documentGrid.select(resource);
					dataProvider.fetch(new HierarchicalQuery<>(dataProvider.getFilter(), resource))
							.forEach(child -> documentGrid.select(child));
				});
	}

	private void toggleResponsibilityFilter() {
		documentGrid.getColumn(DocumentGridColumn.RESPONSIBLE.name()).setHidden(miToggleResponsibilityFilter.isChecked());

		// TODO: stop calling initData and work directly with the data source to add/remove items (will require some changes to the Resource types)
		//       also see handleDocumentChanged, handleCollectionChanged & handleTagsetChange
		// will filter out collections that the user isn't responsible for if miToggleResponsibilityFilter.isChecked()
		// (via buildResourceDataProvider)
		initData();
	}


	// tagsets actions
	private void handleTagsetClick(ItemClick<TagsetDefinition> itemClickEvent) {
		if (itemClickEvent.getMouseEventDetails().isDoubleClick()) {
			TagsetDefinition tagsetDefinition = itemClickEvent.getItem();
			eventBus.post(new RouteToTagsEvent(project, tagsetDefinition));
		}
	}

	private void handleAddTagsetRequest() {
		SingleTextInputDialog tagsetNameDlg = new SingleTextInputDialog(
				"Create Tagset",
				"Please enter the tagset name:",
				new SaveCancelListener<String>() {
					@Override
					public void savePressed(String result) {
						IDGenerator idGenerator = new IDGenerator();
						TagsetDefinition tagsetDefinition = new TagsetDefinition(idGenerator.generateTagsetId(), result);
						tagsetDefinition.setResponsibleUser(project.getCurrentUser().getIdentifier());
						project.getTagManager().addTagsetDefinition(tagsetDefinition);
					}
				}
		);

		tagsetNameDlg.show();
	}

	private void handleEditTagsetRequest() {
		final Set<TagsetDefinition> selectedTagsets = tagsetGrid.getSelectedItems();

		if (selectedTagsets.isEmpty()) {
			Notification.show("Info", "Please select a tagset first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}

		// TODO: this silently ignores all but the first selected tagset - disallow multi-select?
		// only one tagset can be edited at a time
		if (selectedTagsets.size() > 1) {
			tagsetGrid.setSelectionMode(SelectionMode.SINGLE);
		}
		// take the first selected tagset
		final TagsetDefinition tagsetToEdit = selectedTagsets.iterator().next();

		boolean isBeyondCurrentUsersResponsibility = !tagsetToEdit.isResponsible(project.getCurrentUser().getIdentifier());

		try {
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					isBeyondCurrentUsersResponsibility,
					project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.TAGSET_DELETE_OR_EDIT),
					new Action() {
						@Override
						public void execute() {
							EditTagsetDialog editTagsetDialog = new EditTagsetDialog(
									new TagsetMetadata(tagsetToEdit.getName(), tagsetToEdit.getDescription(), tagsetToEdit.getResponsibleUser()),
									membersByIdentifier.values(),
									new SaveCancelListener<TagsetMetadata>() {
										@Override
										public void savePressed(TagsetMetadata result) {
											try {
												project.getTagManager().setTagsetMetadata(tagsetToEdit, result);
											}
											catch (Exception e) {
												errorHandler.showAndLogError(
														String.format("Failed to update tagset \"%s\"", tagsetToEdit.getName()),
														e
												);
											}
										}
									}
							);

							editTagsetDialog.show();
						}
					}
			);
		}
		catch (IOException e) {
			errorHandler.showAndLogError(
					String.format("Failed to update tagset \"%s\"", tagsetToEdit.getName()),
					e
			);
		}
	}

	private void handleDeleteTagsetRequest() {
		final Set<TagsetDefinition> selectedTagsets = tagsetGrid.getSelectedItems();

		if (selectedTagsets.isEmpty()) {
			Notification.show("Info", "Please select one or more tagsets first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}

		boolean isBeyondCurrentUsersResponsibility = selectedTagsets.stream().anyMatch(
				tagset -> !tagset.isResponsible(project.getCurrentUser().getIdentifier())
		);

		List<String> selectedTagsetNames = selectedTagsets.stream()
				.map(TagsetDefinition::getName)
				.sorted()
				.collect(Collectors.toList());
		String quotedCommaSeparatedTagsetNames = String.format("\"%s\"", String.join("\", \"", selectedTagsetNames));

		try {
			BeyondResponsibilityConfirmDialog.executeWithConfirmDialog(
					isBeyondCurrentUsersResponsibility,
					project.hasPermission(project.getCurrentUserProjectRole(), RBACPermission.TAGSET_DELETE_OR_EDIT),
					new Action() {
						@Override
						public void execute() {
							ConfirmDialog.show(
									UI.getCurrent(),
									"Warning",
									String.format(
											"Are you sure you want to delete the tagset(s) %s and all associated tags and annotations?",
											quotedCommaSeparatedTagsetNames
									),
									"Delete",
									"Cancel",
									dlg -> {
										if (dlg.isConfirmed()) {
											try {
												for (TagsetDefinition tagset : selectedTagsets) {
													project.getTagManager().removeTagsetDefinition(tagset);
												}
											}
											catch (Exception e) {
												errorHandler.showAndLogError(
														String.format("Failed to delete tagset(s) %s", quotedCommaSeparatedTagsetNames),
														e
												);
											}
										}
									}
							);
						}
					}
			);
		}
		catch (IOException e) {
			errorHandler.showAndLogError(
					String.format("Failed to delete tagset(s) %s", quotedCommaSeparatedTagsetNames),
					e
			);
		}
	}

	private void handleImportTagsetsRequest() {
		GenericUploadDialog uploadDialog = new GenericUploadDialog(
				"Upload a tag library with one or more tagsets:",
				new SaveCancelListener<byte[]>() {
					public void savePressed(byte[] result) {
						InputStream is = new ByteArrayInputStream(result);

						try {
							if (BOMFilterInputStream.hasBOM(result)) {
								is = new BOMFilterInputStream(is, StandardCharsets.UTF_8);
							}

							List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses = project.prepareTagLibraryForImport(is);

							TagsetImportDialog tagsetImportDialog = new TagsetImportDialog(
									tagsetDefinitionImportStatuses,
									new SaveCancelListener<List<TagsetDefinitionImportStatus>>() {
										@Override
										public void savePressed(List<TagsetDefinitionImportStatus> result) {
											try {
												project.importTagsets(result);
											}
											catch (IOException e) {
												errorHandler.showAndLogError("Failed to import tagset(s)", e);
											}
										}
									}
							);

							tagsetImportDialog.show();
						}
						catch (IOException e) {
							errorHandler.showAndLogError("Failed to import tagset(s)", e);
						}
						finally {
							CloseSafe.close(is);
						}
					}
				}
		);

		uploadDialog.show();
	}


	// team actions
	private void handleEditMembers() {
		// remove any owner members from the selection and display an informational message
		// TODO: GitLab allows for multiple owners:
		//       - allow all owners to add additional owners (NB: also needs to be handled when inviting or adding members directly)
		//       - allow owners to be edited by other owners, as long as the owner being modified is not the project creator (because then we would be talking
		//         about transferring the project to another namespace)
		if (memberGrid.getSelectedItems().stream().anyMatch(member -> member.getRole() == RBACRole.OWNER)) {
			Notification ownerMembersSelectedNotification = new Notification(
					"Your selection includes members with the 'Owner' role, whose role you cannot change.\n"
							+ "Those members have been ignored. (click to dismiss)",
					Notification.Type.WARNING_MESSAGE
			);
			ownerMembersSelectedNotification.setDelayMsec(-1);
			ownerMembersSelectedNotification.show(Page.getCurrent());
		}

		if (memberGrid.getSelectedItems().stream().anyMatch(member -> !member.isDirect())) {
			Notification ownerMembersSelectedNotification = new Notification(
					"Your selection includes members participating via a user group. You cannot change the role of those members directly,\n"
							+ "you need to change the role of the whole user group! Those members have been ignored. (click to dismiss)",
					Notification.Type.WARNING_MESSAGE
			);
			ownerMembersSelectedNotification.setDelayMsec(-1);
			ownerMembersSelectedNotification.show(Page.getCurrent());
		}

		final Set<ProjectParticipant> membersToEdit = 
				memberGrid.getSelectedItems().stream()
				.filter(member -> member.getRole() != RBACRole.OWNER && member.isDirect())
				.collect(Collectors.toSet());
		
		if (memberGrid.getSelectedItems().isEmpty()) {
			Notification.show("Info", "Please select one or more direct members or groups first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}

		if (!membersToEdit.isEmpty()) {
			new EditMemberDialog(
					membersToEdit,
					new SaveCancelListener<Pair<RBACRole, LocalDate>>() {
						public void savePressed(Pair<RBACRole, LocalDate> roleAndExpiresAt) {
							try {
								for (ProjectParticipant participant : membersToEdit) {
									if (participant instanceof GroupParticipant) {
										project.assignRoleToGroup(((GroupParticipant) participant).getSharedGroup(), roleAndExpiresAt.getFirst(), roleAndExpiresAt.getSecond(), true);
									}
									else {
										project.assignRoleToSubject(((ProjectMemberParticipant)participant).getMember(), roleAndExpiresAt.getFirst(), roleAndExpiresAt.getSecond());
									}
								}
							}
							catch (Exception e) {
								errorHandler.showAndLogError("Error changing role!", e);
							}
							
							eventBus.post(new MembersChangedEvent());
						};
					}
			).show();
		}
	}

	private void handleRemoveMembers() {
		// remove any owner members from the selection and display an informational message
		// TODO: GitLab allows for multiple owners:
		//       - allow all owners to remove other owners, as long as the owner being removed is not the project creator (because then we would be talking
		//         about transferring the project to another namespace)
		if (memberGrid.getSelectedItems().stream().anyMatch(member -> member.getRole() == RBACRole.OWNER)) {
			Notification ownerMembersSelectedNotification = new Notification(
					"Your selection includes members with the 'Owner' role, who you cannot remove.\n"
							+ "Those members have been ignored. (click to dismiss)",
					Notification.Type.WARNING_MESSAGE
			);
			ownerMembersSelectedNotification.setDelayMsec(-1);
			ownerMembersSelectedNotification.show(Page.getCurrent());
		}

		// remove the current user from the selection and display an informational message
		Optional<ProjectParticipant> selectedMemberCurrentUser = memberGrid.getSelectedItems().stream().filter(
				member -> member.getId().equals(project.getCurrentUser().getUserId())
		).findAny();

		if (selectedMemberCurrentUser.isPresent()) {
			Notification selfSelectedNotification = new Notification(
					"You cannot remove yourself from the project.\n"
							+ "Please use the 'Leave Project' button on the project card on the dashboard instead.\n"
							+ "\n"
							+ "If you are the owner of the project, please contact support to request a transfer\n"
							+ "of ownership. (click to dismiss)",
					Notification.Type.WARNING_MESSAGE
			);
			selfSelectedNotification.setDelayMsec(-1);
			selfSelectedNotification.show(Page.getCurrent());
		}

		if (memberGrid.getSelectedItems().stream().anyMatch(member -> !member.isDirect())) {
			Notification ownerMembersSelectedNotification = new Notification(
					"Your selection includes members participating via a user group. You cannot remove those members directly,\n"
							+ "you need to remove them from the user group! Those members have been ignored. (click to dismiss)",
					Notification.Type.WARNING_MESSAGE
			);
			ownerMembersSelectedNotification.setDelayMsec(-1);
			ownerMembersSelectedNotification.show(Page.getCurrent());
		}

		final Set<ProjectParticipant> membersToRemove = memberGrid.getSelectedItems().stream()
				.filter(
						member -> member.getRole() != RBACRole.OWNER 
					&& (selectedMemberCurrentUser.isEmpty() || !member.equals(selectedMemberCurrentUser.get())) 
					&& member.isDirect())
				.collect(Collectors.toSet());

		if (memberGrid.getSelectedItems().isEmpty()) {
			Notification.show("Info", "Please select one or more members first!", Notification.Type.HUMANIZED_MESSAGE);
			return;
		}

		if (!membersToRemove.isEmpty()) {
			new RemoveMemberDialog(
					"project",
					membersToRemove,
					members -> {
						for (ProjectParticipant participant : members) {
							try {
								if (participant instanceof GroupParticipant) {
									project.removeGroup(((GroupParticipant)participant).getSharedGroup());
								}
								else {
									project.removeSubject(((ProjectMemberParticipant)participant).getMember());
								}
							}
							catch (Exception e) {
								errorHandler.showAndLogError(String.format("Failed to remove member %s from project %s", participant, project.getName()), e);
							}
						}

						eventBus.post(new MembersChangedEvent());						
					}
			).show();
		}
	}

	private void handleAddMemberByNameRequest() {
		new AddMemberDialog(
				(query) -> projectsManager.findUser(query.getFilter().orElse("")),
				(evt) -> {
					try {
						project.assignRoleToSubject(evt.user(), evt.role(), evt.expiresAt());
						eventBus.post(new MembersChangedEvent());
					} catch (IOException e) {
						errorHandler.showAndLogError(String.format("Failed to add new member %s to project %s", evt.user().toString(), project.toString()), e);
					}
				}
		).show();
	}


	private void handleInviteUserRequest() {
		InviteMembersWithGroupDialog.buildInviteProjectMembersDialog(new SaveCancelListener<InviteMembersWithGroupDialog.MemberData>() {
			
			@Override
			public void savePressed(MemberData result) {
				handleMemberData(result);
			}
		}).show();
	}


	private void handleAddGroupRequest() {
		
		InviteMembersWithGroupDialog.buildAddGroupDialog(
				new SaveCancelListener<InviteMembersWithGroupDialog.MemberData>() {
					
					@Override
					public void savePressed(MemberData result) {
						handleMemberData(result);
					}
				},
				() -> {
					try {
						return projectsManager.getGroups(RBACRole.MAINTAINER, true);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}).show();
	}
	

	private void handleMemberData(MemberData result) {
		Group group = null;
		try {
			
			if (result.group() != null) {
				group = result.group();
			}
			else if ((result.groupName() != null) && !result.groupName().isEmpty()) {
				// TODO: pass description? would need to modify InviteMembersWithGroupDialog - irrelevant while group creation via that dialog is disabled
				group = projectsManager.createGroup(result.groupName(), null);

				if ((result.emailAdresses() != null) && !result.emailAdresses().isEmpty()) {									
					SignupTokenManager signupTokenManager = new SignupTokenManager();
					for (String address : result.emailAdresses()) {			
						try {
							signupTokenManager.sendGroupSignupEmail(address, group, result.expiresAt());
						} catch (EmailException e) {
							errorHandler.showAndLogError(String.format("Error sending group invitation link to address %s" ,  address), e);
						}
					}
				}
			}							
			
			if (group != null) {
				project.assignRoleToGroup(new SharedGroup(group.getId(), group.getName(), result.projectRole()), result.projectRole(), result.expiresAt(), false);
				eventBus.post(new MembersChangedEvent());
			}
			else if ((result.emailAdresses() != null) && !result.emailAdresses().isEmpty()) {
				SignupTokenManager signupTokenManager = new SignupTokenManager();
				for (String address : result.emailAdresses()) {			
					try {
						signupTokenManager.sendProjectSignupEmail(address, projectReference, result.projectRole(), result.expiresAt());
					} catch (EmailException e) {
						errorHandler.showAndLogError(String.format("Error sending group invitation link to address %s" ,  address), e);
					}
				}
			}
		}
		catch (IOException e) {
			if (group != null) {
				errorHandler.showAndLogError(String.format("Error sharing this project '%s' with group '%s'!", project.getName(), result.group()), e);
			}
		}
	}


	private void handleProjectInvitationByCodeRequest() {
		@SuppressWarnings("unchecked")
		TreeDataProvider<Resource> resourceDataProvider = (TreeDataProvider<Resource>) documentGrid.getDataProvider();

		List<DocumentResource> documentResources = resourceDataProvider.getTreeData().getRootItems()
				.stream()
				.filter(resource -> resource instanceof DocumentResource)
				.map(resource -> (DocumentResource) resource)
				.collect(Collectors.toList());

		new ProjectInvitationDialog(
				project,
				documentResources,
				eventBus,
				((CatmaApplication) UI.getCurrent()).getHazelCastService()
		).show();
	}


	// global project actions
	private void setProgressBarVisible(boolean visible) {
		progressBar.setIndeterminate(visible);
		progressBar.setVisible(visible);
		if (!visible) {
			progressBar.setCaption("");
		}
	}

	private void handleSynchronize() {
		if (
				lastSynchronization != null
						&& lastSynchronization.plus(
						CATMAPropertyKey.MIN_TIME_BETWEEN_SYNCHRONIZATIONS_SECONDS.getIntValue(),
						ChronoUnit.SECONDS
				).isAfter(LocalTime.now())
		) {
			Notification.show(
					"Info",
					"You just synchronized a few seconds ago - please be patient, you can synchronize again in a few moments.",
					Notification.Type.HUMANIZED_MESSAGE
			);
			return;
		}

		lastSynchronization = LocalTime.now();
		synchronizeProject();
	}

	private void synchronizeProject() {
		setProgressBarVisible(true);
		setEnabled(false);

		final UI ui = UI.getCurrent();

		try {
			project.synchronizeWithRemote(new OpenProjectListener() {
				@Override
				public void progress(String msg, Object... params) {
					ui.access(() -> {
						if (params != null) {
							progressBar.setCaption(String.format(msg, params));
						}
						else {
							progressBar.setCaption(msg);
						}
						ui.push();
					});
				}

				@Override
				public void ready(Project project) {
					setProgressBarVisible(false);
					reloadAll();
					setEnabled(true);

					if (project == null) {
						Notification syncFailedNotification = new Notification(
								"Your project cannot be synchronized right now.\n" +
										"Try again later or check the CATMA GitLab backend for open merge requests\n" +
										"that may require manual conflict resolution. (click to dismiss)",
								Notification.Type.WARNING_MESSAGE
						);
						syncFailedNotification.setDelayMsec(-1);
						syncFailedNotification.show(Page.getCurrent());
						return;
					}

					Notification.show("Info", "Your project has been synchronized", Notification.Type.TRAY_NOTIFICATION);
				}

				@Override
				public void failure(Throwable t) {
					setProgressBarVisible(false);
					setEnabled(true);

					errorHandler.showAndLogError("Failed to synchronize or re-open project. Please contact support.", t);
				}
			});
		}
		catch (Exception e) {
			setProgressBarVisible(false);
			setEnabled(true);

			errorHandler.showAndLogError("Failed to synchronize project. Please contact support.", e);
		}
	}

	private void handleToggleViewSynchronizedOrLatestContributions() {
		// default is synchronized view, false = synchronized, true = latest contributions
		boolean isLatestContributionsViewCurrentlyEnabled = (Boolean) btnToggleViewSynchronizedOrLatestContributions.getData();

		try {
			if (isLatestContributionsViewCurrentlyEnabled) {
				setLatestContributionsView(false);
				return;
			}

			// switching to latest contributions view, check for uncommitted changes first
			if (!project.hasUncommittedChanges() && !project.hasUntrackedChanges()) {
				setLatestContributionsView(true);
			}
			else {
				// there are uncommitted changes that need to be committed first
				SingleTextInputDialog dlg = new SingleTextInputDialog(
						"Commit All Changes",
						"You have changes that need to be committed first, please enter a short description for this commit:",
						commitMsg -> {
							try {
								project.commitAndPushChanges(commitMsg);
								setLatestContributionsView(true);
							}
							catch (Exception e) {
								errorHandler.showAndLogError("Failed to switch view", e);
							}
						}
				);
				dlg.show();
			}
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Failed to switch view", e);
		}
	}

	private void setLatestContributionsView(final boolean enabled) throws Exception {
		setEnabled(false); // disable the entire layout
		setProgressBarVisible(true);

		final UI ui = UI.getCurrent();

		project.setLatestContributionsView(enabled, new OpenProjectListener() {
			@Override
			public void progress(String msg, Object... params) {
				ui.access(() -> {
					if (params != null) {
						progressBar.setCaption(String.format(msg, params));
					}
					else {
						progressBar.setCaption(msg);
					}
					ui.push();
				});
			}

			@Override
			public void ready(Project project) {
				// default is synchronized view, false = synchronized, true = latest contributions
				btnToggleViewSynchronizedOrLatestContributions.setData(enabled);

				eventBus.post(new HeaderContextChangeEvent(projectReference.getName(), enabled));

				setProgressBarVisible(false);
				reloadAll();
				setEnabled(true);
			}

			@Override
			public void failure(Throwable t) {
				setProgressBarVisible(false);
				setEnabled(true);
				errorHandler.showAndLogError("Failed to switch view", t);
			}
		});
	}

	private void handleCommitRequest() {
		try {
			if (!project.hasUncommittedChanges() && !project.hasUntrackedChanges()) {
				Notification.show("Info", "There are no uncommitted changes", Notification.Type.TRAY_NOTIFICATION);
				return;
			}
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Failed to check for uncommitted changes", e);
			return;
		}

		SingleTextInputDialog dlg = new SingleTextInputDialog(
				"Commit All Changes",
				"Please enter a short description for this commit:",
				commitMsg -> {
					try {
						project.commitAndPushChanges(commitMsg);
						Notification.show("Info", "Your changes have been committed", Notification.Type.TRAY_NOTIFICATION);
					}
					catch (IOException e) {
						errorHandler.showAndLogError("Failed to commit changes", e);
					}
				}
		);
		dlg.show();
	}

	private void handleShareProjectResources() {
		if (projectResourceExportApiDialog == null) {
			projectResourceExportApiDialog = new ProjectResourceExportApiDialog(project);
		}
		projectResourceExportApiDialog.show();
	}

	public void close() {
		try {
			eventBus.unregister(this);

			if (projectResourceExportApiDialog != null) {
				projectResourceExportApiDialog.removeRequestHandlerFromVaadinService();
			}
			
			if (projectEventPanel != null) {
				projectEventPanel.close();
			}

			if (project != null) {
				if (projectExceptionListener != null) {
					project.removeEventListener(
							ProjectEvent.exceptionOccurred,
							projectExceptionListener
					);
				}

				if (tagsetChangeListener != null) {
					project.getTagManager().removePropertyChangeListener(
							TagManagerEvent.tagsetDefinitionChanged,
							tagsetChangeListener
					);
				}

				project.close();
				project = null;
			}
		}
		catch (Exception e) {
			errorHandler.showAndLogError("Failed to close ProjectView", e);
		}
	}
 }
