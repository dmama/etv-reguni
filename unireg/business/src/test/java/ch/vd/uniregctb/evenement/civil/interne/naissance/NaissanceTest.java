package ch.vd.uniregctb.evenement.civil.interne.naissance;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.cache.ServiceCivilCache;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.EvenementFiscalNaissance;
import ch.vd.uniregctb.evenement.EvenementFiscalSituationFamille;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;


@SuppressWarnings({"JavaDoc"})
public class NaissanceTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = Logger.getLogger(NaissanceTest.class);

	/**
	 * Le numéro d'individu du nouveau né.
	 */
	private static final long NOUVEAU_NE = 983254L;
	private static final long NOUVEAU_NE_MAJEUR = 89123L;
	private static final long NOUVEAU_NE_FIN_ANNEE = 123456L;

	private EvenementFiscalDAO evenementFiscalDAO;
	private CacheManager cacheManager;
	private UniregCacheManager uniregCacheManager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil());
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");

		cacheManager = getBean(CacheManager.class, "ehCacheManager");
		uniregCacheManager = getBean(UniregCacheManager.class, "uniregCacheManager");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandle() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de naissance.");

		{
			List<Tiers> tierss = tiersDAO.getAll();
			assertEquals("le tiers correspondant au nouveau n'a pas été créé", 0, tierss.size());
		}

		Individu bebe = serviceCivil.getIndividu(NOUVEAU_NE, date(2007, 12, 31));
		Naissance naissance = createValidNaissance(bebe, true);

		final MessageCollector collector = buildMessageCollector();
		naissance.validate(collector, collector);
		naissance.handle(collector);

		assertFalse("Une erreur est survenue lors du traitement de la naissance", collector.hasErreurs());

		List<Tiers> tierss = tiersDAO.getAll();
		assertEquals("le tiers correspondant au nouveau n'a pas été créé", 1, tierss.size());
		/*
		 * une événement doit être créé et un événement doit être publié
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(tierss.get(0)).size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleNaissanceFinAnnee() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de naissance en fin d'année.");

		{
			List<Tiers> tierss = tiersDAO.getAll();
			assertEquals("le tiers correspondant au nouveau n'a pas été créé", 0, tierss.size());
		}

		Individu bebe = serviceCivil.getIndividu(NOUVEAU_NE_FIN_ANNEE, date(2007, 12, 31));
		Naissance naissance = createValidNaissance(bebe, true);

		final MessageCollector collector = buildMessageCollector();
		naissance.validate(collector, collector);
		naissance.handle(collector);

		assertFalse("Une erreur est survenue lors du traitement de la naissance", collector.hasErreurs());

		List<Tiers> tierss = tiersDAO.getAll();
		assertEquals("le tiers correspondant au nouveau n'a pas été créé", 1, tierss.size());
		/*
		 * une événement doit être créé et un événement doit être publié
		 */
		assertEquals(1, eventSender.count);
		assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(tierss.get(0)).size());

		// l'événement fiscal ne doit pas avoir eu sa date décalé à l'année suivante!
		final EvenementFiscal evtFiscal = getEvenementFiscalService().getEvenementsFiscaux(tierss.get(0)).iterator().next();
		assertEquals(bebe.getDateNaissance(), evtFiscal.getDateEvenement());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleMajeur() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de naissance.");

		Individu bebe = serviceCivil.getIndividu(NOUVEAU_NE_MAJEUR, date(2007, 12, 31));
		Naissance naissance = createValidNaissance(bebe, true);

		final MessageCollector collector = buildMessageCollector();
		naissance.validate(collector, collector);
		naissance.handle(collector);

		assertTrue("Une erreur aurait du survenir puisque l'individu est majeur", collector.hasErreurs());
	}

	@Test
	public void testNaissanceTiersExistantRegPP() throws Exception {

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addHabitant(NOUVEAU_NE);
				return null;
			}
		});

		final Individu bebe = serviceCivil.getIndividu(NOUVEAU_NE, date(2007, 12, 31));

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Naissance naissance = createValidNaissance(bebe, true);
				final MessageCollector collector = buildMessageCollector();
				try {
					naissance.validate(collector, collector);
					naissance.handle(collector);
					Assert.fail();
				}
				catch (EvenementCivilException e) {
					Assert.assertEquals("Le tiers existe déjà avec cet individu " + NOUVEAU_NE + " alors que c'est une naissance", e.getMessage());
				}
				return null;
			}
		});
	}


	@Test
	public void testNaissanceTiersExistantRCPers() throws Exception {

		final long indPere = 392465236L;
		final long indMere = 5423678234L;
		final long indEnfant = 34678425L;
		final RegDate dateNaissanceEnfant = date(2010, 2, 8);

		// On crée la situation de départ : une mère et un fils mineur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pere = addIndividu(indPere, date(1980, 1, 1), "Cognac", "Raoul", true);
				MockIndividu mere = addIndividu(indMere, date(1980, 1, 1), "Cognac", "Josette", false);
				MockIndividu enfant = addIndividu(indEnfant, dateNaissanceEnfant, "Cognac", "Yvan", true);
				enfant.setParentsFromIndividus(Arrays.<Individu>asList(pere, mere));
			}
		});

		class Ids {
			Long pere;
			Long mere;
			Long enfant;
		}

		// On crée le père et la mère
		final Ids ids = doInNewTransactionAndSession(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pere = addHabitant(indPere);
				final PersonnePhysique mere = addHabitant(indMere);
				final PersonnePhysique enfant = addHabitant(indEnfant);
				final Ids ids = new Ids();
				ids.pere = pere.getId();
				ids.mere = mere.getId();
				ids.enfant = enfant.getId();
				return ids;
			}
		});

		final Individu bebe = serviceCivil.getIndividu(indEnfant, dateNaissanceEnfant);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Naissance naissance = createValidNaissance(bebe, false);
				final MessageCollector collector = buildMessageCollector();
				try {
					naissance.validate(collector, collector);
					naissance.handle(collector);
					Assert.assertFalse(collector.hasErreurs());
					Assert.assertFalse(collector.hasWarnings());
				}
				catch (EvenementCivilException e) {
					Assert.fail(e.getMessage());
				}

				// on vérifie les événements fiscaux (qui doivent quand-même avoir été envoyés)
				// on vérifie que il y a eu :
				// - un événement de changement de situation de famille
				// - un événement de naissane
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertNotNull(events);
				assertEquals(2, events.size());

				final EvenementFiscalSituationFamille event0 = (EvenementFiscalSituationFamille) events.get(0);
				assertNotNull(event0);

				final EvenementFiscalNaissance event1 = (EvenementFiscalNaissance) events.get(1);
				assertNotNull(event1);
				assertEquals(ids.mere, event1.getTiers().getNumero());
				assertEquals(ids.enfant, event1.getEnfant().getNumero());
				assertEquals(dateNaissanceEnfant, event1.getDateEvenement());

				return null;
			}
		});
	}

	private Naissance createValidNaissance(Individu individu, boolean regpp) {
		return new Naissance(individu, individu.getDateNaissance(), 4848, context, regpp);
	}

	/**
	 * [UNIREG-3244] Teste que le traitement d'un événement civil de naissance provoque bien l'envoi d'un événement fiscal de naissance.
	 */
	@Test
	public void testHandlePourEnvoiEvenementFiscalDeNaissance() throws Exception {

		final long indPere = 1;
		final long indMere = 2;
		final long indFils = 3;

		// On crée la situation de départ : une mère et un fils mineur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pere = addIndividu(indPere, date(1980, 1, 1), "Cognac", "Raoul", true);
				MockIndividu mere = addIndividu(indMere, date(1980, 1, 1), "Cognac", "Josette", false);
				MockIndividu fils = addIndividu(indFils, date(2010, 2, 8), "Cognac", "Yvan", true);
				fils.setParentsFromIndividus(Arrays.<Individu>asList(pere, mere));
			}
		});

		class Ids {
			Long pere;
			Long mere;
			Long fils;
		}
		final Ids ids = new Ids();

		// On crée le père et la mère
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				return null;
			}
		});

		// On envoie l'événement de naissance
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu fils = serviceCivil.getIndividu(indFils, date(2010, 12, 31));
				final Naissance naissance = createValidNaissance(fils, true);

				final MessageCollector collector = buildMessageCollector();
				naissance.validate(collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				final HandleStatus code = naissance.handle(collector);
				assertEquals(HandleStatus.TRAITE, code);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				ids.fils = tiersDAO.getNumeroPPByNumeroIndividu(indFils, false);
				return null;
			}
		});


		// On vérifie que il y a eu :
		// - un événement de changement de situation de famille
		// - un événement de naissane
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertNotNull(events);
				assertEquals(2, events.size());

				final EvenementFiscalSituationFamille event0 = (EvenementFiscalSituationFamille) events.get(0);
				assertNotNull(event0);

				final EvenementFiscalNaissance event1 = (EvenementFiscalNaissance) events.get(1);
				assertNotNull(event1);
				assertEquals(ids.mere, event1.getTiers().getNumero());
				assertEquals(ids.fils, event1.getEnfant().getNumero());
				assertEquals(date(2010, 2, 8), event1.getDateEvenement());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-5521] On s'assure que les parents du nouveau-né sont rafraîchis dans le cache du service civil avant le traitement de l'événement.
	 */
	@Test
	public void testHandleNaissanceEtEvictionParentsDuCache() throws Exception {

		final long indPere = 1;
		final long indMere = 2;
		final long indFils = 3;

		final RegDate dateNaissance = date(2010, 2, 8);

		// On crée la situation de départ : le service civil est vide
		final MockServiceCivil realService = new MockServiceCivil() {

			@Override
			protected void init() {
			}

			@Override
			public void step1() {
				// on crée toute la famille d'un coup
				MockIndividu pere = addIndividu(indPere, date(1980, 1, 1), "Tord-boyaux", "Raoul", true);
				MockIndividu mere = addIndividu(indMere, date(1980, 1, 1), "Tord-boyaux", "Josette", false);
				marieIndividus(pere, mere, date(2005, 1, 1));

				MockIndividu fils = addIndividu(indFils, dateNaissance, "Tord-boyaux", "Yvan", true);
				fils.setParentsFromIndividus(Arrays.<Individu>asList(pere, mere));
				pere.setEnfantsFromIndividus(Arrays.<Individu>asList(fils));
				mere.setEnfantsFromIndividus(Arrays.<Individu>asList(fils));
			}
		};

		// On setup le service civil avec un cache
		final ServiceCivilCache cache = new ServiceCivilCache();
		cache.setTarget(realService);
		cache.setCacheManager(cacheManager);
		cache.setCacheName("serviceCivil");
		cache.setUniregCacheManager(uniregCacheManager);
		cache.setDataEventService(dataEventService);
		cache.afterPropertiesSet();
		cache.reset();
		serviceCivil.setUp(cache);

		// On s'assure que ni la mère ni le père n'existent à ce stade (et que leurs inexistance est bien enregistrée dans le cache)
		assertNull(serviceCivil.getIndividu(indMere, dateNaissance, AttributeIndividu.ENFANTS));
		assertNull(serviceCivil.getIndividu(indPere, dateNaissance, AttributeIndividu.ENFANTS));

		// On créer l'arrivée des parents et la naissance de l'enfant (cas bizarre, mais on a vu des choses semblables en production)
		realService.step1();

		class Ids {
			Long pere;
			Long mere;
			Long fils;
		}
		final Ids ids = new Ids();

		// On crée le père et la mère
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique mere = addHabitant(indMere);
				ids.mere = mere.getId();
				return null;
			}
		});

		// On envoie l'événement de naissance
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu fils = serviceCivil.getIndividu(indFils, date(2010, 12, 31));
				final Naissance naissance = createValidNaissance(fils, true);

				final MessageCollector collector = buildMessageCollector();
				naissance.validate(collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				final HandleStatus code = naissance.handle(collector);
				assertEquals(HandleStatus.TRAITE, code);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				ids.fils = tiersDAO.getNumeroPPByNumeroIndividu(indFils, false);
				return null;
			}
		});


		// On vérifie que la mère et le père sont trouvés dans le cache et qu'ils possèdent bien un enfant
		assertParent(indPere, indFils, dateNaissance);
		assertParent(indMere, indFils, dateNaissance);
	}

	private void assertParent(long indParent, long indFils, RegDate dateNaissance) {
		final Individu parent = serviceCivil.getIndividu(indParent, null, AttributeIndividu.ENFANTS);
		assertNotNull(parent);
		final Collection<RelationVersIndividu> enfants = parent.getEnfants();
		assertNotNull(enfants);
		assertEquals(1, enfants.size());
		final RelationVersIndividu enfant0 = enfants.iterator().next();
		assertNotNull(enfant0);
		assertEquals(dateNaissance, enfant0.getDateDebut());
		assertEquals(indFils, enfant0.getNumeroAutreIndividu());
	}

}
