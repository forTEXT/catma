package de.catma.v10ui.modules.project;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;
import de.catma.v10ui.components.IconButton;
import de.catma.v10ui.components.actiongrid.ActionGridComponent;
import de.catma.v10ui.components.hugecard.HugeCard;
import de.catma.v10ui.modules.main.ErrorLogger;
import de.catma.v10ui.modules.main.HeaderContextChangeEvent;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

/**
 *
 * Renders one project with all resources
 *
 * @author db
 */
public class ProjectView extends Composite<HugeCard> implements HasUrlParameter<String> {

    private final ProjectManager projectManager;

    private final ErrorLogger errorLogger;

    private final EventBus eventBus;
    private VerticalLayout resources;
    private VerticalLayout team;

    private ProjectReference projectReference;
    private Repository repository;

    private final DataProvider<SourceDocument, Void> sourceDocumentDP = DataProvider.fromCallbacks(
            (query) -> {
                query.getOffset(); //NOOP calls *sigh*
                query.getLimit(); //NOOP calls *sigh*
                return getResources(repository::getSourceDocuments);
            },
            (query) -> getResourceCount(repository::getSourceDocumentsCount)
    );

    private final DataProvider<TagsetDefinition, Void> tagsetsDP = DataProvider.fromCallbacks(
            (query) -> {
                query.getOffset(); //NOOP calls *sigh*
                query.getLimit(); //NOOP calls *sigh*
                return getResources(repository::getTagsets);
            },
            (query) -> getResourceCount(repository::getTagsetsCount)
    );

    private final DataProvider<User, Void> membersDP;


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
    }

    @Override
    protected HugeCard initContent() {
        HugeCard content = new HugeCard("Project");

        HorizontalLayout mainColumns = new HorizontalLayout();
        mainColumns.getStyle().set("flex-wrap","wrap");

        resources = new VerticalLayout();
        resources.setSizeUndefined(); // don't set width 100%
        resources.add(new Span("Resources"));

        mainColumns.add(resources);


        team = new VerticalLayout();
        team.setSizeUndefined(); // don't set width 100%
        team.add(new Span("Team"));

        mainColumns.add(team);

        content.add(mainColumns);
        content.expand(mainColumns);

        ContextMenu hugeCardMoreOptions = content.getBtnMoreOptionsContextMenu();
        hugeCardMoreOptions.addItem("Share Ressources", e -> Notification.show("Sharing"));
        hugeCardMoreOptions .addItem("Delete Ressources", e -> Notification.show("Deleting"));
        hugeCardMoreOptions .setOpenOnClick(true);

        HorizontalLayout resourceContent = new HorizontalLayout();
        resources.add(resourceContent);

        Grid<SourceDocument> documentsGrid = new Grid<>();
        documentsGrid.setDataProvider(sourceDocumentDP);

        documentsGrid.addColumn(
                new ComponentRenderer(() -> VaadinIcon.FILE.create())).setFlexGrow(0).setWidth("3em");
        documentsGrid.addColumn(
                SourceDocument::toString).setFlexGrow(1);
        documentsGrid.addColumn(
                new ComponentRenderer((nan) -> new IconButton(VaadinIcon.ELLIPSIS_DOTS_V.create())))
                .setFlexGrow(0).setWidth("2em");
        documentsGrid.addColumn(
                new ComponentRenderer((nan) -> new IconButton(VaadinIcon.ANGLE_DOWN.create())))
                .setFlexGrow(0).setWidth("2em");

        Span documentsAnnotations = new Span("Documents & Annotations");

        documentsAnnotations.setWidth("100%");
        ActionGridComponent sourceDocumentsGridComponent = new ActionGridComponent<Grid<SourceDocument>>(
                documentsAnnotations,
                documentsGrid
        );

        ContextMenu contextMenu = sourceDocumentsGridComponent.getActionGridBar().getBtnAddContextMenu();
        contextMenu.addItem("Add Document", e -> Notification.show("Hell"));
        contextMenu.addItem("Add Annotation Collection", e -> Notification.show("Fire"));
        contextMenu.setOpenOnClick(true);

        resourceContent.add(sourceDocumentsGridComponent);

        Grid<TagsetDefinition> tagsetGrid = new Grid<>();
        tagsetGrid.setDataProvider(tagsetsDP);

        tagsetGrid.addColumn(
                new ComponentRenderer(() -> VaadinIcon.TAGS.create())).setFlexGrow(0).setWidth("3em");
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

        HorizontalLayout teamContent = new HorizontalLayout();
        team.add(teamContent);

        Grid<User> teamGrid = new Grid<>();
        teamGrid.setDataProvider(membersDP);

        teamGrid.addColumn(
                new ComponentRenderer(() -> VaadinIcon.USER.create())).setFlexGrow(0).setWidth("3em");
        teamGrid.addColumn(
                User::getName).setFlexGrow(1);
        teamGrid.addColumn(
                new ComponentRenderer((nan) -> new IconButton(VaadinIcon.ELLIPSIS_DOTS_V.create())))
                .setFlexGrow(0).setWidth("2em");

        Span membersAnnotations = new Span("Members");
        documentsAnnotations.setWidth("100%");
        ActionGridComponent membersGridComponent = new ActionGridComponent<Grid<User>>(
                membersAnnotations,
                teamGrid
        );

        teamContent.add(membersGridComponent);

        return content;
    }

    @Override
    public void setParameter(BeforeEvent event, String projectId) {
        try {
            projectReference = projectManager.findProjectReferenceById(Objects.requireNonNull(projectId));
            eventBus.post(new HeaderContextChangeEvent(
                    new Div(new Span(projectReference.getName()))));
            initData();
        } catch (IOException e) {
            errorLogger.showAndLogError("Project " + projectId+ "couldn't be opened", e);
        }
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


    public void initData() {
        initProject(projectReference);
        sourceDocumentDP.refreshAll();
        tagsetsDP.refreshAll();
        membersDP.refreshAll();
    }

    /* Actions */

 }
