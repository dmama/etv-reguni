package ch.vd.unireg.evenement;

import javax.jms.ConnectionFactory;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.jms.EsbMessageHelper;
import ch.vd.unireg.utils.UniregProperties;
import ch.vd.unireg.utils.UniregPropertiesImpl;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class EvenementHelper {

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

	public static void clearQueue(EsbJmsTemplate esbTemplate, String queueName, PlatformTransactionManager transactionManager) {
		final long timeout = esbTemplate.getReceiveTimeout();
		esbTemplate.setReceiveTimeout(100); // on ne veut pas attendre trop longtemps si la queue est déjà vide
		try {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			while (true) {
				final boolean found = template.execute(status -> {
					try {
						return esbTemplate.receive(queueName) != null;
					}
					catch (Exception e) {
						throw new RuntimeException(e);
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

	public static void sendTextMessage(EsbJmsTemplate esbTemplate, String queueName, String texte, String businessId, @Nullable Map<String, String> customAttributes, PlatformTransactionManager transactionManager) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		final String myBusinessId = businessId == null ? String.valueOf(m.hashCode()) : businessId;
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(myBusinessId);
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		if (customAttributes != null) {
			EsbMessageHelper.setHeaders(m, customAttributes, true);
		}
		sendMessage(esbTemplate, m, transactionManager);
	}

	public static void sendMessage(EsbJmsTemplate esbTemplate, EsbMessage message, PlatformTransactionManager transactionManager) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);            // nouvelle transaction si hors transaction, en conservant la transaction existante si une existe déjà
		template.execute(status -> {
			try {
				esbTemplate.send(message);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			return null;
		});
	}

	public static EsbMessage getMessage(EsbJmsTemplate esbTemplate, String queueName, long timeoutMs, PlatformTransactionManager transactionManager) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> {
			esbTemplate.setReceiveTimeout(timeoutMs);
			try {
				return esbTemplate.receive(queueName);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
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

