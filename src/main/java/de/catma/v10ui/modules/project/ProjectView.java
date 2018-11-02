package de.catma.v10ui.modules.project;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;
import de.catma.v10ui.CanReloadAll;
import de.catma.v10ui.components.CenteredIcon;
import de.catma.v10ui.components.IconButton;
import de.catma.v10ui.components.actiongrid.ActionGridComponent;
import de.catma.v10ui.components.hugecard.HugeCard;
import de.catma.v10ui.events.ResourcesChangedEvent;
import de.catma.v10ui.modules.main.ErrorLogger;
import de.catma.v10ui.modules.main.HeaderContextChangeEvent;
import de.catma.v10ui.util.Styles;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 *
 * Renders one project with all resources
 *
 * @author db
 */
public class ProjectView extends Composite<HugeCard> implements CanReloadAll, HasUrlParameter<String> {

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


    @Inject
    public ProjectView(ProjectManager projectManager, ErrorLogger errorLogger, EventBus eventBus){
        this.projectManager = projectManager;
        this.errorLogger = errorLogger;
        this.eventBus = eventBus;

        membersDP = DataProvider.fromCallbacks(
                (query) -> {
                    query.getOffset(); //NOOP calls *sigh*
                    query.getLimit(); //NOOP calls *sigh*
                    return getResources(() -> projectManager.getProjectMembers(projectReference.getProjectId()));
                },
                (query) -> getResourceCount(() -> projectManager.getProjectMembers(projectReference.getProjectId()).size())
        );
        eventBus.register(this);
    }

    /* build the GUI */

    @Override
    protected HugeCard initContent() {
        HugeCard content = new HugeCard("Project");

        HorizontalLayout mainColumns = new HorizontalLayout();
        mainColumns.getStyle().set("flex-wrap","wrap");

        VerticalLayout resources = new VerticalLayout();

        resources.setSizeUndefined(); // don't set width 100%
        resources.add(new Span("Resources"));

        mainColumns.add(resources);

        VerticalLayout team = new VerticalLayout();
        team.setSizeUndefined(); // don't set width 100%
        team.add(new Span("Team"));

        mainColumns.add(team);

        content.add(mainColumns);
        content.expand(mainColumns);

        ContextMenu hugeCardMoreOptions = content.getBtnMoreOptionsContextMenu();
        hugeCardMoreOptions.addItem("Share Ressources", e -> Notification.show("Sharing"));// TODO: 29.10.18 actually share something
        hugeCardMoreOptions.addItem("Delete Ressources", e -> Notification.show("Deleting")); // TODO: 29.10.18 actually delete something
        hugeCardMoreOptions.setOpenOnClick(true);

        resources.add(initResourceContent());
        team.add(initTeamContent());

        return content;
    }

    /**
     * initialize the resource part
     * @return
     */
    private Component initResourceContent() {
        HorizontalLayout resourceContent = new HorizontalLayout();

        resourceGrid.addClassName(Styles.actiongrid__hidethead);

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

        resourceGrid.addColumn(TemplateRenderer
                .<Resource> of(rt.toString())

                .withProperty("leaf",
                        item -> ! resourceGrid.getDataProvider().hasChildren(item))
                .withProperty("icon",
                        item -> {
                            if(item instanceof SourceDocumentResource){
                                return "vaadin:file";
                            }
                            if(item instanceof UserMarkupResource){
                                return "vaadin:file-text";
                            }
                            return "";
                        })

                .withProperty("title", res -> res.toString())
                .withProperty("detail", res -> res.hasDetail()?res.detail():null)

        ).setFlexGrow(1);

        resourceGrid.addColumn(
                new ComponentRenderer((nan) -> new IconButton(VaadinIcon.ELLIPSIS_DOTS_V.create())))
                .setFlexGrow(0).setWidth("2em");
        Span documentsAnnotations = new Span("Documents & Annotations");

        documentsAnnotations.setWidth("100%");
        ActionGridComponent sourceDocumentsGridComponent = new ActionGridComponent<>(
                documentsAnnotations,
                resourceGrid
        );

        ContextMenu addContextMenu = sourceDocumentsGridComponent.getActionGridBar().getBtnAddContextMenu();
        addContextMenu.addItem("Add Document", e -> Notification.show("Hell"));// TODO: 29.10.18 actually do something
        addContextMenu.addItem("Add Annotation Collection", e -> Notification.show("Fire")); // TODO: 29.10.18 actually do something
        addContextMenu.setOpenOnClick(true);

        ContextMenu BtnMoreOptionsContextMenu = sourceDocumentsGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
        BtnMoreOptionsContextMenu.addItem("Delete documents / collections",(evt) -> handleDeleteResources(evt, resourceGrid));
        BtnMoreOptionsContextMenu.addItem("Share documents / collections", (evt) -> handleShareResources(evt, resourceGrid));
        BtnMoreOptionsContextMenu.setOpenOnClick(true);


        resourceContent.add(sourceDocumentsGridComponent);

        Grid<TagsetDefinition> tagsetGrid = new Grid<>();
        tagsetGrid.addClassName(Styles.actiongrid__hidethead);

        tagsetGrid.setDataProvider(tagsetsDP);

        tagsetGrid.addColumn(
                new ComponentRenderer(() -> new CenteredIcon(VaadinIcon.TAGS.create()))).setFlexGrow(0).setWidth("3em");
        tagsetGrid.addColumn(
                TagsetDefinition::getName).setFlexGrow(1);
        tagsetGrid.addColumn(
                new ComponentRenderer((nan) -> new IconButton(VaadinIcon.ELLIPSIS_DOTS_V.create())))
                .setFlexGrow(0).setWidth("2em");

        Span tagsetsAnnotations = new Span("Tagsets");
        documentsAnnotations.setWidth("100%");
        ActionGridComponent tagsetsGridComponent = new ActionGridComponent<Grid<TagsetDefinition>>(
                tagsetsAnnotations,
                tagsetGrid
        );

        resourceContent.add(tagsetsGridComponent);
        return resourceContent;
    }

