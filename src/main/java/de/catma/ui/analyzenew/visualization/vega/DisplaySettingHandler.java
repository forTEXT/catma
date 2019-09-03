package de.catma.ui.analyzenew.visualization.vega;

import java.io.IOException;

import de.catma.ui.analyzenew.queryresultpanel.DisplaySetting;

public interface DisplaySettingHandler {
	public void handleDisplaySetting(DisplaySetting displaySetting, VegaPanel vegaPanel) throws IOException;
}
