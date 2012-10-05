package de.catma.ui.analyzer;

public class SourceDocumentItemID {
	
	private String sourceDocumentID;
	private String id;
	
	public SourceDocumentItemID(String id, String sourceDocumentID) {
		this.id = id;
		this.sourceDocumentID = sourceDocumentID;
	}
	
	
	@Override
	public String toString() {
		return id;
	}
	
	public String getSourceDocumentID() {
		return sourceDocumentID;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SourceDocumentItemID other = (SourceDocumentItemID) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}
	
	

}
