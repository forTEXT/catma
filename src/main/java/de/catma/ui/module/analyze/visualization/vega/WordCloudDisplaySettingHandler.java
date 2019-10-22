package de.catma.ui.module.analyze.visualization.vega;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.compress.utils.IOUtils;

import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;

public class WordCloudDisplaySettingHandler implements DisplaySettingHandler {
	
	/*
	 * String vegaScript;
	 * 
	 * @Override public void handleDisplaySetting(DisplaySetting displaySetting,
	 * VegaPanel vegaPanel) throws IOException {
	 * 
	 * 
	 * ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	 * 
	 * IOUtils.copy(Thread.currentThread().getContextClassLoader().
	 * getResourceAsStream(
	 * "/de/catma/ui/module/analyze/visualization/vega/resources/word_cloud.json"),
	 * buffer); vegaScript = buffer.toString("UTF-8");
	 * 
	 * vegaPanel.setVegaScript(vegaScript); // TODO Auto-generated method stub
	 * 
	 * }
	 */
	
	
	private String groupByPhraseScript = null;
	private String groupByTagScript = null;
	
	@Override
	public void handleDisplaySetting(DisplaySetting displaySetting, VegaPanel vegaPanel) throws IOException {
		
		if (displaySetting.equals(DisplaySetting.GROUPED_BY_PHRASE)) {
			vegaPanel.setVegaScript(getGroupByPhraseScript());
		}
		else {
			vegaPanel.setVegaScript(getGroupByTagScript());
		}
	}
	
	private String getGroupByPhraseScript() throws IOException {
		if (groupByPhraseScript == null) {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			
			IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream(
					"/de/catma/ui/module/analyze/visualization/vega/resources/phrase_word_cloud.json"), buffer);
			groupByPhraseScript = buffer.toString("UTF-8");
		}
		return groupByPhraseScript;
	}
	
	private String getGroupByTagScript() throws IOException {
		if (groupByTagScript == null) {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			
			IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream(
					"/de/catma/ui/module/analyze/visualization/vega/resources/tag_word_cloud.json"), buffer);
			groupByTagScript = buffer.toString("UTF-8");
		}
		return groupByTagScript;
	}
	
	
	

}
