package ch.vd.uniregctb.evenement;

import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;
import javax.resource.spi.ResourceAdapter;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ra.ActiveMQActivationSpec;
import org.apache.activemq.ra.ActiveMQResourceAdapter;
import org.springframework.jca.support.SimpleBootstrapContext;
import org.springframework.jca.work.SimpleTaskWorkManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Log4jConfigurer;

import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.jms.EsbMessageEndpointManager;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.utils.UniregProperties;
import ch.vd.uniregctb.utils.UniregPropertiesImpl;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class EvenementHelper {

	public static void initLog4j() {
		try {
			Log4jConfigurer.initLogging("classpath:ut/log4j.xml");
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static UniregProperties initProps() {
		try {
			final UniregPropertiesImpl impl = new UniregPropertiesImpl();
			impl.setFilename("../base/unireg-ut.properties");
			impl.afterPropertiesSet();
			return impl;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static ConnectionFactory initConnectionFactory(UniregProperties uniregProperties) {
		final String url = uniregProperties.getProperty("testprop.esb.jms.url");
		final String username = uniregProperties.getProperty("testprop.esb.jms.username");
		final String password = uniregProperties.getProperty("testprop.esb.jms.password");

		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
		factory.setBrokerURL(url);
		factory.setUserName(username);
		factory.setPassword(password);
		return factory;
	}

	public static ActiveMQResourceAdapter initResourceAdapter(UniregProperties uniregProperties) {
		final String url = uniregProperties.getProperty("testprop.esb.jms.url");
		final String username = uniregProperties.getProperty("testprop.esb.jms.username");
		final String password = uniregProperties.getProperty("testprop.esb.jms.password");

		ActiveMQResourceAdapter resourceAdapter = new ActiveMQResourceAdapter();
		resourceAdapter.setServerUrl(url);
		resourceAdapter.setUserName(username);
		resourceAdapter.setPassword(password);

		try {
			resourceAdapter.start(new SimpleBootstrapContext(new SimpleTaskWorkManager(), null));
			return resourceAdapter;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static EsbMessageEndpointManager initEndpointManager(ResourceAdapter resourceAdapter, String queueName, MessageListener listener) {

		final ActiveMQActivationSpec activationSpec = new ActiveMQActivationSpec();
		activationSpec.setDestination(queueName);
		activationSpec.setDestinationType("javax.jms.Queue");
		activationSpec.setMaxSessions("1");
		activationSpec.setMaxMessagesPerSessions("1");

		EsbMessageEndpointManager manager = new EsbMessageEndpointManager();
		manager.setResourceAdapter(resourceAdapter);
		manager.setActivationSpec(activationSpec);
		manager.setMessageListener(listener);

		try {
			manager.afterPropertiesSet();
			manager.start();
			return manager;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void clearQueue(EsbJmsTemplate esbTemplate, String queueName) throws Exception {
		final long timeout = esbTemplate.getReceiveTimeout();
		esbTemplate.setReceiveTimeout(100); // on ne veut pas attendre trop longtemps si la queue est déjà vide
		try {
			while (true) {
				final boolean found = esbTemplate.receive(queueName) != null;
				if (!found) {
					break;
				}
			}
		}
		finally {
			esbTemplate.setReceiveTimeout(timeout);
		}
	}

	public static void clearQueue(EsbJmsTemplate esbTemplate, String queueName, PlatformTransactionManager transactionManager) throws Exception {
		final long timeout = esbTemplate.getReceiveTimeout();
		esbTemplate.setReceiveTimeout(100); // on ne veut pas attendre trop longtemps si la queue est déjà vide
		try {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			while (true) {
				final boolean found = template.execute(new TxCallback<Boolean>() {
					@Override
					public Boolean execute(TransactionStatus status) throws Exception {
						return esbTemplate.receive(queueName) != null;
					}
				});
				if (!found) {
					break;
				}
			}
		}
		finally {
			esbTemplate.setReceiveTimeout(timeout);
		}
	}

	public static void sendMessage(EsbJmsTemplate esbTemplate, EsbMessage message, PlatformTransactionManager transactionManager) throws Exception {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);            // nouvelle transaction si hors transaction, en conservant la transaction existante si une existe déjà
		template.execute(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				esbTemplate.send(message);
			}
		});
	}

	public static EsbMessage getMessage(EsbJmsTemplate esbTemplate, String queueName, long timeoutMs, PlatformTransactionManager transactionManager) throws Exception {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TxCallback<EsbMessage>() {
			@Override
			public EsbMessage execute(TransactionStatus status) throws Exception {
				esbTemplate.setReceiveTimeout(timeoutMs);
				return esbTemplate.receive(queueName);
			}
		});
	}

	/**
	 * @param year  l'année
	 * @param month le mois (1-12)
	 * @param day   le jour (1-31)
	 * @return une date initialisée au jour, mois et année spécifiés.
	 */
	public static Date newUtilDate(int year, int month, int day) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.clear();
		cal.set(year, month - 1, day);
		return cal.getTime();
	}
}

