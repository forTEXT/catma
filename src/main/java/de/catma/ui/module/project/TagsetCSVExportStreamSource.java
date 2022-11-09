package de.catma.ui.module.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.UI;

import de.catma.project.Project;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.ColorConverter;
import de.catma.util.IDGenerator;

public class TagsetCSVExportStreamSource implements StreamSource {

	private Supplier<Set<TagsetDefinition>> tagsetsSupplier;
	private Supplier<Project> projectSupplier;

	public TagsetCSVExportStreamSource(Supplier<Set<TagsetDefinition>> tagsetsSupplier, Supplier<Project> projectSupplier) {
		this.tagsetsSupplier = tagsetsSupplier;
		this.projectSupplier = projectSupplier;
	}

	@Override
	public InputStream getStream() {
		final UI ui = UI.getCurrent();
		
		Set<TagsetDefinition> tagsets = tagsetsSupplier.get();
		Project project = projectSupplier.get();
		
		if (tagsets != null && !tagsets.isEmpty()) {
			
			try {
				File tempFile = File.createTempFile(new IDGenerator().generate() + "_TagLibrary_Export", "xml");
				try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile))) {
					try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withDelimiter(';'))) {
						
						csvPrinter.print("Tagset");
						csvPrinter.print("Tagset ID");
						csvPrinter.print("Tag");
						csvPrinter.print("Tag ID");
						csvPrinter.print("Tag Path");
						csvPrinter.print("Tag Parent ID");
						csvPrinter.print("Tag Color");
						csvPrinter.print("Tag Author");
						csvPrinter.print("Project Name");
						csvPrinter.print("Project ID");
						csvPrinter.print("Tag Properties & Values & Property ID");
						csvPrinter.println();

						for (TagsetDefinition tagset : tagsets) {
							Collection<TagDefinition> sortedTags = 
									tagset.stream().sorted((t1, t2) -> {
										if (t1.getName().equals(t2.getName())) {
											return t1.getUuid().compareTo(t2.getUuid());
										}
										
										return t1.getName().compareTo(t2.getName());
									})
									.collect(Collectors.toList());
							
							for (TagDefinition tag : sortedTags) {
								csvPrinter.print(tagset.getName());
								csvPrinter.print(tagset.getUuid());
								csvPrinter.print(tag.getName());
								csvPrinter.print(tag.getUuid());
								csvPrinter.print(tagset.getTagPath(tag));
								csvPrinter.print(tag.getParentUuid());
								csvPrinter.print("#"+ColorConverter.toHex(tag.getColor()));
								csvPrinter.print(tag.getAuthor());
								csvPrinter.print(project.getName());
								csvPrinter.print(project.getId());
								ArrayList<PropertyDefinition> sortedProperties = 
									new ArrayList<>(tag.getUserDefinedPropertyDefinitions());
								
								Collections.sort(sortedProperties, (p1, p2) -> {
									if (p1.getName().equals(p2.getName())) {
										return p1.getUuid().compareTo(p2.getUuid());
									}
									return p1.getName().compareTo(p2.getName());
								});
								for (PropertyDefinition propertyDef : sortedProperties) {
									csvPrinter.print(
										propertyDef.getName()
										+propertyDef.getPossibleValueList().stream().sorted().collect(Collectors.toList())
										+" "+propertyDef.getUuid());
								}
								csvPrinter.println();
							}
						}
					}
				}

		        return new FileInputStream(tempFile);
		        		
			} catch (Exception e) {
				((ErrorHandler)ui).showAndLogError("Error exporting Tagsets to XML!", e);
			}
		}
		
		
		return null;
	}

}
