package de.catma.ui.modules.project;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
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
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.hugecard.HugeCard;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.modules.main.CanReloadAll;
import de.catma.ui.modules.main.ErrorLogger;
import de.catma.ui.modules.main.HeaderContextChangeEvent;
import de.catma.ui.util.Styles;
import de.catma.user.User;

/**
 *
 * Renders one project with all resources
 *
 * @author db
 */
public class ProjectView extends HugeCard implements CanReloadAll {

    private ProjectReference projectReference;
    private Repository repository;

    private final ProjectManager projectManager;

    private final ErrorLogger errorLogger;

    private final EventBus eventBus;

    private final DataProvider<TagsetDefinition, Void> tagsetsDP = DataProvider.fromCallbacks(
            (query) -> {
                query.getOffset(); //NOOP calls *sigh*
                query.getLimit(); //NOOP calls *sigh*
                return getResources(repository::getTagsets);
            },
            (query) -> getResourceCount(repository::getTagsetsCount)
    );

    private final DataProvider<User, Void> membersDP;

    private final TreeGrid<Resource> resourceGrid = new TreeGrid<>();;


    public ProjectView(ProjectManager projectManager, EventBus eventBus){
    	super("Project");
        this.projectManager = projectManager;
        this.errorLogger = (ErrorLogger)UI.getCurrent();
        this.eventBus = eventBus;

        membersDP = DataProvider.fromCallbacks(
                (query) -> {
                    query.getOffset(); //NOOP calls *sigh*
                    query.getLimit(); //NOOP calls *sigh*
                    return getResources(() -> projectManager.getProjectMembers(projectReference.getProjectId()));
                },
                (query) -> getResourceCount(() -> projectManager.getProjectMembers(projectReference.getProjectId()).size())
        );
        initComponents();
        eventBus.register(this);
    }

    /* build the GUI */

    private void initComponents() {
    	FlexLayout mainColumns = new FlexLayout();
    	mainColumns.addStyleNames("flex-horizontal","flex-wrap");
    	
//        mainColumns.getStyle().set("flex-wrap","wrap"); TODO: flexwrap

    	FlexLayout resources = new FlexLayout();
    	resources.addStyleNames("flex-vertical");
    	
        resources.setSizeUndefined(); // don't set width 100%
        resources.addComponent(new Label("Resources"));

        mainColumns.addComponent(resources);

        FlexLayout team = new FlexLayout();
        team.addStyleNames("flex-vertical");
        team.setSizeUndefined(); // don't set width 100%
        team.addComponent(new Label("Team"));

        mainColumns.addComponent(team);

        addComponent(mainColumns);
        
//        TODO: expand content.expand(mainColumns);

        ContextMenu hugeCardMoreOptions = getBtnMoreOptionsContextMenu();
        hugeCardMoreOptions.addItem("Share Ressources", e -> Notification.show("Sharing"));// TODO: 29.10.18 actually share something
        hugeCardMoreOptions.addItem("Delete Ressources", e -> Notification.show("Deleting")); // TODO: 29.10.18 actually delete something

        resources.addComponent(initResourceContent());
        team.addComponent(initTeamContent());

    }

