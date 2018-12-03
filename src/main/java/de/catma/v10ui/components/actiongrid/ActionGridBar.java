package de.catma.v10ui.components.actiongrid;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.contextmenu.ContextMenu;
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
    private final IconButton btnSearch;
    private final IconButton btnAdd;
    private final IconButton btnMoreOptions;
    private final IconButton btnToggleMultiselect;

    private final ContextMenu ctmAdd;
    private final ContextMenu ctmSearch;
    private final ContextMenu ctmMoreOptions;

    public ActionGridBar(Component titleComponent){
        btnSearch = new IconButton( VaadinIcon.SEARCH.create());
        btnAdd = new IconButton( VaadinIcon.PLUS.create());
        btnMoreOptions = new IconButton(VaadinIcon.ELLIPSIS_DOTS_V.create());
        btnToggleMultiselect = new IconButton(VaadinIcon.LIST_SELECT.create());
        ctmAdd = new ContextMenu(btnAdd);
        ctmSearch = new ContextMenu(btnSearch);
        ctmMoreOptions = new ContextMenu(btnMoreOptions);

        this.titleComponent = titleComponent;
    }

    @Override
    protected HorizontalLayout initContent() {
        HorizontalLayout content = new HorizontalLayout();
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.addClassName(".actiongrid__bar");
        content.setSpacing(false);

        content.add(btnToggleMultiselect);
        content.add(titleComponent);

        content.add(btnSearch);
        content.add(btnAdd);
        content.add(btnMoreOptions);

        content.expand(titleComponent);
        return content;
    }

    public ContextMenu getBtnAddContextMenu() {
        return this.ctmAdd;
    }

    public ContextMenu getBtnSearchContextMenu() {
        return this.ctmSearch;
    }

    public ContextMenu getBtnMoreOptionsContextMenu() { return this.ctmMoreOptions; }

    public Registration addBtnAddClickListener(ComponentEventListener<ClickEvent<NativeButton>> listener){
       return btnAdd.addClickListener(listener);
    }


    public Registration addBtnSearchClickListener(ComponentEventListener<ClickEvent<NativeButton>> listener){
        return btnSearch.addClickListener(listener);
    }

    public Registration addBtnMoreOptionsClickListener(ComponentEventListener<ClickEvent<NativeButton>> listener){
        return btnMoreOptions.addClickListener(listener);
    }

    public Registration addBtnToggleListSelect(ComponentEventListener<ClickEvent<NativeButton>> listener){
        return btnToggleMultiselect.addClickListener(listener);
    }
}
