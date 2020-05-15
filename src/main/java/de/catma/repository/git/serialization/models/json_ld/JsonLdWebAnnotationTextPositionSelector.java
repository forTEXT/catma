package de.catma.repository.git.serialization.models.json_ld;

import java.util.Objects;

public class JsonLdWebAnnotationTextPositionSelector implements Comparable<JsonLdWebAnnotationTextPositionSelector> {
	private Integer start;
	private Integer end;
	private String type = "TextPositionSelector";

	public JsonLdWebAnnotationTextPositionSelector() {

	}

	public JsonLdWebAnnotationTextPositionSelector(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public String getType() {
		return this.type;
	}

	public Integer getStart() {
		return this.start;
	}

	public void setStart(Integer start) {
		this.start = start;
	}

	public Integer getEnd() {
		return this.end;
	}

	public void setEnd(Integer end) {
		this.end = end;
	}

	@Override
	public int compareTo(JsonLdWebAnnotationTextPositionSelector o) {
		int startComparisonResult = this.start.compareTo(o.getStart());
		if (startComparisonResult != 0) {
			return startComparisonResult;
		}

		return this.end.compareTo(o.getEnd());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}

		return this.compareTo((JsonLdWebAnnotationTextPositionSelector)obj) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.start, this.end);
	}
}
