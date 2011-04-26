package ch.vd.uniregctb.common;

import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.NotImplementedException;

/**
 * Classe interne utilisée pour initialiser les membres statiques de la classe de base
 * abstraite depuis Spring
 */
public final class JobResultsInitializingBean extends JobResults<Object, JobResultsInitializingBean> implements InitializingBean {

	public void addErrorException(Object element, Exception e) {
		throw new NotImplementedException();
	}

	public void addAll(JobResultsInitializingBean right) {
		throw new NotImplementedException();
	}

	public void afterPropertiesSet() throws Exception {
		checkServices();
	}
}
