package de.catma.project;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class CommitInfo {
	
	private final String id;
	private final String msgTitle;
	private final String msg;
	private final LocalDateTime committedDate;
	private final String author;
	
		
	public CommitInfo(String id,  String msgTitle, String msg, Date committedDate, String author) {
		super();
		this.id = id;
		this.msgTitle = msgTitle;
		this.msg = msg;
		this.committedDate = committedDate==null?null:LocalDateTime.ofInstant(
			      committedDate.toInstant(), ZoneId.systemDefault());
		this.author = author;
	}

	public String getId() {
		return id;
	}
	
	public String getMsg() {
		return msg;
	}
	
	/**
	 * @return the first line of the commit message
	 */
	public String getMsgTitle() {
		return msgTitle;
	}
	
	@Override
	public String toString() {
		return id + " "  + msg;
	}
	
	public LocalDateTime getCommittedDate() {
		return committedDate;
	}
	
	public String getAuthor() {
		return author;
	}
}
