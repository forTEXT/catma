package de.catma.repository.git.graph.indexer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;

import de.catma.document.Range;

public class TinkerPopTest {
	
	private Logger logger = Logger.getAnonymousLogger();
	private int i=0;
	
	public static void main(String[] args) {

		try {
			new TinkerPopTest().run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void run() throws Exception {
		logger.info("start test");
		
		Any content = JsonIterator.deserialize(FileUtils.readFileToString(
			new File("C:/data/catmadata/localgit/mp/CATMA_B357DB2F-B78C-41E8-A775-5CAAAD1D6BA0_thunder/"
					+ "CATMA_6A0CD14C-B67F-4C7B-8463-AE23DC9DCCA0_sourcedocument/"
					+ "CATMA_6A0CD14C-B67F-4C7B-8463-AE23DC9DCCA0.json"), "UTF-8"));
		
		logger.info("load finished");
		
		TinkerGraph graph = TinkerGraph.open();
		Map<Integer, Vertex> adjacencyMap = new HashMap<>();
		for (Map.Entry<String, Any> entry : content.asMap().entrySet()) {
			String term = entry.getKey();
			Vertex termV = graph.addVertex("term");
			termV.property("literal", term);
			
			for (Any posEntry : entry.getValue().asList()) {
				int startOffset = posEntry.get("startOffset").as(Integer.class);
				int endOffset = posEntry.get("endOffset").as(Integer.class);
				int tokenOffset = posEntry.get("tokenOffset").as(Integer.class);
				
				Vertex positionV = graph.addVertex("position");
				positionV.property(
					"startOffset", startOffset);
				positionV.property( 
					"endOffset", endOffset);
				positionV.property(
					"tokenOffset", tokenOffset);
				
				termV.addEdge("hasPosition", positionV);
				adjacencyMap.put(tokenOffset, positionV);
				
			}
		}
		for (int i=0; i<adjacencyMap.size()-1; i++) {
			adjacencyMap.get(i).addEdge("isAdjacent", adjacencyMap.get(i+1));
		}
		
		System.out.println(graph.toString());
		
		logger.info("graph creation finished");
		
		graph.createIndex("literal", Vertex.class);

		logger.info("graph index finished");

		GraphTraversalSource g = graph.traversal();

		List<Path> result = g.V().has("term", "literal", "Miss")
		.outE("hasPosition")
		.inV().hasLabel("position")
		.outE("isAdjacent")
		.inV().hasLabel("position")
		.inE("hasPosition")
		.outV().has("term", "literal", "Emily")
		.path()
		.fill(new ArrayList<>());
		
		
//		.forEach(e -> System.out.println((i++) +" "+ e));
		TreeSet<Range> ranges = new TreeSet<>();
		
		for (Path p : result) {
			Vertex startPos = (Vertex)p.get(2);
			Vertex endPos = (Vertex)p.get(4);
			
			int start = startPos.value("startOffset");
			int end = endPos.value("endOffset");
			Range range = new Range(start, end);
			ranges.add(range);
//			System.out.println("[" + start +"," + end +"]");
		}
		
		for (Range r : ranges) {
			System.out.println(r);
		}
 		
		List<String> terms = g.V().has("term", "literal", "Grierson")
		.outE("hasPosition")
		.inV().hasLabel("position")
		.outE("isAdjacent")
		.inV().hasLabel("position")
		.inE("hasPosition")
		.outV().hasLabel("term")
		.properties("literal")
		.map(prop -> (String)prop.get().orElse(null))
		.toList();
		
		for (String term : terms) {
			System.out.println(term);
		}
		
		logger.info("finished test");
	}

}
