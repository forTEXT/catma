package de.catma.ui.module.project.documentwizard;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Locale;

import de.catma.document.source.LanguageItem;

public class UploadFile {
	
	private final String uuid;
	private final URI tempFilename;
	private final String originalFilename;
	private String mimetype;
	private final long fileLength;
	private String encoding;
	private Charset charset;
	private Locale locale = Locale.getDefault();
	private String title;
	private String author;
	private String publisher;
	private String description;
	
	public UploadFile(String uuid, URI tempFilename, String originalFilename, String mimetype, long fileLength) {
		super();
		this.uuid = uuid;
		this.tempFilename = tempFilename;
		this.originalFilename = originalFilename;
		if (mimetype != null) {
			this.mimetype = mimetype.trim().toLowerCase();
		}
		this.fileLength = fileLength;
	}

	public URI getTempFilename() {
		return tempFilename;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public String getMimetype() {
		return mimetype;
	}

	public long getFileLength() {
		return fileLength;
	}
	
	public String getEncoding() {
		return encoding;
	}
	
	public void setEncoding(String encoding) {
		this.encoding = encoding;
		if (encoding != null && !encoding.trim().isEmpty()) {
			try {
				this.setCharset(Charset.forName(encoding));
			}
			catch (Exception ignore) {}
		}
	}
	
	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	public LanguageItem getLanguage() {
		
		return new LanguageItem(locale);
	}
	
	public void setLanguage(LanguageItem language) {
		this.locale = language.getLocale();
	}
	

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
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

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Charset getCharset() {
		return charset;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
	
	
	
}
