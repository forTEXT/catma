package de.catma.v10ui.components;

import com.vaadin.flow.component.*;

/**
 * A simple centered icon for grids
 *
 * @author db
 */
@Tag("centered-icon")
public class CenteredIcon extends Component implements HasStyle, HasComponents, HasSize {

    public CenteredIcon(Component component){
        super();
        add(component);
    }
}
