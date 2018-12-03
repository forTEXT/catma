package de.catma.ui.modules.main;

public interface ErrorLogger {

    void showAndLogError(String message, Throwable e);
}
