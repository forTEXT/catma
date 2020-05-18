package de.catma.repository.git.serialization;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Locale;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;

import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;

public class SerializationHelper<T> {
	public String serialize(T object) {
		return toJson(object);
	}

	public T deserialize(String serialized, Class<T> clazz) {
		return fromJson(serialized, clazz);
	}
		
	public String toJson(T object) {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(Charset.class, new CharsetAdapter());
		gson.registerTypeAdapter(Locale.class, new LocaleAdapter());
		gson.registerTypeAdapter(FileOSType.class, new FileOSTypeAdapter());
		gson.registerTypeAdapter(FileType.class, new FileTypeAdapter());
		gson.registerTypeAdapterFactory(new CharsetAdapterFactory());
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
		gson.registerTypeAdapter(Locale.class, new LocaleAdapter());
		gson.registerTypeAdapter(FileOSType.class, new FileOSTypeAdapter());
		gson.registerTypeAdapter(FileType.class, new FileTypeAdapter());
		gson.registerTypeAdapterFactory(new CharsetAdapterFactory());
		
		return gson.create().fromJson(json, clazz);
	}
}
