package de.catma.v10ui.components.html;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;i&gt;</code> element.
 *
 * @author db
 */
@Tag("I")
public class I extends HtmlContainer implements ClickNotifier {

    /**
     * Creates a new empty i.
     */
    public I() {
        super();
    }

    /**
     * Creates a new i with the given child components.
     *
     * @param components the child components
     */
    public I(Component... components) {
        super(components);
    }

    /**
     * Creates a new i with the given text.
     *
     * @param text
     */
    public I(String text) {
        super();
        setText(text);
    }
}