    private Component initTeamContent() {
        HorizontalLayout teamContent = new HorizontalLayout();

        Grid<User> teamGrid = new Grid<>();
        teamGrid.addClassName(Styles.actiongrid__hidethead);

        teamGrid.setDataProvider(membersDP);

        teamGrid.addColumn(
                new ComponentRenderer(() -> new CenteredIcon(VaadinIcon.USER.create()))).setFlexGrow(0).setWidth("3em");
        teamGrid.addColumn(
                User::getName).setFlexGrow(1);
        teamGrid.addColumn(
                new ComponentRenderer((nan) -> new IconButton(VaadinIcon.ELLIPSIS_DOTS_V.create())))
                .setFlexGrow(0).setWidth("2em");

        Span membersAnnotations = new Span("Members");
        ActionGridComponent membersGridComponent = new ActionGridComponent<>(
                membersAnnotations,
                teamGrid
        );

        teamContent.add(membersGridComponent);
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
     * handler for vaadin navigation
     * @param event
     * @param projectId
     */
    @Override
    public void setParameter(BeforeEvent event, String projectId) {
        try {
            projectReference = projectManager.findProjectReferenceById(Objects.requireNonNull(projectId));
            eventBus.post(new HeaderContextChangeEvent(
                    new Div(new Span(projectReference.getName()))));
            reloadAll();
        } catch (IOException e) {
            errorLogger.showAndLogError("Project " + projectId+ "couldn't be opened", e);
        }
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
    private void handleDeleteResources(ClickEvent<MenuItem> clickEvent, TreeGrid<Resource> resourceGrid) {
        Dialog dialog = new Dialog();
        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setWidth("100%");
        dialog.add(dialogContent);

        ListBox<Resource> listBox = new ListBox<>();
        Set<Resource> resources = resourceGrid.getSelectedItems();
        listBox.setDataProvider(DataProvider.fromStream(resources.stream()));
        listBox.setReadOnly(true);

        dialogContent.add(new Span("The following resources will be deleted"));

        dialogContent.add(listBox);

        HorizontalLayout buttonPanel = new HorizontalLayout();
        Button delete = new Button("YES DELETE");
        Button cancel = new Button("Cancel");
        buttonPanel.add(cancel);
        buttonPanel.add(delete);
        buttonPanel.expand(cancel);
        dialogContent.add(buttonPanel);

        delete.addClickListener((evt) -> {
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
        });
        cancel.addClickListener((evt)-> dialog.close());
        dialog.open();
    }

    /**
     * TODO: 29.10.18 actually share resources
     *
     * @param clickEvent
     * @param resourceGrid
     */
    private void handleShareResources(ClickEvent<MenuItem> clickEvent, TreeGrid<Resource> resourceGrid) {
        Dialog dialog = new Dialog();
        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setWidth("100%");
        dialog.add(dialogContent);

        dialogContent.add(new Span("The following resources will be shared"));

        HorizontalLayout buttonPanel = new HorizontalLayout();
        Button share = new Button("Share");
        Button cancel = new Button("Cancel");
        buttonPanel.add(cancel);
        buttonPanel.add(share);
        buttonPanel.expand(cancel);
        dialogContent.add(buttonPanel);

        share.addClickListener((evt) -> {
            dialog.close();
        });
        cancel.addClickListener((evt)-> dialog.close());
        dialog.open();
    }
 }
