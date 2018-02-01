package ch.vd.unireg.xml;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.w3c.dom.ls.LSResourceResolver;

import ch.vd.shared.cxf.LSResourceCatalogManager;

/**
 * Factory qui crée le bus CXF avec les spécialisations nécessaires pour Unireg.
 */
public class UniregCxfBusFactory implements FactoryBean<SpringBus>, InitializingBean, DisposableBean, ApplicationContextAware {

	private SpringBus bus;
	private ApplicationContext applicationContext;
	private LSResourceResolver resourceResolver;

	public void setResourceResolver(LSResourceResolver resourceResolver) {
		this.resourceResolver = resourceResolver;
	}

	@Override
	public SpringBus getObject() throws Exception {
		return bus;
	}

	@Override
	public Class<?> getObjectType() {
		return SpringBus.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		bus = new SpringBus();
		bus.setApplicationContext(applicationContext);

		// [SIFISC-26031] on enregistre le catalog manager spécialisé qui utilise le ClasspathCatalogResolver.
		if (resourceResolver != null) {
			bus.setExtension(new LSResourceCatalogManager(resourceResolver), OASISCatalogManager.class);
		}

		BusFactory.setDefaultBus(bus);
	}

	@Override
	public void destroy() throws Exception {
		// on ne shutdown pas le bus courant car il peut être partagé entre plusieurs contextes Spring (cas des tests)
//		if (bus != null) {
//			bus.shutdown();
//		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
