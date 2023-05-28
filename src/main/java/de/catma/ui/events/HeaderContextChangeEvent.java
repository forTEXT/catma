package de.catma.ui.events;

/**
 * Fired when the information displayed in the header needs to be changed
 * <p>
 * See instantiations and {@link de.catma.ui.module.main.CatmaHeader}
 */
public class HeaderContextChangeEvent  {
	private final String projectName;
	private final boolean dashboard;
	private final boolean readOnly;

	public HeaderContextChangeEvent() {
		this.projectName = "";
		this.dashboard = true;
		this.readOnly = false;
	}

	public HeaderContextChangeEvent(String projectName) {
		this(projectName, false);
	}

	public HeaderContextChangeEvent(String projectName, boolean readOnly) {
		this.projectName = projectName;
		this.dashboard = false;
		this.readOnly = readOnly;
	}

	public String getProjectName() {
		return projectName;
	}

	public boolean isDashboard() {
		return dashboard;
	}

	public boolean isReadOnly() {
		return readOnly;
	}
}
