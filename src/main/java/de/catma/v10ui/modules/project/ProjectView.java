package de.catma.v10ui.modules.project;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import de.catma.v10ui.components.hugecard.HugeCard;
import de.catma.v10ui.components.IconButton;
import de.catma.v10ui.components.actiongrid.ActionGridComponent;
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
                return getDocuments();
            },
            (query) -> getDocumentsCount()
    );

    @Inject
    public ProjectView(ProjectManager projectManager, ErrorLogger errorLogger, EventBus eventBus){
        this.projectManager = projectManager;
        this.errorLogger = errorLogger;
        this.eventBus = eventBus;
    }

    @Override
    protected HugeCard initContent() {
        HugeCard content = new HugeCard("Project");

        HorizontalLayout mainColumns = new HorizontalLayout();

        resources = new VerticalLayout();

        resources.add(new Span("resources"));

        mainColumns.add(resources);

        team = new VerticalLayout();
        team.add(new Span("team"));

        mainColumns.add(team);
        mainColumns.expand(resources);

        content.add(mainColumns);
        content.expand(mainColumns);

        HorizontalLayout resourceContent = new HorizontalLayout();
        resourceContent.setFlexGrow(1);
        resources.add(resourceContent);
        NativeButton nativeButton = new NativeButton();
        nativeButton.add();

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
        ActionGridComponent actionGridComponent = new ActionGridComponent<Grid<SourceDocument>>(
                documentsAnnotations,
                documentsGrid
        );

        resourceContent.add(actionGridComponent);

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

    private Stream<SourceDocument> getDocuments(){
        try {
            if(repository == null)
                return Stream.empty();
            return repository.getSourceDocuments().stream();
        } catch (Exception e) {
            errorLogger.showAndLogError("Documents couldn't be retrieved",e);
            return Stream.empty();
        }
    }

    /**
     * @return number of documents
     */
    private int getDocumentsCount(){
        try {
            if(repository == null){
                return 0;
            }
            return repository.getSourceDocumentsCount();
        } catch (Exception e) {
            errorLogger.showAndLogError("Documentscount couldn't be retrieved",e);
            return 0;
        }
    }

    public void initData() {
        initProject(projectReference);
        sourceDocumentDP.refreshAll();
    }


 }
