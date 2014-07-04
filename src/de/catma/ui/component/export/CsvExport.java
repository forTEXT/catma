package de.catma.ui.component.export;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.csvreader.CsvWriter;
import com.vaadin.addon.tableexport.TableExport;
import com.vaadin.ui.Table;

import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;

public class CsvExport extends TableExport {
	
	public static class CsvExportException extends RuntimeException {

		CsvExportException(Throwable cause) {
			super(cause);
		}
	}
	
	private File exportFile;

	public CsvExport(Table table) {
		super(table);
	}

	@Override
	public void convertTable() {
		FileOutputStream fileOut = null;
        try {
        	Table table = getTable();
			exportFile = File.createTempFile(new IDGenerator().generate(), ".csv");
			fileOut = new FileOutputStream(exportFile);
			final CsvWriter writer = new CsvWriter(fileOut, ',', Charset.forName("UTF-8"));

			for (Object itemId : table.getItemIds()) {
				for (Object propertyId : 
					table.getContainerDataSource().getContainerPropertyIds()) {
					
					Object value =
						table.getItem(itemId).getItemProperty(propertyId).getValue();
					
					if (value == null) {
						writer.write("");
					}
					else {
						writer.write(value.toString());
					}
				}
				writer.endRecord();
			}
			
			CloseSafe.close(new Closeable() {
				public void close() throws IOException {
					writer.close();
				}
			});
			
			CloseSafe.close(fileOut);
		} catch (Exception e) {
			CloseSafe.close(fileOut);
			throw new CsvExportException(e);
		}
		
	}

	@Override
	public boolean sendConverted() {
		return sendConvertedFileToUser(
			getTable().getApplication(), 
			exportFile, "Table-Export.csv", "text/csv");
	}

}
