package de.catma.repository.git.serialization;

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.lang.LocaleUtils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class LocaleAdapter extends TypeAdapter<Locale> {

	@Override
	public void write(JsonWriter out, Locale value) throws IOException {
		out.value(value!=null?value.toString():null);
	}

	@Override
	public Locale read(JsonReader in) throws IOException {

		if (in.hasNext()) {
			String value = in.nextString();
			if (value != null) {
				return LocaleUtils.toLocale(value);
			}
		}
		return null;
	}

}
