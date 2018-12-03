package de.catma.v10ui.modules.main;

public interface ErrorLogger {

    void showAndLogError(String message, Throwable e);
}
