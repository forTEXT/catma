package de.catma.repository.git.serialization;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import de.catma.document.source.FileOSType;

public class FileOSTypeAdapter extends TypeAdapter<FileOSType> {

	@Override
	public void write(JsonWriter out, FileOSType value) throws IOException {
		out.value(value==null?null:value.name());
	}

	@Override
	public FileOSType read(JsonReader in) throws IOException {
		if (in.hasNext()) {
			if (in.peek().equals(JsonToken.STRING)) {
				String value = in.nextString();
				if (value != null) {
					return FileOSType.valueOf(value);
				}
			}
			else if (in.peek() == JsonToken.NULL) {
				in.nextNull();
			}

		}
		return null;
	}
	
	

}
