package ch.vd.unireg.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import ch.vd.registre.base.date.RegDate;

public class RegDateJsonSerializer extends JsonSerializer<RegDate> {
	@Override
	public void serialize(RegDate value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeStartObject();
		jgen.writeNumberField("year", value.year());
		jgen.writeNumberField("month", value.month());
		jgen.writeNumberField("day", value.day());
		jgen.writeEndObject();
	}
}
