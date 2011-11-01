package ch.vd.uniregctb.webservices.party3.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.BatchPartyEntry;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetDebtorInfoRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.unireg.xml.party.debtor.v1.DebtorInfo;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.unireg.xml.party.taxdeclaration.v1.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.taxpayer.v1.Taxpayer;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.SituationFamillePersonnePhysique;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class PartyWebServiceCacheTest extends WebserviceTest {

	private Ehcache ehcache;
	private PartyWebServiceCache cache;
	private PartyWebServiceCacheManager wsCacheManager;
	private PartyWebServiceTracing implementation;

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
		PartyWebService webService = getBean(PartyWebService.class, "partyService3Impl");
		implementation = new PartyWebServiceTracing(webService);

		cache = new PartyWebServiceCache();
		cache.setCacheManager(manager);
		cache.setTarget(implementation);
		cache.setCacheName("webServiceParty3");
		ehcache = cache.getEhCache();

		wsCacheManager = getBean(PartyWebServiceCacheManager.class, "partyService3CacheManager");
		wsCacheManager.setCache(cache);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(123456, date(1965, 4, 13), "Eric", "Bolomey", true);
				final Individu papa = addIndividu(111111, date(1925, 11, 29), "Papa", "Bolomey", true);
				ind.setParents(Arrays.asList(papa));
				final Individu junior = addIndividu(222222, date(2002, 3, 7), "Junior", "Bolomey", true);
				ind.setEnfants(Arrays.asList(junior));
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				// Un tiers avec avec toutes les parties renseignées
				final PersonnePhysique eric = addHabitant(123456);
				addAdresseSuisse(eric, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				eric.setNumeroCompteBancaire("CH9308440717427290198");

				final PersonnePhysique pupille = addNonHabitant("Slobodan", "Pupille", date(1987, 7, 23), Sexe.MASCULIN);
				addTutelle(pupille, eric, null, date(2005, 7, 1), null);

				final SituationFamillePersonnePhysique situation = new SituationFamillePersonnePhysique();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setNombreEnfants(0);
				eric.addSituationFamille(situation);

				final PeriodeFiscale periode = addPeriodeFiscale(2003);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire di = addDeclarationImpot(eric, periode, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(di, date(2003, 1, 10));

				addImmeuble(eric, "132/543", date(1988, 3, 14), null, "Lausanne", "Place jardin", GenrePropriete.COMMUNE, 923000, "1994", null, "1");
				ids.eric = eric.getNumero();

				// le père et l'enfant
				addHabitant(111111);
				addHabitant(222222);

				// un débiteur
				final DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.ANNUEL, date(2000, 1, 1));
				ids.debiteur = debiteur.getId();

				return null;
			}
		});

		// Un ménage commun
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique monsieur = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				PersonnePhysique madame = addNonHabitant("Monique", "Bolomey", date(1969, 12, 3), Sexe.FEMININ);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, date(1989, 5, 1), null);
				ch.vd.uniregctb.tiers.MenageCommun mc = ensemble.getMenage();
				mc.setNumeroCompteBancaire("CH9308440717427290198");

				SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setNombreEnfants(0);
				mc.addSituationFamille(situation);

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
		wsCacheManager.setCache(getBean(PartyWebServiceCache.class, "partyService3Cache"));
		super.onTearDown();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetParty() throws Exception {

		GetPartyRequest paramsNoPart = new GetPartyRequest();
		paramsNoPart.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		paramsNoPart.setPartyNumber(ids.eric.intValue());

		final Set<PartyPart> adressesPart = new HashSet<PartyPart>();
		adressesPart.add(PartyPart.ADDRESSES);

		final GetPartyRequest paramsAdressePart = new GetPartyRequest();
		paramsAdressePart.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		paramsAdressePart.setPartyNumber(ids.eric.intValue());
		paramsAdressePart.getParts().add(PartyPart.ADDRESSES);

		final GetPartyRequest paramsForsPart = new GetPartyRequest();
		paramsForsPart.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		paramsForsPart.setPartyNumber(ids.eric.intValue());
		paramsForsPart.getParts().add(PartyPart.TAX_RESIDENCES);

		final Set<PartyPart> forsEtAdressesParts = new HashSet<PartyPart>();
		forsEtAdressesParts.add(PartyPart.ADDRESSES);
		forsEtAdressesParts.add(PartyPart.TAX_RESIDENCES);

		final GetPartyRequest paramsForsEtAdressesParts = new GetPartyRequest();
		paramsForsEtAdressesParts.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		paramsForsEtAdressesParts.setPartyNumber(ids.eric.intValue());
		paramsForsEtAdressesParts.getParts().add(PartyPart.ADDRESSES);
		paramsForsEtAdressesParts.getParts().add(PartyPart.TAX_RESIDENCES);

		// sans parts
		{
			assertNoPart(cache.getParty(paramsNoPart));

			final GetPartyValue value = getCacheValue(paramsNoPart.getPartyNumber());
			assertNotNull(value);
			assertEmpty(value.getParts());
		}

		// ajout des adresses
		{
			assertAddressPart(cache.getParty(paramsAdressePart));
			assertNoPart(cache.getParty(paramsNoPart)); // on vérifie que le tiers sans part fonctionne toujours bien

			final GetPartyValue value = getCacheValue(paramsNoPart.getPartyNumber());
			assertNotNull(value);
			assertEquals(adressesPart, value.getParts());
		}

		// ajout des fors
		{
			assertTaxResidenceAndAddressePart(cache.getParty(paramsForsEtAdressesParts));
			assertTaxResidencePart(cache.getParty(paramsForsPart)); // on vérifie que le tiers avec seulement les fors est correct
			assertNoPart(cache.getParty(paramsNoPart)); // on vérifie que le tiers sans part fonctionne toujours bien
			assertAddressPart(cache.getParty(paramsAdressePart)); // on vérifie que le tiers avec adresse fonctionne toujours bien

			final GetPartyValue value = getCacheValue(paramsNoPart.getPartyNumber());
			assertNotNull(value);
			assertEquals(forsEtAdressesParts, value.getParts());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyAllParts() throws Exception {

		// on demande tour-à-tour les parties et on vérifie que 1) on les reçoit bien; et 2) qu'on ne reçoit qu'elles.
		for (PartyPart p : PartyPart.values()) {
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
			params.setPartyNumber(ids.eric.intValue());
			params.getParts().add(p);
			assertOnlyPart(p, cache.getParty(params));
		}

		// maintenant que le cache est chaud, on recommence la manipulation pour vérifier que cela fonctionne toujours
		for (PartyPart p : PartyPart.values()) {
			final GetPartyRequest params = new GetPartyRequest();
			params.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
			params.setPartyNumber(ids.eric.intValue());
			params.getParts().add(p);
			assertOnlyPart(p, cache.getParty(params));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEvictParty() throws Exception {

		// On charge le cache avec des tiers

		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		params.setPartyNumber(ids.eric.intValue());

		assertNotNull(cache.getParty(params));
		assertNotNull(getCacheValue(params.getPartyNumber()));

		GetPartyRequest paramsHisto = new GetPartyRequest();
		paramsHisto.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		paramsHisto.setPartyNumber(ids.eric.intValue());

		assertNotNull(cache.getParty(paramsHisto));
		assertNotNull(getCacheValue(paramsHisto.getPartyNumber()));

		// On evicte les tiers

		cache.evictParty(ids.eric);

		// On vérifie que le cache est vide

		assertNull(getCacheValue(params.getPartyNumber()));
		assertNull(getCacheValue(paramsHisto.getPartyNumber()));
	}

	/**
	 * [UNIREG-2588] Vérifie que l'éviction d'un tiers se propage automatiquement à tous les tiers liés par rapport-entre-tiers
	 */
	@Test
	public void testEvictPartyCommonHousehold() throws Exception {

		// On charge le cache avec le ménage commun
		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		params.setPartyNumber(ids.menage.intValue());
		params.getParts().add(PartyPart.ADDRESSES);

		final CommonHousehold menageAvant = (CommonHousehold) cache.getParty(params);
		assertNotNull(menageAvant);

		// On vérifie l'adresse d'envoi
		final List<Address> mailAddressesAvant = menageAvant.getMailAddresses();
		final FormattedAddress adressesAvant = mailAddressesAvant.get(mailAddressesAvant.size() - 1).getFormattedAddress();
		assertEquals("Monsieur et Madame", adressesAvant.getLine1());
		assertEquals("Eric Bolomey", adressesAvant.getLine2());
		assertEquals("Monique Bolomey", adressesAvant.getLine3());
		assertEquals("Av de Beaulieu", adressesAvant.getLine4());
		assertEquals("1000 Lausanne", adressesAvant.getLine5());
		assertNull(adressesAvant.getLine6());

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

		final CommonHousehold menageApres = (CommonHousehold) cache.getParty(params);
		assertNotNull(menageApres);

		// On vérifie l'adresse d'envoi
		final List<Address> mailAddressesApres = menageApres.getMailAddresses();
		final FormattedAddress adressesApres = mailAddressesApres.get(mailAddressesApres.size() - 1).getFormattedAddress();
		assertEquals("Monsieur et Madame", adressesApres.getLine1());
		assertEquals("Eric Bolomey", adressesApres.getLine2());
		assertEquals("Gudrun Bolomey", adressesApres.getLine3());
		assertEquals("Av de Beaulieu", adressesApres.getLine4());
		assertEquals("1000 Lausanne", adressesApres.getLine5());
		assertNull(adressesApres.getLine6());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEvictDebtorInfo() throws Exception {

		// On charge le cache avec des tiers

		GetDebtorInfoRequest params = new GetDebtorInfoRequest();
		params.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		params.setDebtorNumber(ids.debiteur.intValue());
		params.setTaxPeriod(2010);

		assertNotNull(cache.getDebtorInfo(params));
		assertNotNull(getCacheValue(params));

		// On evicte les tiers

		cache.evictParty(ids.debiteur);

		// On vérifie que le cache est vide

		assertNull(getCacheValue(params));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyInexistant() throws Exception {

		// Essaie une fois sans part
		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		params.setPartyNumber(1233455);

		assertNull(cache.getParty(params));
		assertNotNull(getCacheValue(params.getPartyNumber())); // not null -> on cache aussi la réponse pour un tiers inexistant !

		// Essai une seconde fois avec parts
		params.getParts().add(PartyPart.ADDRESSES);
		assertNull(cache.getParty(params));
		assertNotNull(getCacheValue(params.getPartyNumber()));
	}

	/**
	 * [UNIREG-2587] Vérifie que le cache fonctionne correctement lorsqu'un tiers est demandé successivement <ol> <li>avec ses fors fiscaux virtuels, puis</li> <li>juste avec ses fors fiscaux, et</li>
	 * <li>finalement de nouveau avec ses fors fiscaux virtuels.</li> </ol>
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartySpecialCaseVirtualTaxResidences() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		params.setPartyNumber(ids.monsieur.intValue());

		// 1. on demande le tiers avec les fors fiscaux virtuels
		{
			params.getParts().add(PartyPart.VIRTUAL_TAX_RESIDENCES);

			final Party tiers = cache.getParty(params);
			assertNotNull(tiers);
			assertNotNull(tiers.getMainTaxResidences());
			assertEquals(1, tiers.getMainTaxResidences().size());

			final TaxResidence ffp = tiers.getMainTaxResidences().get(0);
			assertEquals(new Date(1989, 5, 1), ffp.getDateFrom());
			assertNull(ffp.getDateTo());
		}

		// 2. on demande le tiers *sans* les fors fiscaux virtuels
		{
			params.getParts().clear();
			params.getParts().add(PartyPart.TAX_RESIDENCES);

			final Party tiers = cache.getParty(params);
			assertNotNull(tiers);
			assertEmpty(tiers.getMainTaxResidences());
		}

		// 3. on demande de nouveau le tiers avec les fors fiscaux virtuels => le résultat doit être identique à la demande du point 1.
		{
			params.getParts().clear();
			params.getParts().add(PartyPart.VIRTUAL_TAX_RESIDENCES);

			final Party tiers = cache.getParty(params);
			assertNotNull(tiers);
			assertNotNull(tiers.getMainTaxResidences());
			assertEquals(1, tiers.getMainTaxResidences().size());

			final TaxResidence ffp = tiers.getMainTaxResidences().get(0);
			assertEquals(new Date(1989, 5, 1), ffp.getDateFrom());
			assertNull(ffp.getDateTo());
		}
	}

	/**
	 * Vérifie que le cache fonctionne correctement lorsqu'un tiers est demandé successivement <ol> <li>avec ses déclarations et leurs états, puis</li> <li>juste avec ses déclarations, et</li>
	 * <li>finalement de nouveau avec ses déclarations et leurs états.</li> </ol>
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartySpecialCaseTaxDeclarationsAndStatuses() throws Exception {

		final GetPartyRequest params = new GetPartyRequest();
		params.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		params.setPartyNumber(ids.eric.intValue());

		// 1. on demande le tiers avec les déclarations et leurs états
		{
			params.getParts().add(PartyPart.TAX_DECLARATIONS);
			params.getParts().add(PartyPart.TAX_DECLARATIONS_STATUSES);

			final Party tiers = cache.getParty(params);
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final TaxDeclarationStatus etat0 = etats.get(0);
			assertNotNull(etat0);
			assertEquals(new Date(2003, 1, 10), etat0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, etat0.getType());
		}

		// 2. on demande les déclarations *sans* les états
		{
			params.getParts().clear();
			params.getParts().add(PartyPart.TAX_DECLARATIONS);

			final Party tiers = cache.getParty(params);
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());
			assertEmpty(di.getStatuses());
		}

		// 3. on demande de nouveau les déclarations avec leurs états => le résultat doit être identique à la demande du point 1.
		{
			params.getParts().clear();
			params.getParts().add(PartyPart.TAX_DECLARATIONS);
			params.getParts().add(PartyPart.TAX_DECLARATIONS_STATUSES);

			final Party tiers = cache.getParty(params);
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final TaxDeclarationStatus etat0 = etats.get(0);
			assertNotNull(etat0);
			assertEquals(new Date(2003, 1, 10), etat0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, etat0.getType());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetBatchParty() throws Exception {

		GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		params.getPartyNumbers().add(ids.monsieur.intValue());
		params.getPartyNumbers().add(ids.madame.intValue());

		// Etat initial : aucun appel au web-service
		assertEmpty(implementation.getBatchTiersCalls);

		// 1er appel
		{
			final BatchParty batch = cache.getBatchParty(params);
			assertNotNull(batch);
			assertEquals(2, batch.getEntries().size());

			// on vérifique que les données retournées sont correctes
			final BatchPartyEntry batch0 = batch.getEntries().get(0);
			final BatchPartyEntry batch1 = batch.getEntries().get(1);
			final Party monsieur = (batch0.getNumber() == ids.monsieur ? batch0.getParty() : batch1.getParty());
			final Party madame = (batch0.getNumber() == ids.madame ? batch0.getParty() : batch1.getParty());
			assertNoPart(monsieur);
			assertNoPart(madame);

			// on vérifie qu'il y a bien eu un appel au web-service
			assertEquals(1, implementation.getBatchTiersCalls.size());
			assertEquals(params.getPartyNumbers(), implementation.getBatchTiersCalls.get(0).getPartyNumbers());
		}

		// 2ème appel : identique au premier
		{
			final BatchParty batch = cache.getBatchParty(params);
			assertNotNull(batch);
			assertEquals(2, batch.getEntries().size());

			// on vérifique que les données retournées sont correctes
			final BatchPartyEntry batch0 = batch.getEntries().get(0);
			final BatchPartyEntry batch1 = batch.getEntries().get(1);
			final Party monsieur = (batch0.getNumber() == ids.monsieur ? batch0.getParty() : batch1.getParty());
			final Party madame = (batch0.getNumber() == ids.madame ? batch0.getParty() : batch1.getParty());
			assertNoPart(monsieur);
			assertNoPart(madame);

			// on vérifie qu'il n'y a pas de second appel au web-service, c'est-à-dire que toutes les données ont été trouvées dans le cache
			assertEquals(1, implementation.getBatchTiersCalls.size());
		}

		// 3ème appel : avec un tiers de plus
		{
			params.getPartyNumbers().add(ids.eric.intValue());

			final BatchParty batch = cache.getBatchParty(params);
			assertNotNull(batch);
			assertEquals(3, batch.getEntries().size());

			// on vérifique que les données retournées sont correctes
			Party monsieur = null;
			Party madame = null;
			Party eric = null;
			for (BatchPartyEntry entry : batch.getEntries()) {
				if (entry.getNumber() == ids.monsieur) {
					monsieur = entry.getParty();
				}
				else if (entry.getNumber() == ids.madame) {
					madame = entry.getParty();
				}
				else if (entry.getNumber() == ids.eric) {
					eric = entry.getParty();
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
			assertEquals(params.getPartyNumbers(), implementation.getBatchTiersCalls.get(0).getPartyNumbers());
			assertEquals(Arrays.asList(ids.eric.intValue()), implementation.getBatchTiersCalls.get(1).getPartyNumbers());
		}
	}

	/**
	 * [UNIREG-3288] Vérifie que les exceptions levées dans la méthode getBatchParty sont correctement gérées au niveau du cache.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetBatchPartyWithExceptionOnTiers() throws Exception {

		GetBatchPartyRequest params = new GetBatchPartyRequest();
		params.setLogin(new UserLogin("[PartyWebServiceCacheTest]", 21));
		params.getPartyNumbers().add(ids.monsieur.intValue());
		params.getPartyNumbers().add(ids.madame.intValue());

		// on intercale une implémentation du web-service qui lèvera une exception lors de la récupération de madame
		cache.setTarget(new PartyWebServiceCrashing(implementation, ids.madame.intValue()));

		// 1er appel : monsieur est correctement récupéré et une exception est retournée à la place de madame.
		{
			final BatchParty batch = cache.getBatchParty(params);
			assertNotNull(batch);
			assertEquals(2, batch.getEntries().size());

			// on vérifie que : monsieur est bien retourné et qu'une exception a été levée sur madame
			final BatchPartyEntry batch0 = batch.getEntries().get(0);
			final BatchPartyEntry batch1 = batch.getEntries().get(1);

			final BatchPartyEntry entryMonsieur = (batch0.getNumber() == ids.monsieur ? batch0 : batch1);
			assertNotNull(entryMonsieur.getParty());

			final BatchPartyEntry entryMadame = (batch0.getNumber() == ids.madame ? batch0 : batch1);
			assertNull(entryMadame.getParty());
			assertEquals("Exception de test", entryMadame.getExceptionInfo().getMessage());

			// on vérifie que : seul monsieur est stocké dans le cache et que madame n'y est pas (parce qu'une exception a été levée)
			assertNotNull(getCacheValue(ids.monsieur));
			assertNull(getCacheValue(ids.madame));
		}

		// 2ème appel : identique au premier pour vérifier que le cache est dans un état cohérent (provoquait un crash avant la correction de UNIREG-3288)
		{
			final BatchParty batch = cache.getBatchParty(params);
			assertNotNull(batch);
			assertEquals(2, batch.getEntries().size());

			// on vérifie que : monsieur est bien retourné et qu'une exception a été levée sur madame
			final BatchPartyEntry batch0 = batch.getEntries().get(0);
			final BatchPartyEntry batch1 = batch.getEntries().get(1);

			final BatchPartyEntry entryMonsieur = (batch0.getNumber() == ids.monsieur ? batch0 : batch1);
			assertNotNull(entryMonsieur.getParty());

			final BatchPartyEntry entryMadame = (batch0.getNumber() == ids.madame ? batch0 : batch1);
			assertNull(entryMadame.getParty());
			assertEquals("Exception de test", entryMadame.getExceptionInfo().getMessage());

			// on vérifie que : seul monsieur est stocké dans le cache et que madame n'y est pas (parce qu'une exception a été levée)
			assertNotNull(getCacheValue(ids.monsieur));
			assertNull(getCacheValue(ids.madame));
		}
	}

	private GetPartyValue getCacheValue(long tiersNumber) {
		GetPartyValue value = null;
		final GetPartyKey key = new GetPartyKey(tiersNumber);
		final Element element = ehcache.get(key);
		if (element != null) {
			value = (GetPartyValue) element.getObjectValue();
		}
		return value;
	}

	private DebtorInfo getCacheValue(GetDebtorInfoRequest params) {
		DebtorInfo value = null;
		final GetDebtorInfoKey key = new GetDebtorInfoKey(params.getDebtorNumber(), params.getTaxPeriod());
		final Element element = ehcache.get(key);
		if (element != null) {
			value = (DebtorInfo) element.getObjectValue();
		}
		return value;
	}

	/**
	 * Assert que la partie spécifiée et uniquement celle-ci est renseignée sur le tiers.
	 */
	private static void assertOnlyPart(PartyPart p, Party tiers) {

		boolean checkAddresses = PartyPart.ADDRESSES == p;
		boolean checkTaxLiabilities = PartyPart.TAX_LIABILITIES == p;
		boolean checkSimplifiedTaxLiabilities = PartyPart.SIMPLIFIED_TAX_LIABILITIES == p;
		boolean checkHouseholdMembers = PartyPart.HOUSEHOLD_MEMBERS == p;
		boolean checkBankAccounts = PartyPart.BANK_ACCOUNTS == p;
		boolean checkTaxDeclarations = PartyPart.TAX_DECLARATIONS == p;
		boolean checkTaxDeclarationsStatuses = PartyPart.TAX_DECLARATIONS_STATUSES == p;
		boolean checkTaxResidences = PartyPart.TAX_RESIDENCES == p;
		boolean checkVirtualTaxResidences = PartyPart.VIRTUAL_TAX_RESIDENCES == p;
		boolean checkManagingTaxResidences = PartyPart.MANAGING_TAX_RESIDENCES == p;
		boolean checkTaxationPeriods = PartyPart.TAXATION_PERIODS == p;
		boolean checkRelationsBetweenParties = PartyPart.RELATIONS_BETWEEN_PARTIES == p;
		boolean checkFamilyStatuses = PartyPart.FAMILY_STATUSES == p;
		boolean checkCapitals = PartyPart.CAPITALS == p;
		boolean checkCorporationStatuses = PartyPart.CORPORATION_STATUSES == p;
		boolean checkLegalForms = PartyPart.LEGAL_FORMS == p;
		boolean checkTaxSystems = PartyPart.TAX_SYSTEMS == p;
		boolean checkLegalSeats = PartyPart.LEGAL_SEATS == p;
		boolean checkDebtorPeriodicities = PartyPart.DEBTOR_PERIODICITIES == p;
		boolean checkImmovableProperties = PartyPart.IMMOVABLE_PROPERTIES == p;
		boolean checkChildren = PartyPart.CHILDREN == p;
		boolean checkParents = PartyPart.PARENTS == p;
		Assert.isTrue(checkAddresses || checkTaxLiabilities || checkHouseholdMembers || checkBankAccounts || checkTaxDeclarations || checkTaxDeclarationsStatuses ||
				checkTaxResidences || checkVirtualTaxResidences || checkManagingTaxResidences || checkTaxationPeriods || checkRelationsBetweenParties || checkFamilyStatuses || checkCapitals ||
				checkCorporationStatuses || checkLegalForms || checkTaxSystems || checkLegalSeats || checkDebtorPeriodicities || checkSimplifiedTaxLiabilities || checkImmovableProperties ||
				checkChildren || checkParents, "La partie [" + p + "] est inconnue");

		assertNullOrNotNull(checkAddresses, tiers.getMailAddresses(), "mailAddresses");
		assertNullOrNotNull(checkAddresses, tiers.getResidenceAddresses(), "residenceAddresses");
		assertNullOrNotNull(checkAddresses, tiers.getDebtProsecutionAddresses(), "debtProsecutionAddresses");
		assertNullOrNotNull(checkAddresses, tiers.getRepresentationAddresses(), "representationAddresses");
		assertNullOrNotNull(checkBankAccounts, tiers.getBankAccounts(), "bankAccounts");
		assertNullOrNotNull(checkTaxResidences || checkVirtualTaxResidences, tiers.getMainTaxResidences(), "mainTaxResidences");
		assertNullOrNotNull(checkTaxResidences || checkVirtualTaxResidences, tiers.getOtherTaxResidences(), "otherTaxResidences");
		assertNullOrNotNull(checkManagingTaxResidences, tiers.getManagingTaxResidences(), "managingTaxResidences");
		assertNullOrNotNull(checkRelationsBetweenParties || checkChildren || checkParents, tiers.getRelationsBetweenParties(), "relationsBetweenParties (" + p + ")");

		if (tiers instanceof Taxpayer) {
			Taxpayer ctb = (Taxpayer) tiers;
			assertNullOrNotNull(checkTaxLiabilities, ctb.getTaxLiabilities(), "taxLiabilities");
			assertNullOrNotNull(checkSimplifiedTaxLiabilities, ctb.getSimplifiedTaxLiabilityVD(), "simplifiedTaxLiabilityVD");
			assertNullOrNotNull(checkSimplifiedTaxLiabilities, ctb.getSimplifiedTaxLiabilityCH(), "simplifiedTaxLiabilityCH");
			assertNullOrNotNull(checkTaxDeclarations || checkTaxDeclarationsStatuses, ctb.getTaxDeclarations(), "taxDeclarations");
			assertNullOrNotNull(checkTaxationPeriods, ctb.getTaxationPeriods(), "taxationPeriods");
			assertNullOrNotNull(checkFamilyStatuses, ctb.getFamilyStatuses(), "familyStatuses");
			assertNullOrNotNull(checkImmovableProperties, ctb.getImmovableProperties(), "immovableProperties");
		}

		if (tiers instanceof CommonHousehold) {
			CommonHousehold mc = (CommonHousehold) tiers;
			assertNullOrNotNull(checkHouseholdMembers, mc.getMainTaxpayer(), "mainTaxpayer");
			assertNullOrNotNull(checkHouseholdMembers, mc.getSecondaryTaxpayer(), "secondaryTaxpayer");
		}

		if (tiers instanceof Corporation) {
			Corporation pm = (Corporation) tiers;
			assertNullOrNotNull(checkCapitals, pm.getCapitals(), "capitals");
			assertNullOrNotNull(checkCorporationStatuses, pm.getStatuses(), "statuses");
			assertNullOrNotNull(checkLegalForms, pm.getLegalForms(), "legalForms");
			assertNullOrNotNull(checkTaxSystems, pm.getTaxSystemsVD(), "taxSystemsVD");
			assertNullOrNotNull(checkTaxSystems, pm.getTaxSystemsCH(), "taxSystemsCH");
			assertNullOrNotNull(checkLegalSeats, pm.getLegalSeats(), "legalSeats");
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

	private static void assertTaxResidenceAndAddressePart(final Party tiers) {
		assertNotNull(tiers);
		assertNotEmpty(tiers.getMailAddresses());
		assertNotEmpty(tiers.getResidenceAddresses());
		assertNotEmpty(tiers.getDebtProsecutionAddresses());
		assertNotEmpty(tiers.getRepresentationAddresses());
		assertNotEmpty(tiers.getMainTaxResidences());
		assertNotEmpty(tiers.getOtherTaxResidences());
	}

	private static void assertTaxResidencePart(final Party tiers) {
		assertNotNull(tiers);
		assertEmpty(tiers.getMailAddresses());
		assertEmpty(tiers.getResidenceAddresses());
		assertEmpty(tiers.getDebtProsecutionAddresses());
		assertEmpty(tiers.getRepresentationAddresses());
		assertNotEmpty(tiers.getMainTaxResidences());
		assertNotEmpty(tiers.getOtherTaxResidences());
	}

	private static void assertAddressPart(final Party tiers) {
		assertNotNull(tiers);
		assertNotNull(tiers.getMailAddresses());
		assertNotEmpty(tiers.getResidenceAddresses());
		assertNotEmpty(tiers.getDebtProsecutionAddresses());
		assertNotEmpty(tiers.getRepresentationAddresses());
		assertEmpty(tiers.getMainTaxResidences());
		assertEmpty(tiers.getOtherTaxResidences());
	}

	private static void assertNoPart(final Party tiers) {
		assertNotNull(tiers);
		assertEmpty(tiers.getMailAddresses());
		assertEmpty(tiers.getResidenceAddresses());
		assertEmpty(tiers.getDebtProsecutionAddresses());
		assertEmpty(tiers.getRepresentationAddresses());
		assertEmpty(tiers.getMainTaxResidences());
		assertEmpty(tiers.getOtherTaxResidences());
	}

	private static void assertNotEmpty(Collection<?> coll) {
		assertNotNull(coll);
		assertFalse(coll.isEmpty());
	}
}
