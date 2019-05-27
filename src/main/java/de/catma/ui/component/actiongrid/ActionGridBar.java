package de.catma.ui.component.actiongrid;

import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;

import de.catma.ui.component.IconButton;
import de.catma.ui.layout.FlexLayout;

/**
 * An action bar with a description component and control buttons used by ActionGridComponent
 *
 * @author db
 */
public class ActionGridBar extends FlexLayout {

    private final Component titleComponent;
    private final IconButton btnAdd;
    private final IconButton btnMoreOptions;
    private final IconButton btnToggleMultiselect;

    private final ContextMenu ctmAdd;
    private final ContextMenu ctmMoreOptions;
	private TextField searchField;

    public ActionGridBar(Component titleComponent){
        btnAdd = new IconButton( VaadinIcons.PLUS);
        btnMoreOptions = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
        btnToggleMultiselect = new IconButton(VaadinIcons.LIST_SELECT);
        searchField = new TextField();
        searchField.setPlaceholder("\u2315");
        ctmAdd = new ContextMenu(btnAdd,true);
        ctmMoreOptions = new ContextMenu(btnMoreOptions,true);

        btnAdd.addClickListener((evt) ->  ctmAdd.open(evt.getClientX(), evt.getClientY()));
        btnMoreOptions.addClickListener((evt) ->  ctmMoreOptions.open(evt.getClientX(), evt.getClientY()));

        this.titleComponent = titleComponent;
        initComponents();
    }

    protected void initComponents() {
        setAlignItems(FlexLayout.AlignItems.CENTER);
        addStyleName("actiongrid__bar");

        addComponent(btnToggleMultiselect);
        addComponent(titleComponent);
        addComponent(searchField);
        addComponent(btnAdd);
        addComponent(btnMoreOptions);

        titleComponent.setWidth("100%");
    }

    public ContextMenu getBtnAddContextMenu() {
        return this.ctmAdd;
    }

    public ContextMenu getBtnMoreOptionsContextMenu() { return this.ctmMoreOptions; }

    public Registration addBtnAddClickListener(ClickListener listener){
       return btnAdd.addClickListener(listener);
    }

    public Registration addBtnMoreOptionsClickListener(ClickListener listener){
        return btnMoreOptions.addClickListener(listener);
    }

    public Registration addBtnToggleListSelect(ClickListener listener){
        return btnToggleMultiselect.addClickListener(listener);
    }
    
    public Registration addSearchValueChangeListener(ValueChangeListener<String> valueChangeListener) {
    	return searchField.addValueChangeListener(valueChangeListener);
    }

	public void setSearchInputVisible(boolean visible) {
		searchField.setVisible(visible);
	}
}
