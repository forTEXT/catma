package de.catma.repository.git.serialization;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.catma.document.source.FileType;

public class FileTypeAdapter extends TypeAdapter<FileType> {

	@Override
	public void write(JsonWriter out, FileType value) throws IOException {
		out.value(value==null?null:value.name());
	}

	@Override
	public FileType read(JsonReader in) throws IOException {
		if (in.hasNext()) {
			String value = in.nextString();
			if (value != null) {
				return FileType.valueOf(value);
			}
		}
		return null;
	}
	
	

}
