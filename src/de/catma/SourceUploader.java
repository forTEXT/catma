package de.catma;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private static final String SOLIDSPACE = "&nbsp;";
	

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
      
        File dir = new File("c:/data/eclipse_workspace/clea/tmp/uploads");
        if (!dir.exists()) {
        	dir.mkdirs();
        }
        file = new File("c:/data/eclipse_workspace/clea/tmp/uploads/" + filename);
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
			Matcher matcher = Pattern.compile("(\\p{Blank}*)(\\S*)(\\p{Blank}*)((\r\n)|\n)?").matcher(text);
			StringBuilder builder = new StringBuilder();
			int lineLength = 0;
			while(matcher.find()) {
				if (lineLength + matcher.group().length()>80) {
					builder.append("<br />");
					lineLength = 0;
				}
				if (matcher.group(1) != null) {
					builder.append(getSolidSpace(matcher.group(1).length()));
				}
				if (matcher.group(2) != null) {
					builder.append(matcher.group(2));
				}
				if (matcher.group(3) != null) {
					builder.append(getSolidSpace(matcher.group(3).length()));
				}
				if (matcher.group(4) != null) {
					builder.append("<br />");
					lineLength = 0;
				}
				else {
					lineLength += matcher.group().length();
				}
			}
			text = builder.toString();
			text = text.replaceAll("(\r\n)|\n", "<br />");
	        tagger.setHTML("<div id=\"raw-text\">"+text+"</div>");
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private String getSolidSpace(int count) {
    	StringBuilder builder = new StringBuilder();
    	for (int i=0; i<count;i++) {
    		builder.append(SOLIDSPACE);
    	}
    	return builder.toString();
    }
    
    // This is called if the upload fails.
    public void uploadFailed(Upload.FailedEvent event) {
        // Log the failure on screen.
        root.addComponent(new Label("Uploading "
                + event.getFilename() + " of type '"
                + event.getMIMEType() + "' failed."));
    }
}
