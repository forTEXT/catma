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
	        tagger.setText(text);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    // This is called if the upload fails.
    public void uploadFailed(Upload.FailedEvent event) {
        // Log the failure on screen.
        root.addComponent(new Label("Uploading "
                + event.getFilename() + " of type '"
                + event.getMIMEType() + "' failed."));
    }
}
