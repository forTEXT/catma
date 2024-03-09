package de.catma.repository.git.serialization;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime>{
	
	@Override
	public LocalDateTime read(JsonReader in) throws IOException {
		return LocalDateTime.parse(in.nextString());
	}
	
	@Override
	public void write(JsonWriter out, LocalDateTime value) throws IOException {
		
		if (value == null) {
			out.nullValue();
		}
		else {
			out.value(value.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
		}
		
	}

}
