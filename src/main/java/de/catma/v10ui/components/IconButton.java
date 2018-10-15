package de.catma.v10ui.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.shared.Registration;
import de.catma.v10ui.components.html.I;
import de.catma.v10ui.util.Styles;

/**
 * A native button that is very small and only renders an icon inside an i tag.
 * @author db
 */
public class IconButton extends Composite<NativeButton> implements ClickNotifier<NativeButton>,
        HasStyle {

    private final Icon icon;
    private final ComponentEventListener<ClickEvent<NativeButton>> listener;

    public IconButton(Icon icon){
        this(icon,(evt -> {}));
    }

    public IconButton(Icon icon, ComponentEventListener<ClickEvent<NativeButton>> listener ){
       this(icon,listener,"1.5em");
    }

    public IconButton(Icon icon, ComponentEventListener<ClickEvent<NativeButton>> listener , String size){
        this.icon = icon;
        icon.setSize(size);
        this.listener = listener;
        addClassName(Styles.button__icon);
    }

    @Override
    protected NativeButton initContent() {
        NativeButton nativeButton = new NativeButton();
        nativeButton.addClickListener(listener);
        nativeButton.add(new I(icon));
        return nativeButton;
    }

    @Override
    public Registration addClickListener(ComponentEventListener<ClickEvent<NativeButton>> listener) {
        return this.addClickListener(listener);
    }

}
