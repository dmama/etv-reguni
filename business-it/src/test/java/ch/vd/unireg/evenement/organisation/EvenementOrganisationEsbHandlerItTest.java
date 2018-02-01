package ch.vd.unireg.evenement.organisation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.evd0022.v3.TypeOfNotice;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeEvenementOrganisation;

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
			public boolean dejaRecu(String businessId) {
				return false;
			}

			@Override
			@NotNull
			public List<EvenementOrganisation> saveIncomingEvent(List<EvenementOrganisation> events) {
				// pas de sauvegarde ici...
				return events;
			}

			@Override
			public List<EvenementOrganisation> handleEvents(List<EvenementOrganisation> events, EvenementOrganisationProcessingMode mode) throws EvenementOrganisationException {
				evenementsTraites.addAll(events);
				return events;
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

		initListenerContainer(INPUT_QUEUE, esbHandler);

		sender = new EvenementOrganisationSenderImpl();
		sender.setEsbTemplate(esbTemplate);
		sender.setOutputQueue(INPUT_QUEUE);
		sender.afterPropertiesSet();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenement() throws Exception {
		final Long noEvenement = 5640006354L;
		final RegDate dateEvenement = RegDate.get();
		long noOrganisation = 657133465L;
		final TypeEvenementOrganisation type = TypeEvenementOrganisation.FOSC_AUTRE_MUTATION;

		final EvenementOrganisation evt = new EvenementOrganisation(
				noEvenement,
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
		Assert.assertEquals((long) noEvenement, recu.getNoEvenement());
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

			final Long noEvenement = 5640006354L + type.ordinal();
			final RegDate dateEvenement = RegDate.get();
			final String refData = "26541888874";
			long noOrganisation = 657133465L;

			final EvenementOrganisation evt = new EvenementOrganisation(
					noEvenement,
					type,
					dateEvenement,
					noOrganisation,
					EtatEvenementOrganisation.A_TRAITER
			);
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


			final OrganisationsOfNotice orgOfNotice = evenementsIgnores.get(0);
			Assert.assertNotNull("type " + type, orgOfNotice);
			Assert.assertEquals("type " + type, noEvenement.longValue(), orgOfNotice.getNotice().getNoticeId().longValue());
			Assert.assertEquals("type " + type, dateEvenement, orgOfNotice.getNotice().getNoticeDate());
		}
	}

	@Test
	public void testEvenementSansDate() throws Exception {

		evenementsVusPasser.clear();
		evenementsTraites.clear();
		evenementsIgnores.clear();
		evenementsExploses.clear();

		final Long noEvenement = 48515544L;
		final RegDate dateEvenement = null;
		final String refData = "26541888874";
		long noOrganisation = 657133465L;
		final TypeEvenementOrganisation type = TypeEvenementOrganisation.FOSC_AUTRE_MUTATION;

		final EvenementOrganisation evt = new EvenementOrganisation(
				noEvenement,
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

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenementMultiplesOrganisations() throws Exception {
		final Long noEvenement = 5640006354L;
		final RegDate dateEvenement = RegDate.get();
		long noOrganisation = 0;
		final TypeEvenementOrganisation type = TypeEvenementOrganisation.FOSC_AUTRE_MUTATION;

		final EvenementOrganisation evt = new EvenementOrganisation(
				noEvenement,
				type,
				dateEvenement,
				noOrganisation,
				EtatEvenementOrganisation.A_TRAITER
		);
		evt.setCommentaireTraitement("turlututu");
		evt.setDateTraitement(DateHelper.getCurrentDate());
		evt.setEtat(EtatEvenementOrganisation.A_VERIFIER);

		Assert.assertEquals(0, evenementsTraites.size());
		sender.sendEventWithMultipleOrga(evt, Arrays.asList(1L, 2L , 3L, 4L), "toto", true);

		// On attend le message
		synchronized (evenementsVusPasser) {
			while (evenementsVusPasser.size() == 0) {
				evenementsVusPasser.wait();
			}
		}
		Assert.assertEquals(1, evenementsVusPasser.size());
		Assert.assertEquals(4, evenementsTraites.size());

		Collections.sort(evenementsTraites, new Comparator<EvenementOrganisation>() {
			@Override
			public int compare(EvenementOrganisation o1, EvenementOrganisation o2) {
				return Long.valueOf(o1.getNoOrganisation()).compareTo(o2.getNoOrganisation());
			}
		});

		{
			final EvenementOrganisation recu = evenementsTraites.get(0);
			Assert.assertNotNull(recu);
			Assert.assertEquals(noEvenement.longValue(), recu.getNoEvenement());
			Assert.assertEquals(dateEvenement, recu.getDateEvenement());
			Assert.assertNull(recu.getCommentaireTraitement());
			Assert.assertNull(recu.getDateTraitement());
			Assert.assertEquals(EtatEvenementOrganisation.A_TRAITER, recu.getEtat());
			Assert.assertEquals(1L, recu.getNoOrganisation());
			Assert.assertEquals(type, recu.getType());

			Assert.assertEquals(0, evenementsIgnores.size());
			Assert.assertEquals(0, evenementsExploses.size());
		}
		{
			final EvenementOrganisation recu = evenementsTraites.get(1);
			Assert.assertNotNull(recu);
			Assert.assertEquals(noEvenement.longValue(), recu.getNoEvenement());
			Assert.assertEquals(dateEvenement, recu.getDateEvenement());
			Assert.assertNull(recu.getCommentaireTraitement());
			Assert.assertNull(recu.getDateTraitement());
			Assert.assertEquals(EtatEvenementOrganisation.A_TRAITER, recu.getEtat());
			Assert.assertEquals(2L, recu.getNoOrganisation());
			Assert.assertEquals(type, recu.getType());

			Assert.assertEquals(0, evenementsIgnores.size());
			Assert.assertEquals(0, evenementsExploses.size());
		}
		{
			final EvenementOrganisation recu = evenementsTraites.get(2);
			Assert.assertNotNull(recu);
			Assert.assertEquals(noEvenement.longValue(), recu.getNoEvenement());
			Assert.assertEquals(dateEvenement, recu.getDateEvenement());
			Assert.assertNull(recu.getCommentaireTraitement());
			Assert.assertNull(recu.getDateTraitement());
			Assert.assertEquals(EtatEvenementOrganisation.A_TRAITER, recu.getEtat());
			Assert.assertEquals(3L, recu.getNoOrganisation());
			Assert.assertEquals(type, recu.getType());

			Assert.assertEquals(0, evenementsIgnores.size());
			Assert.assertEquals(0, evenementsExploses.size());
		}
		{
			final EvenementOrganisation recu = evenementsTraites.get(3);
			Assert.assertNotNull(recu);
			Assert.assertEquals(noEvenement.longValue(), recu.getNoEvenement());
			Assert.assertEquals(dateEvenement, recu.getDateEvenement());
			Assert.assertNull(recu.getCommentaireTraitement());
			Assert.assertNull(recu.getDateTraitement());
			Assert.assertEquals(EtatEvenementOrganisation.A_TRAITER, recu.getEtat());
			Assert.assertEquals(4L, recu.getNoOrganisation());
			Assert.assertEquals(type, recu.getType());

			Assert.assertEquals(0, evenementsIgnores.size());
			Assert.assertEquals(0, evenementsExploses.size());
		}
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenementMultiplesOrganisationsDejaRecu() throws Exception {
		final Long noEvenement = 5640006354L;
		final RegDate dateEvenement = RegDate.get();
		long noOrganisation = 0;
		final TypeEvenementOrganisation type = TypeEvenementOrganisation.FOSC_AUTRE_MUTATION;

		final EvenementOrganisation evt = new EvenementOrganisation(
				noEvenement,
				type,
				dateEvenement,
				noOrganisation,
				EtatEvenementOrganisation.A_TRAITER
		);
		evt.setCommentaireTraitement("turlututu");
		evt.setDateTraitement(DateHelper.getCurrentDate());
		evt.setEtat(EtatEvenementOrganisation.A_VERIFIER);
		evt.setBusinessId("maBizId");

		// Un receptionHandler "pipé" qui donne l'événement comme déjà reçu.
		final EvenementOrganisationReceptionHandler receptionHandler = new EvenementOrganisationReceptionHandler() {

			@Override
			@NotNull
			public List<EvenementOrganisation> saveIncomingEvent(List<EvenementOrganisation> events) {
				// pas de sauvegarde ici...
				return events;
			}

			@Override
			public boolean dejaRecu(String businessId) {
				return true;
			}

			@Override
			public List<EvenementOrganisation> handleEvents(List<EvenementOrganisation> events, EvenementOrganisationProcessingMode mode) throws EvenementOrganisationException {
				evenementsTraites.addAll(events);
				return events;
			}
		};

		esbHandler.setReceptionHandler(receptionHandler);

		Assert.assertEquals(0, evenementsTraites.size());
		sender.sendEventWithMultipleOrga(evt, Arrays.asList(1L, 2L , 3L, 4L), "toto", true);

		// On attend le message
		synchronized (evenementsVusPasser) {
			while (evenementsVusPasser.size() == 0) {
				evenementsVusPasser.wait();
			}
		}
		Assert.assertEquals(1, evenementsVusPasser.size());
		Assert.assertEquals(0, evenementsTraites.size());

	}

	// SIFISC-20423 PROD: Exception à la récéption d'une annonce FOSC de type communication dans la poursuite (Problème de conversion de type)
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenementCommunicationDansPoursuite() throws Exception {
		final Long noEvenement = 5640006354L;
		final RegDate dateEvenement = RegDate.get();
		long noOrganisation = 657133465L;
		final TypeOfNotice type = TypeOfNotice.FOSC_COMMUNICATION_DANS_LA_POURSUITE;

		Assert.assertEquals(0, evenementsTraites.size());
		sender.sendEvent("toto", true, noEvenement, dateEvenement, type, noOrganisation);

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
		Assert.assertEquals((long) noEvenement, recu.getNoEvenement());
		Assert.assertEquals(dateEvenement, recu.getDateEvenement());
		Assert.assertNull(recu.getCommentaireTraitement());
		Assert.assertNull(recu.getDateTraitement());
		Assert.assertEquals(EtatEvenementOrganisation.A_TRAITER, recu.getEtat());
		Assert.assertEquals(noOrganisation, recu.getNoOrganisation());
		Assert.assertEquals(TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_LA_POURSUITE, recu.getType());

		Assert.assertEquals(0, evenementsIgnores.size());
		Assert.assertEquals(0, evenementsExploses.size());
	}
}
