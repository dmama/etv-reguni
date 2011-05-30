package ch.vd.uniregctb.webservices.tiers3.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.webservices.tiers3.BatchTiers;
import ch.vd.uniregctb.webservices.tiers3.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers3.Contribuable;
import ch.vd.uniregctb.webservices.tiers3.Date;
import ch.vd.uniregctb.webservices.tiers3.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers3.ForFiscal;
import ch.vd.uniregctb.webservices.tiers3.GetBatchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.GetDebiteurInfoRequest;
import ch.vd.uniregctb.webservices.tiers3.GetTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.MenageCommun;
import ch.vd.uniregctb.webservices.tiers3.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.TiersWebService;
import ch.vd.uniregctb.webservices.tiers3.UserLogin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class TiersWebServiceCacheTest extends WebserviceTest {

	private Ehcache ehcache;
	private TiersWebServiceCache cache;
	private TiersWebServiceCacheManager wsCacheManager;
	private TiersWebServiceTracing implementation;

	private static class Ids {
		public Long eric;
		public Long debiteur;

		public Long monsieur;
		public Long madame;
		public Long menage;
	}

	private final Ids ids = new Ids();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		CacheManager manager = getBean(CacheManager.class, "ehCacheManager");
		TiersWebService webService = getBean(TiersWebService.class, "tiersService3Impl");
		implementation = new TiersWebServiceTracing(webService);

		cache = new TiersWebServiceCache();
		cache.setCacheManager(manager);
		cache.setTarget(implementation);
		cache.setCacheName("webServiceTiers3");
		ehcache = cache.getEhCache();

		wsCacheManager = getBean(TiersWebServiceCacheManager.class, "tiersService3CacheManager");
		wsCacheManager.setCache(cache);

		serviceCivil.setUp(new DefaultMockServiceCivil());

		// Un tiers avec une adresse et un fors fiscal
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				addAdresseSuisse(eric, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFSEtendu(),
						MotifRattachement.IMMEUBLE_PRIVE);
				ids.eric = eric.getNumero();

				final DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.ANNUEL, date(2000, 1, 1));
				ids.debiteur = debiteur.getId();

				return null;
			}
		});

		// Un ménage commun avec toutes les parties renseignées
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				PersonnePhysique monsieur = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				PersonnePhysique madame = addNonHabitant("Monique", "Bolomey", date(1969, 12, 3), Sexe.FEMININ);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, date(1989, 5, 1), null);
				ch.vd.uniregctb.tiers.MenageCommun mc = ensemble.getMenage();
				mc.setNumeroCompteBancaire("CH9308440717427290198");

				SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setNombreEnfants(0);
				mc.addSituationFamille(situation);

				PeriodeFiscale periode = addPeriodeFiscale(2003);
				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				addDeclarationImpot(mc, periode, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);

				addAdresseSuisse(mc, TypeAdresseTiers.COURRIER, date(1989, 5, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(mc, date(1989, 5, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForSecondaire(mc, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFSEtendu(),
						MotifRattachement.IMMEUBLE_PRIVE);

				ids.monsieur = monsieur.getNumero();
				ids.madame = madame.getNumero();
				ids.menage = mc.getNumero();
				return null;
			}
		});

	}

	@Override
	public void onTearDown() throws Exception {
		wsCacheManager.setCache(getBean(TiersWebServiceCache.class, "tiersService3Cache"));
		super.onTearDown();
	}

	@Test
	public void testGetTiers() throws Exception {

		GetTiersRequest paramsNoPart = new GetTiersRequest();
		paramsNoPart.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		paramsNoPart.setTiersNumber(ids.eric);

		final Set<TiersPart> adressesPart = new HashSet<TiersPart>();
		adressesPart.add(TiersPart.ADRESSES);

		final GetTiersRequest paramsAdressePart = new GetTiersRequest();
		paramsAdressePart.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		paramsAdressePart.setTiersNumber(ids.eric);
		paramsAdressePart.getParts().add(TiersPart.ADRESSES);

		final GetTiersRequest paramsForsPart = new GetTiersRequest();
		paramsForsPart.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		paramsForsPart.setTiersNumber(ids.eric);
		paramsForsPart.getParts().add(TiersPart.FORS_FISCAUX);

		final Set<TiersPart> forsEtAdressesParts = new HashSet<TiersPart>();
		forsEtAdressesParts.add(TiersPart.ADRESSES);
		forsEtAdressesParts.add(TiersPart.FORS_FISCAUX);

		final GetTiersRequest paramsForsEtAdressesParts = new GetTiersRequest();
		paramsForsEtAdressesParts.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		paramsForsEtAdressesParts.setTiersNumber(ids.eric);
		paramsForsEtAdressesParts.getParts().add(TiersPart.ADRESSES);
		paramsForsEtAdressesParts.getParts().add(TiersPart.FORS_FISCAUX);

		// sans parts
		{
			assertNoPart(cache.getTiers(paramsNoPart));

			final GetTiersValue value = getCacheValue(paramsNoPart.getTiersNumber());
			assertNotNull(value);
			assertEmpty(value.getParts());
		}

		// ajout des adresses
		{
			assertAdressePart(cache.getTiers(paramsAdressePart));
			assertNoPart(cache.getTiers(paramsNoPart)); // on vérifie que le tiers sans part fonctionne toujours bien

			final GetTiersValue value = getCacheValue(paramsNoPart.getTiersNumber());
			assertNotNull(value);
			assertEquals(adressesPart, value.getParts());
		}

		// ajout des fors
		{
			assertForsEtAdressePart(cache.getTiers(paramsForsEtAdressesParts));
			assertForsPart(cache.getTiers(paramsForsPart)); // on vérifie que le tiers avec seulement les fors est correct
			assertNoPart(cache.getTiers(paramsNoPart)); // on vérifie que le tiers sans part fonctionne toujours bien
			assertAdressePart(cache.getTiers(paramsAdressePart)); // on vérifie que le tiers avec adresse fonctionne toujours bien

			final GetTiersValue value = getCacheValue(paramsNoPart.getTiersNumber());
			assertNotNull(value);
			assertEquals(forsEtAdressesParts, value.getParts());
		}
	}

	@Test
	public void testGetTiersAllParts() throws Exception {

		// on demande tour-à-tour les parties et on vérifie que 1) on les reçoit bien; et 2) qu'on ne reçoit qu'elles.
		for (TiersPart p : TiersPart.values()) {
			final GetTiersRequest params = new GetTiersRequest();
			params.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
			params.setTiersNumber(ids.menage);
			params.getParts().add(p);
			assertOnlyPart(p, cache.getTiers(params));
		}

		// maintenant que le cache est chaud, on recommence la manipulation pour vérifier que cela fonctionne toujours
		for (TiersPart p : TiersPart.values()) {
			final GetTiersRequest params = new GetTiersRequest();
			params.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
			params.setTiersNumber(ids.menage);
			params.getParts().add(p);
			assertOnlyPart(p, cache.getTiers(params));
		}
	}

	@Test
	public void testEvictTiers() throws Exception {

		// On charge le cache avec des tiers

		GetTiersRequest params = new GetTiersRequest();
		params.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		params.setTiersNumber(ids.eric);

		assertNotNull(cache.getTiers(params));
		assertNotNull(getCacheValue(params.getTiersNumber()));

		GetTiersRequest paramsHisto = new GetTiersRequest();
		paramsHisto.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		paramsHisto.setTiersNumber(ids.eric);

		assertNotNull(cache.getTiers(paramsHisto));
		assertNotNull(getCacheValue(paramsHisto.getTiersNumber()));

		// On evicte les tiers

		cache.evictTiers(ids.eric);

		// On vérifie que le cache est vide

		assertNull(getCacheValue(params.getTiersNumber()));
		assertNull(getCacheValue(paramsHisto.getTiersNumber()));
	}

	/**
	 * [UNIREG-2588] Vérifie que l'éviction d'un tiers se propage automatiquement à tous les tiers liés par rapport-entre-tiers
	 */
	@NotTransactional
	@Test
	public void testEvictTiersMenageCommun() throws Exception {

		// On charge le cache avec le ménage commun
		GetTiersRequest params = new GetTiersRequest();
		params.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		params.setTiersNumber(ids.menage);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final MenageCommun menageAvant = (MenageCommun) cache.getTiers(params);
		assertNotNull(menageAvant);

		// On vérifie l'adresse d'envoi
		assertEquals("Monsieur et Madame", menageAvant.getAdresseCourrierFormattee().getLigne1());
		assertEquals("Eric Bolomey", menageAvant.getAdresseCourrierFormattee().getLigne2());
		assertEquals("Monique Bolomey", menageAvant.getAdresseCourrierFormattee().getLigne3());
		assertEquals("Av de Beaulieu", menageAvant.getAdresseCourrierFormattee().getLigne4());
		assertEquals("1000 Lausanne", menageAvant.getAdresseCourrierFormattee().getLigne5());
		assertNull(menageAvant.getAdresseCourrierFormattee().getLigne6());

		// On modifie le prénom de madame
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final ch.vd.uniregctb.tiers.MenageCommun mc = hibernateTemplate.get(ch.vd.uniregctb.tiers.MenageCommun.class, ids.menage);
				assertNotNull(mc);

				final Set<RapportEntreTiers> rapports = mc.getRapportsObjet();

				PersonnePhysique madame = null;
				for (RapportEntreTiers r : rapports) {
					final AppartenanceMenage am = (AppartenanceMenage) r;
					final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, am.getSujetId());
					assertNotNull(pp);

					if (pp.getPrenom().equals("Monique")) {
						madame = pp;
						break;
					}
				}
				assertNotNull(madame);
				madame.setPrenom("Gudrun");
				return null;
			}
		});

		// Cette modification va provoquer l'éviction de madame du cache, et par transitivité l'éviction du ménage commun. Si ce n'était pas le cas, les données (périmées) du ménage commun seraient encore dans le cache.
		// On vérifie donc que l'adresse d'envoi du ménage commun est bien mise-à-jour.

		final MenageCommun menageApres = (MenageCommun) cache.getTiers(params);
		assertNotNull(menageApres);

		// On vérifie l'adresse d'envoi
		assertEquals("Monsieur et Madame", menageApres.getAdresseCourrierFormattee().getLigne1());
		assertEquals("Eric Bolomey", menageApres.getAdresseCourrierFormattee().getLigne2());
		assertEquals("Gudrun Bolomey", menageApres.getAdresseCourrierFormattee().getLigne3());
		assertEquals("Av de Beaulieu", menageApres.getAdresseCourrierFormattee().getLigne4());
		assertEquals("1000 Lausanne", menageApres.getAdresseCourrierFormattee().getLigne5());
		assertNull(menageApres.getAdresseCourrierFormattee().getLigne6());
	}

	@Test
	public void testEvictDebiteurInfo() throws Exception {

		// On charge le cache avec des tiers

		GetDebiteurInfoRequest params = new GetDebiteurInfoRequest();
		params.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		params.setNumeroDebiteur(ids.debiteur);
		params.setPeriodeFiscale(2010);

		assertNotNull(cache.getDebiteurInfo(params));
		assertNotNull(getCacheValue(params));

		// On evicte les tiers

		cache.evictTiers(ids.debiteur);

		// On vérifie que le cache est vide

		assertNull(getCacheValue(params));
	}

	@Test
	public void testGetTiersInexistant() throws Exception {

		// Essaie une fois sans part
		GetTiersRequest params = new GetTiersRequest();
		params.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		params.setTiersNumber(1233455);

		assertNull(cache.getTiers(params));
		assertNotNull(getCacheValue(params.getTiersNumber())); // not null -> on cache aussi la réponse pour un tiers inexistant !

		// Essai une seconde fois avec parts
		params.getParts().add(TiersPart.ADRESSES);
		assertNull(cache.getTiers(params));
		assertNotNull(getCacheValue(params.getTiersNumber()));
	}

	/**
	 * [UNIREG-2587] Vérifie que le cache fonctionne correctement lorsqu'un tiers est demandé successivement <ol> <li>avec ses fors fiscaux virtuels, puis</li> <li>juste avec ses fors fiscaux, et</li>
	 * <li>finalement de nouveau avec ses fors fiscaux virtuels.</li> </ol>
	 */
	@Test
	public void testGetTiersCasSpecialForFiscauxVirtuels() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		params.setTiersNumber(ids.monsieur);

		// 1. on demande le tiers avec les fors fiscaux virtuels
		{
			params.getParts().add(TiersPart.FORS_FISCAUX_VIRTUELS);

			final Tiers tiers = cache.getTiers(params);
			assertNotNull(tiers);
			assertNotNull(tiers.getForsFiscauxPrincipaux());
			assertEquals(1, tiers.getForsFiscauxPrincipaux().size());

			final ForFiscal ffp = tiers.getForsFiscauxPrincipaux().get(0);
			assertEquals(new Date(1989, 5, 1), ffp.getDateDebut());
			assertNull(ffp.getDateFin());
		}

		// 2. on demande le tiers *sans* les fors fiscaux virtuels
		{
			params.getParts().clear();
			params.getParts().add(TiersPart.FORS_FISCAUX);

			final Tiers tiers = cache.getTiers(params);
			assertNotNull(tiers);
			assertEmpty(tiers.getForsFiscauxPrincipaux());
		}

		// 3. on demande de nouveau le tiers avec les fors fiscaux virtuels => le résultat doit être identique à la demande du point 1.
		{
			params.getParts().clear();
			params.getParts().add(TiersPart.FORS_FISCAUX_VIRTUELS);

			final Tiers tiers = cache.getTiers(params);
			assertNotNull(tiers);
			assertNotNull(tiers.getForsFiscauxPrincipaux());
			assertEquals(1, tiers.getForsFiscauxPrincipaux().size());

			final ForFiscal ffp = tiers.getForsFiscauxPrincipaux().get(0);
			assertEquals(new Date(1989, 5, 1), ffp.getDateDebut());
			assertNull(ffp.getDateFin());
		}
	}

	@Test
	public void testGetBatchTiers() throws Exception {

		GetBatchTiersRequest params = new GetBatchTiersRequest();
		params.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		params.getTiersNumbers().add(ids.monsieur);
		params.getTiersNumbers().add(ids.madame);

		// Etat initial : aucun appel au web-service
		assertEmpty(implementation.getBatchTiersCalls);

		// 1er appel
		{
			final BatchTiers batch = cache.getBatchTiers(params);
			assertNotNull(batch);
			assertEquals(2, batch.getEntries().size());

			// on vérifique que les données retournées sont correctes
			final BatchTiersEntry batch0 = batch.getEntries().get(0);
			final BatchTiersEntry batch1 = batch.getEntries().get(1);
			final Tiers monsieur = (batch0.getNumber() == ids.monsieur ? batch0.getTiers() : batch1.getTiers());
			final Tiers madame = (batch0.getNumber() == ids.madame ? batch0.getTiers() : batch1.getTiers());
			assertNoPart(monsieur);
			assertNoPart(madame);

			// on vérifie qu'il y a bien eu un appel au web-service
			assertEquals(1, implementation.getBatchTiersCalls.size());
			assertEquals(params.getTiersNumbers(), implementation.getBatchTiersCalls.get(0).getTiersNumbers());
		}

		// 2ème appel : identique au premier
		{
			final BatchTiers batch = cache.getBatchTiers(params);
			assertNotNull(batch);
			assertEquals(2, batch.getEntries().size());

			// on vérifique que les données retournées sont correctes
			final BatchTiersEntry batch0 = batch.getEntries().get(0);
			final BatchTiersEntry batch1 = batch.getEntries().get(1);
			final Tiers monsieur = (batch0.getNumber() == ids.monsieur ? batch0.getTiers() : batch1.getTiers());
			final Tiers madame = (batch0.getNumber() == ids.madame ? batch0.getTiers() : batch1.getTiers());
			assertNoPart(monsieur);
			assertNoPart(madame);

			// on vérifie qu'il n'y a pas de second appel au web-service, c'est-à-dire que toutes les données ont été trouvées dans le cache
			assertEquals(1, implementation.getBatchTiersCalls.size());
		}

		// 3ème appel : avec un tiers de plus
		{
			params.getTiersNumbers().add(ids.eric);

			final BatchTiers batch = cache.getBatchTiers(params);
			assertNotNull(batch);
			assertEquals(3, batch.getEntries().size());

			// on vérifique que les données retournées sont correctes
			Tiers monsieur = null;
			Tiers madame = null;
			Tiers eric = null;
			for (BatchTiersEntry entry : batch.getEntries()) {
				if (entry.getNumber() == ids.monsieur) {
					monsieur = entry.getTiers();
				}
				else if (entry.getNumber() == ids.madame) {
					madame = entry.getTiers();
				}
				else if (entry.getNumber() == ids.eric) {
					eric = entry.getTiers();
				}
				else {
					fail("Le batch contient un numéro de tiers inconnu = [" + entry.getNumber() + "]");
				}
			}
			assertNoPart(monsieur);
			assertNoPart(madame);
			assertNoPart(eric);

			// on vérifie qu'il y a un second appel au web-service, mais qu'il ne concerne que le tiers Eric
			assertEquals(2, implementation.getBatchTiersCalls.size());
			assertEquals(params.getTiersNumbers(), implementation.getBatchTiersCalls.get(0).getTiersNumbers());
			assertEquals(Arrays.asList(ids.eric), implementation.getBatchTiersCalls.get(1).getTiersNumbers());
		}
	}

	/**
	 * [UNIREG-3288] Vérifie que les exceptions levées dans la méthode getBatchTiers sont correctement gérées au niveau du cache.
	 */
	@Test
	public void testGetBatchTiersAvecExceptionSurUnTiers() throws Exception {

		GetBatchTiersRequest params = new GetBatchTiersRequest();
		params.setLogin(new UserLogin("[TiersWebServiceCacheTest]", 21));
		params.getTiersNumbers().add(ids.monsieur);
		params.getTiersNumbers().add(ids.madame);

		// on intercale une implémentation du web-service qui lèvera une exception lors de la récupération de madame
		cache.setTarget(new TiersWebServiceCrashing(implementation, ids.madame));

		// 1er appel : monsieur est correctement récupéré et une exception est retournée à la place de madame.
		{
			final BatchTiers batch = cache.getBatchTiers(params);
			assertNotNull(batch);
			assertEquals(2, batch.getEntries().size());

			// on vérifie que : monsieur est bien retourné et qu'une exception a été levée sur madame
			final BatchTiersEntry batch0 = batch.getEntries().get(0);
			final BatchTiersEntry batch1 = batch.getEntries().get(1);

			final BatchTiersEntry entryMonsieur = (batch0.getNumber() == ids.monsieur ? batch0 : batch1);
			assertNotNull(entryMonsieur.getTiers());

			final BatchTiersEntry entryMadame = (batch0.getNumber() == ids.madame ? batch0 : batch1);
			assertNull(entryMadame.getTiers());
			assertEquals("Exception de test", entryMadame.getExceptionInfo().getMessage());

			// on vérifie que : seul monsieur est stocké dans le cache et que madame n'y est pas (parce qu'une exception a été levée)
			assertNotNull(getCacheValue(ids.monsieur));
			assertNull(getCacheValue(ids.madame));
		}

		// 2ème appel : identique au premier pour vérifier que le cache est dans un état cohérent (provoquait un crash avant la correction de UNIREG-3288)
		{
			final BatchTiers batch = cache.getBatchTiers(params);
			assertNotNull(batch);
			assertEquals(2, batch.getEntries().size());

			// on vérifie que : monsieur est bien retourné et qu'une exception a été levée sur madame
			final BatchTiersEntry batch0 = batch.getEntries().get(0);
			final BatchTiersEntry batch1 = batch.getEntries().get(1);

			final BatchTiersEntry entryMonsieur = (batch0.getNumber() == ids.monsieur ? batch0 : batch1);
			assertNotNull(entryMonsieur.getTiers());

			final BatchTiersEntry entryMadame = (batch0.getNumber() == ids.madame ? batch0 : batch1);
			assertNull(entryMadame.getTiers());
			assertEquals("Exception de test", entryMadame.getExceptionInfo().getMessage());

			// on vérifie que : seul monsieur est stocké dans le cache et que madame n'y est pas (parce qu'une exception a été levée)
			assertNotNull(getCacheValue(ids.monsieur));
			assertNull(getCacheValue(ids.madame));
		}
	}

	private GetTiersValue getCacheValue(long tiersNumber) {
		GetTiersValue value = null;
		final GetTiersKey key = new GetTiersKey(tiersNumber);
		final Element element = ehcache.get(key);
		if (element != null) {
			value = (GetTiersValue) element.getObjectValue();
		}
		return value;
	}

	private DebiteurInfo getCacheValue(GetDebiteurInfoRequest params) {
		DebiteurInfo value = null;
		final GetDebiteurInfoKey key = new GetDebiteurInfoKey(params.getNumeroDebiteur(), params.getPeriodeFiscale());
		final Element element = ehcache.get(key);
		if (element != null) {
			value = (DebiteurInfo) element.getObjectValue();
		}
		return value;
	}

	/**
	 * Assert que la partie spécifiée et uniquement celle-ci est renseignée sur le tiers.
	 */
	private static void assertOnlyPart(TiersPart p, Tiers tiers) {

		boolean checkAdresses = TiersPart.ADRESSES == p;
		boolean checkAdressesEnvoi = TiersPart.ADRESSES_ENVOI == p;
		boolean checkAssujettissement = TiersPart.ASSUJETTISSEMENTS == p;
		boolean checkPeriodesAssujettissement = TiersPart.PERIODES_ASSUJETTISSEMENT == p;
		boolean checkComposantsMenage = TiersPart.COMPOSANTS_MENAGE == p;
		boolean checkComptesBancaires = TiersPart.COMPTES_BANCAIRES == p;
		boolean checkDeclarations = TiersPart.DECLARATIONS == p;
		boolean checkForsFiscaux = TiersPart.FORS_FISCAUX == p;
		boolean checkForsFiscauxVirtuels = TiersPart.FORS_FISCAUX_VIRTUELS == p;
		boolean checkForsGestion = TiersPart.FORS_GESTION == p;
		boolean checkPeriodeImposition = TiersPart.PERIODES_IMPOSITION == p;
		boolean checkRapportEntreTiers = TiersPart.RAPPORTS_ENTRE_TIERS == p;
		boolean checkSituationFamille = TiersPart.SITUATIONS_FAMILLE == p;
		boolean checkCapitaux = TiersPart.CAPITAUX == p;
		boolean checkEtatsPM = TiersPart.ETATS_PM == p;
		boolean checkFormesJuridiques = TiersPart.FORMES_JURIDIQUES == p;
		boolean checkRegimesFiscaux = TiersPart.REGIMES_FISCAUX == p;
		boolean checkSieges = TiersPart.SIEGES == p;
		boolean checkPeriodicite = TiersPart.PERIODICITES == p;
		Assert.isTrue(checkAdresses || checkAdressesEnvoi || checkAssujettissement || checkComposantsMenage || checkComptesBancaires
				|| checkDeclarations || checkForsFiscaux || checkForsFiscauxVirtuels || checkForsGestion || checkPeriodeImposition
				|| checkRapportEntreTiers || checkSituationFamille || checkCapitaux || checkEtatsPM || checkFormesJuridiques
				|| checkRegimesFiscaux || checkSieges || checkPeriodicite || checkPeriodesAssujettissement, "La partie [" + p + "] est inconnue");

		assertNullOrNotNull(checkAdresses, tiers.getAdressesCourrier(), "adressesCourrier");
		assertNullOrNotNull(checkAdresses, tiers.getAdressesDomicile(), "adressesDomicile");
		assertNullOrNotNull(checkAdresses, tiers.getAdressesPoursuite(), "adressesPoursuite");
		assertNullOrNotNull(checkAdresses, tiers.getAdressesRepresentation(), "adressesRepresentation");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.getAdresseCourrierFormattee(), "getAdresseCourrierFormattee()");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.getAdresseDomicileFormattee(), "adresseDomicileFormattee");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.getAdresseRepresentationFormattee(), "adresseRepresentationFormattee");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.getAdressePoursuiteFormattee(), "adressePoursuiteFormattee");
		assertNullOrNotNull(checkComptesBancaires, tiers.getComptesBancaires(), "comptesBancaires");
		assertNullOrNotNull(checkForsFiscaux || checkForsFiscauxVirtuels, tiers.getForsFiscauxPrincipaux(), "forsFiscauxPrincipaux");
		assertNullOrNotNull(checkForsFiscaux || checkForsFiscauxVirtuels, tiers.getAutresForsFiscaux(), "autresForsFiscaux");
		assertNullOrNotNull(checkForsGestion, tiers.getForsGestions(), "forsGestions");
		assertNullOrNotNull(checkRapportEntreTiers, tiers.getRapportsEntreTiers(), "rapportsEntreTiers");

		if (tiers instanceof Contribuable) {
			Contribuable ctb = (Contribuable) tiers;
			assertNullOrNotNull(checkAssujettissement, ctb.getAssujettissementsRole(), "assujettissementsRole");
			assertNullOrNotNull(checkPeriodesAssujettissement, ctb.getPeriodesAssujettissementLIC(), "periodesAssujettissementLIC");
			assertNullOrNotNull(checkPeriodesAssujettissement, ctb.getPeriodesAssujettissementLIFD(), "periodesAssujettissementLIFD");
			assertNullOrNotNull(checkDeclarations, ctb.getDeclarations(), "declarations");
			assertNullOrNotNull(checkPeriodeImposition, ctb.getPeriodesImposition(), "periodesImposition");
			assertNullOrNotNull(checkSituationFamille, ctb.getSituationsFamille(), "situationsFamille");
		}

		if (tiers instanceof MenageCommun) {
			MenageCommun mc = (MenageCommun) tiers;
			assertNullOrNotNull(checkComposantsMenage, mc.getContribuablePrincipal(), "contribuablePrincipal");
			assertNullOrNotNull(checkComposantsMenage, mc.getContribuableSecondaire(), "contribuablePrincipal");
		}

		if (tiers instanceof PersonneMorale) {
			PersonneMorale pm = (PersonneMorale) tiers;
			assertNullOrNotNull(checkCapitaux, pm.getCapitaux(), "capital");
			assertNullOrNotNull(checkEtatsPM, pm.getEtats(), "etat");
			assertNullOrNotNull(checkFormesJuridiques, pm.getFormesJuridiques(), "formeJuridique");
			assertNullOrNotNull(checkRegimesFiscaux, pm.getRegimesFiscauxICC(), "regimesFiscauxICC");
			assertNullOrNotNull(checkRegimesFiscaux, pm.getRegimesFiscauxIFD(), "regimesFiscauxIFD");
			assertNullOrNotNull(checkSieges, pm.getSieges(), "siege");
		}
	}

	private static void assertNullOrNotNull(boolean notNull, Object value, String prefix) {
		if (value instanceof Collection) {
			final Collection<?> coll = (Collection<?>) value;
			if (notNull) {
				assertNotNull(prefix + " expected=not null actual=" + coll, coll);
				assertFalse(prefix + " expected=not empty actual=" + coll, coll.isEmpty());
			}
			else {
				assertEmpty(prefix + " expected=empty actual=" + coll, coll);
			}
		}
		else {
			if (notNull) {
				assertNotNull(prefix + " expected=not null actual=" + value, value);
			}
			else {
				assertNull(prefix + " expected=null actual=" + value, value);
			}
		}
	}

	private static void assertForsEtAdressePart(final Tiers tiers) {
		assertNotNull(tiers);
		assertNotEmpty(tiers.getAdressesCourrier());
		assertNotEmpty(tiers.getAdressesDomicile());
		assertNotEmpty(tiers.getAdressesPoursuite());
		assertNotEmpty(tiers.getAdressesRepresentation());
		assertNotEmpty(tiers.getForsFiscauxPrincipaux());
		assertNotEmpty(tiers.getAutresForsFiscaux());
	}

	private static void assertForsPart(final Tiers tiers) {
		assertNotNull(tiers);
		assertEmpty(tiers.getAdressesCourrier());
		assertEmpty(tiers.getAdressesDomicile());
		assertEmpty(tiers.getAdressesPoursuite());
		assertEmpty(tiers.getAdressesRepresentation());
		assertNotEmpty(tiers.getForsFiscauxPrincipaux());
		assertNotEmpty(tiers.getAutresForsFiscaux());
	}

	private static void assertAdressePart(final Tiers tiers) {
		assertNotNull(tiers);
		assertNotNull(tiers.getAdressesCourrier());
		assertNotEmpty(tiers.getAdressesDomicile());
		assertNotEmpty(tiers.getAdressesPoursuite());
		assertNotEmpty(tiers.getAdressesRepresentation());
		assertEmpty(tiers.getForsFiscauxPrincipaux());
		assertEmpty(tiers.getAutresForsFiscaux());
	}

	private static void assertNoPart(final Tiers tiers) {
		assertNotNull(tiers);
		assertEmpty(tiers.getAdressesCourrier());
		assertEmpty(tiers.getAdressesDomicile());
		assertEmpty(tiers.getAdressesPoursuite());
		assertEmpty(tiers.getAdressesRepresentation());
		assertEmpty(tiers.getForsFiscauxPrincipaux());
		assertEmpty(tiers.getAutresForsFiscaux());
	}

	private static void assertNotEmpty(Collection<?> coll) {
		assertNotNull(coll);
		assertFalse(coll.isEmpty());
	}
}
