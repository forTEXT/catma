package de.catma.ui.tagger;

import java.io.IOException;
import java.util.List;

import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagManager;
import de.catma.ui.client.ui.tagger.shared.TagInstance;
import de.catma.ui.tagger.MarkupCollectionsPanel.TagDefinitionSelectionListener;
import de.catma.ui.tagger.Tagger.TaggerListener;
import de.catma.ui.tagger.pager.Pager;
import de.catma.ui.tagger.pager.PagerComponent;
import de.catma.ui.tagger.pager.PagerComponent.PageChangeListener;
import de.catma.ui.tagmanager.ColorButtonColumnGenerator.ColorButtonListener;

public class TaggerView extends VerticalLayout {
	
	private SourceDocument sourceDocument;
	private Tagger tagger;
	private Pager pager;
	private MarkupPanel markupPanel;
	private boolean init = true;
	
	public TaggerView(TagManager tagManager, SourceDocument sourceDocument) {
		this.sourceDocument = sourceDocument;
		initComponents(tagManager);
	}

	private void initComponents(final TagManager tagManager) {
		VerticalLayout taggerPanel = new VerticalLayout();
		taggerPanel.setSpacing(true);
		
		pager = new Pager(80, 30);
		
		tagger = new Tagger(pager, new TaggerListener() {
			
			public void tagInstanceAdded(TagInstance tagInstance) {
			}
		});
		
		tagger.setSizeFull();
		taggerPanel.addComponent(tagger);

		PagerComponent pagerComponent = new PagerComponent(
				pager, new PageChangeListener() {
					
			public void pageChanged(int number) {
				tagger.setPage(number);
			}
		});

		taggerPanel.addComponent(pagerComponent);
		
		markupPanel = new MarkupPanel(
				tagManager,
				new ColorButtonListener() {
			
					public void colorButtonClicked(TagDefinition tagDefinition) {
						tagger.addTagInstanceWith(tagDefinition);
					}
				},
				new TagDefinitionSelectionListener() {
					
					public void tagDefinitionSelectionChanged(
							List<TagReference> tagReferences,
							boolean selected) {
						tagger.setVisible(tagReferences, selected);
						
					}
				});
		
		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		splitPanel.addComponent(taggerPanel);
		splitPanel.addComponent(markupPanel);
		addComponent(splitPanel);
	}

	public SourceDocument getSourceDocument() {
		return sourceDocument;
	}
	
	@Override
	public void attach() {
		super.attach();
		if (init) {
			WebApplicationContext context = 
					((WebApplicationContext) getApplication().getContext());
			WebBrowser wb = context.getBrowser();
	
			float lines = (wb.getScreenHeight()/3)/12;
			pager.setMaxPageLengthInLines(Math.round(lines));
			
			try {
				tagger.setText(sourceDocument.getContent());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			init = false;
		}
	}

	public void openUserMarkupCollection(
			UserMarkupCollection userMarkupCollection) {
		markupPanel.openUserMarkupCollection(userMarkupCollection);
	}

	public void close() {
		markupPanel.close();
	}
}
