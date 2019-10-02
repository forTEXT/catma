package de.catma.ui.module.analyze.visualization.vega;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.compress.utils.IOUtils;

import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;

public class WordCloudDisplaySettingHandler implements DisplaySettingHandler {
	
	String vegaScript;

	@Override
	public void handleDisplaySetting(DisplaySetting displaySetting, VegaPanel vegaPanel) throws IOException {
		

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream(
				"/de/catma/ui/module/analyze/visualization/vega/resources/word_cloud.json"), buffer);
		vegaScript = buffer.toString("UTF-8");
		
		vegaPanel.setVegaScript(vegaScript);
		// TODO Auto-generated method stub

	}

}
