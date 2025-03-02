package de.catma.repository.git.serialization;

import java.io.IOException;
import java.nio.charset.Charset;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class CharsetAdapter extends TypeAdapter<Charset> {

	@Override
	public void write(JsonWriter out, Charset value) throws IOException {
		out.value(value!=null?value.toString():null);
	}

	@Override
	public Charset read(JsonReader in) throws IOException {
		if (in.hasNext()) {
			if (in.peek().equals(JsonToken.STRING)) {
				String value = in.nextString();
				if (value != null) {
					return Charset.forName(value);
				}
			}
			else if (in.peek() == JsonToken.NULL) {
				in.nextNull();
			}
		}
		return null;
	}

}
