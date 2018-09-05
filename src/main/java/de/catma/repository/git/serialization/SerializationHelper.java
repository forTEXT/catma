package de.catma.repository.git.serialization;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.DecodingMode;
import com.jsoniter.spi.JsonException;

public class SerializationHelper<T> {
	public String serialize(T object) {
		try {
			JsoniterAlphabeticOrderingExtension.enable();
		}
		catch (JsonException e) {
//			e.printStackTrace(); TODO:
		}
		JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
		JsonStream.setIndentionStep(2);
		return JsonStream.serialize(object);
	}

	public T deserialize(String serialized, Class<T> clazz) {
		JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_STRICTLY);
		return JsonIterator.deserialize(serialized, clazz);
	}
}
