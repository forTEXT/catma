package de.catma.serialization.xls;

import java.io.OutputStream;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;

public class QueryResultExcelExporter {
	
	public void export(
			Repository repository,
			QueryResult queryResult, String sheetName, OutputStream outputStream) 
					throws Exception {

		XSSFWorkbook wb = new XSSFWorkbook();

		CellStyle numberRowStyle = wb.createCellStyle();
		numberRowStyle.setAlignment(CellStyle.ALIGN_CENTER);
//		numberRowStyle.setAlignment(HorizontalAlignment.CENTER);
		
		Sheet sheet = wb.createSheet(sheetName);
		
		int rowIdx = 
			createHeader(
				sheet, 
				sheetName, numberRowStyle);
		
		Set<GroupedQueryResult> groupedQueryResults = queryResult.asGroupedSet();
		
		for (GroupedQueryResult phraseResult : groupedQueryResults) { 
			Row totalFreqRow = sheet.createRow(rowIdx);
			rowIdx++;
			
			createCell(phraseResult.getGroup().toString(), Cell.CELL_TYPE_STRING, totalFreqRow, 0);
			createCell(null, Cell.CELL_TYPE_STRING, totalFreqRow, 1);
			createCell(phraseResult.getTotalFrequency(), Cell.CELL_TYPE_NUMERIC, totalFreqRow, 3);
			
			for (String sourceDocumentID : phraseResult.getSourceDocumentIDs()) {
				Row perDocFreqRow = sheet.createRow(rowIdx);
				rowIdx++;

				SourceDocument sourceDocument = 
						repository.getSourceDocument(sourceDocumentID);
				
				createCell(phraseResult.getGroup().toString(), Cell.CELL_TYPE_STRING, perDocFreqRow, 0);
				createCell(sourceDocument.toString(), Cell.CELL_TYPE_STRING, perDocFreqRow, 1);
				createCell(phraseResult.getFrequency(sourceDocumentID), Cell.CELL_TYPE_NUMERIC, perDocFreqRow, 2);
			}
		}
		
        wb.write(outputStream);
	}

	private void createCell(
			Object curVal,
			int cellType,
			Row sheetRow, int colIdx) {
		
		Cell sheetCell = sheetRow.createCell(colIdx);
		
		sheetCell.setCellType(cellType);
		if ((curVal != null) && (!curVal.toString().isEmpty())) {
			if (cellType == Cell.CELL_TYPE_NUMERIC) {
					sheetCell.setCellValue(((Integer)curVal).doubleValue());
			}
			else {
				sheetCell.setCellValue(curVal.toString());
			}
		}
		else {
			sheetCell.setCellValue("");
		}
	}

	private int createHeader(
			Sheet sheet, String name, CellStyle numberRowStyle) {
		
		int rowIdx = 0;
		
		Row headerLabelRow = sheet.createRow(rowIdx++);
		
		headerLabelRow.createCell(0).setCellValue("Phrase");
		headerLabelRow.createCell(1).setCellValue("Document");
		headerLabelRow.createCell(2).setCellValue("Document Frequency");
		headerLabelRow.createCell(3).setCellValue("Total Frequency");

		Row emptyRow = sheet.createRow(rowIdx++);
		emptyRow.createCell(0);
		
		sheet.createFreezePane(0, rowIdx);
		
		return rowIdx;
	}

}
