package ch.vd.uniregctb.json;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.springframework.beans.factory.FactoryBean;

import ch.vd.registre.base.date.RegDate;

public class JsonObjectMapperFactory implements FactoryBean<ObjectMapper> {

	@Override
	public ObjectMapper getObject() throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		// custom mapper pour les regdates
		SimpleModule module = new SimpleModule("RegDateSupportModule", new Version(1, 0, 0, null));
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
