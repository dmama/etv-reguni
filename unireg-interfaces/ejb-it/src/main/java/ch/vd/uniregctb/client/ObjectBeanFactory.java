package ch.vd.uniregctb.client;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ObjectBeanFactory extends ApplicationObjectSupport {

	public static final String UINTF_EJB_INTERFACES = "classpath:uintf-ejbit-interfaces.xml";


	private static final String[] configLocations = new String[] {

			UINTF_EJB_INTERFACES

	};

	private final static ObjectBeanFactory factory = new ObjectBeanFactory();

	public static ObjectBeanFactory getInstance() {
		return factory;
	}

	private synchronized ApplicationContext getContext() {
		if (factory.getApplicationContext() == null) {
			factory.setApplicationContext(new ClassPathXmlApplicationContext(configLocations));
		}
		return factory.getApplicationContext();
	}

	public Object getBean(String name) {
		return getContext().getBean(name);
	}

}
