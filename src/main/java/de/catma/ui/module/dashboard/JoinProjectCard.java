package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.user.User;

/**
 * Renders a join project link styled as a card.
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

		CssLayout joinProjectLayout = new CssLayout();
		joinProjectLayout.addStyleName("projectlist__newproject__link");
		joinProjectLayout.addLayoutClickListener(
				layoutClickEvent -> new JoinProjectDialog(
						currentUser,
						eventBus
				).show()
		);

		Label labelDesc = new Label("join project");
		labelDesc.setWidth("100%");
		joinProjectLayout.addComponents(labelDesc);

		addComponent(joinProjectLayout);

		HorizontalFlexLayout titleAndActionsLayout = new HorizontalFlexLayout();
		titleAndActionsLayout.addStyleName("projectlist__card__title-and-actions");
		titleAndActionsLayout.setAlignItems(FlexLayout.AlignItems.BASELINE);
		titleAndActionsLayout.setWidth("100%");

		addComponents(titleAndActionsLayout);
	}
}
