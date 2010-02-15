package ch.vd.uniregctb.interfaces;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ObjectBeanFactory extends ApplicationObjectSupport {

	public static final String UNIREG_CORE_DAO = "classpath:unireg-core-dao.xml";
	public static final String UNIREG_CORE_SF = "classpath:unireg-core-sf.xml";
	public static final String UNIREG_BUSINESS_INTERFACES = "classpath:unireg-business-interfaces.xml";
	public static final String UNIREG_BUSINESS_SERVICES = "classpath:unireg-business-services.xml";
	//public static final String UNIREG_BUSINESS_CACHE = "classpath:unireg-business-cache.xml";
	public static final String UNIREG_BUSINESS_APIREG = "classpath:unireg-business-apireg.xml";


	public static final String UINTF_EJB_DATABASE = "classpath:uintf-ejb-database.xml";
	public static final String UINTF_EJB_INTERFACES = "classpath:uintf-ejb-interfaces.xml";
	public static final String UINTF_EJB_BEANS = "classpath:uintf-ejb-beans.xml";
	public static final String UINTF_EJB_SERVICES = "classpath:uintf-ejb-services.xml";
	public static final String UINTF_EJB_JMS = "classpath:uintf-ejb-jms.xml";

	private static final String[] configLocations = new String[] {
			UNIREG_CORE_DAO,
			UNIREG_CORE_SF,
			UNIREG_BUSINESS_INTERFACES,
			UINTF_EJB_DATABASE,
			UINTF_EJB_INTERFACES,
			UINTF_EJB_BEANS,
			UNIREG_BUSINESS_SERVICES,
		//	UNIREG_BUSINESS_CACHE,
			UNIREG_BUSINESS_APIREG,
			UINTF_EJB_SERVICES,
			UINTF_EJB_JMS
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
