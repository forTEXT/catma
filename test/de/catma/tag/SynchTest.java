package de.catma.tag;


import java.net.URISyntaxException;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionManager;
import de.catma.query.DummyRepository;
import de.catma.util.ContentInfoSet;

public class SynchTest {

	private TagsetDefinition tsd1;
	private TagDefinition td1;
	private PropertyDefinition pd1;
	private PropertyDefinition pd2;
	private PropertyDefinition u1;
	private PropertyDefinition u3;
	
	private TagsetDefinition tsd2;
	private TagDefinition td2;
	private PropertyDefinition pd3;
	private PropertyDefinition pd4;
	private PropertyDefinition u2;
	private PropertyDefinition u4;

	@Before
	public void init() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -1);
		
		tsd1 = new TagsetDefinition(
				1, "TS_A", "tsNameA", new Version(calendar.getTime()));
		td1 = new TagDefinition(
				1, "T_A", "tNameA", new Version(calendar.getTime()), 15, "Parent1");
		
		pd1 = new PropertyDefinition(
			1, "P_A_1", PropertyDefinition.SystemPropertyName.catma_displaycolor.name(), 
			new PropertyPossibleValueList("Red"));

		pd2 = new PropertyDefinition(
			2, "P_A_2", PropertyDefinition.SystemPropertyName.catma_markupauthor.name(), 
			new PropertyPossibleValueList("mp"));
		
		u1 = new PropertyDefinition(
			5, "UP_A_1", "user1", new PropertyPossibleValueList("userVal1"));
		
		u3 = new PropertyDefinition(
			5, "UP_A_3", "user3", new PropertyPossibleValueList("userVal3"));
		
		td1.addSystemPropertyDefinition(pd1);
		td1.addSystemPropertyDefinition(pd2);
		td1.addUserDefinedPropertyDefinition(u1);
		td1.addUserDefinedPropertyDefinition(u3);
		tsd1.addTagDefinition(td1);
		
		tsd2 = new TagsetDefinition(2, "TS_A", "tsNameB", new Version());
		td2 = new TagDefinition(
				2, "T_A", "tNameB", new Version(), null, null);
		
		pd3 = new PropertyDefinition(
			3, "P_A_1", PropertyDefinition.SystemPropertyName.catma_displaycolor.name(), 
			new PropertyPossibleValueList("Blue"));

		pd4 = new PropertyDefinition(
			4, "P_A_4", PropertyDefinition.SystemPropertyName.catma_markupauthor.name(), 
			new PropertyPossibleValueList("mp2"));

		u2 = new PropertyDefinition(
			6, "UP_A_2", "user2", new PropertyPossibleValueList("userVal2"));
		
		u4 = new PropertyDefinition(
			5, "UP_A_3", "user3", new PropertyPossibleValueList("userVal3"));
		td2.addSystemPropertyDefinition(pd3);
		td2.addSystemPropertyDefinition(pd4);
		td2.addUserDefinedPropertyDefinition(u2);
		td2.addUserDefinedPropertyDefinition(u4);
		tsd2.addTagDefinition(td2);
	}
	
	@Test
	public void synchLibraryTest() {
		TagManager tagManager = new TagManager();

		tagManager.synchronize(tsd1, tsd2);
		
		Assert.assertTrue(tsd1.getName().equals("tsNameB"));
		Assert.assertTrue(tsd1.getName().equals(tsd2.getName()));
		Assert.assertTrue(tsd1.getVersion().equals(tsd2.getVersion()));
		Assert.assertTrue(tsd1.getUuid().equals(tsd2.getUuid()));
		Assert.assertFalse(tsd1.getId().equals(tsd2.getId()));
		
		Assert.assertTrue(td1.getAuthor().equals("mp2"));
		Assert.assertTrue(td1.getAuthor().equals(td2.getAuthor()));
		Assert.assertTrue(td1.getColor().equals(td2.getColor()));
		Assert.assertTrue(td1.getVersion().equals(td2.getVersion()));
		Assert.assertTrue(td1.getUuid().equals(td2.getUuid()));
		Assert.assertTrue(td1.getPropertyDefinition("P_A_4")  != null);
		Assert.assertTrue(td1.getPropertyDefinition("P_A_2") == null);
		Assert.assertTrue(td2.getPropertyDefinition("P_A_4").equals(pd4));
		Assert.assertTrue(td1.getPropertyDefinition("P_A_1").equals(pd1));
		Assert.assertTrue(td2.getPropertyDefinition("UP_A_2") != null);
		Assert.assertTrue(td2.getPropertyDefinition("UP_A_2").equals(u2));
		
		Assert.assertTrue(td2.getPropertyDefinition("UP_A_1") == null);
		
		Assert.assertTrue(td1.getPropertyDefinition("UP_A_2") != null);
		Assert.assertFalse(td1.getPropertyDefinition("UP_A_2").equals(u2));
		Assert.assertTrue(td1.getPropertyDefinition("UP_A_1") == null);
		Assert.assertTrue(td1.getDeletedPropertyDefinitions().contains(pd2.getId()));
		Assert.assertTrue(td1.getDeletedPropertyDefinitions().contains(u1.getId()));
		Assert.assertTrue(td2.getDeletedPropertyDefinitions().isEmpty());
		
		Assert.assertTrue(td1.getPropertyDefinition("UP_A_3").equals(u3));
		Assert.assertTrue(td2.getPropertyDefinition("UP_A_3").equals(u4));
		
		Assert.assertTrue(
			td1.getPropertyDefinition("P_A_1").getFirstValue().equals("Blue"));
		
		Assert.assertTrue(td1.getParentUuid().isEmpty());
		Assert.assertTrue(td1.getParentId() == null);
		
	}
	
	public void synchCollectionTest() throws URISyntaxException {
		TagDefinition toBeDeleted =
				new TagDefinition(
						3, "T_B", "toBeDeleted", new Version(), null, null);
		tsd1.addTagDefinition(toBeDeleted);
		
		PropertyDefinition pd5 = new PropertyDefinition(
				5, "P_A_5", PropertyDefinition.SystemPropertyName.catma_displaycolor.name(), 
				new PropertyPossibleValueList("Yellow"));

		PropertyDefinition pd6 = new PropertyDefinition(
				6, "P_A_6", PropertyDefinition.SystemPropertyName.catma_markupauthor.name(), 
				new PropertyPossibleValueList("Dieter"));
		
		toBeDeleted.addSystemPropertyDefinition(pd5);
		toBeDeleted.addSystemPropertyDefinition(pd6);
		
		TagLibrary tagLibrary = new TagLibrary("1", "TagLibA");
		tagLibrary.add(tsd1);
		
		UserMarkupCollection userMarkupCollection = 
				new UserMarkupCollection(
					"1", new ContentInfoSet("Collection 1"), tagLibrary);
		
		TagInstance ti1 = new TagInstance("TI1", td1);

		Property p1 = new Property(pd1, new PropertyValueList("Red"));
		Property p2 = new Property(pd2, new PropertyValueList("mp"));
		
		Property p3 = new Property(u1, new PropertyValueList("specialValue1"));
		Property p4 = new Property(u3, new PropertyValueList("specialValue2"));
		
		ti1.addSystemProperty(p1);
		ti1.addSystemProperty(p2);
		ti1.addUserDefinedProperty(p3);
		ti1.addUserDefinedProperty(p4);
		
		TagReference tr1 = new TagReference(ti1, "catma://42", new Range(1,5));
		userMarkupCollection.addTagReference(tr1);
		
		TagInstance ti2 = new TagInstance("TI2", toBeDeleted);
		Property p5 = new Property(pd5, new PropertyValueList("Yellow"));
		Property p6 = new Property(pd6, new PropertyValueList("Dieter"));
		
		ti2.addSystemProperty(p5);
		ti2.addSystemProperty(p6);
		
		TagReference tr2 = new TagReference(ti2, "catma://42", new Range(6,10));
		userMarkupCollection.addTagReference(tr2);
		
		TagManager tagManager = new TagManager();
		UserMarkupCollectionManager manager = 
				new UserMarkupCollectionManager(
						tagManager, new DummyRepository(null));
		manager.add(userMarkupCollection);
		
		tagManager.removeUserDefinedPropertyDefinition(u4, td2);
		
		Assert.assertTrue(
			td2.getPropertyDefinition(
				ti1.getProperty("UP_A_2").getPropertyDefinition().getUuid()).equals(u3));

		manager.updateUserMarkupCollections(
				manager.getUserMarkupCollections(), tsd2);
		
		Assert.assertTrue(p1.getPropertyValueList().getFirstValue().equals("Blue"));
		Assert.assertTrue(p2.getPropertyValueList().getFirstValue().equals("mp"));
		Assert.assertTrue(ti1.getProperty("P_A_4") != null);
		Assert.assertTrue(
			ti1.getProperty("P_A_4").getPropertyValueList().getFirstValue().equals("mp2"));
		Assert.assertTrue(ti1.getProperty("P_A_2") == null);
		Assert.assertTrue(ti1.getProperty("P_A_1").equals(p1));
		Assert.assertTrue(ti1.getProperty("P_A_1").getPropertyDefinition().equals(pd1));

		Assert.assertTrue(
			ti1.getProperty("UP_A_1").getPropertyValueList().getFirstValue().equals("specialValue1"));
		Assert.assertTrue(
			ti1.getProperty("UP_A_2").getPropertyValueList().getFirstValue().equals("specialValue2"));
		
		Assert.assertTrue(userMarkupCollection.getTagReferences().contains(tr1));
		Assert.assertFalse(userMarkupCollection.getTagReferences().contains(tr2));
		Assert.assertFalse(tagLibrary.contains(tsd2));
		Assert.assertTrue(tagLibrary.contains(tsd1));
		
		Assert.assertTrue(
			td2.getPropertyDefinition(
				ti1.getProperty("UP_A_2").getPropertyDefinition().getUuid())==null);
	}
}
