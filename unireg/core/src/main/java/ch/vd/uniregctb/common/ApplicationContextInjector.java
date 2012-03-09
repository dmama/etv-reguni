package ch.vd.uniregctb.common;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Ce bean permet d'exposer explicitement l'application context pour ensuite l'injecter manuellement dans d'autres beans. Cela peut se réveler nécessaire sur des beans qui ne peuvent pas implémenter
 * l'interface ApplicationContextAwawre (les beans @Controller, par exemple) pour une raison ou pour une autre.
 */
public class ApplicationContextInjector implements FactoryBean, ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public Object getObject() throws Exception {
		return applicationContext;
	}

	@Override
	public Class<?> getObjectType() {
		return ApplicationContext.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
