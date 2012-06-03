package de.catma.serialization.tei;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.tag.TagLibrary;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;

public class TeiSerializerTest {
	
	

	@Test
	public void testProblem2LibTagLibraryDeserializer() 
			throws FileNotFoundException, IOException {
		
		TagManager tagManager = new TagManager();
		
		FilterInputStream is = new BOMFilterInputStream(
				new FileInputStream(
						"testdocs/Tagsets_user_problem.xml"), 
				Charset.forName( "UTF-8" ));
		TagLibrary tagLibrary = 
				new TeiTagLibrarySerializationHandler(tagManager).deserialize(
						"testdocs/Tagsets_user_problem.xml", is);
		is.close();
		
	}

	@Test
	public void testProblem1LibTagLibraryDeserializer() 
			throws FileNotFoundException, IOException {
		
		TagManager tagManager = new TagManager();
		FilterInputStream is = new BOMFilterInputStream(
				new FileInputStream(
						"testdocs/Wehmeier_problem_DefaultTagsetDB.xml"), 
				Charset.forName( "UTF-8" ));
		TagLibrary tagLibrary = 
				new TeiTagLibrarySerializationHandler(tagManager).deserialize(
						"testdocs/Wehmeier_problem_DefaultTagsetDB.xml",is);
		is.close();
		
	}
	
	@Test
	public void testStructureFileDeserializer() throws FileNotFoundException, IOException {
		TagManager tagManager = new TagManager();
		FilterInputStream is = new BOMFilterInputStream(
				new FileInputStream("testdocs/rose_for_emily_structure.xml"), Charset.forName( "UTF-8" ));
		TagLibrary tagLibrary = 
				new TeiTagLibrarySerializationHandler(tagManager).deserialize(
						"testdocs/rose_for_emily_structure.xml", is);
		is.close();
	}
	
	@Test
	public void testTagLibraryDeserializer() throws FileNotFoundException, IOException {
		TagManager tagManager = new TagManager();
		FilterInputStream is = new BOMFilterInputStream(
				new FileInputStream(
						"testdocs/rose_for_emily_user_simple.xml"), 
				Charset.forName( "UTF-8" ));
		TagLibrary tagLibrary = 
				new TeiTagLibrarySerializationHandler(tagManager).deserialize(
						"testdocs/rose_for_emily_user_simple.xml", is);
		is.close();
		Set<String> availableTagsetDefs = new HashSet<String>();
		availableTagsetDefs.add("CATMA_STANDARD_TAGSET");
		availableTagsetDefs.add("CATMA_c5bd46a1-a884-4e86-985c-713baf0b9476");
		TagsetDefinition stdTagsetDefinition = 
				tagLibrary.getTagsetDefinition("CATMA_STANDARD_TAGSET");
		Assert.assertNotNull(stdTagsetDefinition);
		Assert.assertTrue(
				stdTagsetDefinition.hasTagDefinition("CATMA_BASE_TAG"));
		Assert.assertTrue(
				stdTagsetDefinition.hasTagDefinition("CATMA_2fb2dca9-14e8-4127-8993-144f9dfc56fb"));
		Assert.assertTrue(
				stdTagsetDefinition.hasTagDefinition("CATMA_ac559bf8-f7aa-4c8d-aee3-da8144d34f20"));
		
		TagsetDefinition testTagsetDef = 
				tagLibrary.getTagsetDefinition("CATMA_c5bd46a1-a884-4e86-985c-713baf0b9476");
		
		Assert.assertNotNull(testTagsetDef);
		
		TagDefinition td = 
				testTagsetDef.getTagDefinition(
						"CATMA_9e7446f5-9ae1-4cef-90af-c444b0512c99");
		Assert.assertNotNull(td);
		
		PropertyDefinition pd = td.getPropertyDefinitionByName("catma_displaycolor");
		Assert.assertNotNull(pd);
		
		String value = pd.getFirstValue();
		Assert.assertNotNull(value);
		Assert.assertTrue(value.equals("-52225"));
		
	}
	
	@Test
	public void testTeiUserMarkupCollectionsDeserializer() 
			throws FileNotFoundException, IOException {
		
		TagManager tagManager = new TagManager();
		FilterInputStream is = new BOMFilterInputStream(
				new FileInputStream(
						"testdocs/rose_for_emily_user_simple.xml"), 
				Charset.forName( "UTF-8" ));
		UserMarkupCollection umc = 
				new TeiUserMarkupCollectionSerializationHandler(
						tagManager).deserialize(
								"testdocs/rose_for_emily_user_simple.xml", is);
		is.close();
		
		for (TagReference tagReference : umc.getTagReferences()) {
			System.out.println(tagReference);
		}
		Assert.assertTrue(umc.getTagReferences().size() == 4);
	}

}
