package de.catma.repository.git.serialization;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;

import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;

public class SerializationHelper<T> {
	public String serialize(T object) {
		return toJson(object);
	}

	public String serialize(Collection<T> objects) {
		return toJson(objects);
	}

	public T deserialize(String serialized, Class<T> clazz) {
		return fromJson(serialized, clazz);
	}

	public T deserialize(String serialized, Type type) {
		return fromJson(serialized, type);
	}

	private String toJson(T object) {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(Charset.class, new CharsetAdapter());
		gson.registerTypeAdapter(Locale.class, new LocaleAdapter());
		gson.registerTypeAdapter(FileOSType.class, new FileOSTypeAdapter());
		gson.registerTypeAdapter(FileType.class, new FileTypeAdapter());
		gson.registerTypeAdapterFactory(new CharsetAdapterFactory());
		gson.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
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
		return gson.setPrettyPrinting().serializeNulls().create().toJson(object);
	}
	
	private T fromJson(String json, Class<T> clazz) {
		GsonBuilder gson = new GsonBuilder();
		gson.registerTypeAdapter(Charset.class, new CharsetAdapter());
		gson.registerTypeAdapter(Locale.class, new LocaleAdapter());
		gson.registerTypeAdapter(FileOSType.class, new FileOSTypeAdapter());
		gson.registerTypeAdapter(FileType.class, new FileTypeAdapter());
		gson.registerTypeAdapterFactory(new CharsetAdapterFactory());
		gson.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
		
		return gson.create().fromJson(json, clazz);
	}
	
	private T fromJson(String json, Type type) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Charset.class, new CharsetAdapter());
		gsonBuilder.registerTypeAdapter(Locale.class, new LocaleAdapter());
		gsonBuilder.registerTypeAdapter(FileOSType.class, new FileOSTypeAdapter());
		gsonBuilder.registerTypeAdapter(FileType.class, new FileTypeAdapter());
		gsonBuilder.registerTypeAdapterFactory(new CharsetAdapterFactory());
		gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
		
		Gson gson = gsonBuilder.create();
		return gson.fromJson(json, type);		
	}
	
	private String toJson(Collection<T> objects) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Charset.class, new CharsetAdapter());
		gsonBuilder.registerTypeAdapter(Locale.class, new LocaleAdapter());
		gsonBuilder.registerTypeAdapter(FileOSType.class, new FileOSTypeAdapter());
		gsonBuilder.registerTypeAdapter(FileType.class, new FileTypeAdapter());
		gsonBuilder.registerTypeAdapterFactory(new CharsetAdapterFactory());
		gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
		Gson gson = gsonBuilder.setPrettyPrinting().serializeNulls().create();
		return gson.toJson(objects);	
	}

}
