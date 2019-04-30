package ch.vd.unireg.transaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.evenement.EvenementHelper;
import ch.vd.unireg.tiers.AutreCommunaute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests dédiés à la vérification de la configuration XA des transactions.
 */
public class XaTransactionTest extends BusinessItTest {

	private EsbJmsTemplate esbTemplate;
	private String queueName;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");
		queueName = uniregProperties.getProperty("testprop.jms.queue.xa.transaction");
		assertNotNull(queueName);

		clearQueue();
		clearData();
	}

	/**
	 * Ce test vérifie que l'envoi de messages JMS et l'insertions simultanées de données en DB fonctionne bien sans exception.
	 */
	@Test(timeout = 10000)
	public void testSendMessageInsertDataWithoutException() throws Exception {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);

		// on envoie 20 messages et insère 20 données en autant de transactions différentes
		for (int i = 0; i < 20; ++i) {
			final String body = buildBody(i);
			template.execute(status -> {
				sendTextMessage(queueName, body, null);
				insertData(body);
				return null;
			});
		}

		// on vérifie que les messages sont bien envoyés
		final List<EsbMessage> messages = getMessages(queueName);
		assertEquals(20, messages.size());
		messages.sort(Comparator.comparing(XaTransactionTest::getBodyAsString));
		for (int i = 0; i < 20; ++i) {
			final String body = buildBody(i);
			assertEquals(body, messages.get(i).getBodyAsString());
		}

		// on vérifie que les données sont bien insérées
		final List<AutreCommunaute> lines = doInNewTransaction(status -> hibernateTemplate.find("from AutreCommunaute", FlushMode.MANUAL));
		assertEquals(20, lines.size());
		lines.sort(Comparator.comparing(AutreCommunaute::getNom));
		for (int i = 0; i < 20; ++i) {
			final String body = buildBody(i);
			assertEquals(body, lines.get(i).getNom());
		}
	}

	/**
	 * Ce test vérifie que l'envoi de messages JMS et l'insertions simultanées de données en DB ne fait rien lorsqu'il y a des exceptions.
	 */
	@Test(timeout = 10000)
	public void testSendMessageInsertDataWithException() throws Exception {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);

		// on envoie 20 messages et insère 20 données en autant de transactions différentes
		// et à chaque fois on lève une exception
		for (int i = 0; i < 20; ++i) {
			final String body = buildBody(i);
			try {
				template.execute(status -> {
					sendTextMessage(queueName, body, null);
					insertData(body);
					throw new RuntimeException("exception de test");
				});
			}
			catch (RuntimeException ignored) {
				assertEquals("exception de test", ignored.getMessage());
			}
		}

		// on vérifie qu'aucun message n'a été envoyé
		final List<EsbMessage> messages = getMessages(queueName);
		assertEquals(0, messages.size());

		// on vérifie qu'aucune donnée n'a été insérée
		final List<AutreCommunaute> lines = doInNewTransaction(status -> hibernateTemplate.find("from AutreCommunaute", FlushMode.MANUAL));
		assertEquals(0, lines.size());
	}

	/**
	 * Ce test vérifie que l'envoi de messages JMS et l'insertions simultanées de données en DB ne fait rien lorsque la transaction est marqué 'rollback-only'
	 */
	@Test(timeout = 10000)
	public void testSendMessageInsertDataWithRollbackOnly() throws Exception {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);

		// on envoie 20 messages et insère 20 données en autant de transactions différentes
		// et à chaque fois on lève une exception
		for (int i = 0; i < 20; ++i) {
			final String body = buildBody(i);
			template.execute(status -> {
				sendTextMessage(queueName, body, null);
				insertData(body);
				status.setRollbackOnly();
				return null;
			});
		}

		// on vérifie qu'aucun message n'a été envoyé
		final List<EsbMessage> messages = getMessages(queueName);
		assertEquals(0, messages.size());

		// on vérifie qu'aucune donnée n'a été insérée
		final List<AutreCommunaute> lines = doInNewTransaction(status -> hibernateTemplate.find("from AutreCommunaute", FlushMode.MANUAL));
		assertEquals(0, lines.size());
	}

	@NotNull
	private static String buildBody(int i) {
		return String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><testData>%02d</testData>", i);
	}

	/**
	 * @return le businessId du message envoyé
	 */
	private String sendTextMessage(String queueName, String texte, String replyTo) {
		try {
			final EsbMessage m = buildTextMessage(queueName, texte, replyTo);
			EvenementHelper.sendMessage(esbTemplate, m, transactionManager);
			return m.getBusinessId();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void clearQueue() throws Exception {
		doInNewTransaction(status -> {
			try {
				EvenementHelper.clearQueue(esbTemplate, queueName);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			return null;
		});
	}

	@SuppressWarnings("CodeBlock2Expr")
	private void clearData() throws Exception {
		doInNewTransaction(status -> {
			return hibernateTemplate.execute(session -> {
				return session.createQuery("delete from AutreCommunaute").executeUpdate();
			});
		});
	}

	private Long insertData(String texte) {
		// on utilise une autre communauté parce qu'il s'agit d'une entité indépendante, mais n'importe quelle autre entité aurait été possible
		AutreCommunaute data = new AutreCommunaute();
		data.setNom(texte);
		data = hibernateTemplate.merge(data);
		return data.getId();
	}

	private EsbMessage buildTextMessage(String queueName, String texte, String replyTo) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessUser("XaTransactionTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		m.setServiceReplyTo(replyTo);
		return m;
	}

	@NotNull
	private List<EsbMessage> getMessages(String queueName) throws Exception {
		final long timeout = esbTemplate.getReceiveTimeout();
		esbTemplate.setReceiveTimeout(100); // on ne veut pas attendre trop longtemps si la queue est déjà vide
		try {
			final List<EsbMessage> messages = new ArrayList<>();
			while (true) {
				final EsbMessage message = doInNewTransaction(status -> receiveMessage(queueName));
				if (message == null) {
					break;
				}
				messages.add(message);
			}
			return messages;
		}
		finally {
			esbTemplate.setReceiveTimeout(timeout);
		}
	}

	private EsbMessage receiveMessage(String queueName) {
		try {
			return esbTemplate.receive(queueName);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String getBodyAsString(EsbMessage message) {
		try {
			return message.getBodyAsString();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
