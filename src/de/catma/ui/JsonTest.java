package de.catma.ui;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonTest {
	
	private void go() throws IOException {
		
		JsonNodeFactory factory = JsonNodeFactory.instance;
		
		factory.objectNode();
		
		factory.arrayNode();
		
		ObjectMapper mapper = new ObjectMapper();
		 
		String bla = "{}";
		
		
		ObjectNode rootNode = mapper.readValue(bla, ObjectNode.class);
		
		
		rootNode.put("blase", "phase");
		System.out.println(rootNode.get("hase'"));
		System.out.println(rootNode.get("blase"));
		
		System.out.println(rootNode);
	}
	
	public static void main(String[] args) {
		try {
			new JsonTest().go();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
