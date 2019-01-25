package de.catma.ui.tagger.annotationpanel;

import java.io.IOException;

import javax.sound.sampled.SourceDataLine;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.document.source.KeywordInContext;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.Annotation;
import de.catma.indexer.KwicProvider;
import de.catma.ui.component.IconButton;

public class AnnotationDetailsRibbon extends VerticalLayout {
	
	private HorizontalLayout ribbonPanel;
	private Button btMinimize;
	private Repository project;
	private KwicProvider kwicProvider;

	public AnnotationDetailsRibbon(Repository project) {
		this.project = project;
		initComponents();
	}
	
	public void setDocument(SourceDocument document) throws IOException {
		this.kwicProvider = new KwicProvider(document);
		ribbonPanel.removeAllComponents();
	}
	
	private void initComponents() {
		setSpacing(true);
		setSizeFull();
		
		HorizontalLayout headerPanel = new HorizontalLayout();
		headerPanel.setWidth("100%");
		addComponent(headerPanel);
		
		Label recentlyLabel = new Label("Recently selected Annotations");
		headerPanel.addComponent(recentlyLabel);
		headerPanel.setExpandRatio(recentlyLabel, 1.0f);
		
		btMinimize = new IconButton(VaadinIcons.ANGLE_DOUBLE_DOWN);
		headerPanel.addComponent(btMinimize);
		headerPanel.setComponentAlignment(btMinimize, Alignment.TOP_RIGHT);
		
		ribbonPanel = new HorizontalLayout();
		
		ribbonPanel.setSpacing(true);
		ribbonPanel.setHeight("100%");
		addComponent(ribbonPanel);
		setExpandRatio(ribbonPanel, 1.0f);
	}
	
	
	
	public Registration addMinimizeButtonClickListener(ClickListener listener) {
		return btMinimize.addClickListener(listener);
	}

	public void addAnnotation(Annotation annotation) throws IOException {
		AnnotationDetailsCard annotationDetailsCard = 
				new AnnotationDetailsCard(project, kwicProvider, annotation);
		ribbonPanel.addComponent(annotationDetailsCard, 0);
	}

}
