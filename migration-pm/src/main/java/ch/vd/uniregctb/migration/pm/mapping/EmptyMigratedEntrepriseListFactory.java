package ch.vd.uniregctb.migration.pm.mapping;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.FactoryBean;

public class EmptyMigratedEntrepriseListFactory implements FactoryBean<Set<Long>> {

	@Override
	public Set<Long> getObject() throws Exception {
		return Collections.emptySet();
	}

	@Override
	public Class<?> getObjectType() {
		return Set.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
