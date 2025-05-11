package de.catma.repository.git.serialization;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ZonedDateTimeAdapter extends TypeAdapter<ZonedDateTime>{
	
	@Override
	public ZonedDateTime read(JsonReader in) throws IOException {
		if (in.hasNext()) {
			if (in.peek().equals(JsonToken.STRING)) {
				return ZonedDateTime.parse(in.nextString());
			}
			else if (in.peek() == JsonToken.NULL) {
				in.nextNull();
			}
		}
			
		return null;
	}
	
	@Override
	public void write(JsonWriter out, ZonedDateTime value) throws IOException {
		
		if (value == null) {
			out.nullValue();
		}
		else {
			// note that serialized annotation timestamp formatting is subtly different (no colon in offset), see de.catma.tag.Version.DATETIMEPATTERN
			out.value(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		}
		
	}

}
