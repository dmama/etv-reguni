package ch.vd.uniregctb.webservices.tiers2.cache;

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
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.Contribuable;
import ch.vd.uniregctb.webservices.tiers2.data.ContribuableHisto;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommun;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommunHisto;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMoraleHisto;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers;
import ch.vd.uniregctb.webservices.tiers2.data.TiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersHisto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class TiersWebServiceCacheTest extends WebserviceTest {

	private Ehcache cache;
	private TiersWebServiceCache service;
	private TiersWebServiceCacheManager wsCacheManager;

	private Long ericId;
	private Long menageId;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		CacheManager manager = getBean(CacheManager.class, "ehCacheManager");
		TiersWebService target = getBean(TiersWebService.class, "tiersService2Bean");

		service = new TiersWebServiceCache();
		service.setCacheManager(manager);
		service.setTarget(target);
		service.setCacheName("webServiceTiers2");
		cache = service.getEhCache();

		wsCacheManager = getBean(TiersWebServiceCacheManager.class, "tiersService2CacheManager");
		wsCacheManager.setCache(service);

		// Un tiers avec une adresse et un fors fiscal
		ericId = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				addAdresseSuisse(eric, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFSEtendu(),
						MotifRattachement.IMMEUBLE_PRIVE);
				return eric.getNumero();
			}
		});

		// Un ménage commun avec toutes les parties renseignées
		menageId = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				PersonnePhysique monique = addNonHabitant("Monique", "Bolomey", date(1969, 12, 3), Sexe.FEMININ);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(eric, monique, date(1989, 5, 1), null);
				ch.vd.uniregctb.tiers.MenageCommun mc = ensemble.getMenage();
				mc.setNumeroCompteBancaire("CH9308440717427290198");

				SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setDateFin(null);
				situation.setNombreEnfants(0);
				mc.addSituationFamille(situation);

				PeriodeFiscale periode = addPeriodeFiscale(2003);
				ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				addDeclarationImpot(mc, periode, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);

				addAdresseSuisse(mc, TypeAdresseTiers.COURRIER, date(1989, 5, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(mc, date(1989, 5, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForSecondaire(mc, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFSEtendu(),
						MotifRattachement.IMMEUBLE_PRIVE);
				return mc.getNumero();
			}
		});

	}

	@Override
	public void onTearDown() throws Exception {
		wsCacheManager.setCache(getBean(TiersWebServiceCache.class, "tiersService2Cache"));
		super.onTearDown();
	}

	@Test
	public void testGetTiers() throws Exception {

		GetTiers paramsNoPart = new GetTiers();
		paramsNoPart.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
		paramsNoPart.date = new Date(2008, 12, 31);
		paramsNoPart.tiersNumber = ericId;
		paramsNoPart.parts = null;

		final Set<TiersPart> adressesPart = new HashSet<TiersPart>();
		adressesPart.add(TiersPart.ADRESSES);
		final GetTiers paramsAdressePart = paramsNoPart.clone(adressesPart);

		final Set<TiersPart> forsPart = new HashSet<TiersPart>();
		forsPart.add(TiersPart.FORS_FISCAUX);
		final GetTiers paramsForsPart = paramsNoPart.clone(forsPart);

		final Set<TiersPart> forsEtAdressesParts = new HashSet<TiersPart>();
		forsEtAdressesParts.add(TiersPart.ADRESSES);
		forsEtAdressesParts.add(TiersPart.FORS_FISCAUX);
		final GetTiers paramsForsEtAdressesParts = paramsNoPart.clone(forsEtAdressesParts);

		// sans parts
		{
			assertNoPart(service.getTiers(paramsNoPart));

			final GetTiersValue value = getCacheValue(paramsNoPart);
			assertNotNull(value);
			assertNull(value.getParts());
		}

		// ajout des adresses
		{
			assertAdressePart(service.getTiers(paramsAdressePart));
			assertNoPart(service.getTiers(paramsNoPart)); // on vérifie que le tiers sans part fonctionne toujours bien

			final GetTiersValue value = getCacheValue(paramsNoPart);
			assertNotNull(value);
			assertEquals(adressesPart, value.getParts());
		}

		// ajout des fors
		{
			assertForsEtAdressePart(service.getTiers(paramsForsEtAdressesParts));
			assertForsPart(service.getTiers(paramsForsPart)); // on vérifie que le tiers avec seulement les fors est correct
			assertNoPart(service.getTiers(paramsNoPart)); // on vérifie que le tiers sans part fonctionne toujours bien
			assertAdressePart(service.getTiers(paramsAdressePart)); // on vérifie que le tiers avec adresse fonctionne toujours bien

			final GetTiersValue value = getCacheValue(paramsNoPart);
			assertNotNull(value);
			assertEquals(forsEtAdressesParts, value.getParts());
		}
	}

	@Test
	public void testGetTiersAllParts() throws Exception {

		GetTiers paramsNoPart = new GetTiers();
		paramsNoPart.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
		paramsNoPart.date = new Date(2003, 7, 1);
		paramsNoPart.tiersNumber = menageId;
		paramsNoPart.parts = null;

		// on demande tour-à-tour les parties et on vérifie que 1) on les reçoit bien; et 2) qu'on ne reçoit qu'elles.
		for (TiersPart p : TiersPart.values()) {
			Set<TiersPart> parts = new HashSet<TiersPart>();
			parts.add(p);
			final GetTiers params = paramsNoPart.clone(parts);
			assertOnlyPart(p, service.getTiers(params));
		}

		// maintenant que le cache est chaud, on recommence la manipulation pour vérifier que cela fonctionne toujours
		for (TiersPart p : TiersPart.values()) {
			Set<TiersPart> parts = new HashSet<TiersPart>();
			parts.add(p);
			final GetTiers params = paramsNoPart.clone(parts);
			assertOnlyPart(p, service.getTiers(params));
		}
	}

	@Test
	public void testGetTiersHisto() throws Exception {

		GetTiersHisto paramsNoPart = new GetTiersHisto();
		paramsNoPart.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
		paramsNoPart.tiersNumber = ericId;
		paramsNoPart.parts = null;

		final Set<TiersPart> adressesPart = new HashSet<TiersPart>();
		adressesPart.add(TiersPart.ADRESSES);
		final GetTiersHisto paramsAdressePart = paramsNoPart.clone(adressesPart);

		final Set<TiersPart> forsPart = new HashSet<TiersPart>();
		forsPart.add(TiersPart.FORS_FISCAUX);
		final GetTiersHisto paramsForsPart = paramsNoPart.clone(forsPart);

		final Set<TiersPart> forsEtAdressesParts = new HashSet<TiersPart>();
		forsEtAdressesParts.add(TiersPart.ADRESSES);
		forsEtAdressesParts.add(TiersPart.FORS_FISCAUX);
		final GetTiersHisto paramsForsEtAdressesParts = paramsNoPart.clone(forsEtAdressesParts);

		// sans parts
		{
			assertNoPart(service.getTiersHisto(paramsNoPart));

			final GetTiersHistoValue value = getCacheValue(paramsNoPart);
			assertNotNull(value);
			assertNull(value.getParts());
		}

		// ajout des adresses
		{
			assertAdressePart(service.getTiersHisto(paramsAdressePart));
			assertNoPart(service.getTiersHisto(paramsNoPart)); // on vérifie que le tiers sans part fonctionne toujours bien

			final GetTiersHistoValue value = getCacheValue(paramsNoPart);
			assertNotNull(value);
			assertEquals(adressesPart, value.getParts());
		}

		// ajout des fors
		{
			assertForsEtAdressePart(service.getTiersHisto(paramsForsEtAdressesParts));
			assertForsPart(service.getTiersHisto(paramsForsPart)); // on vérifie que le tiers avec seulement les fors est correct
			assertNoPart(service.getTiersHisto(paramsNoPart)); // on vérifie que le tiers sans part fonctionne toujours bien
			assertAdressePart(service.getTiersHisto(paramsAdressePart)); // on vérifie que le tiers avec adresse fonctionne toujours bien

			final GetTiersHistoValue value = getCacheValue(paramsNoPart);
			assertNotNull(value);
			assertEquals(forsEtAdressesParts, value.getParts());
		}
	}

	@Test
	public void testGetTiersHistoAllParts() throws Exception {

		GetTiersHisto paramsNoPart = new GetTiersHisto();
		paramsNoPart.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
		paramsNoPart.tiersNumber = menageId;
		paramsNoPart.parts = null;

		// on demande tour-à-tour les parties et on vérifie que 1) on les reçoit bien; et 2) qu'on ne reçoit qu'elles.
		for (TiersPart p : TiersPart.values()) {
			Set<TiersPart> parts = new HashSet<TiersPart>();
			parts.add(p);
			final GetTiersHisto params = paramsNoPart.clone(parts);
			assertOnlyPart(p, service.getTiersHisto(params));
		}

		// maintenant que le cache est chaud, on recommence la manipulation pour vérifier que cela fonctionne toujours
		for (TiersPart p : TiersPart.values()) {
			Set<TiersPart> parts = new HashSet<TiersPart>();
			parts.add(p);
			final GetTiersHisto params = paramsNoPart.clone(parts);
			assertOnlyPart(p, service.getTiersHisto(params));
		}
	}

	@Test
	public void testEvictTiers() throws Exception {

		// On charge le cache avec des tiers

		GetTiers params = new GetTiers();
		params.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
		params.tiersNumber = ericId;
		params.date = new Date(2008, 1, 1);
		params.parts = null;

		assertNotNull(service.getTiers(params));
		assertNotNull(getCacheValue(params));

		GetTiersHisto paramsHisto = new GetTiersHisto();
		paramsHisto.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
		paramsHisto.tiersNumber = ericId;
		paramsHisto.parts = null;

		assertNotNull(service.getTiersHisto(paramsHisto));
		assertNotNull(getCacheValue(paramsHisto));

		// On evicte les tiers

		service.evictTiers(ericId);

		// On vérifie que le cache est vide

		assertNull(getCacheValue(params));
		assertNull(getCacheValue(paramsHisto));
	}

	/**
	 * [UNIREG-2588] Vérifie que l'éviction d'un tiers se propage automatiquement à tous les tiers liés par rapport-entre-tiers
	 */
	@NotTransactional
	@Test
	public void testEvictTiersMenageCommun() throws Exception {

		// On charge le cache avec le ménage commun
		GetTiers params = new GetTiers();
		params.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
		params.tiersNumber = menageId;
		params.date = new Date(2008, 1, 1);
		params.parts = new HashSet<TiersPart>();
		params.parts.add(TiersPart.ADRESSES_ENVOI);

		final MenageCommun menageAvant = (MenageCommun) service.getTiers(params);
		assertNotNull(menageAvant);

		// On vérifie l'adresse d'envoi
		assertEquals("Monsieur et Madame", menageAvant.adresseEnvoi.ligne1);
		assertEquals("Eric Bolomey", menageAvant.adresseEnvoi.ligne2);
		assertEquals("Monique Bolomey", menageAvant.adresseEnvoi.ligne3);
		assertEquals("Av de Beaulieu", menageAvant.adresseEnvoi.ligne4);
		assertEquals("1000 Lausanne", menageAvant.adresseEnvoi.ligne5);
		assertNull(menageAvant.adresseEnvoi.ligne6);

		// On modifie le prénom de madame
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final ch.vd.uniregctb.tiers.MenageCommun mc = (ch.vd.uniregctb.tiers.MenageCommun) hibernateTemplate.get(ch.vd.uniregctb.tiers.MenageCommun.class, menageId);
				assertNotNull(mc);

				final Set<RapportEntreTiers> rapports = mc.getRapportsObjet();

				PersonnePhysique madame = null;
				for (RapportEntreTiers r :rapports) {
					final AppartenanceMenage am = (AppartenanceMenage) r;
					final PersonnePhysique pp = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, am.getSujetId());
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

		final MenageCommun menageApres = (MenageCommun) service.getTiers(params);
		assertNotNull(menageApres);

		// On vérifie l'adresse d'envoi
		assertEquals("Monsieur et Madame", menageApres.adresseEnvoi.ligne1);
		assertEquals("Eric Bolomey", menageApres.adresseEnvoi.ligne2);
		assertEquals("Gudrun Bolomey", menageApres.adresseEnvoi.ligne3);
		assertEquals("Av de Beaulieu", menageApres.adresseEnvoi.ligne4);
		assertEquals("1000 Lausanne", menageApres.adresseEnvoi.ligne5);
		assertNull(menageApres.adresseEnvoi.ligne6);
	}

	@Test
	public void testGetTiersInexistant() throws Exception {

		final Set<TiersPart> adressesPart = new HashSet<TiersPart>();
		adressesPart.add(TiersPart.ADRESSES);

		// Essaie une fois sans part
		GetTiers params = new GetTiers();
		params.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
		params.tiersNumber = 1233455;
		params.date = new Date(2008, 1, 1);
		params.parts = null;

		assertNull(service.getTiers(params));
		assertNotNull(getCacheValue(params)); // not null -> on cache aussi la réponse pour un tiers inexistant !

		// Essai une seconde fois avec parts
		params.parts = adressesPart;
		assertNull(service.getTiers(params));
		assertNotNull(getCacheValue(params));
	}

	@Test
	public void testGetTiersHistoInexistant() throws Exception {

		final Set<TiersPart> adressesPart = new HashSet<TiersPart>();
		adressesPart.add(TiersPart.ADRESSES);

		// Essaie une fois sans part
		GetTiersHisto params = new GetTiersHisto();
		params.login = new UserLogin("[TiersWebServiceCacheTest]", 21);
		params.tiersNumber = 1233455;
		params.parts = null;

		assertNull(service.getTiersHisto(params));
		assertNotNull(getCacheValue(params)); // not null -> on cache aussi la réponse pour un tiers inexistant !

		// Essai une seconde fois avec parts
		params.parts = adressesPart;
		assertNull(service.getTiersHisto(params));
		assertNotNull(getCacheValue(params));
	}

	private GetTiersValue getCacheValue(final GetTiers paramsNoPart) {
		GetTiersValue value = null;
		final GetTiersKey key = new GetTiersKey(paramsNoPart.tiersNumber, paramsNoPart.date);
		final Element element = cache.get(key);
		if (element != null) {
			value = (GetTiersValue) element.getObjectValue();
		}
		return value;
	}

	private GetTiersHistoValue getCacheValue(final GetTiersHisto paramsNoPart) {
		GetTiersHistoValue value = null;
		final GetTiersHistoKey key = new GetTiersHistoKey(paramsNoPart.tiersNumber);
		final Element element = cache.get(key);
		if (element != null) {
			value = (GetTiersHistoValue) element.getObjectValue();
		}
		return value;
	}

	/**
	 * Assert que la partie spécifiée et uniquement celle-ci est renseignée sur le tiers.
	 */
	private static void assertOnlyPart(TiersPart p, Tiers tiers) {

		boolean checkAdresses = TiersPart.ADRESSES.equals(p);
		boolean checkAdressesEnvoi = TiersPart.ADRESSES_ENVOI.equals(p);
		boolean checkAssujettissement = TiersPart.ASSUJETTISSEMENTS.equals(p);
		boolean checkComposantsMenage = TiersPart.COMPOSANTS_MENAGE.equals(p);
		boolean checkComptesBancaires = TiersPart.COMPTES_BANCAIRES.equals(p);
		boolean checkDeclarations = TiersPart.DECLARATIONS.equals(p);
		boolean checkForsFiscaux = TiersPart.FORS_FISCAUX.equals(p);
		boolean checkForsFiscauxVirtuels = TiersPart.FORS_FISCAUX_VIRTUELS.equals(p);
		boolean checkForsGestion = TiersPart.FORS_GESTION.equals(p);
		boolean checkPeriodeImposition = TiersPart.PERIODE_IMPOSITION.equals(p);
		boolean checkRapportEntreTiers = TiersPart.RAPPORTS_ENTRE_TIERS.equals(p);
		boolean checkSituationFamille = TiersPart.SITUATIONS_FAMILLE.equals(p);
		boolean checkCapital = TiersPart.CAPITAUX.equals(p);
		boolean checkEtatPM = TiersPart.ETATS_PM.equals(p);
		boolean checkFormeJuridique = TiersPart.FORMES_JURIDIQUES.equals(p);
		boolean checkRegimesFiscaux = TiersPart.REGIMES_FISCAUX.equals(p);
		boolean checkSiege = TiersPart.SIEGES.equals(p);

		Assert.isTrue(checkAdresses || checkAdressesEnvoi || checkAssujettissement || checkComposantsMenage || checkComptesBancaires
				|| checkDeclarations || checkForsFiscaux || checkForsFiscauxVirtuels || checkForsGestion || checkPeriodeImposition
				|| checkRapportEntreTiers || checkSituationFamille || checkCapital || checkEtatPM || checkFormeJuridique
				|| checkRegimesFiscaux || checkSiege, "La partie [" + p + "] est inconnue");

		assertNullOrNotNull(checkAdresses, tiers.adresseCourrier, "adresseCourrier");
		assertNullOrNotNull(checkAdresses, tiers.adresseDomicile, "adresseDomicile");
		assertNullOrNotNull(checkAdresses, tiers.adressePoursuite, "adressePoursuite");
		assertNullOrNotNull(checkAdresses, tiers.adresseRepresentation, "adresseRepresentation");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.adresseEnvoi, "adresseEnvoi");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.adresseDomicileFormattee, "adresseDomicileFormattee");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.adresseRepresentationFormattee, "adresseRepresentationFormattee");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.adressePoursuiteFormattee, "adressePoursuiteFormattee");
		assertNullOrNotNull(checkComptesBancaires, tiers.comptesBancaires, "comptesBancaires");
		assertNullOrNotNull(checkForsFiscaux || checkForsFiscauxVirtuels, tiers.forFiscalPrincipal, "forFiscalPrincipal");
		assertNullOrNotNull(checkForsFiscaux || checkForsFiscauxVirtuels, tiers.autresForsFiscaux, "autresForsFiscaux");
		assertNullOrNotNull(checkForsGestion, tiers.forGestion, "forGestion");
		assertNullOrNotNull(checkRapportEntreTiers, tiers.rapportsEntreTiers, "rapportsEntreTiers");

		if (tiers instanceof Contribuable) {
			Contribuable ctb = (Contribuable) tiers;
			assertNullOrNotNull(checkAssujettissement, ctb.assujettissementLIC, "assujettissementLIC");
			assertNullOrNotNull(checkAssujettissement, ctb.assujettissementLIFD, "assujettissementLIFD");
			assertNullOrNotNull(checkDeclarations, ctb.declaration, "declaration");
			assertNullOrNotNull(checkPeriodeImposition, ctb.periodeImposition, "periodeImposition");
			assertNullOrNotNull(checkSituationFamille, ctb.situationFamille, "situationFamille");
		}

		if (tiers instanceof MenageCommun) {
			MenageCommun mc = (MenageCommun) tiers;
			assertNullOrNotNull(checkComposantsMenage, mc.contribuablePrincipal, "contribuablePrincipal");
			assertNullOrNotNull(checkComposantsMenage, mc.contribuableSecondaire, "contribuableSecondaire");
		}

		if (tiers instanceof PersonneMorale) {
			PersonneMorale pm = (PersonneMorale) tiers;
			assertNullOrNotNull(checkCapital, pm.capital, "capital");
			assertNullOrNotNull(checkEtatPM, pm.etat, "etat");
			assertNullOrNotNull(checkFormeJuridique, pm.formeJuridique, "formeJuridique");
			assertNullOrNotNull(checkRegimesFiscaux, pm.regimeFiscalICC, "regimeFiscalICC");
			assertNullOrNotNull(checkRegimesFiscaux, pm.regimeFiscalIFD, "regimeFiscalIFD");
			assertNullOrNotNull(checkSiege, pm.siege, "siege");
		}
	}

	/**
	 * Assert que la partie spécifiée et uniquement celle-ci est renseignée sur le tiers.
	 */
	private static void assertOnlyPart(TiersPart p, TiersHisto tiers) {

		boolean checkAdresses = TiersPart.ADRESSES.equals(p);
		boolean checkAdressesEnvoi = TiersPart.ADRESSES_ENVOI.equals(p);
		boolean checkAssujettissement = TiersPart.ASSUJETTISSEMENTS.equals(p);
		boolean checkComposantsMenage = TiersPart.COMPOSANTS_MENAGE.equals(p);
		boolean checkComptesBancaires = TiersPart.COMPTES_BANCAIRES.equals(p);
		boolean checkDeclarations = TiersPart.DECLARATIONS.equals(p);
		boolean checkForsFiscaux = TiersPart.FORS_FISCAUX.equals(p);
		boolean checkForsFiscauxVirtuels = TiersPart.FORS_FISCAUX_VIRTUELS.equals(p);
		boolean checkForsGestion = TiersPart.FORS_GESTION.equals(p);
		boolean checkPeriodeImposition = TiersPart.PERIODE_IMPOSITION.equals(p);
		boolean checkRapportEntreTiers = TiersPart.RAPPORTS_ENTRE_TIERS.equals(p);
		boolean checkSituationFamille = TiersPart.SITUATIONS_FAMILLE.equals(p);
		boolean checkCapitaux = TiersPart.CAPITAUX.equals(p);
		boolean checkEtatsPM = TiersPart.ETATS_PM.equals(p);
		boolean checkFormesJuridiques = TiersPart.FORMES_JURIDIQUES.equals(p);
		boolean checkRegimesFiscaux = TiersPart.REGIMES_FISCAUX.equals(p);
		boolean checkSieges = TiersPart.SIEGES.equals(p);

		Assert.isTrue(checkAdresses || checkAdressesEnvoi || checkAssujettissement || checkComposantsMenage || checkComptesBancaires
				|| checkDeclarations || checkForsFiscaux || checkForsFiscauxVirtuels || checkForsGestion || checkPeriodeImposition
				|| checkRapportEntreTiers || checkSituationFamille || checkCapitaux || checkEtatsPM || checkFormesJuridiques
				|| checkRegimesFiscaux || checkSieges, "La partie [" + p + "] est inconnue");

		assertNullOrNotNull(checkAdresses, tiers.adressesCourrier, "adressesCourrier");
		assertNullOrNotNull(checkAdresses, tiers.adressesDomicile, "adressesDomicile");
		assertNullOrNotNull(checkAdresses, tiers.adressesPoursuite, "adressesPoursuite");
		assertNullOrNotNull(checkAdresses, tiers.adressesRepresentation, "adressesRepresentation");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.adresseEnvoi, "adresseEnvoi");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.adresseDomicileFormattee, "adresseDomicileFormattee");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.adresseRepresentationFormattee, "adresseRepresentationFormattee");
		assertNullOrNotNull(checkAdressesEnvoi, tiers.adressePoursuiteFormattee, "adressePoursuiteFormattee");
		assertNullOrNotNull(checkComptesBancaires, tiers.comptesBancaires, "comptesBancaires");
		assertNullOrNotNull(checkForsFiscaux || checkForsFiscauxVirtuels, tiers.forsFiscauxPrincipaux, "forsFiscauxPrincipaux");
		assertNullOrNotNull(checkForsFiscaux || checkForsFiscauxVirtuels, tiers.autresForsFiscaux, "autresForsFiscaux");
		assertNullOrNotNull(checkForsGestion, tiers.forsGestions, "forsGestions");
		assertNullOrNotNull(checkRapportEntreTiers, tiers.rapportsEntreTiers, "rapportsEntreTiers");

		if (tiers instanceof ContribuableHisto) {
			ContribuableHisto ctb = (ContribuableHisto) tiers;
			assertNullOrNotNull(checkAssujettissement, ctb.assujettissementsLIC, "assujettissementsLIC");
			assertNullOrNotNull(checkAssujettissement, ctb.assujettissementsLIFD, "assujettissementsLIFD");
			assertNullOrNotNull(checkDeclarations, ctb.declarations, "declarations");
			assertNullOrNotNull(checkPeriodeImposition, ctb.periodesImposition, "periodesImposition");
			assertNullOrNotNull(checkSituationFamille, ctb.situationsFamille, "situationsFamille");
		}

		if (tiers instanceof MenageCommunHisto) {
			MenageCommunHisto mc = (MenageCommunHisto) tiers;
			assertNullOrNotNull(checkComposantsMenage, mc.contribuablePrincipal, "contribuablePrincipal");
			assertNullOrNotNull(checkComposantsMenage, mc.contribuableSecondaire, "contribuablePrincipal");
		}

		if (tiers instanceof PersonneMoraleHisto) {
			PersonneMoraleHisto pm = (PersonneMoraleHisto) tiers;
			assertNullOrNotNull(checkCapitaux, pm.capitaux, "capital");
			assertNullOrNotNull(checkEtatsPM, pm.etats, "etat");
			assertNullOrNotNull(checkFormesJuridiques, pm.formesJuridiques, "formeJuridique");
			assertNullOrNotNull(checkRegimesFiscaux, pm.regimesFiscauxICC, "regimesFiscauxICC");
			assertNullOrNotNull(checkRegimesFiscaux, pm.regimesFiscauxIFD, "regimesFiscauxIFD");
			assertNullOrNotNull(checkSieges, pm.sieges, "siege");
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
		assertNotNull(tiers.adresseCourrier);
		assertNotNull(tiers.adresseDomicile);
		assertNotNull(tiers.adressePoursuite);
		assertNotNull(tiers.adresseRepresentation);
		assertNotNull(tiers.forFiscalPrincipal);
		assertNotNull(tiers.autresForsFiscaux);
	}

	private static void assertForsPart(final Tiers tiers) {
		assertNotNull(tiers);
		assertNull(tiers.adresseCourrier);
		assertNull(tiers.adresseDomicile);
		assertNull(tiers.adressePoursuite);
		assertNull(tiers.adresseRepresentation);
		assertNotNull(tiers.forFiscalPrincipal);
		assertNotNull(tiers.autresForsFiscaux);
	}

	private static void assertAdressePart(final Tiers tiers) {
		assertNotNull(tiers);
		assertNotNull(tiers.adresseCourrier);
		assertNotNull(tiers.adresseDomicile);
		assertNotNull(tiers.adressePoursuite);
		assertNotNull(tiers.adresseRepresentation);
		assertNull(tiers.forFiscalPrincipal);
		assertNull(tiers.autresForsFiscaux);
	}

	private static void assertNoPart(final Tiers tiers) {
		assertNotNull(tiers);
		assertNull(tiers.adresseCourrier);
		assertNull(tiers.adresseDomicile);
		assertNull(tiers.adressePoursuite);
		assertNull(tiers.adresseRepresentation);
		assertNull(tiers.forFiscalPrincipal);
		assertNull(tiers.autresForsFiscaux);
	}

	private static void assertForsEtAdressePart(final TiersHisto tiers) {
		assertNotNull(tiers);
		assertNotNull(tiers.adressesCourrier);
		assertNotNull(tiers.adressesDomicile);
		assertNotNull(tiers.adressesPoursuite);
		assertNotNull(tiers.adressesRepresentation);
		assertNotNull(tiers.forsFiscauxPrincipaux);
		assertNotNull(tiers.autresForsFiscaux);
	}

	private static void assertForsPart(final TiersHisto tiers) {
		assertNotNull(tiers);
		assertNull(tiers.adressesCourrier);
		assertNull(tiers.adressesDomicile);
		assertNull(tiers.adressesPoursuite);
		assertNull(tiers.adressesRepresentation);
		assertNotNull(tiers.forsFiscauxPrincipaux);
		assertNotNull(tiers.autresForsFiscaux);
	}

	private static void assertAdressePart(final TiersHisto tiers) {
		assertNotNull(tiers);
		assertNotNull(tiers.adressesCourrier);
		assertNotNull(tiers.adressesDomicile);
		assertNotNull(tiers.adressesPoursuite);
		assertNotNull(tiers.adressesRepresentation);
		assertNull(tiers.forsFiscauxPrincipaux);
		assertNull(tiers.autresForsFiscaux);
	}

	private static void assertNoPart(final TiersHisto tiers) {
		assertNotNull(tiers);
		assertNull(tiers.adressesCourrier);
		assertNull(tiers.adressesDomicile);
		assertNull(tiers.adressesPoursuite);
		assertNull(tiers.adressesRepresentation);
		assertNull(tiers.forsFiscauxPrincipaux);
		assertNull(tiers.autresForsFiscaux);
	}
}
