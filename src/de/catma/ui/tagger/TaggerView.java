package de.catma.ui.tagger;

import java.io.IOException;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.terminal.gwt.server.WebBrowser;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;

import de.catma.core.document.source.SourceDocument;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagsetDefinition;
import de.catma.ui.client.ui.tagger.shared.TagInstance;
import de.catma.ui.tagger.Tagger.TaggerListener;
import de.catma.ui.tagger.pager.Pager;
import de.catma.ui.tagger.pager.PagerComponent;
import de.catma.ui.tagger.pager.PagerComponent.PageChangeListener;

public class TaggerView extends HorizontalLayout {
	
	private SourceDocument sourceDocument;
	private Tagger tagger;
	private Pager pager;
	private Tree tagsetsInUseTree;

	public TaggerView(SourceDocument sourceDocument) {
		this.sourceDocument = sourceDocument;
		initComponents();
	}

	private void initComponents() {
		setSpacing(true);
		
		Panel taggerPanel = new Panel();
		
		pager = new Pager(80, 30);
		
		tagger = new Tagger(pager, new TaggerListener() {
			
			public void tagInstanceAdded(TagInstance tagInstance) {
			}
		});
		
		tagger.setSizeFull();
		taggerPanel.addComponent(tagger);

		PagerComponent pagerComponent = new PagerComponent(pager, new PageChangeListener() {
			public void pageChanged(int number) {
				tagger.setPage(number);
			}
		});

		taggerPanel.addComponent(pagerComponent);
		
		addComponent(taggerPanel);
		
		tagsetsInUseTree = new Tree("Tagsets in use");
		tagsetsInUseTree.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
//				tagger.
				
			}
		});
		addComponent(tagsetsInUseTree);
	}

	public SourceDocument getSourceDocument() {
		return sourceDocument;
	}
	
	@Override
	public void attach() {
		super.attach();
		WebApplicationContext context = ((WebApplicationContext) getApplication().getContext());
		WebBrowser wb = context.getBrowser();

		float lines = (wb.getScreenHeight()/3)/12;
		pager.setMaxPageLengthInLines(Math.round(lines));
		
		try {
			tagger.setText(sourceDocument.getContent());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void attachTagsetDefinition(TagsetDefinition tagsetDefinition) {
		tagsetsInUseTree.addItem(tagsetDefinition.getName());
		for (TagDefinition tagDefinition : tagsetDefinition) {
			tagsetsInUseTree.addItem(tagDefinition.getType());
		}
		for (TagDefinition tagDefinition : tagsetDefinition) {
			String baseID = tagDefinition.getBaseID();
			TagDefinition parent = tagsetDefinition.getTagDefinition(baseID);
			if ((parent==null)||(parent.getID().equals(TagDefinition.CATMA_BASE_TAG.getID()))) {
				tagsetsInUseTree.setParent(tagDefinition, tagsetDefinition);
			}
			else {
				tagsetsInUseTree.setParent(tagDefinition, parent);
			}
		}
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			if (!tagsetsInUseTree.hasChildren(tagDefinition)) {
				tagsetsInUseTree.setChildrenAllowed(tagDefinition, false);
			}
		}	
	}
}
