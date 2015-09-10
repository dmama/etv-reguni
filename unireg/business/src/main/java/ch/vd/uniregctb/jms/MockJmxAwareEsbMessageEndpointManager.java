package ch.vd.uniregctb.jms;

import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ra.ActiveMQActivationSpec;
import org.apache.activemq.ra.ActiveMQConnectionRequestInfo;
import org.apache.activemq.ra.MessageActivationSpec;
import org.apache.activemq.ra.MessageResourceAdapter;
import org.jencks.DefaultEndpointFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe endpointManagerpour le testing
 * </ul>
 */
public class MockJmxAwareEsbMessageEndpointManager extends JmxAwareEsbMessageEndpointManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(MockJmxAwareEsbMessageEndpointManager.class);



	@Override
	public String getDestinationName() {
		return "mock-JMS";
	}

	@Override
	public int getMaxConcurrentConsumers() {
		return 0;
	}

	@Override
	public void setMessageListener(final MessageListener messageListener) {

	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {

	}

	@Override
	public int getReceivedMessages() {
		return 0;
	}

	@Override
	public ActivationSpec getActivationSpec() {
		return new ActiveMQActivationSpec();
	}



	@Override
	public int getMessagesWithException() {
		return 0;
	}

	@Override
	public int getMessagesWithBusinessError() {
		return 0;
	}

	@Override
	public String getDescription() {
		return null;
	}

	public void setDescription(String description) {

	}

	@Override
	public ResourceAdapter getResourceAdapter() {
		return new MessageResourceAdapter() {
			@Override
			public ActiveMQConnection makeConnection(ActiveMQConnectionRequestInfo activeMQConnectionRequestInfo) throws JMSException {
				return null;
			}

			@Override
			public ActiveMQConnection makeConnection(MessageActivationSpec messageActivationSpec) throws JMSException {
				return null;
			}

			@Override
			public BootstrapContext getBootstrapContext() {
				return null;
			}

			@Override
			public String getBrokerXmlConfig() {
				return null;
			}

			@Override
			public ActiveMQConnectionRequestInfo getInfo() {
				return null;
			}

			@Override
			public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {

			}

			@Override
			public void stop() {

			}

			@Override
			public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {

			}

			@Override
			public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {

			}

			@Override
			public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
				return new XAResource[0];
			}
		};
	}

	@Override
	public MessageEndpointFactory getMessageEndpointFactory() {
		return new DefaultEndpointFactory();
	}
}
