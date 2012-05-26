package de.catma.ui.dialog;

public interface SaveCancelListener<T> {
	public void savePressed(T result);
	public void cancelPressed();
}