package de.catma.ui.component.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.csvreader.CsvWriter;
import com.vaadin.addon.tableexport.TableExport;
import com.vaadin.ui.Table;

public class CsvExport extends TableExport {
	
	public CsvExport(Table table) {
		super(table);
		// TODO Auto-generated constructor stub
	}

	private File exportFile;

	@Override
	public void convertTable() {
		// TODO Auto-generated method stub

		
        try {
        	Table table = getTable();
			exportFile = File.createTempFile("tmp", ".csv");
			final FileOutputStream fileOut = new FileOutputStream(exportFile);
			CsvWriter writer = new CsvWriter(fileOut, ',', Charset.forName("UTF-8"));
			String temp = new String();
			for (Object itemId : table.getItemIds()){
				for (Object propertyId : table.getContainerDataSource().getContainerPropertyIds()){
					    temp = (String) table.getItem(itemId).getItemProperty(propertyId).getValue();
						writer.write(temp);
				}
				writer.endRecord();
			}
			
			fileOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public boolean sendConverted() {
		
		return sendConvertedFileToUser(getTable().getApplication(), exportFile, "Table-Export.csv", "text/csv");
	}

}
