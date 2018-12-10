package de.catma.ui.modules.project;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.events.routing.RouteToAnnotateEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.modules.main.CanReloadAll;
import de.catma.ui.modules.main.ErrorLogger;
import de.catma.ui.modules.main.HeaderContextChangeEvent;
import de.catma.user.User;

/**
 *
 * Renders one project with all resources
 *
 * @author db
 */
public class ProjectView extends HugeCard implements CanReloadAll {

	private ProjectManager projectManager;
    private ProjectReference projectReference;
    private Repository project;

    private final ErrorLogger errorLogger;
    private final EventBus eventBus;

    private TreeGrid<Resource> resourceGrid;
    private Grid<TagsetDefinition> tagsetGrid;
	private Grid<User> teamGrid;

    public ProjectView(ProjectManager projectManager, EventBus eventBus) {
    	super("Project");
    	this.projectManager = projectManager;
        this.eventBus = eventBus;
        this.errorLogger = (ErrorLogger)UI.getCurrent();

        initComponents();
        initActions();
        
        eventBus.register(this);
    }

    private void initActions() {
    	resourceGrid.addItemClickListener(itemClickEvent -> handleResourceItemClick(itemClickEvent));
	}

    private void handleResourceItemClick(ItemClick<Resource> itemClickEvent) {
    	if (itemClickEvent.getMouseEventDetails().isDoubleClick()) {
    		Resource resource = itemClickEvent.getItem();
    		
    		@SuppressWarnings("unchecked")
			TreeDataProvider<Resource> resourceDataProvider = 
    				(TreeDataProvider<Resource>) resourceGrid.getDataProvider();
    		
    		Resource root = 
    			resourceDataProvider.getTreeData().getParent(resource);
    		Resource child = null;
    		
    		if (root == null) {
    			root = resource;
    		}
    		else {
    			child = resource;
    		}
    		
    		SourceDocument document = ((DocumentResource)root).getDocument();
    		UserMarkupCollectionReference collectionReference = 
    			(child==null?null:((CollectionResource)child).getCollectionReference());
    		
    		eventBus.post(new RouteToAnnotateEvent(project, document, collectionReference));
    	}
    }
    

	/* build the GUI */

	private void initComponents() {
    	FlexLayout mainPanel = new FlexLayout();
    	mainPanel.addStyleNames("flex-horizontal","flex-wrap");
    	
//        mainColumns.getStyle().set("flex-wrap","wrap"); TODO: flexwrap

    	FlexLayout resourcePanel = new FlexLayout();
    	resourcePanel.addStyleNames("flex-vertical");
    	
        resourcePanel.setSizeUndefined(); // don't set width 100%
        resourcePanel.addComponent(new Label("Resources"));

        mainPanel.addComponent(resourcePanel);

        FlexLayout teamPanel = new FlexLayout();
        teamPanel.addStyleNames("flex-vertical");
        teamPanel.setSizeUndefined(); // don't set width 100%
        teamPanel.addComponent(new Label("Team"));

        mainPanel.addComponent(teamPanel);

        addComponent(mainPanel);
        
//        TODO: expand content.expand(mainColumns);

        ContextMenu hugeCardMoreOptions = getBtnMoreOptionsContextMenu();
        hugeCardMoreOptions.addItem("Share Ressources", e -> Notification.show("Sharing"));// TODO: 29.10.18 actually share something
        hugeCardMoreOptions.addItem("Delete Ressources", e -> Notification.show("Deleting")); // TODO: 29.10.18 actually delete something

        resourcePanel.addComponent(initResourceContent());
        teamPanel.addComponent(initTeamContent());

    }

