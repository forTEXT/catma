package de.catma.ui.tagger.annotationpanel;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.source.KeywordInContext;
import de.catma.document.standoffmarkup.usermarkup.Annotation;
import de.catma.indexer.KwicProvider;
import de.catma.util.ColorConverter;

public class AnnotationDetailsCard extends VerticalLayout {
	
	private Annotation annotation;
	private Repository project;
	private KwicProvider kwicProvider;

	public AnnotationDetailsCard(
			Repository project, KwicProvider kwicProvider, Annotation annotation) throws IOException {
		this.project = project;
		this.kwicProvider = kwicProvider;
		this.annotation = annotation;
		//Delete
		//Collection
		//Tagset
		//Tag
		//Tagpath
		//Author
		//Userdefined Properties with values (Edit/Delete) 
		//Annotated KWIC (shortened) (Edit Range)
		//selected Text?
		initComponents();
	}

	private void initComponents() throws IOException {
		
		setMargin(true);
		setSpacing(true);

		setHeight("100%");
		setWidth("180px");
		
		StringBuilder tagLine = new StringBuilder();
		tagLine.append(
			Jsoup.clean(
				annotation.getTagInstance().getTagDefinition().getName(), 
				Whitelist.simpleText()));
		tagLine.append(
			"<span class=\"annotation-details-card-tag-color\" style=\"background:#");
		tagLine.append(
			ColorConverter.toHex(
				annotation.getTagInstance().getTagDefinition().getColor()));
		tagLine.append(";\">");
		tagLine.append("&nbsp;");
		tagLine.append("</span>");
		Label tagLineLabel = new Label(tagLine.toString());
		tagLineLabel.setContentMode(ContentMode.HTML);
		
		addComponent(tagLineLabel);
		
		List<Range> ranges =
			annotation.getTagReferences()
			.stream()
			.map(tagRef -> tagRef.getRange())
			.collect(Collectors.toList());
		
		List<KeywordInContext> keywordInContext = kwicProvider.getKwic(ranges, 5);
		//TODO:
		addComponent(new Label(keywordInContext.get(0).toString()));
		
		FormLayout form = new FormLayout();
		Label tagPathLabel = new Label(annotation.getTagPath());
		tagPathLabel.setCaption("Tag");
		form.addComponent(tagPathLabel);
		
		Label tagsetLabel = 
			new Label(
				project.getTagManager().getTagLibrary().getTagsetDefinition(
					annotation.getTagInstance().getTagDefinition().getTagsetDefinitionUuid()).getName());
		tagsetLabel.setCaption("Tagset");
		
		form.addComponent(tagsetLabel);
		
		Label collectionLabel = new Label(
			annotation.getUserMarkupCollection().getName());
		collectionLabel.setCaption("Collection");
		form.addComponent(collectionLabel);
		
		Label authorLabel = new Label(annotation.getTagInstance().getAuthor());
		authorLabel.setCaption("Author");
		
		
		addComponent(form);
		setExpandRatio(form, 1.0f);
		
		
	}

}
