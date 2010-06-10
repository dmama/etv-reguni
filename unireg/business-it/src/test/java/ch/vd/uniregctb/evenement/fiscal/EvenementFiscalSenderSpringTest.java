package ch.vd.uniregctb.evenement.fiscal;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementFiscalFor;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-jms.xml"
})
public class EvenementFiscalSenderSpringTest extends BusinessItTest {

	public static final Logger LOGGER = Logger.getLogger(EvenementFiscalSenderSpringTest.class);

	private EsbJmsTemplate esbTemplate;
	private EvenementFiscalSenderImpl sender;
	private final String OUTPUT_QUEUE = "ch.vd.unireg.test.output";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");
		final EsbMessageFactory esbMsgFactory = getBean(EsbMessageFactory.class, "esbMessageFactory");

		sender = new EvenementFiscalSenderImpl();
		sender.setEsbMessageFactory(esbMsgFactory);
		sender.setEnabled(true);
		sender.setEsbTemplate(esbTemplate);
		sender.setOutputQueue(OUTPUT_QUEUE);

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
	@NotTransactional
	public void testTransactionaliteSend() throws Exception {

		// test positif : le message arrive bien
		{
			sendEvent(false);

			// on doit maintenant vérifier que le message a bien été envoyé
			LOGGER.info("Attente du premier message message pendant 3s maximum");

			esbTemplate.setReceiveTimeout(3000);        // On attend le message jusqu'à 3 secondes
			final EsbMessage msg = esbTemplate.receive(OUTPUT_QUEUE);
			LOGGER.info("Message reçu ou timeout expiré");
			Assert.assertNotNull(msg);
		}

		// test négatif : si une exception provoque le rollback de la transaction, alors rien ne doit revenir
		{
			try {
				sendEvent(true);
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
		}
	}

	private void sendEvent(final boolean saute) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// Création du message
				final Tiers tiers = new PersonnePhysique(false);
				tiers.setNumero(10000001L);

				final EvenementFiscalFor event = new EvenementFiscalFor(tiers, RegDate.get(2009, 12, 9), TypeEvenementFiscal.OUVERTURE_FOR, MotifFor.ARRIVEE_HS, ModeImposition.ORDINAIRE, (long) 1);
				event.setId(1234L);

				try {
					sender.sendEvent(event);
				}
				catch (EvenementFiscalException e) {
					throw new RuntimeException("Exception inattendue", e);
				}

				if (saute) {
					throw new RuntimeException("Exception de test");
				}
				else {
					return null;
				}
			}
		});
	}
}
