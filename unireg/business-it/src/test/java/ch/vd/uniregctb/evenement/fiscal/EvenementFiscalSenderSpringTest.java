package ch.vd.uniregctb.evenement.fiscal;

import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementFiscalFor;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-jms.xml"
})
public class EvenementFiscalSenderSpringTest extends BusinessItTest {

	public static final Logger LOGGER = Logger.getLogger(EvenementFiscalSenderSpringTest.class);

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
		while (esbTemplate.receive(queueName) != null) {}
	}

	@Test
	public void testTransactionaliteSend() throws Exception {

		// test positif : le message arrive bien
		{
			final MutableLong ppId = new MutableLong();
			sendEvent(false, ppId);

			// on doit maintenant vérifier que le message a bien été envoyé
			LOGGER.info("Attente du premier message message pendant 3s maximum");

			esbTemplate.setReceiveTimeout(3000);        // On attend le message jusqu'à 3 secondes
			final EsbMessage msg = esbTemplate.receive(OUTPUT_QUEUE);
			LOGGER.info("Message reçu ou timeout expiré");
			Assert.assertNotNull(msg);

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

			esbTemplate.setReceiveTimeout(3000);        // On attend le message jusqu'à 3 secondes
			final EsbMessage msg = esbTemplate.receive(OUTPUT_QUEUE);
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
	private void sendEvent(final boolean saute, final MutableLong ppId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// Création du message
				final Tiers tiers = addNonHabitant("Maria", "Goldberg", null, Sexe.FEMININ);
				final EvenementFiscalFor event = new EvenementFiscalFor(tiers, RegDate.get(2009, 12, 9), TypeEvenementFiscal.OUVERTURE_FOR, MotifFor.ARRIVEE_HS, ModeImposition.ORDINAIRE, (long) 1);
				event.setId(1234L);
				ppId.setValue(tiers.getNumero());

				try {
					sender.sendEvent(event);
				}
				catch (EvenementFiscalException e) {
					throw new RuntimeException("Exception inattendue", e);
				}

				if (saute) {
					throw new RuntimeException("Exception de test");
				}

				return null;
			}
		});
	}

	/**
	 * Vérifie qu'aucun événement n'est envoyé dans une transaction marquée comme rollback-only (voir utilisation de la méthode ForFiscalManagerImpl#buildSynchronizeActionsTableSurFermetureDeFor)
	 */
	@Test
	public void testSendEvenementInRollbackOnlyTransaction() throws Exception {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);

		// Premier essai avec une transaction normale
		template.setReadOnly(false);
		template.execute(new SendEventCallback(false));

		// On vérifie que le message a été envoyé et bien reçu
		esbTemplate.setReceiveTimeout(3000);        // On attend le message jusqu'à 3 secondes
		EsbMessage msg = esbTemplate.receive(OUTPUT_QUEUE);
		assertNotNull(msg);

		clearQueue(OUTPUT_QUEUE);

		// Second essai avec une transaction rollback-only
		template.setReadOnly(true);
		template.execute(new SendEventCallback(true));

		// On vérifie que le message n'a *pas* été envoyé
		esbTemplate.setReceiveTimeout(3000);        // On attend le message jusqu'à 3 secondes
		msg = esbTemplate.receive(OUTPUT_QUEUE);
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
			final Tiers tiers = new PersonnePhysique(false);
			tiers.setNumero(10001111L);
			final EvenementFiscalFor event = new EvenementFiscalFor(tiers, RegDate.get(2009, 12, 9), TypeEvenementFiscal.OUVERTURE_FOR, MotifFor.ARRIVEE_HS, ModeImposition.ORDINAIRE, (long) 1);
			event.setId(1234L);

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
