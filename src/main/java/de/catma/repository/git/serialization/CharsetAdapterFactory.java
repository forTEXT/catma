package de.catma.repository.git.serialization;

import java.nio.charset.Charset;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class CharsetAdapterFactory implements TypeAdapterFactory {

	@SuppressWarnings("unchecked")
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {

		if (Charset.class.isAssignableFrom(type.getRawType())) {
			return (TypeAdapter<T>) new CharsetAdapter();
		}
		
		return null;
	}

}
