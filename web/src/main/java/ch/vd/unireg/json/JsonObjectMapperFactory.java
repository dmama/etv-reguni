package ch.vd.unireg.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.FactoryBean;

import ch.vd.registre.base.date.RegDate;

public class JsonObjectMapperFactory implements FactoryBean<ObjectMapper> {

	@Override
	public ObjectMapper getObject() throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		// custom mapper pour les regdates
		SimpleModule module = new SimpleModule("RegDateSupportModule", new Version(1, 0, 0, null, null, null));
		module.addSerializer(RegDate.class, new RegDateJsonSerializer());

		mapper.registerModule(module);
		return mapper;
	}

	@Override
	public Class<?> getObjectType() {
		return ObjectMapper.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}
}
