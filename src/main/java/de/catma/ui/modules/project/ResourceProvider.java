package de.catma.ui.modules.project;


import java.util.Collection;

@FunctionalInterface
public interface ResourceProvider<T> {

    Collection<T> getResources() throws Exception;
}
