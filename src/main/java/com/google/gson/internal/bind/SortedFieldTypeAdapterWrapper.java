package com.google.gson.internal.bind;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory.BoundField;
import com.google.gson.internal.reflect.ReflectionHelper;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class SortedFieldTypeAdapterWrapper<T> extends TypeAdapter<T> {
	
	private ReflectiveTypeAdapterFactory.Adapter<T, T> delegate;

	public SortedFieldTypeAdapterWrapper(ReflectiveTypeAdapterFactory.Adapter<T, T> delegate) {
      this.delegate = delegate;
    }

	@Override
	public void write(JsonWriter out, T value) throws IOException {
	    if (value == null) {
          out.nullValue();
          return;
        }

        out.beginObject();
        Collection<BoundField> sortedFields = delegate.boundFields.values()
          		.stream()
          		.sorted((bf1,bf2) -> bf1.name.compareTo(bf2.name)) // mpetris we need a fixed order to minimize git conflicts and to support git diff 
          		.collect(Collectors.toList());

        try {
          for (BoundField boundField : sortedFields) {
            boundField.write(out, value);
          }
        } catch (IllegalAccessException e) {
          throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
        }
        out.endObject();
	}

	@Override
	public T read(JsonReader in) throws IOException {
		return this.delegate.read(in);
	}

}
