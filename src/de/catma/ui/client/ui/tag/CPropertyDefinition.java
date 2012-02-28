package de.catma.ui.client.ui.tag;


public class CPropertyDefinition implements DisplayableTagChild {

	private String name;
	private CPropertyPossibleValueList possibleValueList;
	
	public CPropertyDefinition(String name,
			CPropertyPossibleValueList possibleValueList) {
		super();
		this.name = name;
		this.possibleValueList = possibleValueList;
	}
	
	
	@Override
	public String toString() {
		return "PROP["+name+"="+possibleValueList+"]";
	}


	public String getName() {
		return name;
	}
	
	public String getFirstValue() {
		return possibleValueList.getFirstValue();
	}
	
	public CPropertyPossibleValueList getPossibleValueList() {
		return possibleValueList;
	}
	
	public String getDisplayString() {
		return getName();
	}

}
