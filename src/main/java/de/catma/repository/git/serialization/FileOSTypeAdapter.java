package de.catma.repository.git.serialization;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
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
			String value = in.nextString();
			if (value != null) {
				return FileOSType.valueOf(value);
			}
		}
		return null;
	}
	
	

}
