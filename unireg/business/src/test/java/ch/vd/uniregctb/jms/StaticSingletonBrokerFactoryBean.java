package ch.vd.uniregctb.jms;

import java.beans.PropertyEditorManager;
import java.net.URI;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.xbean.XBeanBrokerService;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.xbean.spring.context.ResourceXmlApplicationContext;
import org.apache.xbean.spring.context.impl.URIEditor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

/**
 * Singleton statique du broker JMS, c'est-à-dire qu'il peut-être partagé par plusieurs contextes Spring.
 */
public class StaticSingletonBrokerFactoryBean implements FactoryBean, InitializingBean, DisposableBean, ApplicationContextAware {

	static {
		PropertyEditorManager.registerEditor(URI.class, URIEditor.class);
	}

	private static XBeanBrokerService broker;
	private static final MutableInt count = new MutableInt(0);
	private static ResourceXmlApplicationContext context;

	private Resource config;
	private boolean start;
	private ApplicationContext parentContext;

	private boolean systemExitOnShutdown;
	private int systemExitOnShutdownExitCode;

	public StaticSingletonBrokerFactoryBean() {
	}

	public StaticSingletonBrokerFactoryBean(Resource config) {
		this.config = config;
	}

	public Object getObject() throws Exception {
		return broker;
	}

	public Class getObjectType() {
		return BrokerService.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void setApplicationContext(ApplicationContext parentContext) throws BeansException {
		this.parentContext = parentContext;
	}

	public void afterPropertiesSet() throws Exception {
		synchronized (count) {
			if (broker == null) {
				initBroker();
			}
			count.increment();
		}
	}

	private void initBroker() throws Exception {
		if (config == null) {
			throw new IllegalArgumentException("config property must be set");
		}
		context = new ResourceXmlApplicationContext(config, parentContext);

		try {
			broker = (XBeanBrokerService) context.getBean("broker");
		}
		catch (BeansException e) {
			// ignore...
			// log.trace("No bean named broker available: " + e, e);
		}
		if (broker == null) {
			// lets try find by type
			String[] names = context.getBeanNamesForType(BrokerService.class);
			for (int i = 0; i < names.length; i++) {
				String name = names[i];
				broker = (XBeanBrokerService) context.getBean(name);
				if (broker != null) {
					break;
				}
			}
		}
		if (broker == null) {
			throw new IllegalArgumentException("The configuration has no BrokerService instance for resource: " + config);
		}

		if (systemExitOnShutdown) {
			broker.addShutdownHook(() -> System.exit(systemExitOnShutdownExitCode));
		}
		if (start) {
			broker.start();
		}
	}

	public void destroy() throws Exception {
		synchronized (count) {
			count.decrement();
			if (count.intValue() == 0) {
				destroyBroker();
			}
		}
	}

	private void destroyBroker() throws Exception {
		if (context != null) {
			context.close();
			context = null;
		}
		if (broker != null) {
			broker.stop();
			broker = null;
		}
	}

	public Resource getConfig() {
		return config;
	}

	public void setConfig(Resource config) {
		this.config = config;
	}

	public BrokerService getBroker() {
		return broker;
	}

	public boolean isStart() {
		return start;
	}

	public void setStart(boolean start) {
		this.start = start;
	}

	public boolean isSystemExitOnStop() {
		return systemExitOnShutdown;
	}

	public void setSystemExitOnStop(boolean systemExitOnStop) {
		this.systemExitOnShutdown = systemExitOnStop;
	}

	public boolean isSystemExitOnShutdown() {
		return systemExitOnShutdown;
	}

	public void setSystemExitOnShutdown(boolean systemExitOnShutdown) {
		this.systemExitOnShutdown = systemExitOnShutdown;
	}

	public int getSystemExitOnShutdownExitCode() {
		return systemExitOnShutdownExitCode;
	}

	public void setSystemExitOnShutdownExitCode(int systemExitOnShutdownExitCode) {
		this.systemExitOnShutdownExitCode = systemExitOnShutdownExitCode;
	}

}

