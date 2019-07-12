package de.catma.ui.modules.dashboard;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;

/**
 * Renders a new Project link styled as a card.
 * 
 * @author db
 *
 */
public class JoinProjectCard extends VerticalFlexLayout {

	private final Provider<JoinProjectDialog> joinProjectProvider;
	
	@Inject
	public JoinProjectCard(Provider<JoinProjectDialog> joinProjectProvider){
        this.joinProjectProvider = joinProjectProvider;
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
        	joinProjectProvider.get().show();
        });
        addComponent(newproject);

        HorizontalFlexLayout descriptionBar = new HorizontalFlexLayout();
        descriptionBar.addStyleName("projectlist__card__descriptionbar");
        descriptionBar.setAlignItems(FlexLayout.AlignItems.BASELINE);
        descriptionBar.setWidth("100%");

        addComponents(descriptionBar);
		
	}
}
