package de.catma.v10ui.modules.project;

@FunctionalInterface
public interface ResourceCountProvider {
    int getResourceCount() throws Exception;
}
