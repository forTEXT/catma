package de.catma.v10ui.components.actiongrid;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.shared.Registration;
import de.catma.v10ui.components.IconButton;

/**
 * An action bar with a description component and control buttons used by ActionGridComponent
 *
 * @author db
 */
public class ActionGridBar extends Composite<HorizontalLayout> {

    private final Component titleComponent;

    private IconButton btnSearch;
    private IconButton btnAdd;
    private IconButton btnMoreOptions;

    public ActionGridBar(Component titleComponent){
        this.titleComponent = titleComponent;
    }

    @Override
    protected HorizontalLayout initContent() {
        HorizontalLayout content = new HorizontalLayout();
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.addClassName(".actiongrid__bar");
        content.setSpacing(false);
        content.add(titleComponent);
        btnSearch = new IconButton( VaadinIcon.SEARCH.create(),(evt) -> {});
        btnAdd = new IconButton( VaadinIcon.PLUS.create(),(evt) -> {});
        btnMoreOptions = new IconButton(VaadinIcon.ELLIPSIS_DOTS_V.create(), (evt) -> {});

        content.add(btnSearch);
        content.add(btnAdd);
        content.add(btnMoreOptions);

        content.expand(titleComponent);
        return content;
    }

    public Registration addBtnAddClickListener(ComponentEventListener<ClickEvent<NativeButton>> listener){
       return btnAdd.addClickListener(listener);
    }


    public Registration addBtnSearchClickListener(ComponentEventListener<ClickEvent<NativeButton>> listener){
        return btnSearch.addClickListener(listener);
    }

    public Registration addBtnMoreOptionsClickListener(ComponentEventListener<ClickEvent<NativeButton>> listener){
        return btnMoreOptions.addClickListener(listener);
    }
}
