package de.catma.ui.module.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Supplier;

import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.UI;

import de.catma.document.source.ContentInfoSet;
import de.catma.project.Project;
import de.catma.serialization.tei.TeiDocument;
import de.catma.serialization.tei.TeiDocumentFactory;
import de.catma.serialization.tei.TeiTagLibrarySerializer;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.util.IDGenerator;

public class TagsetXMLExportStreamSource implements StreamSource {

	private Supplier<Set<TagsetDefinition>> tagsetsSupplier;
	private Supplier<Project> projectSupplier;

	public TagsetXMLExportStreamSource(Supplier<Set<TagsetDefinition>> tagsetsSupplier, Supplier<Project> projectSupplier) {
		this.tagsetsSupplier = tagsetsSupplier;
		this.projectSupplier = projectSupplier;
	}

	@Override
	public InputStream getStream() {
		final UI ui = UI.getCurrent();
		
		Set<TagsetDefinition> tagsets = tagsetsSupplier.get();
		Project project = projectSupplier.get();
		
		if (tagsets != null && !tagsets.isEmpty()) {
			
			TeiDocumentFactory teiDocumentFactory = new TeiDocumentFactory();
			try {
				final TeiDocument teiDocument = teiDocumentFactory.createEmptyDocument(null);
				
				final TeiTagLibrarySerializer teiTagSerializer = 
						new TeiTagLibrarySerializer(teiDocument, project.getVersion());
				final TagManager tagManager = new TagManager(new TagLibrary());
				tagsets.forEach(tagset -> tagManager.addTagsetDefinition(tagset));
				
				final ContentInfoSet contentInfoSet = 
						new ContentInfoSet(
								project.getUser().toString(),
								project.getDescription(),
								project.getUser().toString(), 
								project.getName());
				
				teiDocument.getTeiHeader().setValues(contentInfoSet);
				
            	
				teiTagSerializer.serialize(tagManager.getTagLibrary());

				File tempFile = File.createTempFile(new IDGenerator().generate() + "_TagLibrary_Export", "xml");
				try (FileOutputStream fos = new FileOutputStream(tempFile)) {
					teiDocument.printXmlDocument(fos);
				}

		        return new FileInputStream(tempFile);
		        		
			} catch (Exception e) {
				((ErrorHandler)ui).showAndLogError("Error exporting Tagsets to XML!", e);
			}
		}
		
		
		return null;
	}

}
