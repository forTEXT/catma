package de.catma.ui.tagger.client.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.DOM;

public class TaggedSpanFactory {

	private String tag;
	private String instanceID;
	public TaggedSpanFactory(String tag) {
		super();
		this.tag = tag;
		instanceID = String.valueOf(new Date().getTime());
	}
	
	private Element createTaggedSpan(String innerHtml) {
		Element taggedSpan = DOM.createSpan();
		taggedSpan.addClassName(tag);
		taggedSpan.setAttribute("instanceID", instanceID);
		taggedSpan.setInnerHTML(innerHtml);
		return taggedSpan;
	}

	public List<Node> createTaggedSpanSequence(String textOnly) {
		List<Node> resultList = new ArrayList<Node>();
		
		RegExp pattern = RegExp.compile("(\\s*)(\\S+)(\\s*)", "g");
		MatchResult matchResult = null;
		
		while((matchResult=pattern.exec(textOnly))!=null) {
			String result = matchResult.getGroup(1);
			if ((result != null) && (!result.isEmpty())) {
				resultList.add(Document.get().createTextNode(result));
			}
			
			result = matchResult.getGroup(2);
			if ((result != null) && (!result.isEmpty())) {
				resultList.add(createTaggedSpan(result));
			}			
			
			result = matchResult.getGroup(3);
			if ((result != null) && (!result.isEmpty())) {
				resultList.add(Document.get().createTextNode(result));
			}
		}
		
		return resultList;
	}
	public String getTag() {
		return tag;
	}
	
}