    /**
     * initialize the resource part
     * @return
     */
    private Component initResourceContent() {
    	FlexLayout resourceContent = new FlexLayout();
    	resourceContent.addStyleNames("flex-horizontal");
    	resourceGrid = new TreeGrid<>();
        resourceGrid.addStyleName("project-view-document-grid");
        resourceGrid.setWidth("402px");
        resourceGrid.setHeaderVisible(false);
        
		resourceGrid
			.addColumn(resource -> resource.getIcon(), new HtmlRenderer());
        
        resourceGrid
        	.addColumn(resource -> resource.getName())
        	.setCaption("Name")
        	.setExpandRatio(2);
        
        //TODO: see MD for when it is appropriate to offer row options
//        ButtonRenderer<Resource> resourceOptionsRenderer = new ButtonRenderer<>(
//				resourceOptionClickedEvent -> handleResourceOptionClicked(resourceOptionClickedEvent));
//        resourceOptionsRenderer.setHtmlContentAllowed(true);
        
//		resourceGrid.addColumn(
//			(nan) -> VaadinIcons.ELLIPSIS_DOTS_V.getHtml(), 
//			resourceOptionsRenderer);
        
        
        
        Label documentsAnnotations = new Label("Documents & Annotations");

        documentsAnnotations.setWidth("100%");
        ActionGridComponent<TreeGrid<Resource>> sourceDocumentsGridComponent = new ActionGridComponent<>(
                documentsAnnotations,
                resourceGrid
        );

        ContextMenu addContextMenu = sourceDocumentsGridComponent.getActionGridBar().getBtnAddContextMenu();
        addContextMenu.addItem("Add Document", e -> Notification.show("Hell"));// TODO: 29.10.18 actually do something
        addContextMenu.addItem("Add Annotation Collection", e -> Notification.show("Fire")); // TODO: 29.10.18 actually do something

        ContextMenu BtnMoreOptionsContextMenu = sourceDocumentsGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
        BtnMoreOptionsContextMenu.addItem("Delete documents / collections",(menuItem) -> handleDeleteResources(menuItem, resourceGrid));
        BtnMoreOptionsContextMenu.addItem("Share documents / collections", (menuItem) -> handleShareResources(menuItem, resourceGrid));


        resourceContent.addComponent(sourceDocumentsGridComponent);

        tagsetGrid = new Grid<>();
        tagsetGrid.setHeaderVisible(false);
        tagsetGrid.setWidth("400px");

        tagsetGrid.addColumn(tagset -> VaadinIcons.TAGS.getHtml(), new HtmlRenderer());
		tagsetGrid
			.addColumn(tagset -> tagset.getName())
			.setCaption("Name")
			.setExpandRatio(2);
	

        Label tagsetsAnnotations = new Label("Tagsets");
        tagsetsAnnotations.setWidth("100%");
        ActionGridComponent<Grid<TagsetDefinition>> tagsetsGridComponent = new ActionGridComponent<>(
                tagsetsAnnotations,
                tagsetGrid
        );

        resourceContent.addComponent(tagsetsGridComponent);
        return resourceContent;
    }

	private Component initTeamContent() {
    	FlexLayout teamContent = new FlexLayout();
    	teamContent.addStyleNames("flex-horizontal");
        teamGrid = new Grid<>();
        teamGrid.setHeaderVisible(false);
        teamGrid.setWidth("402px");
        teamGrid.addColumn((user) -> VaadinIcons.USER.getHtml(), new HtmlRenderer());
        teamGrid.addColumn(User::getName).setExpandRatio(1);

        Label membersAnnotations = new Label("Members");
        ActionGridComponent<Grid<User>> membersGridComponent = new ActionGridComponent<>(
                membersAnnotations,
                teamGrid
        );

        teamContent.addComponent(membersGridComponent);
        return teamContent;
    }




    /**
     * @param projectReference
     */
    private void initProject(ProjectReference projectReference) {
        projectManager.openProject(projectReference, new OpenProjectListener() {

            @Override
            public void progress(String msg, Object... params) {
            }

            @Override
            public void ready(Repository repository) {
                ProjectView.this.project = repository;
				initData();
            }

            @Override
            public void failure(Throwable t) {
                errorLogger.showAndLogError("error opening project", t);
            }
        });
    }

