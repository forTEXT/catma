package de.catma.repository.git.model_wrappers;

import com.jsoniter.annotation.JsonIgnore;
import de.catma.document.source.ContentInfoSet;

public class GitContentInfoSet {
	private ContentInfoSet contentInfoSet;

	public GitContentInfoSet() {
		this.contentInfoSet = new ContentInfoSet();
	}

	public GitContentInfoSet(ContentInfoSet contentInfoSet) {
		this.contentInfoSet = contentInfoSet;
	}

	@JsonIgnore
	public ContentInfoSet getContentInfoSet() {
		return this.contentInfoSet;
	}

	public String getAuthor() {
		return this.contentInfoSet.getAuthor();
	}

	public void setAuthor(String author) {
		this.contentInfoSet.setAuthor(author);
	}

	public String getDescription() {
		return this.contentInfoSet.getDescription();
	}

	public void setDescription(String description) {
		this.contentInfoSet.setDescription(description);
	}

	public String getPublisher() {
		return this.contentInfoSet.getPublisher();
	}

	public void setPublisher(String publisher) {
		this.contentInfoSet.setPublisher(publisher);
	}

	public String getTitle() {
		return this.contentInfoSet.getTitle();
	}

	public void setTitle(String title) {
		this.contentInfoSet.setTitle(title);
	}
}
