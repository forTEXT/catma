package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;

public interface TreeItem {

public String 	getTreeKey(); 
public int 	getFrequency();
public GroupedQueryResult getRows();


}
