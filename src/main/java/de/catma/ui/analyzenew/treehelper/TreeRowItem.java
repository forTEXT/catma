package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;

import com.vaadin.icons.VaadinIcons;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public interface TreeRowItem {



public String 	getTreeKey(); 
public int 	getFrequency();
public QueryResultRowArray getRows();
public String getShortenTreeKey();




public default String getSelectIcon() {
	   return VaadinIcons.ARROW_CIRCLE_DOWN_O.getHtml();
}

public  String getArrowIcon();

}
