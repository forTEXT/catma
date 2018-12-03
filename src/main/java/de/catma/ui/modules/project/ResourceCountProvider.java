package de.catma.ui.modules.project;

@FunctionalInterface
public interface ResourceCountProvider {
    int getResourceCount() throws Exception;
}
