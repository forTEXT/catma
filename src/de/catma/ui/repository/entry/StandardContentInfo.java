package de.catma.ui.repository.entry;

public class StandardContentInfo implements ContentInfo {

	private String title = "N/A";
	private String author = "N/A";
	private String description = "N/A";
	private String publisher = "N/A";
	
	public StandardContentInfo() {
	}
	
	public StandardContentInfo(String title, String author, String description,
			String publisher) {
		super();
		this.title = title;
		this.author = author;
		this.description = description;
		this.publisher = publisher;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	
	
}
