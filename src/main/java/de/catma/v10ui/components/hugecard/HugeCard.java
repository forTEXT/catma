package de.catma.v10ui.components.hugecard;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.shared.Registration;
import de.catma.v10ui.util.Styles;

/**
 * Renders one huge card with action bar in the main view section
 *
 * @author db
 */
public class HugeCard extends Composite<Div> implements HasComponents,
        ClickNotifier<NativeButton>, FlexComponent<Div> {

    private final HugeCardBar hugeCardBar;

    public HugeCard(String title) {
        this.hugeCardBar = new HugeCardBar(title);
    }

    @Override
    protected Div initContent() {
        Div content = new Div();
        content.setClassName(Styles.hugecard);
        content.add(hugeCardBar);

        return content;
    }

    @Override
    public Registration addClickListener(ComponentEventListener<ClickEvent<NativeButton>> listener) {
        return hugeCardBar.addClickListener(listener);
    }
}
