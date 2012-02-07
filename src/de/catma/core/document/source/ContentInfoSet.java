package de.catma.core.document.source;

import java.util.Locale;

public class ContentInfoSet {

	private String author;
	private String description;
	private String publisher;
	private String title;
    private Locale locale;
    
	public ContentInfoSet(String author, String description, String publisher,
			String title, Locale locale) {
		super();
		this.author = author;
		this.description = description;
		this.publisher = publisher;
		this.title = title;
		this.locale = locale;
	}

	public ContentInfoSet() {
		this.author = "";
		this.description = "";
		this.title = "";
		this.publisher = "";
	}

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
    
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	
}
