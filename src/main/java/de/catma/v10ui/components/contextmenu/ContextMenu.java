package de.catma.v10ui.components.contextmenu;


import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.dom.Element;

@Tag("vaadin-context-menu")
@HtmlImport("frontend://bower_components/vaadin-context-menu/src/vaadin-context-menu.html")
public class ContextMenu<T> extends Component
        implements HasStyle, ClickNotifier<ContextMenu>, HasText, Focusable<ContextMenu>, HasSize, HasEnabled, HasComponents {

    private Element template;

    public ContextMenu(ListBox<T> listBox) {
        template = new Element("template");
        template.appendChild(listBox.getElement());
        getElement().appendChild(template);
    }

    public void setFilterKey(String cssSelector){
        getElement().setProperty("selector", cssSelector);
    }

}
