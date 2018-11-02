package de.catma.v10ui.events;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.binder.HasDataProvider;

/**
 * This event indicates that resources e.g. {@link de.catma.document.source.SourceDocument} are dirty
 * and should be reloaded from Repository
 *
 * @author db
 */
public class ResourcesChangedEvent<T extends Component & HasDataProvider<?>> {
    private final T component;

    public ResourcesChangedEvent(T component){
        this.component = component;
    }

    public T getComponent() {
        return component;
    }
}
