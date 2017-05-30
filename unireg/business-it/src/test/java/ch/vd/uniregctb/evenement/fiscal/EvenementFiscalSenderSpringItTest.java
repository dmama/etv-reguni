package ch.vd.uniregctb.evenement.fiscal;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-jms.xml"
})
public class EvenementFiscalSenderSpringItTest extends BusinessItTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(EvenementFiscalSenderSpringItTest.class);

	private EsbJmsTemplate esbTemplate;
	private EvenementFiscalSender sender;
	private String OUTPUT_QUEUE;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");
		sender = getBean(EvenementFiscalSender.class, "evenementFiscalSenderPourTest");

		// doit-être identique au nom donné dans le fichier Spring!
		OUTPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtFiscal");

		clearQueue(OUTPUT_QUEUE);

		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderSpringTest");
	}

	@Override
	public void onTearDown() throws Exception {
		AuthenticationHelper.popPrincipal();
		super.onTearDown();
	}

	protected void clearQueue(String queueName) throws Exception {
		while (true) {
			final boolean found = doInNewReadOnlyTransaction(new TxCallback<Boolean>() {
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

	protected EsbMessage receive(String queueName, long timeoutMs) throws Exception {
		return doInNewTransactionAndSession(new TxCallback<EsbMessage>() {
			@Override
			public EsbMessage execute(TransactionStatus status) throws Exception {
				esbTemplate.setReceiveTimeout(timeoutMs);
				return esbTemplate.receive(queueName);
			}
		});
	}

	@Test
	public void testTransactionaliteSend() throws Exception {

		// test positif : le message arrive bien
		{
			final MutableLong ppId = new MutableLong();
			sendEvent(false, ppId);

			// on doit maintenant vérifier que le message a bien été envoyé
			LOGGER.info("Attente du premier message message pendant 3s maximum");

			final Set<String> received = new HashSet<>();
			for (int i = 0 ; i < 4 ; ++ i) {            // pour l'instant, les v1, v2 et v3 sont envoyés (= 3 événements)
				final EsbMessage msg = receive(OUTPUT_QUEUE, 3000);         // On attend le message jusqu'à 3 secondes
				LOGGER.info("Message reçu ou timeout expiré");
				Assert.assertNotNull(msg);

				final String version = msg.getHeader(EvenementFiscalSender.VERSION_ATTRIBUTE);
				Assert.assertNotNull("Message reçu sans version...", version);
				if (received.contains(version)) {
					Assert.fail("Au moins deux messages reçus avec la même version '" + version + "'");
				}
				received.add(version);
			}
			Assert.assertEquals(4, received.size());
			Assert.assertTrue("v1 absent", received.contains("1"));
			Assert.assertTrue("v2 absent", received.contains("2"));
			Assert.assertTrue("v3 absent", received.contains("3"));
			Assert.assertTrue("v4 absent", received.contains("4"));

			// la personne physique doit avoir été sauvegardée en base
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId.getValue());
					Assert.assertNotNull(pp);
					return null;
				}
			});
		}

		// test négatif : si une exception provoque le rollback de la transaction, alors rien ne doit revenir
		{
			final MutableLong ppId = new MutableLong();
			try {
				sendEvent(true, ppId);
				Assert.fail("Où est passée l'exception ?");
			}
			catch (RuntimeException e) {
				Assert.assertEquals(RuntimeException.class, e.getClass());
				Assert.assertEquals("Exception de test", e.getMessage());
			}

			// on doit maintenant vérifier qu'aucun message n'a été envoyé
			LOGGER.info("Attente du premier message message pendant 3s maximum");

			final EsbMessage msg = receive(OUTPUT_QUEUE, 3000);         // On attend le message jusqu'à 3 secondes
			LOGGER.info("Message reçu ou timeout expiré");
			Assert.assertNull(msg);

			// la personne physique de doit pas avoir été sauvegardée en base
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId.getValue());
					Assert.assertNull(pp);
					return null;
				}
			});
		}
	}

	/**
	 * @param saute vrai si la transaction doit sauter
	 * @return le numéro du tiers créé
	 */
	private void sendEvent(final boolean saute, final MutableLong ppId) throws Exception {

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				// Création du message
				final PersonnePhysique pp = addNonHabitant("Maria", "Goldberg", null, Sexe.FEMININ);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2009, 12, 9), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				final EvenementFiscalFor event = hibernateTemplate.merge(new EvenementFiscalFor(ffp.getDateDebut(), ffp, EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE));
				ppId.setValue(pp.getNumero());

				try {
					sender.sendEvent(event);
				}
				catch (EvenementFiscalException e) {
					throw new RuntimeException("Exception inattendue", e);
				}

				if (saute) {
					throw new RuntimeException("Exception de test");
				}
			}
		});
	}

	/**
	 * Vérifie qu'aucun événement n'est envoyé dans une transaction marquée comme rollback-only (voir utilisation de la méthode ForFiscalManagerImpl#buildSynchronizeActionsTableSurFermetureDeFor)
	 */
	@Test           //(timeout = 10000L)
	public void testSendEvenementInRollbackOnlyTransaction() throws Exception {

		// Premier essai avec une transaction normale
		doInNewTransactionAndSession(new SendEventCallback(false));

		// On vérifie que le message a été envoyé et bien reçu
		EsbMessage msg = receive(OUTPUT_QUEUE, 3000);       // On attend le message jusqu'à 3 secondes
		assertNotNull(msg);

		clearQueue(OUTPUT_QUEUE);

		// Second essai avec une transaction rollback-only
		doInNewTransactionAndSession(new SendEventCallback(true));

		// On vérifie que le message n'a *pas* été envoyé
		msg = receive(OUTPUT_QUEUE, 3000);      // On attend le message jusqu'à 3 secondes
		assertNull(msg);
	}

	private class SendEventCallback implements TransactionCallback<Object> {
		private final boolean simul;

		public SendEventCallback(boolean simul) {
			this.simul = simul;
		}

		@Override
		public Object doInTransaction(TransactionStatus status) {

			if (simul) {
				status.setRollbackOnly();
			}

			// Création du message
			final PersonnePhysique pp = addNonHabitant("Maria", "Goldberg", null, Sexe.FEMININ);
			final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2009, 12, 9), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			final EvenementFiscalFor event = hibernateTemplate.merge(new EvenementFiscalFor(RegDate.get(2009, 12, 9), ffp, EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE));

			// Envoi du message
			try {
				sender.sendEvent(event);
			}
			catch (EvenementFiscalException e) {
				throw new RuntimeException(e);
			}

			return null;
		}
	}}
