package de.catma.ui.modules.dashboard;

import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import de.catma.project.ProjectManager;
import de.catma.rbac.IRBACManager;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.main.ErrorHandler;

/**
 * Renders a new Project link styled as a card.
 * 
 * @author db
 *
 */
public class JoinProjectCard extends VerticalLayout {

	private final ErrorHandler errorLogger;
	private final EventBus eventBus;
	private final IRBACManager privilegedRBACManager;
	private final RBACSubject currentUser;
	
	@Inject
	public JoinProjectCard(IRBACManager privilegedRBACManager, RBACSubject currentUser, EventBus eventBus){
		this.privilegedRBACManager = privilegedRBACManager;
		this.currentUser = currentUser;
		this.errorLogger = (ErrorHandler) UI.getCurrent();
        this.eventBus = eventBus;
        initComponents();

	}

	private void initComponents() {
        addStyleName("projectlist__newproject");

        CssLayout newproject = new CssLayout();
        newproject.addStyleName("projectlist__newproject__link");
        Label labelDesc = new Label("join project");
        labelDesc.setWidth("100%");
        newproject.addComponents(labelDesc);

        newproject.addLayoutClickListener(evt -> {
        	new JoinProjectDialog(privilegedRBACManager, currentUser, eventBus ).show();
        });
        addComponent(newproject);

        HorizontalLayout descriptionBar = new HorizontalLayout();
        descriptionBar.addStyleName("projectlist__card__descriptionbar");
        descriptionBar.setAlignItems(FlexLayout.AlignItems.BASELINE);
        descriptionBar.setWidth("100%");

        addComponents(descriptionBar);
		
	}
}
