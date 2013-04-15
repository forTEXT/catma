package de.catma.ui.visualizer;

import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Link;
import com.vaadin.ui.Slider.ValueOutOfBoundsException;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.source.KeywordInContext;
import de.catma.ui.Slider;
import de.catma.ui.tabbedview.ClosableTab;

public class DoubleTreeView  extends VerticalLayout implements ClosableTab {
	
	private Slider widthSlider;
	private DoubleTree doubleTree;
	private List<KeywordInContext> kwics;
	private CheckBox cbCaseSensitive;

	public DoubleTreeView(List<KeywordInContext> kwics) {
		initComponents();
		initActions();
		this.kwics = kwics;
		doubleTree.setupFromArrays(kwics, true);
	}

	private void initActions() {
		widthSlider.addListener(new ValueChangeListener() {
			public void valueChange(ValueChangeEvent event) {
				int width = ((Double)widthSlider.getValue()).intValue()*1600/100;
				doubleTree.setVisWidth(width);
				requestRepaint();
			}
		});
		
		cbCaseSensitive.addListener(new ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				
				doubleTree.setupFromArrays(kwics, cbCaseSensitive.booleanValue());
				requestRepaint();
			}
		});
		
	}

	private void initComponents() {
		setMargin(true);
		setSpacing(true);
		Link citeLink = new Link(
				"DoubleTreeJS info",
				new ExternalResource(
				"http://www.sfs.uni-tuebingen.de/~cculy/software/DoubleTreeJS/index.html"));
		
		citeLink.setTargetName("_blank");
		addComponent(citeLink);
		setComponentAlignment(citeLink, Alignment.TOP_RIGHT);
		
		widthSlider = new Slider("Tree width", 1, 100, "%");
		widthSlider.setImmediate(true);
		try {
			widthSlider.setValue(50d);
		} catch (ValueOutOfBoundsException ignore) {/*noop*/}
		
		addComponent(widthSlider);
		setComponentAlignment(widthSlider, Alignment.TOP_LEFT);
		
		cbCaseSensitive = new CheckBox("case sensitive", true);
		cbCaseSensitive.setImmediate(true);
		addComponent(cbCaseSensitive);

		doubleTree = new DoubleTree();
		doubleTree.setHeight("100%");
		
		addComponent(doubleTree);
	}
	
	@Override
	public void attach() {
		super.attach();
	}

	public void close() { /* noop */ }

	public void addClickshortCuts() { /* noop */ }
	
	public void removeClickshortCuts() { /* noop */ }	
	
	@Override
	public String toString() {
		return "KWIC as a DoubleTree";
	}
}