    private void initData() {
        try {
        	resourceGrid.setDataProvider(buildResourceDataProvider());
        	ListDataProvider<TagsetDefinition> tagsetData = new ListDataProvider<>(project.getTagsets());
        	tagsetGrid.setDataProvider(tagsetData);
        	
        	ListDataProvider<User> memberData = new ListDataProvider<>(project.getProjectMembers());
        	teamGrid.setDataProvider(memberData);
		} catch (Exception e) {
			errorLogger.showAndLogError("error initializing data", e);
		}
	}

    private TreeDataProvider<Resource> buildResourceDataProvider() throws Exception {
        if(project != null){
            TreeData<Resource> treeData = new TreeData<>();
            Collection<SourceDocument> srcDocs = project.getSourceDocuments();
            for(SourceDocument srcDoc : srcDocs){
                DocumentResource srcDocResource = new DocumentResource(srcDoc);
                treeData.addItem(null,srcDocResource);
                List<UserMarkupCollectionReference> collections = srcDoc.getUserMarkupCollectionRefs();
                if(!collections.isEmpty()){
                    treeData.addItems(srcDocResource,collections.stream().map(CollectionResource::new));
                }
            }
            return new TreeDataProvider<>(treeData);
        }
        return new TreeDataProvider<>(new TreeData<>());
    }

    /* Event handler */

    /**
     * reloads all data in this view
     */
    @Override
    public void reloadAll() {
        initProject(projectReference);
    }

    /**
     * handler for project selection
     */
    public void setProjectReference(ProjectReference projectReference) {
        this.projectReference = projectReference;
        eventBus.post(new HeaderContextChangeEvent(new Label(projectReference.getName())));
        reloadAll();
    }

    /**
     * called when {@link ResourcesChangedEvent} is fired e.g. when source documents have been removed or added
     * @param resourcesChangedEvent
     */
    @Subscribe
    public void handleResourceChanged(ResourcesChangedEvent<TreeGrid<Resource>> resourcesChangedEvent){
    	//TODO:
//        resourcesChangedEvent.getComponent().setDataProvider(buildResourceDataProvider());
    }

    /**
     * deletes selected resources
     *
     * @param clickEvent
     * @param resourceGrid
     */
    private void handleDeleteResources(MenuBar.MenuItem menuItem, TreeGrid<Resource> resourceGrid) {
        ConfirmDialog dialog = new ConfirmDialog();
        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setWidth("100%");
        
        ListSelect<Resource> listBox = new ListSelect<>();
        Set<Resource> resources = resourceGrid.getSelectedItems();
        listBox.setDataProvider(DataProvider.fromStream(resources.stream()));
        listBox.setReadOnly(true);
        dialogContent.addComponent(new Label("The following resources will be deleted"));
        dialogContent.addComponent(listBox);
        dialog.getCancelButton().addClickListener((evt)-> dialog.close());
        dialog.setContent(dialogContent);
        
        dialog.show(UI.getCurrent(), (evt) -> {
            for (Resource resource: resources) {
                //try {
                Notification.show("resouce has been fake deleted");
                //                if(resource instanceof SourceDocument) {
                //                    repository.delete((SourceDocument) resource);
                //                }
                //                if(resource instanceof UserMarkupCollectionReference) {
                //                    repository.delete((UserMarkupCollectionReference) resource);
                //                }
                //repository.delete(resource); // TODO: 29.10.18 delete all resources at once wrapped in a transaction
                //} catch (IOException e) {
                //    errorLogger.showAndLogError("Resouce couldn't be deleted",e);
                //    dialog.close();
                //}

            }
            eventBus.post(new ResourcesChangedEvent(resourceGrid));
            dialog.close();
        }, true);
    }

    /**
     * TODO: 29.10.18 actually share resources
     *
     * @param clickEvent
     * @param resourceGrid
     */
    private void handleShareResources(MenuBar.MenuItem menuItem, TreeGrid<Resource> resourceGrid) {
    	ConfirmDialog dialog = new ConfirmDialog();
        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setWidth("100%");
        dialogContent.addComponent(new Label("The following resources will be shared"));
        dialog.getCancelButton().addClickListener((evt)-> dialog.close());
        dialog.setContent(dialogContent);
        dialog.show(UI.getCurrent(),(evt) -> {
            dialog.close();
        },true);
    }
 }
