package ch.vd.uniregctb.json;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

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
