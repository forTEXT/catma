package de.catma.ui.component.export;

import java.util.Collection;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.vaadin.addon.tableexport.ExcelExport;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.Table;

public class HierarchicalExcelExport extends ExcelExport {

	private Table table;
	
	public HierarchicalExcelExport(Table table, String sheetName,
			String reportTitle, String exportFileName, boolean hasTotalsRow) {
		super(table, sheetName, reportTitle, exportFileName, hasTotalsRow);
		this.table = table;
	}

	public HierarchicalExcelExport(Table table, String sheetName,
			String reportTitle, String exportFileName) {
		super(table, sheetName, reportTitle, exportFileName);
		this.table = table;
	}

	public HierarchicalExcelExport(Table table, String sheetName,
			String reportTitle) {
		super(table, sheetName, reportTitle);
		this.table = table;
	}

	public HierarchicalExcelExport(Table table, String sheetName) {
		super(table, sheetName);
		this.table = table;
	}

	public HierarchicalExcelExport(Table table, Workbook wkbk,
			String sheetName, String reportTitle, String exportFileName,
			boolean hasTotalsRow) {
		super(table, wkbk, sheetName, reportTitle, exportFileName, hasTotalsRow);
		this.table = table;
	}

	public HierarchicalExcelExport(Table table) {
		super(table);
		this.table = table;
	}
	
	protected int addHierarchicalDataRows(org.apache.poi.ss.usermodel.Sheet sheetToAddTo, int row) {
        final Collection<?> roots;
        int localRow = row;
        roots = ((HierarchicalContainer) table.getContainerDataSource()).rootItemIds();
        /*
         * For HierarchicalContainers, the outlining/grouping in the sheet is with the summary row
         * at the top and the grouped/outlined subcategories below.
         */
        sheet.setRowSumsBelow(false);
        int count = 0;
        for (final Object rootId : roots) {
            count = addDataRowRecursively(sheetToAddTo, rootId, localRow);
            // for totals purposes, we just want to add rootIds which contain totals
            // so we store just the totals in a separate sheet.
            if (displayTotals) {
                addDataRow(hierarchicalTotalsSheet, rootId, localRow);
            }
            localRow = localRow + count;
        }
        return localRow;
	};
	
    private int addDataRowRecursively(final Sheet sheetToAddTo, final Object rootItemId,
            final int row) {
        int numberAdded = 0;
        addDataRow(sheetToAddTo, rootItemId, row);
        numberAdded++;
        if (((HierarchicalContainer) table.getContainerDataSource()).hasChildren(rootItemId)) {
            final Collection<?> children =
                    ((HierarchicalContainer) table.getContainerDataSource())
                            .getChildren(rootItemId);
            for (final Object child : children) {
                numberAdded = numberAdded + addDataRowRecursively(sheetToAddTo, child, row+numberAdded);
                
            }
            sheet.groupRow(row + 1, row+numberAdded-1);
            sheet.setRowGroupCollapsed(row + 1, true);
        }
        return numberAdded;
    }
	

}
