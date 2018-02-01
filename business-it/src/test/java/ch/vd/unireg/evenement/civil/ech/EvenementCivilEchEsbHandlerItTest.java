package ch.vd.unireg.evenement.civil.ech;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class EvenementCivilEchEsbHandlerItTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementCivilEchSenderImpl sender;
	private EvenementCivilEchEsbHandler esbHandler;
	private List<EvenementCivilEch> evenementsTraites;
	private List<EvenementCivilEch> evenementsIgnores;
	private List<Pair<String, Throwable>> evenementsExploses;
	private List<String> evenementsVusPasser;

	private static final Set<TypeEvenementCivilEch> IGNORED = EnumSet.of(TypeEvenementCivilEch.CHGT_BLOCAGE_ADRESSE,
	                                                                     TypeEvenementCivilEch.CHGT_RELIGION,
	                                                                     TypeEvenementCivilEch.CORR_RELIGION,
	                                                                     TypeEvenementCivilEch.CORR_LIEU_NAISSANCE);

	private static final Set<TypeEvenementCivilEch> NULL_DATE_WITH_REPLACEMENT = EnumSet.of(TypeEvenementCivilEch.ATTRIBUTION_DONNEES_UPI,
	                                                                                        TypeEvenementCivilEch.CORR_DONNEES_UPI,
	                                                                                        TypeEvenementCivilEch.ANNULATION_DONNEES_UPI);

	public void setUp() throws Exception {
		super.setUp();

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtCivilEch");

		clearQueue(INPUT_QUEUE);

		evenementsTraites = new LinkedList<>();
		final EvenementCivilEchReceptionHandler receptionHandler = new EvenementCivilEchReceptionHandler() {
			@Override
			public EvenementCivilEch saveIncomingEvent(EvenementCivilEch event) {
				// pas de sauvegarde ici...
				return event;
			}

			@Override
			public EvenementCivilEch handleEvent(EvenementCivilEch event, EvenementCivilEchProcessingMode mode) throws EvenementCivilException {
				evenementsTraites.add(event);
				return event;
			}
		};

		evenementsVusPasser = new LinkedList<>();
		evenementsIgnores = new LinkedList<>();
		evenementsExploses = new LinkedList<>();
		esbHandler = new EvenementCivilEchEsbHandler() {
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
			protected void onIgnoredEvent(EvenementCivilEch evt) {
				super.onIgnoredEvent(evt);
				evenementsIgnores.add(evt);
			}
		};
		esbHandler.setRecuperateur(null);
		esbHandler.setIgnoredEventTypes(IGNORED);
		esbHandler.setEventTypesWithNullEventDateReplacement(NULL_DATE_WITH_REPLACEMENT);
		esbHandler.setReceptionHandler(receptionHandler);
		esbHandler.afterPropertiesSet();

		initListenerContainer(INPUT_QUEUE, esbHandler);

		sender = new EvenementCivilEchSenderImpl();
		sender.setEsbTemplate(esbTemplate);
		sender.setOutputQueue(INPUT_QUEUE);
		sender.afterPropertiesSet();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenement() throws Exception {
		final Long idEvenement = 34674524122L;
		final RegDate dateEvenement = RegDate.get();
		final Long refMessageId = 12L;
		final ActionEvenementCivilEch action = ActionEvenementCivilEch.PREMIERE_LIVRAISON;
		final TypeEvenementCivilEch type = TypeEvenementCivilEch.CHGT_RELATION_ANNONCE;

		final EvenementCivilEch evt = new EvenementCivilEch();
		evt.setId(idEvenement);
		evt.setAction(action);
		evt.setDateEvenement(dateEvenement);
		evt.setCommentaireTraitement("turlututu");
		evt.setDateTraitement(DateHelper.getCurrentDate());
		evt.setEtat(EtatEvenementCivil.A_VERIFIER);
		evt.setNumeroIndividu(23153612L);
		evt.setRefMessageId(refMessageId);
		evt.setType(type);

		Assert.assertEquals(0, evenementsTraites.size());
		sender.sendEvent(evt, "toto");

		// On attend le message
		synchronized (evenementsVusPasser) {
			while (evenementsVusPasser.size() == 0) {
				evenementsVusPasser.wait();
			}
		}
		Assert.assertEquals(1, evenementsVusPasser.size());
		Assert.assertEquals(1, evenementsTraites.size());

		final EvenementCivilEch recu = evenementsTraites.get(0);
		Assert.assertNotNull(recu);
		Assert.assertEquals(idEvenement, recu.getId());
		Assert.assertEquals(action, recu.getAction());
		Assert.assertEquals(dateEvenement, recu.getDateEvenement());
		Assert.assertNull(recu.getCommentaireTraitement());
		Assert.assertNull(recu.getDateTraitement());
		Assert.assertEquals(EtatEvenementCivil.A_TRAITER, recu.getEtat());
		Assert.assertNull(recu.getNumeroIndividu());
		Assert.assertEquals(refMessageId, recu.getRefMessageId());
		Assert.assertEquals(type, recu.getType());

		Assert.assertEquals(0, evenementsIgnores.size());
		Assert.assertEquals(0, evenementsExploses.size());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testEvenementsIgnores() throws Exception {

		for (TypeEvenementCivilEch type : IGNORED) {

			evenementsVusPasser.clear();
			evenementsTraites.clear();
			evenementsIgnores.clear();
			evenementsExploses.clear();

			final Long idEvenement = 34674524122L + type.ordinal();
			final RegDate dateEvenement = RegDate.get();
			final Long refMessageId = 12L;
			final ActionEvenementCivilEch action = ActionEvenementCivilEch.PREMIERE_LIVRAISON;

			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvenement);
			evt.setAction(action);
			evt.setDateEvenement(dateEvenement);
			evt.setCommentaireTraitement("turlututu");
			evt.setDateTraitement(DateHelper.getCurrentDate());
			evt.setEtat(EtatEvenementCivil.A_VERIFIER);
			evt.setNumeroIndividu(23153612L);
			evt.setRefMessageId(refMessageId);
			evt.setType(type);

			Assert.assertEquals(0, evenementsTraites.size());
			sender.sendEvent(evt, "toto");

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

			final EvenementCivilEch recu = evenementsIgnores.get(0);
			Assert.assertNotNull("type " + type, recu);
			Assert.assertEquals("type " + type, idEvenement, recu.getId());
			Assert.assertEquals("type " + type, action, recu.getAction());
			Assert.assertEquals("type " + type, dateEvenement, recu.getDateEvenement());
			Assert.assertNull("type " + type, recu.getCommentaireTraitement());
			Assert.assertNull("type " + type, recu.getDateTraitement());
			Assert.assertEquals("type " + type, EtatEvenementCivil.A_TRAITER, recu.getEtat());
			Assert.assertNull("type " + type, recu.getNumeroIndividu());
			Assert.assertEquals("type " + type, refMessageId, recu.getRefMessageId());
			Assert.assertEquals("type " + type, type, recu.getType());
		}
	}

	@Test
	public void testEvenementSansDate() throws Exception {
		for (TypeEvenementCivilEch type : TypeEvenementCivilEch.values()) {

			// on ignore ceux-là de toute façon...
			if (IGNORED.contains(type) || type == TypeEvenementCivilEch.TESTING) {
				continue;
			}

			// on construit un événement sans date du type donné
			evenementsVusPasser.clear();
			evenementsTraites.clear();
			evenementsIgnores.clear();
			evenementsExploses.clear();

			final Long idEvenement = 48515544L + type.ordinal();
			final Long refMessageId = 12L;
			final ActionEvenementCivilEch action = ActionEvenementCivilEch.PREMIERE_LIVRAISON;

			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvenement);
			evt.setAction(action);
			evt.setDateEvenement(null);
			evt.setCommentaireTraitement("turlututu");
			evt.setDateTraitement(DateHelper.getCurrentDate());
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(23153612L);
			evt.setRefMessageId(refMessageId);
			evt.setType(type);

			Assert.assertEquals(0, evenementsTraites.size());
			sender.sendEvent(evt, "toto");

			// On attend le message
			synchronized (evenementsVusPasser) {
				while (evenementsVusPasser.size() == 0) {
					evenementsVusPasser.wait();
				}
			}
			Assert.assertEquals("type " + type, 1, evenementsVusPasser.size());
			Assert.assertEquals("type " + type, 0, evenementsIgnores.size());

			if (NULL_DATE_WITH_REPLACEMENT.contains(type)) {
				Assert.assertEquals("type " + type, 1, evenementsTraites.size());
				Assert.assertEquals("type " + type, 0, evenementsExploses.size());

				final EvenementCivilEch traite = evenementsTraites.get(0);
				Assert.assertNotNull(traite);
				Assert.assertEquals(RegDate.get(), traite.getDateEvenement());
			}
			else {
				Assert.assertEquals("type " + type, 0, evenementsTraites.size());
				Assert.assertEquals("type " + type, 1, evenementsExploses.size());

				final Pair<String, Throwable> explosionData = evenementsExploses.get(0);
				Assert.assertNotNull(explosionData);
				Assert.assertNotNull(explosionData.getLeft());
				Assert.assertNotNull(explosionData.getRight());

				//noinspection ThrowableResultOfMethodCallIgnored
				final Throwable t = explosionData.getRight();
				Assert.assertEquals(EvenementCivilEchEsbException.class, t.getClass());
				Assert.assertEquals("L'attribut 'date' est obligatoire pour un événement civil à l'entrée dans Unireg (id="
						                    + idEvenement + ", refId=" + refMessageId + ", type=" + type + ", action=" + action + ", date=null).",
				                    t.getMessage());
			}
		}
	}
}
