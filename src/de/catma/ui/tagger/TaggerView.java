package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;

import de.catma.core.document.Range;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagInstance;
import de.catma.core.tag.TagManager;
import de.catma.core.tag.TagsetDefinition;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.tagger.Tagger.TaggerListener;
import de.catma.ui.tagger.pager.Pager;
import de.catma.ui.tagger.pager.PagerComponent;
import de.catma.ui.tagger.pager.PagerComponent.PageChangeListener;
import de.catma.ui.tagmanager.ColorButtonColumnGenerator.ColorButtonListener;

public class TaggerView extends VerticalLayout implements TaggerListener {
	
	private SourceDocument sourceDocument;
	private Tagger tagger;
	private Pager pager;
	private MarkupPanel markupPanel;
	private boolean init = true;
	private TagManager tagManager;
	
	public TaggerView(TagManager tagManager, SourceDocument sourceDocument, Repository repository) {
		this.tagManager = tagManager;
		this.sourceDocument = sourceDocument;
		initComponents(repository);
	}

	private void initComponents(Repository repository) {
		VerticalLayout taggerPanel = new VerticalLayout();
		taggerPanel.setSpacing(true);
		
		pager = new Pager(80, 30);
		
		tagger = new Tagger(pager, this);
		
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
				repository,
				new ColorButtonListener() {
			
					public void colorButtonClicked(TagDefinition tagDefinition) {
						tagger.addTagInstanceWith(tagDefinition);
					}
				},
				new PropertyChangeListener() {
					
					public void propertyChange(PropertyChangeEvent evt) {
						boolean selected = evt.getNewValue() != null;
						@SuppressWarnings("unchecked")
						List<TagReference> tagReferences = 
							(List<TagReference>)(
									selected?evt.getNewValue():evt.getOldValue());
						
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
			// TODO: should be changeable by the user:
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
	
	public void tagInstanceAdded(
			ClientTagInstance clientTagInstance) {
		
		TagDefinition tagDef = 
				markupPanel.getTagDefinition(
						clientTagInstance.getTagDefinitionID());
		TagsetDefinition tagsetDef = 
				markupPanel.getTagsetDefinition(tagDef);
		
		if (!markupPanel.getCurrentWritableUserMarkupCollection()
				.getTagLibrary().contains(tagsetDef)) {
			tagManager.addTagsetDefinition(
				markupPanel.getCurrentWritableUserMarkupCollection()
					.getTagLibrary(), tagsetDef);
		}
		
		TagInstance ti = 
			new TagInstance(
					clientTagInstance.getInstanceID(),
					markupPanel.getTagDefinition(
							clientTagInstance.getTagDefinitionID()));
		
		List<TagReference> tagReferences = new ArrayList<TagReference>();
		
		try {
			for (TextRange tr : clientTagInstance.getRanges()) {
				Range r = new Range(tr.getStartPos(), tr.getEndPos());
				TagReference ref = 
						new TagReference(ti, sourceDocument.getID() ,r);
				tagReferences.add(ref);
			}
			markupPanel.addTagReferences(tagReferences);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
