package de.catma.ui.dialog;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.ui.UI;

public final class BeyondResponsibilityConfirmDialog {
	public static interface Action {
		public void execute();
	}
	
	public static void executeWithConfirmDialog(boolean needsConfirmation, Action action) {
		if (needsConfirmation) {
			ConfirmDialog.show(UI.getCurrent(), 
				"Warning",
				"One or more selected Resources are beyond your responsibility! "
				+ "Altering these Resources might result in conflicts with operations of other Project members!\n\n"
				+ "Do you want to proceed?",
				"Yes",
				"Cancel",
				new ConfirmDialog.Listener() {
					
					@Override
					public void onClose(ConfirmDialog dialog) {
						if (dialog.isConfirmed()) {
							action.execute();
						}
					}
				});
		}
		else {
			action.execute();
		}
	}
}
