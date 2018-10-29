package de.catma.v10ui.modules.tags;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import de.catma.v10ui.modules.project.ResourceCountProvider;
import de.catma.v10ui.modules.project.ResourceProvider;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

/**
 *
 * Renders one project with all tagsetsPanel
 *
 * @author db
 */
public class TagsView extends Composite<HugeCard> implements HasUrlParameter<String> {

    private final ProjectManager projectManager;

    private final ErrorLogger errorLogger;

    private final EventBus eventBus;
    private VerticalLayout tagsetsPanel;
    private VerticalLayout propertiesPanel;

    private ProjectReference projectReference;
    private Repository repository;

    private final DataProvider<TagsetDefinition, Void> tagsetsDP = DataProvider.fromCallbacks(
            (query) -> {
                query.getOffset(); //NOOP calls *sigh*
                query.getLimit(); //NOOP calls *sigh*
                return getResources(repository::getTagsets);
            },
            (query) -> getResourceCount(repository::getTagsetsCount)
    );

    @Inject
    public TagsView(ProjectManager projectManager, ErrorLogger errorLogger, EventBus eventBus){
        this.projectManager = projectManager;
        this.errorLogger = errorLogger;
        this.eventBus = eventBus;
    }

    @Override
    protected HugeCard initContent() {
        HugeCard content = new HugeCard("Manage Tags");

        HorizontalLayout mainColumns = new HorizontalLayout();
        mainColumns.getStyle().set("flex-wrap","wrap");
        content.add(mainColumns);
        content.expand(mainColumns);

        ContextMenu hugeCardMoreOptions = content.getBtnMoreOptionsContextMenu();
        hugeCardMoreOptions.addItem("Add Tagset", e -> Notification.show("Adding Tagset"));
        hugeCardMoreOptions .setOpenOnClick(true);

        VerticalLayout tagsetPanel = new VerticalLayout();
        tagsetPanel.setSizeUndefined();
        mainColumns.add(tagsetPanel);
        mainColumns.setFlexGrow(1.0, tagsetPanel);

        Grid<TagsetDefinition> tagsetGrid = new Grid<>();
        tagsetGrid.setWidth("100%");
        tagsetGrid.setDataProvider(tagsetsDP);

        tagsetGrid.addColumn(
                new ComponentRenderer(() -> VaadinIcon.TAGS.create())).setFlexGrow(0).setWidth("3em");
        tagsetGrid.addColumn(
                TagsetDefinition::getName).setFlexGrow(1);
        tagsetGrid.addColumn(
                new ComponentRenderer((nan) -> new IconButton(VaadinIcon.ELLIPSIS_DOTS_V.create())))
                .setFlexGrow(0).setWidth("2em");

        Span tagsetsLabel = new Span("Tagsets");
        tagsetsLabel.setWidth("100%");

        ActionGridComponent tagsetsGridComponent = new ActionGridComponent<Grid<TagsetDefinition>>(
                tagsetsLabel,
                tagsetGrid
        );
        tagsetsGridComponent.setWidth("100%");
        tagsetPanel.add(tagsetsGridComponent);

        VerticalLayout propertiesPanel = new VerticalLayout();
        propertiesPanel.setSizeUndefined();
        mainColumns.add(propertiesPanel);

        ContextMenu contextMenu = tagsetsGridComponent.getActionGridBar().getBtnAddContextMenu();
        contextMenu.addItem("Add Tag", e -> Notification.show("Hell"));
        contextMenu.addItem("Add Subtag", e -> Notification.show("Fire"));
        contextMenu.setOpenOnClick(true);

        Grid<User> propertiesGrid = new Grid<>();
//        teamGrid.setDataProvider(membersDP);

//        teamGrid.addColumn(
//                new ComponentRenderer(() -> VaadinIcon.USER.create())).setFlexGrow(0).setWidth("3em");
//        teamGrid.addColumn(
//                User::getName).setFlexGrow(1);
//        teamGrid.addColumn(
//                new ComponentRenderer((nan) -> new IconButton(VaadinIcon.ELLIPSIS_DOTS_V.create())))
//                .setFlexGrow(0).setWidth("2em");

        Span propertiesLabel = new Span("Properties");
        propertiesLabel.setWidth("100%");
        ActionGridComponent propertiesGridComponent = new ActionGridComponent<Grid<User>>(
                propertiesLabel,
                propertiesGrid
        );

        propertiesPanel.add(propertiesGridComponent);

        return content;
    }

    @Override
    public void setParameter(BeforeEvent event, String projectId) {
        try {
            projectReference = projectManager.findProjectReferenceById(Objects.requireNonNull(projectId));
            eventBus.post(new HeaderContextChangeEvent(
                    new Div(new Span(projectReference.getName()))));
            initProject(projectReference);
        } catch (IOException e) {
            errorLogger.showAndLogError("Project " + projectId+ "couldn't be opened", e);
        }
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
                TagsView.this.repository = repository;
                initData();
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
        tagsetsDP.refreshAll();
    }

    /* Actions */

 }
