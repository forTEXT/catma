package de.catma.repository.git.serialization;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.internal.bind.SortedFieldTypeAdapterWrapper;
import com.google.gson.reflect.TypeToken;

import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;

/**
 * Type adapter that reflects over the fields and methods of a class.
 */
public final class SortedReflectiveTypeAdapterFactory implements TypeAdapterFactory {
	
	private ReflectiveTypeAdapterFactory delegate;
	
	public SortedReflectiveTypeAdapterFactory(ReflectiveTypeAdapterFactory delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
	    Class<? super T> raw = type.getRawType();

	    if (!Object.class.isAssignableFrom(raw)) {
	      return null; // it's a primitive!
	    }
	    
	    //mpetris: we handle only our own types
	    if (raw.getPackage() == null || !raw.getPackage().getName().startsWith("de.catma")) {
	    	return null;
	    }
	    
	    if (FileOSType.class.isAssignableFrom(raw)) {
	    	return (TypeAdapter<T>) new FileOSTypeAdapter();
	    }
	    if (FileType.class.isAssignableFrom(raw)) {
	    	return (TypeAdapter<T>) new FileTypeAdapter();
	    }		
		
		TypeAdapter<T> reflectiveTypeAdapter = delegate.create(gson, type);
		
		if (reflectiveTypeAdapter instanceof ReflectiveTypeAdapterFactory.Adapter) {
			return new SortedFieldTypeAdapterWrapper<T>((ReflectiveTypeAdapterFactory.Adapter<T, T>)reflectiveTypeAdapter);
		}
		
		return reflectiveTypeAdapter;
	}
}
