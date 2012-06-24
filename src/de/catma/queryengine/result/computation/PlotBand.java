package de.catma.queryengine.result.computation;


public class PlotBand {
	
	private String id;
	private String label;
	private double start;
	private double end;
	
	public PlotBand(String id, String label, double start, double end) {
		this.id = id;
		this.label = label;
		this.start = start;
		this.end = end;
	}

	public String getLabel() {
		return label;
	}
	
	public double getStart() {
		return start;
	}
	
	public double getEnd() {
		return end;
	}
	
	public String getId() {
		return id;
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
		PlotBand other = (PlotBand) obj;
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