    /**
     * initialize the resource part
     * @return
     */
    private Component initResourceContent() {
    	FlexLayout resourceContent = new FlexLayout();
    	resourceContent.addStyleNames("flex-horizontal");

        resourceGrid.addStyleName(Styles.actiongrid__hidethead);
        resourceGrid.setWidth("400px");
        
        resourceGrid.setDataProvider(buildResourceDataProvider());

        StringBuilder rt = new StringBuilder();
        rt
                .append("<vaadin-grid-tree-toggle ")
                .append("leaf='[[item.leaf]]' expanded='{{expanded}}' level='[[level]]' class='documentsgrid__combined' >")
                .append("</vaadin-grid-tree-toggle>")
                .append("<combined-cell>")
                .append("<centered-icon>")
                .append("<iron-icon icon='[[item.icon]]'>")
                .append("</iron-icon>")
                .append("</centered-icon>")
                .append("<div class='documentsgrid__doc'> ")
                .append("<span class='documentsgrid__doc__title'> ")
                .append("[[item.title]]")
                .append("</span>")
                .append("<span class='documentsgrid__doc__author'> ")
                .append("[[item.detail]]")
                .append("</span>")
                .append("</div>")
                .append("</combined-cell>");

// TODO: old style renderer
//        resourceGrid.addColumn(TemplateRenderer
//                .<Resource> of(rt.toString())
//
//                .withProperty("leaf",
//                        item -> ! resourceGrid.getDataProvider().hasChildren(item))
//                .withProperty("icon",
//                        item -> {
//                            if(item instanceof SourceDocumentResource){
//                                return "vaadin:file";
//                            }
//                            if(item instanceof UserMarkupResource){
//                                return "vaadin:file-text";
//                            }
//                            return "";
//                        })
//
//                .withProperty("title", res -> res.toString())
//                .withProperty("detail", res -> res.hasDetail()?res.detail():null)
//
//        ).setFlexGrow(1);

        Column<Resource, Object> col = resourceGrid.addColumn((nan) -> new IconButton(VaadinIcons.ELLIPSIS_DOTS_V));
        col.setWidth(32);
        Label documentsAnnotations = new Label("Documents & Annotations");

        documentsAnnotations.setWidth("100%");
        ActionGridComponent sourceDocumentsGridComponent = new ActionGridComponent<>(
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

        Grid<TagsetDefinition> tagsetGrid = new Grid<>();
        tagsetGrid.addStyleName(Styles.actiongrid__hidethead);
        tagsetGrid.setWidth("400px");

        tagsetGrid.setDataProvider(tagsetsDP);

        Column<TagsetDefinition, Object> coltag = tagsetGrid.addColumn((nan) -> VaadinIcons.TAGS);
        coltag.setWidth(48);

        tagsetGrid.addColumn(TagsetDefinition::getName);
        Column<TagsetDefinition, Object> coldots = tagsetGrid.addColumn((nan) -> new IconButton(VaadinIcons.ELLIPSIS_DOTS_V));
        coldots.setWidth(32);

        Label tagsetsAnnotations = new Label("Tagsets");
        documentsAnnotations.setWidth("100%");
        ActionGridComponent tagsetsGridComponent = new ActionGridComponent<Grid<TagsetDefinition>>(
                tagsetsAnnotations,
                tagsetGrid
        );

        resourceContent.addComponent(tagsetsGridComponent);
        return resourceContent;
    }

    private Component initTeamContent() {
    	FlexLayout teamContent = new FlexLayout();
    	teamContent.addStyleNames("flex-horizontal");
        Grid<User> teamGrid = new Grid<>();
        teamGrid.addStyleName(Styles.actiongrid__hidethead);
        teamGrid.setWidth("402px");
        teamGrid.setDataProvider(membersDP);

        Column<User, String> coluser = teamGrid.addColumn((nan) -> VaadinIcons.USER.getHtml(), new HtmlRenderer());
//        coluser.setWidth(48);
        //.setFlexGrow(0).setWidth("3em");
        Column<User, String> colName = teamGrid.addColumn(User::getName);
        colName.setExpandRatio(1);
//        colName.setWidth(300);
        //.setFlexGrow(1);
        
        Column<User, Component> coldots = teamGrid.addComponentColumn((nan) -> new IconButton(VaadinIcons.ELLIPSIS_DOTS_V));
//        coldots.setWidth(32);
        
                //.setFlexGrow(0).setWidth("2em");

        
        Label membersAnnotations = new Label("Members");
        ActionGridComponent membersGridComponent = new ActionGridComponent<>(
                membersAnnotations,
                teamGrid
        );

        teamContent.addComponent(membersGridComponent);
        return teamContent;
    }




    /**
     * @// TODO: 15.10.18 refactor ProjectManager to directly return repository, remove listener
     * @deprecated 
     * @param projectReference
     */
    private void initProject(ProjectReference projectReference) {
        projectManager.openProject(projectReference, new OpenProjectListener() {

            @Override
            public void progress(String msg, Object... params) {
            }

            @Override
            public void ready(Repository repository) {

                ProjectView.this.repository = repository;
            }

            @Override
            public void failure(Throwable t) {
                errorLogger.showAndLogError("fehler", t);
            }
        });
    }

    private <T> Stream<T> getResources(ResourceProvider<T> resources){
        try {
            if(repository == null || projectReference == null)
                return Stream.empty();
            return resources.getResources().stream();
        } catch (Exception e) {
            errorLogger.showAndLogError("Resources couldn't be retrieved",e);
            return Stream.empty();
        }
    }

    private int getResourceCount(ResourceCountProvider resourceCountProvider){
        try {
            if(repository == null || projectReference == null){
                return 0;
            }
            return resourceCountProvider.getResourceCount();
        } catch (Exception e) {
            errorLogger.showAndLogError("Resourcecount couldn't be retrieved",e);
            return 0;
        }
    }

    private TreeDataProvider<Resource> buildResourceDataProvider() {
        if(repository == null || projectReference == null){
            return new TreeDataProvider(new TreeData());
        }
        try {
            TreeData<Resource> treeData = new TreeData();
            Collection<SourceDocument> srcDocs = repository.getSourceDocuments();
            for(SourceDocument srcDoc : srcDocs){
                SourceDocumentResource srcDocResource = new SourceDocumentResource(srcDoc);
                treeData.addItem(null,srcDocResource);
                List<UserMarkupCollectionReference> collections = srcDoc.getUserMarkupCollectionRefs();
                if(!collections.isEmpty()){
                    treeData.addItems(srcDocResource,collections.stream().map(UserMarkupResource::new));
                }
            }
            return new TreeDataProvider(treeData);

        } catch (Exception e) {
            errorLogger.showAndLogError("Can't retrieve resources", e);
        }
        return new TreeDataProvider(new TreeData());
    }


    /* Event handler */

    /**
     * reloads all data in this view
     */
    @Override
    public void reloadAll() {
        initProject(projectReference);
        resourceGrid.setDataProvider(buildResourceDataProvider());
        tagsetsDP.refreshAll();
        membersDP.refreshAll();
    }

    /**
     * handler for project selection
     * @param event
     */
    @Subscribe
    public void handleProjectSelectedEvent(RouteToProjectEvent event) {
        projectReference = event.getProjectReference();
        eventBus.post(new HeaderContextChangeEvent(new Label(projectReference.getName())));
        reloadAll();
    }

    /**
     * called when {@link ResourcesChangedEvent} is fired e.g. when source documents have been removed or added
     * @param resourcesChangedEvent
     */
    @Subscribe
    public void handleResourceChanged(ResourcesChangedEvent<TreeGrid<Resource>> resourcesChangedEvent){
        resourcesChangedEvent.getComponent().setDataProvider(buildResourceDataProvider());
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
