package de.catma;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Upload;

import de.catma.ui.tagger.Tagger;

public class SourceUploader extends CustomComponent
                        implements Upload.SucceededListener,
                                   Upload.FailedListener,
                                   Upload.Receiver {

    private Panel root;         // Root element for contained components.
    private File  file;         // File to write to.
	private Tagger tagger;

    public SourceUploader(Tagger tagger) {
    	this.tagger = tagger;
        root = new Panel("Uploader");
        setCompositionRoot(root);

        // Create the Upload component.
        final Upload upload =
                new Upload("Upload the file here", this);

        // Use a custom button caption instead of plain "Upload".
        upload.setButtonCaption("Upload Now");

        // Listen for events regarding the success of upload.
        upload.addListener((Upload.SucceededListener) this);
        upload.addListener((Upload.FailedListener) this);

        root.addComponent(upload);
        root.addComponent(new Label("Click 'Browse' to "+
                "select a file and then click 'Upload'."));

    }

    // Callback method to begin receiving the upload.
    public OutputStream receiveUpload(String filename,
                                      String MIMEType) {
        FileOutputStream fos = null; // Output stream to write to
        File dir = new File("tmp/uploads");
        if (!dir.exists()) {
        	dir.mkdirs();
        }
        file = new File("tmp/uploads/" + filename);
        try {
            if (!file.exists()) {
            	file.createNewFile();
            }
            // Open the file for writing.
            fos = new FileOutputStream(file);
        } catch (Exception e) {
            // Error while opening the file. Not reported here.
            e.printStackTrace();
            return null;
        }

        return fos; // Return the output stream to write to
    }

    // This is called if the upload is finished.
    public void uploadSucceeded(Upload.SucceededEvent event) {
//        // Log the upload on screen.
//        root.addComponent(new Label("File " + event.getFilename()
//                + " of type '" + event.getMIMEType()
//                + "' uploaded."));
        
		try {
			String text = IOUtils.toString(new FileInputStream(file), "UTF-8");
			String[] paragraphs = text.split("(\r\n)|\n");
			StringBuilder builder = new StringBuilder();
			
			for ( int pCounter = 0; pCounter<paragraphs.length; pCounter++) {
				int counter =0;
				String paragraph = paragraphs[pCounter];
				builder.append("<p n=\"" +pCounter + "\">");
//				for (int i=0; i<paragraph.length(); i++) {
//					counter++;
//					if ((counter<90)&&(paragraph.charAt(i) == ' ')) {
//						builder.append("&nbsp;");
//					}
//					else {
//						
//						if (paragraph.charAt(i) == ' ' ) {
//							counter = 0;
//							builder.append("</span><br><span>");
//						}
//						else {
//							builder.append(paragraph.charAt(i));
//						}
//					}
//				}
				String[] spans = paragraph.split("\\s+");
				if (spans.length == 0) {
					System.out.println( "span 0 in p: " + pCounter + " text:" + paragraph); 
				}
				else {
					appendSpan(builder, pCounter, 0, spans[0]);

					/*
					 * hier gehts weiter
					 * 
					 * Problem so: es koennen keine Leerzeichen markiert werden 
					 * und das Taggen klappt nicht immer.
					 */
					
					for (int spanCounter=1; spanCounter<spans.length; spanCounter++) {
//						builder.append("<span>&nbsp;</span>");
						builder.append(" ");
						appendSpan(builder, pCounter, spanCounter, spans[spanCounter]);
					}
				}
				builder.append("</p>");
				
				//builder.append("<br>");
			}
			
			text = builder.toString();
//			text = text.replaceAll("(\r\n)|\n", "</span><br><span>");
//			text = text.replaceAll("\\p{Blank}", "&nbsp;");
			
	        tagger.setHTML("<div id=\"raw-text\">"+text+"</div>");
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private void appendSpan(StringBuilder builder, int paragraphNo, int spanNo, String text) {
		builder.append("<span p=\"");
//		builder.append("<span style=\"padding-left:2px; padding-right:2px;\"p=\"");
		builder.append(paragraphNo);
		builder.append("\" n=\"");
		builder.append(spanNo);
		builder.append("\">");
		builder.append(text);
		builder.append("</span>");
    }
    
    // This is called if the upload fails.
    public void uploadFailed(Upload.FailedEvent event) {
        // Log the failure on screen.
        root.addComponent(new Label("Uploading "
                + event.getFilename() + " of type '"
                + event.getMIMEType() + "' failed."));
    }
}
