package de.catma.ui.component.actiongrid;

import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

import de.catma.ui.component.IconButton;

/**
 * An action bar with a description component and control buttons used by ActionGridComponent
 *
 * @author db
 */
public class ActionGridBar extends HorizontalLayout {

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
    	setWidth("100%");
    	addStyleName("actiongrid__bar");

        addComponent(btnToggleMultiselect);
        addComponent(titleComponent);
        setExpandRatio(titleComponent, 1f);
        addComponent(searchField);
        addComponent(btnAdd);
        setComponentAlignment(btnAdd, Alignment.MIDDLE_RIGHT);
        addComponent(btnMoreOptions);
        setComponentAlignment(btnMoreOptions, Alignment.MIDDLE_RIGHT);
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
	
	IconButton getBtnToggleMultiselect() {
		return btnToggleMultiselect;
	}
	
	public void setAddBtnVisible(boolean visible) {
		btnAdd.setVisible(visible);
	}
	
	public void addButtonRight(Button button) {
        addComponent(button);
        setComponentAlignment(button, Alignment.MIDDLE_RIGHT);		
	}
}
