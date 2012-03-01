package de.catma.ui.client.ui.tagger.tagmanager;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.IconCellDecorator;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.TreeViewModel;
import com.vaadin.terminal.gwt.client.VConsole;

import de.catma.ui.client.ui.resources.ResourceBundle;
import de.catma.ui.client.ui.tag.CPropertyDefinition;
import de.catma.ui.client.ui.tag.CTagDefinition;
import de.catma.ui.client.ui.tag.CTagsetDefinition;
import de.catma.ui.client.ui.tag.ColorConverter;
import de.catma.ui.client.ui.tag.DisplayableTagChild;

public class TagsetDefinitionsTreeModel implements TreeViewModel {
	interface ImageWrapperMiddleTemplate extends SafeHtmlTemplates {
	    /**
	     * The wrapper around the image vertically aligned to the middle.
	     */
	    @Template("<div style=\"{0}position:absolute;top:50%;line-height:0px;\">{1}</div>")
	    SafeHtml imageWrapperMiddle(SafeStyles styles, SafeHtml image);
	}

	
	private List<CTagsetDefinition> tagsetDefinitions;
	private ResourceBundle resourceBundle;
	private ListDataProvider<CTagsetDefinition> tagsetDefDataProvider;
	private ImageWrapperMiddleTemplate template;
	
	public TagsetDefinitionsTreeModel() {
		this(new ArrayList<CTagsetDefinition>());
	}

	public TagsetDefinitionsTreeModel(List<CTagsetDefinition> tagsetDefinitions) {
		super();
		this.tagsetDefinitions = tagsetDefinitions;
		tagsetDefDataProvider =
			new ListDataProvider<CTagsetDefinition>(tagsetDefinitions);
		resourceBundle = GWT.create(ResourceBundle.class);
		template = GWT.create(ImageWrapperMiddleTemplate.class);
	}

	public <T> NodeInfo<?> getNodeInfo(T value) {
		if (value == null) {
			return createTagsetDefinitonsNodeInfo();
		}
		else if (value instanceof CTagsetDefinition) {
			return createToplevelTagDefinitionNodeInfo((CTagsetDefinition)value);
		}
		else if (value instanceof CTagDefinition) {
			return createTagDefinitionNodeInfo((CTagDefinition)value);
		}
		
		return null;
	}

	private NodeInfo<?> createTagDefinitionNodeInfo(CTagDefinition tagDefinition) {
		for (CTagsetDefinition tagsetDef : this.tagsetDefinitions) {
			if (tagsetDef.hasTagDefinition(tagDefinition)) {
				List<DisplayableTagChild> childTagDefinitions =
						createTagChildrenList(tagsetDef, tagDefinition);
				childTagDefinitions.addAll(
						tagDefinition.getUserDefinedPropertyDefinitions());
				
				return new DefaultNodeInfo<DisplayableTagChild>(
					new ListDataProvider<DisplayableTagChild>(childTagDefinitions), 
					createTagDefinitionCell());
			}
		}
		
		return null;
	}

	private NodeInfo<DisplayableTagChild> createToplevelTagDefinitionNodeInfo(
			CTagsetDefinition tagsetDefinition) {
		
		List<DisplayableTagChild> topLevelTagDefinitions = 
				createTagChildrenList(
					tagsetDefinition, CTagDefinition.CATMA_BASE_TAG);
		
		return new DefaultNodeInfo<DisplayableTagChild>(
				new ListDataProvider<DisplayableTagChild>(topLevelTagDefinitions), 
				createTagDefinitionCell());
	}

	private List<DisplayableTagChild> createTagChildrenList(
			CTagsetDefinition tagsetDefinition, CTagDefinition parent) {
		
		List<DisplayableTagChild> result = new ArrayList<DisplayableTagChild>();
		
		for (CTagDefinition tagDef : tagsetDefinition) {
			if (tagDef.getBaseID().equals(parent.getID())) {
				result.add(tagDef);
			}
		}
		
		return result;
	}

	private Cell<DisplayableTagChild> createTagDefinitionCell() {
		return new IconCellDecorator<DisplayableTagChild>(
			resourceBundle.tagDefinitionIcon(), 
			new AbstractCell<DisplayableTagChild>() {
				@Override
				public void render(
						Context context,
						DisplayableTagChild value, SafeHtmlBuilder sb) {
					if (value != null) {
						sb.appendEscaped(value.getDisplayString());
						if (value instanceof CTagDefinition) {
							sb.append(
								SafeHtmlUtils.fromTrustedString(
									"<span style=\"background-color:#"
									+ new ColorConverter(
										Integer.valueOf(((CTagDefinition)value).getColor())).toHex()
									+ ";margin-left:3px;\">&nbsp;&nbsp;&nbsp;&nbsp;</span>"));
						}
					}
				}
		}) {
			@Override
			protected SafeHtml getIconHtml(DisplayableTagChild value) {
				if (value instanceof CPropertyDefinition) {
					
					return getPropertyIconHtml();					
					
				}
				return super.getIconHtml(value);
			}
		};
	}
	
	private NodeInfo<CTagsetDefinition> createTagsetDefinitonsNodeInfo() {
		
		Cell<CTagsetDefinition> tagsetDefCell =
				new IconCellDecorator<CTagsetDefinition>(
					resourceBundle.tagsetDefinitionIcon(), 
					new AbstractCell<CTagsetDefinition>() {
						@Override
						public void render(
								Context context,
								CTagsetDefinition value, SafeHtmlBuilder sb) {
							if (value != null) {
								sb.appendEscaped(value.getName());
							}
						}
				});
		
		return new DefaultNodeInfo<CTagsetDefinition>(
				tagsetDefDataProvider, tagsetDefCell);
	}

	public boolean isLeaf(Object value) {
		if (value != null) {
			
			if (value instanceof CTagsetDefinition) {
				return !((CTagsetDefinition)value).iterator().hasNext();
			}
			else if (value instanceof CTagDefinition) {
				CTagDefinition curTagDefinition = (CTagDefinition)value;
				if (curTagDefinition.getUserDefinedPropertyDefinitions().isEmpty()) {
					VConsole.log("no prop: " +value.toString());
					for (CTagsetDefinition tagsetDef : this.tagsetDefinitions) {
						if (tagsetDef.hasTagDefinition(curTagDefinition)) {
							return createTagChildrenList(
									tagsetDef, curTagDefinition).isEmpty();
						}
					}
					return true;
				}
				else {
					VConsole.log("hasProp: " + value.toString());
					return false;
				}
			}
			else {
				return true;
			}
		}
		return false;
	}

	public void addTagsetDefinition(CTagsetDefinition tagsetDefinition) {
		this.tagsetDefinitions.add(tagsetDefinition);
		tagsetDefDataProvider.refresh();
	}
	
	private SafeHtml getPropertyIconHtml() {
		ImageResource res = resourceBundle.propertyDefinitionIcon();
	    // Get the HTML for the image.
		AbstractImagePrototype proto = 
				AbstractImagePrototype.create(res);	
		SafeHtml image = SafeHtmlUtils.fromTrustedString(proto.getHTML());

	    // Create the wrapper based on the vertical alignment.
	    SafeStylesBuilder cssStyles =
	        new SafeStylesBuilder().appendTrustedString(
	        	(LocaleInfo.getCurrentLocale().isRTL() ? "right" : "left") 
	        	+ ":0px;");
	    
      int halfHeight = (int) Math.round(res.getHeight() / 2.0);
      cssStyles.appendTrustedString("margin-top:-" + halfHeight + "px;");
      
      return template.imageWrapperMiddle(cssStyles.toSafeStyles(), image);
	}

}
