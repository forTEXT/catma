package de.catma.repository.git.serialization;

import java.nio.charset.Charset;
import java.util.Collections;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;

public class SerializationHelper<T> {
	public String serialize(T object) {
//		try {
//			JsoniterAlphabeticOrderingExtension.enable();
//		}
//		catch (JsonException e) {
////			e.printStackTrace(); TODO:
//		}
//		JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
//		JsonStream.setIndentionStep(2);
//		return JsonStream.serialize(object);
		return toJson(object);
	}

	public T deserialize(String serialized, Class<T> clazz) {
//		JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_STRICTLY);
//		return JsonIterator.deserialize(serialized, clazz);
		return fromJson(serialized, clazz);
	}
		
	public String toJson(T object) {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(Charset.class, new CharsetAdapter());
		gson.registerTypeAdapterFactory(
			new SortedReflectiveTypeAdapterFactory(
					new ConstructorConstructor(Collections.emptyMap()), 
					FieldNamingPolicy.IDENTITY, 
					Excluder.DEFAULT)); 
		return gson.setPrettyPrinting().serializeNulls().create().toJson(object);
	}
	
	public T fromJson(String json, Class<T> clazz) {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(Charset.class, new CharsetAdapter());
		
		return gson.create().fromJson(json, clazz);
	}
}
