package de.catma.tag;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.catma.util.IDGenerator;

class TagsetDefinitionTest {

	/**
	 * During traversal each Tag is traversed after its parent Tag.
	 */
	@Test
	void testTagIteration() {
		IDGenerator idGenerator = new IDGenerator();
		
		TagsetDefinition tagset = new TagsetDefinition(idGenerator.generate(), "Test");
		
		TagDefinition root1 = new TagDefinition(idGenerator.generate(), "root1", null, tagset.getUuid());
		tagset.addTagDefinition(root1);
		
		TagDefinition root2 = new TagDefinition(idGenerator.generate(), "root2", null, tagset.getUuid());
		tagset.addTagDefinition(root2);
		
		TagDefinition child1 = new TagDefinition(idGenerator.generate(), "child1", root1.getUuid(), tagset.getUuid());
		tagset.addTagDefinition(child1);
		
		TagDefinition child2 = new TagDefinition(idGenerator.generate(), "child2", root2.getUuid(), tagset.getUuid());
		tagset.addTagDefinition(child2);
		
		TagDefinition grantchild1 = new TagDefinition(idGenerator.generate(), "grantchild1", child1.getUuid(), tagset.getUuid());
		tagset.addTagDefinition(grantchild1);
		
		TagDefinition grantchild2 = new TagDefinition(idGenerator.generate(), "grantchild2", child2.getUuid(), tagset.getUuid());
		tagset.addTagDefinition(grantchild2);
		
		Set<String> tags = new HashSet<String>();
		for (TagDefinition tag : tagset) {
			if (!tag.getParentUuid().isEmpty()) {
				assertTrue(tags.contains(tag.getParentUuid()));
			}
			tags.add(tag.getUuid());
		}
		
	}

}
