package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.user.User;

/**
 * Renders a new Project link styled as a card.
 * 
 * @author db
 *
 */
public class JoinProjectCard extends VerticalFlexLayout {

	
	private EventBus eventBus;
	private User currentUser;

	public JoinProjectCard(User currentUser, EventBus eventBus) {
		this.currentUser = currentUser;
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
        	new JoinProjectDialog(currentUser, eventBus).show();
        });
        addComponent(newproject);

        HorizontalFlexLayout descriptionBar = new HorizontalFlexLayout();
        descriptionBar.addStyleName("projectlist__card__descriptionbar");
        descriptionBar.setAlignItems(FlexLayout.AlignItems.BASELINE);
        descriptionBar.setWidth("100%");

        addComponents(descriptionBar);
		
	}
}
