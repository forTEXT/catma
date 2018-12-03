package de.catma.v10ui.modules.analyze;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;

import java.time.Instant;

/**
 *
 * A dummy View for now
 * @author db
 */
public class AnalyzeView extends Composite<Div> implements HasComponents, HasStyle {

    public AnalyzeView() {
    }

    @Override
    protected Div initContent() {
        Button testButton = new Button("Dummy " + Instant.now());
        Div content = new Div(testButton);
        content.setClassName("analyze-view");
        return content;
    }

}
