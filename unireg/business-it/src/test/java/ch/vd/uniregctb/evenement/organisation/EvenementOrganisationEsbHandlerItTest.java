package ch.vd.uniregctb.evenement.organisation;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.connection.JmsTransactionManager;

import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.GentilEsbMessageEndpointListener;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationEsbHandlerItTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementOrganisationSenderImpl sender;
	private EvenementOrganisationEsbHandler esbHandler;
	private List<EvenementOrganisation> evenementsTraites;
	private List<OrganisationsOfNotice> evenementsIgnores;
	private List<Pair<String, Throwable>> evenementsExploses;
	private List<String> evenementsVusPasser;

	private static final Set<TypeEvenementOrganisation> IGNORED = EnumSet.of(TypeEvenementOrganisation.FOSC_REFUS_HOMOLOGATION_DU_CONCORDAT,
	                                                                     TypeEvenementOrganisation.FOSC_COMMANDEMENT_DE_PAYER);

	@Before
	public void setup() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtOrganisation");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		esbTemplate.setSessionTransacted(true);
		if (esbTemplate instanceof InitializingBean) {
			((InitializingBean) esbTemplate).afterPropertiesSet();
		}

		clearQueue(INPUT_QUEUE);

		evenementsTraites = new LinkedList<>();
		final EvenementOrganisationReceptionHandler receptionHandler = new EvenementOrganisationReceptionHandler() {
			@Override
			public EvenementOrganisation saveIncomingEvent(EvenementOrganisation event) {
				// pas de sauvegarde ici...
				return event;
			}

			@Override
			public EvenementOrganisation handleEvent(EvenementOrganisation event, EvenementOrganisationProcessingMode mode) throws EvenementOrganisationException {
				evenementsTraites.add(event);
				return event;
			}
		};

		evenementsVusPasser = new LinkedList<>();
		evenementsIgnores = new LinkedList<>();
		evenementsExploses = new LinkedList<>();
		esbHandler = new EvenementOrganisationEsbHandler() {
			@Override
			public void onEsbMessage(EsbMessage message) throws EsbBusinessException {
				try {
					super.onEsbMessage(message);
				}
				catch (Throwable t) {
					evenementsExploses.add(Pair.of(message.getBusinessId(), t));
				}
				finally {
					synchronized (evenementsVusPasser) {
						evenementsVusPasser.add(message.getBusinessId());
						evenementsVusPasser.notifyAll();
					}
				}
			}

			@Override
			protected void onIgnoredEvent(OrganisationsOfNotice message) throws EvenementOrganisationEsbException {
				super.onIgnoredEvent(message);
				evenementsIgnores.add(message);
			}
		};
		esbHandler.setIgnoredEventTypes(IGNORED);
		esbHandler.setReceptionHandler(receptionHandler);
		esbHandler.afterPropertiesSet();

		final GentilEsbMessageEndpointListener listener = new GentilEsbMessageEndpointListener();
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setEsbTemplate(esbTemplate);
		listener.setHandler(esbHandler);

		initEndpointManager(INPUT_QUEUE, listener);

		sender = new EvenementOrganisationSenderImpl();
		sender.setEsbTemplate(esbTemplate);
		sender.setOutputQueue(INPUT_QUEUE);
		sender.afterPropertiesSet();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenement() throws Exception {
		final Long idEvenement = 5640006354L;
		final RegDate dateEvenement = RegDate.get();
		long noOrganisation = 657133465L;
		final TypeEvenementOrganisation type = TypeEvenementOrganisation.FOSC_AUTRE_MUTATION;

		final EvenementOrganisation evt = new EvenementOrganisation(
				idEvenement,
		        type,
		        dateEvenement,
				noOrganisation,
		        EtatEvenementOrganisation.A_TRAITER
		);
		evt.setCommentaireTraitement("turlututu");
		evt.setDateTraitement(DateHelper.getCurrentDate());
		evt.setEtat(EtatEvenementOrganisation.A_VERIFIER);

		Assert.assertEquals(0, evenementsTraites.size());
		sender.sendEvent(evt, "toto", true);

		// On attend le message
		synchronized (evenementsVusPasser) {
			while (evenementsVusPasser.size() == 0) {
				evenementsVusPasser.wait();
			}
		}
		Assert.assertEquals(1, evenementsVusPasser.size());
		Assert.assertEquals(1, evenementsTraites.size());

		final EvenementOrganisation recu = evenementsTraites.get(0);
		Assert.assertNotNull(recu);
		Assert.assertEquals((long) idEvenement, recu.getId());
		Assert.assertEquals(dateEvenement, recu.getDateEvenement());
		Assert.assertNull(recu.getCommentaireTraitement());
		Assert.assertNull(recu.getDateTraitement());
		Assert.assertEquals(EtatEvenementOrganisation.A_TRAITER, recu.getEtat());
		Assert.assertEquals(noOrganisation, recu.getNoOrganisation());
		Assert.assertEquals(type, recu.getType());

		Assert.assertEquals(0, evenementsIgnores.size());
		Assert.assertEquals(0, evenementsExploses.size());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testEvenementsIgnores() throws Exception {

		for (TypeEvenementOrganisation type : IGNORED) {

			evenementsVusPasser.clear();
			evenementsTraites.clear();
			evenementsIgnores.clear();
			evenementsExploses.clear();

			final Long idEvenement = 5640006354L + type.ordinal();
			final RegDate dateEvenement = RegDate.get();
			final String refData = "26541888874";
			long noOrganisation = 657133465L;

			final EvenementOrganisation evt = new EvenementOrganisation(
					idEvenement,
					type,
					dateEvenement,
					noOrganisation,
					EtatEvenementOrganisation.A_TRAITER
			);
			evt.setCommentaireTraitement("turlututu");
			evt.setDateTraitement(DateHelper.getCurrentDate());
			evt.setEtat(EtatEvenementOrganisation.A_VERIFIER);

			Assert.assertEquals(0, evenementsTraites.size());
			sender.sendEvent(evt, "toto", true);

			// On attend le message
			synchronized (evenementsVusPasser) {
				while (evenementsVusPasser.size() == 0) {
					evenementsVusPasser.wait();
				}
			}
			Assert.assertEquals("type " + type, 1, evenementsVusPasser.size());
			Assert.assertEquals("type " + type, 0, evenementsTraites.size());
			Assert.assertEquals("type " + type, 1, evenementsIgnores.size());
			Assert.assertEquals("type " + type, 0, evenementsExploses.size());


			final EvenementOrganisation recu = EvenementOrganisationConversionHelper.createEvenement(evenementsIgnores.get(0));
			Assert.assertNotNull("type " + type, recu);
			Assert.assertEquals("type " + type, (long) idEvenement, recu.getId());
			Assert.assertEquals("type " + type, dateEvenement, recu.getDateEvenement());
			Assert.assertNull("type " + type, recu.getCommentaireTraitement());
			Assert.assertNull("type " + type, recu.getDateTraitement());
			Assert.assertEquals("type " + type, EtatEvenementOrganisation.A_TRAITER, recu.getEtat());
			Assert.assertEquals("type " + type, type, recu.getType());
		}
	}

	@Test
	public void testEvenementSansDate() throws Exception {

		evenementsVusPasser.clear();
		evenementsTraites.clear();
		evenementsIgnores.clear();
		evenementsExploses.clear();

		final Long idEvenement = 48515544L;
		final RegDate dateEvenement = null;
		final String refData = "26541888874";
		long noOrganisation = 657133465L;
		final TypeEvenementOrganisation type = TypeEvenementOrganisation.FOSC_AUTRE_MUTATION;

		final EvenementOrganisation evt = new EvenementOrganisation(
				idEvenement,
				type,
				dateEvenement,
				noOrganisation,
				EtatEvenementOrganisation.A_TRAITER
		);
		evt.setCommentaireTraitement("turlututu");
		evt.setDateTraitement(DateHelper.getCurrentDate());
		evt.setEtat(EtatEvenementOrganisation.A_VERIFIER);
		evt.setType(type);

		Assert.assertEquals(0, evenementsTraites.size());
		sender.sendEvent(evt, "toto", false);

		// On attend le message
		synchronized (evenementsVusPasser) {
			while (evenementsVusPasser.size() == 0) {
				evenementsVusPasser.wait();
			}
		}
		Assert.assertEquals("type " + type, 1, evenementsVusPasser.size());
		Assert.assertEquals("type " + type, 0, evenementsIgnores.size());

		Assert.assertEquals("type " + type, 0, evenementsTraites.size());
		Assert.assertEquals("type " + type, 1, evenementsExploses.size());

		final Pair<String, Throwable> explosionData = evenementsExploses.get(0);
		Assert.assertNotNull(explosionData);
		Assert.assertNotNull(explosionData.getLeft());
		Assert.assertNotNull(explosionData.getRight());

		//noinspection ThrowableResultOfMethodCallIgnored
		final Throwable t = explosionData.getRight();
		Assert.assertEquals(EvenementOrganisationEsbException.class, t.getClass());
		Assert.assertEquals("org.xml.sax.SAXParseException; cvc-complex-type.2.4.b: The content of element 'eVD-0022-3:notice' is not complete. One of '{\"http://evd.vd.ch/xmlns/eVD-0022/3\":noticeDate}' is expected.",
		                    t.getMessage());
	}
}
