package de.catma.ui.module.analyze.visualization.vega;

import java.io.IOException;

import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;

public interface DisplaySettingHandler {
	public void handleDisplaySetting(DisplaySetting displaySetting, VegaPanel vegaPanel) throws IOException;
}
