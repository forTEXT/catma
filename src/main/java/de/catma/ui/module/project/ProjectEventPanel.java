package de.catma.ui.module.project;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.lib.Constants;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.BackendPager;
import de.catma.project.CommitInfo;
import de.catma.project.Project;
import de.catma.ui.component.PagerComponent;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.events.RefreshEvent;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Member;

public class ProjectEventPanel extends VerticalLayout {
	
	private static final String SYNCHRONIZED_BRANCH = "synchronized";
	
	private final EventBus eventBus;
	private Project project;
	private final ErrorHandler errorHandler;
	private Grid<CommitInfo> commitsGrid;
	private ActionGridComponent<Grid<CommitInfo>> commitsActionGridComponent;
	private ComboBox<String> cbBranch;
	private DateField activitiesBeforeDateInput;
	private DateField activitiesAfterDateInput;
	private ComboBox<String> cbAuthor;
	private PagerComponent pagerComponent;
	private BackendPager<CommitInfo> commitsPager;

	public ProjectEventPanel(EventBus eventBus) {
		this.eventBus = eventBus;
        this.errorHandler = (ErrorHandler)UI.getCurrent();

		initComponents();
		initActions();
		this.eventBus.register(this);
	}

	private void initData() {
		try {
			String branch = SYNCHRONIZED_BRANCH.equals(cbBranch.getValue())?Constants.MASTER:cbBranch.getValue();
			this.commitsPager = project.getCommits(activitiesAfterDateInput.getValue(), activitiesBeforeDateInput.getValue(), branch, cbAuthor.getValue());

			if (this.commitsPager.hasNext()) {
				this.pagerComponent.setLastPageNumber(this.commitsPager.nextPage());
			}
			else {
				this.pagerComponent.setLastPageNumber(1);
			}
			
			this.commitsGrid.setDataProvider(DataProvider.ofCollection(this.commitsPager.current()));
			
			this.pagerComponent.setPage(1);
		
		} catch (IOException e) {
			this.errorHandler.showAndLogError(String.format("Failed to load commits for project %s", project), e);
		}
	}

	private void initActions() {
		cbAuthor.addValueChangeListener(event -> initData());
		activitiesAfterDateInput.addValueChangeListener(event -> initData());
		activitiesBeforeDateInput.addValueChangeListener(event -> initData());
		cbBranch.addValueChangeListener(event -> initData());
	}

	private void initComponents() {
		
		setWidth("900px");
		setHeight("100%");

		Label commitsLabel = new Label("Activities");
		
		commitsGrid = new Grid<CommitInfo>();

		commitsGrid.addStyleName("project-event-panel-commits-grid");
		commitsGrid.setSelectionMode(SelectionMode.NONE);

		commitsGrid.setSizeFull();
		
		commitsGrid
			.addColumn(commitInfo -> commitInfo.getMsgTitle())
			.setCaption("Title")
			.setMinimumWidthFromContent(false)
			.setExpandRatio(1);

		commitsGrid
			.addColumn(commitInfo -> commitInfo.getAuthor())
			.setCaption("Author")
			.setMinimumWidthFromContent(false)
			.setMinimumWidth(100);

		commitsGrid
			.addColumn(commitInfo -> commitInfo.getCommittedDate() == null ? "" :
					commitInfo.getCommittedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
			.setCaption("Date");

		commitsGrid.setDescriptionGenerator(commitInfo -> commitInfo.getMsg());

		commitsActionGridComponent = new ActionGridComponent<Grid<CommitInfo>>(commitsLabel, commitsGrid);
		commitsActionGridComponent.setSelectionModeFixed(SelectionMode.NONE);

		commitsActionGridComponent.getActionGridBar().setMargin(new MarginInfo(false, false, false, true));
		commitsActionGridComponent.getActionGridBar().setAddBtnVisible(false);
		commitsActionGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
		commitsActionGridComponent.getActionGridBar().setSearchInputVisible(false);
		
		commitsActionGridComponent.addStyleName("project-event-panel-commits-action-grid-component");
		
		activitiesBeforeDateInput = new DateField("Events Up To");
		commitsActionGridComponent.getActionGridBar().addComponentAfterSearchField(activitiesBeforeDateInput);
		activitiesBeforeDateInput.setValue(LocalDate.now());
		
		activitiesAfterDateInput = new DateField("Events From");
		commitsActionGridComponent.getActionGridBar().addComponentAfterSearchField(activitiesAfterDateInput);
		activitiesAfterDateInput.setValue(LocalDate.now().minus(2, ChronoUnit.WEEKS));
		
		cbAuthor = new ComboBox<String>("Author");
		cbAuthor.setEmptySelectionAllowed(true);
		cbAuthor.setTextInputAllowed(true);
		cbAuthor.setNewItemProvider(t -> Optional.of(t));
		commitsActionGridComponent.getActionGridBar().addComponentAfterSearchField(cbAuthor);

		cbBranch = new ComboBox<String>("Branch");
		cbBranch.setEmptySelectionAllowed(false);
		commitsActionGridComponent.getActionGridBar().addComponentAfterSearchField(cbBranch);

		addComponent(commitsActionGridComponent);
		setExpandRatio(commitsActionGridComponent, 0.9f);
		
		pagerComponent = new PagerComponent(
				(pagerListener) -> {}, 
				() -> (commitsPager != null && commitsPager.hasNext())? commitsPager.getCurrentPage()+1:commitsPager.getCurrentPage(), 
				page -> handlePageChange(page));
		pagerComponent.addStyleName("project-event-panel-pager-component");
		this.pagerComponent.setLastPageButtonVisible(false);
		this.pagerComponent.setAllowInputPastLastPageNumber(true);
		
		addComponent(pagerComponent);
		setComponentAlignment(pagerComponent, Alignment.TOP_RIGHT);
		setExpandRatio(pagerComponent, 0.1f);
		setSpacing(false);
		
	}
	
	
	private void handlePageChange(int page) {
		if (this.commitsPager != null) {
			this.commitsGrid.setDataProvider(DataProvider.ofCollection(this.commitsPager.page(page)));
			if (this.commitsPager.hasNext()) {
				this.pagerComponent.setLastPageNumber(this.commitsPager.nextPage());
			}
		}
		else {
			this.commitsGrid.setDataProvider(DataProvider.ofCollection(Collections.emptyList()));
		}
	}

	public void setProject(Project project) {
		this.project = project;
		handleRefresh(new RefreshEvent());
	}
	
	@Subscribe
	public void handleRefresh(RefreshEvent refreshEvent) {
		try {
			List<String> sortedUsers = project.getProjectMembers().stream().map(Member::getIdentifier).sorted().toList();
			cbAuthor.setDataProvider(new ListDataProvider<String>(sortedUsers));
			ArrayList<String> userBranches = new ArrayList<String>();
			userBranches.add(SYNCHRONIZED_BRANCH);
			userBranches.addAll(sortedUsers);
			
			cbBranch.setDataProvider(new ListDataProvider<String>(userBranches));
			cbBranch.setValue(SYNCHRONIZED_BRANCH);
		}
		catch (IOException e) {
			errorHandler.showAndLogError(String.format("Failed to load project event data for project %s", project), e);
		}
	}
	
	public void close() {
		eventBus.unregister(this);
	}
}
