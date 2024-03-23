package de.catma.repository.git.serialization;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;

public class SortedReflectiveTypeAdapterFactoryTest {
	
	public static record DomainEntity(
		String cProperty,
		String aProperty,
		String dProperty,
		String bProperty,
		String eProperty) {}

	@Test
	void gsonWithoutSortedReflectiveTypeAdapterFactoryShouldSerializedPropertiesInGivenOrder() {
		GsonBuilder gson = new GsonBuilder();

		DomainEntity domainEntity = new DomainEntity("cValue", "aValue", "dValue", "bValue", "eValue");
		
		String serialized = gson.setPrettyPrinting().serializeNulls().create().toJson(domainEntity);
		
		// the order asserted here could actually be different as there is no guaranteed sort 
		// order without SortedReflectiveTypeAdapterFactory
		// at the time of writing the sort order was the order of the properties defined in DomainEntity
		// a failing of this test is therefore only a hint that something might have changed in the default behaviour
		// of gson
		assertTrue(serialized.indexOf("cProperty") < serialized.indexOf("aProperty"));
		assertTrue(serialized.indexOf("aProperty") < serialized.indexOf("dProperty"));
		assertTrue(serialized.indexOf("cProperty") < serialized.indexOf("dProperty"));
		assertTrue(serialized.indexOf("dProperty") < serialized.indexOf("bProperty"));
	}
	
	@Test
	void gsonWithSortedReflectiveTypeAdapterFactoryShouldSerializedWithSortedProperties() {
		GsonBuilder gson = new GsonBuilder();

		ConstructorConstructor constructorConstructor = new ConstructorConstructor(Collections.emptyMap(), false, Collections.emptyList());

		gson.registerTypeAdapterFactory(
			new SortedReflectiveTypeAdapterFactory(
				new ReflectiveTypeAdapterFactory(
					constructorConstructor, 
					FieldNamingPolicy.IDENTITY, Excluder.DEFAULT,
					new JsonAdapterAnnotationTypeAdapterFactory(constructorConstructor), Collections.emptyList()
				)
			)
		); 

		DomainEntity domainEntity = new DomainEntity("cValue", "aValue", "dValue", "bValue", "eValue");
		
		String serialized = gson.setPrettyPrinting().serializeNulls().create().toJson(domainEntity);
		
		assertTrue(serialized.indexOf("aProperty") < serialized.indexOf("bProperty"));
		assertTrue(serialized.indexOf("bProperty") < serialized.indexOf("cProperty"));
		assertTrue(serialized.indexOf("cProperty") < serialized.indexOf("dProperty"));
		assertTrue(serialized.indexOf("dProperty") < serialized.indexOf("eProperty"));
	}
	
	@Test
	void serializationHelperWithSortedReflectiveTypeAdapterFactoryShouldSerializedWithSortedProperties() {
		DomainEntity domainEntity = new DomainEntity("cValue", "aValue", "dValue", "bValue", "eValue");
		
		SerializationHelper<DomainEntity> serializationHelper = new SerializationHelper<SortedReflectiveTypeAdapterFactoryTest.DomainEntity>();
		String serialized = serializationHelper.serialize(domainEntity);
		
		assertTrue(serialized.indexOf("aProperty") < serialized.indexOf("bProperty"));
		assertTrue(serialized.indexOf("bProperty") < serialized.indexOf("cProperty"));
		assertTrue(serialized.indexOf("cProperty") < serialized.indexOf("dProperty"));
		assertTrue(serialized.indexOf("dProperty") < serialized.indexOf("eProperty"));
	}

}
