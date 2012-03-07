package de.catma.ui.repository;

public interface ContentInfo {

	public String getAuthor();
	public String getTitle();
	public String getDescription();
	public String getPublisher();
	
	public void setAuthor(String author);
	public void setTitle(String title);
	public void setDescription(String description);
	public void setPublisher(String publisher);
	
}
