package de.catma.repository.git.serialization;

import java.io.IOException;
import java.util.Locale;

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
				return Locale.forLanguageTag(value);
			}
		}
		return null;
	}

}
