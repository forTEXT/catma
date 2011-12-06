package de.catma.core.document.source;

import java.util.Locale;

public class ContentInfoSet {

	private String author;
	private String description;
	private String publisher;
	private String title;
    private Locale locale;
    private String preview;
    
    
	public String getAuthor() {
		return author;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getPublisher() {
		return publisher;
	}
	
	public String getTitle() {
		return title;
	}
	
    /**
     * @return the locale of the Source Document
     */
    public Locale getLocale() {
        return (locale==null) ? Locale.getDefault() : locale;
    }
    
	public String getPreview() {
		return preview;
	}
}
