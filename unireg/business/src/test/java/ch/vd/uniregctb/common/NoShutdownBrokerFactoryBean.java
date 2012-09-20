package ch.vd.uniregctb.common;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.xbean.BrokerFactoryBean;

import ch.vd.registre.base.utils.Assert;

/**
 * Permet de créer un broker définit dans activemq-broker.xml Le broker n'est stoppé par le context Spring mais a la fin de la JVM par shutdown hook
 *
 * @author jec
 */
public class NoShutdownBrokerFactoryBean extends BrokerFactoryBean {

	private static BrokerService broker;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (broker == null) {
			super.afterPropertiesSet();
			broker = super.getBroker();
		}
		Assert.notNull(getBroker());
	}

	@Override
	public BrokerService getBroker() {
		return broker;
	}

	@Override
	public Object getObject() throws Exception {
		return broker;
	}

	@Override
	public void destroy() throws Exception {
		// On ne destroy pas le broker a la destruction du context
		// super.destroy();
	}

}
