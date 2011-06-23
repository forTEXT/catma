package de.catma.ui.tagger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import nu.xom.ParsingException;

import org.catma.Log;
import org.catma.SysOutHandler;
import org.catma.backgroundservice.BackgroundService;
import org.catma.backgroundservice.ExecutionListener;
import org.catma.backgroundservice.ProgressListener;
import org.catma.document.FileManager;
import org.catma.document.source.DocumentMismatchException;
import org.catma.document.source.SourceDocument;
import org.catma.document.standoffmarkup.StandoffMarkupDocument;
import org.catma.tag.Tag;
import org.catma.tag.TagManager;
import org.catma.tei.TeiTagDatabase;

public class UserMarkupDocumentWriter {
	private static class BackendProgressListener implements ProgressListener {
	    public void setIndeterminate(
	            boolean indeterminate, String jobName, Object... args) {
	        Log.DEFAULT_LOGGER.info( "Indeterminate job " + jobName + " run=" + indeterminate);
	    }

	    public void finish() {
	        Log.DEFAULT_LOGGER.info( "job finished" );
	    }

	    public void start(int jobSize, String jobName, Object... args) {
	        Log.DEFAULT_LOGGER.info( "starting job " + jobName + " size " + jobSize );
	    }

	    public void update(int alreadyDoneSize) {
	        Log.DEFAULT_LOGGER.info( "update " + alreadyDoneSize );
	    }
	}
	
    private final static String BASEDIR = "tmp";
    private StandoffMarkupDocument userMarkupDoc;
    
    public UserMarkupDocumentWriter() throws IOException, ParsingException, DocumentMismatchException {
        FileManager.SINGLETON.setLocalStorage( BASEDIR );
        TagManager.SINGLETON.setDefaultTagsetName(
            "Default Tagset");
        TagManager.SINGLETON.setCatmaInstanceUID(
            "1");
        BackgroundService.SINGLETON.setProgressListener(
                new BackendProgressListener() );
        BackgroundService.SINGLETON.shutdown();

        Logger.getLogger(Log.DEFAULT).setUseParentHandlers(false);
        Logger.getLogger(Log.DEFAULT).addHandler(new SysOutHandler());
        Logger.getLogger(Log.EXCEPTION).setUseParentHandlers(false);
        Logger.getLogger(Log.EXCEPTION).addHandler(new SysOutHandler());

        final String testPath = new File(BASEDIR+System.getProperty("file.separator")+"uploads").getAbsolutePath();

        PropertyChangeListener sourceDocLoadListener =
                new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    Log.DEFAULT_LOGGER.info("file loaded " +
                            ((SourceDocument)evt.getNewValue()).getLocalName() );
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        FileManager.SINGLETON.addPropertyChangeListener(
            FileManager.PropertyChangeEvent.sourceDocumentChanged,
            sourceDocLoadListener );

        FileManager.SINGLETON.loadStructureMarkupDocument(
                testPath +
                System.getProperty("file.separator") +
                "rose_for_emily_structure.xml",

                new ExecutionListener<StandoffMarkupDocument>() {

            public void done(StandoffMarkupDocument result) {
                FileManager.SINGLETON.loadSourceDocument(
                        testPath +
                        System.getProperty("file.separator") +
                        "rose_for_emily.txt", result, false);
            }

        });


        FileManager.SINGLETON.removePropertyChangeListener(
            FileManager.PropertyChangeEvent.sourceDocumentChanged,
            sourceDocLoadListener);

        String userMarkupPath =
                testPath + System.getProperty("file.separator") +
                "rose_for_emily_user.xml";
        final TeiTagDatabase tagDB =
            TeiTagDatabase.loadDatabase(userMarkupPath);

        userMarkupDoc = 
        	FileManager.SINGLETON.loadUserMarkupDocument(tagDB, userMarkupPath, false);
    }
    
    public void writeTag(String tagName) {
    	Set<Tag> tags = TagManager.SINGLETON.getTagByName(tagName);
    	Tag tag = tags.iterator().next();
//    	userMarkupDoc.addTag(range, tag);
    }

}
