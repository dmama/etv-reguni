package ch.vd.unireg.webservices.v5.cache;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ch.ech.ech0007.v4.CantonAbbreviation;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WebserviceTest;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.efacture.EFactureServiceProxy;
import ch.vd.unireg.efacture.MockEFactureService;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.tiers.SituationFamillePersonnePhysique;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeDroitAcces;
import ch.vd.unireg.webservices.common.UserLogin;
import ch.vd.unireg.webservices.v5.BusinessWebService;
import ch.vd.unireg.ws.parties.v1.Entry;
import ch.vd.unireg.ws.parties.v1.Parties;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.address.v2.Address;
import ch.vd.unireg.xml.party.address.v2.FormattedAddress;
import ch.vd.unireg.xml.party.corporation.v3.Corporation;
import ch.vd.unireg.xml.party.ebilling.v1.EbillingStatus;
import ch.vd.unireg.xml.party.person.v3.CommonHousehold;
import ch.vd.unireg.xml.party.person.v3.Nationality;
import ch.vd.unireg.xml.party.person.v3.NaturalPerson;
import ch.vd.unireg.xml.party.person.v3.Origin;
import ch.vd.unireg.xml.party.taxdeclaration.v3.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationDeadline;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.taxpayer.v3.Taxpayer;
import ch.vd.unireg.xml.party.taxresidence.v2.ExpenditureBased;
import ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v2.OrdinaryResident;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxResidence;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;

import static ch.vd.unireg.xml.party.v3.PartyPart.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class BusinessWebServiceCacheTest extends WebserviceTest {

	private Ehcache ehcache;
	private BusinessWebServiceCache cache;
	private BusinessWebServiceCacheEventListener wsCacheManager;
	private BusinessWebService implementation;
	private Map<String, List<Object[]>> calls;

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
		final CacheManager manager = getBean(CacheManager.class, "ehCacheManager");
		final BusinessWebService webService = getBean(BusinessWebService.class, "wsv5Business");
		this.calls = new HashMap<>();
		implementation = buildTracingDelegator(webService, this.calls);

		cache = new BusinessWebServiceCache();
		cache.setCacheManager(manager);
		cache.setTarget(implementation);
		cache.setCacheName("webService5");
		cache.setSecurityProvider(getBean(SecurityProviderInterface.class, "securityProviderInterface"));
		cache.afterPropertiesSet();
		ehcache = cache.getEhCache();

		wsCacheManager = getBean(BusinessWebServiceCacheEventListener.class, "wsv5CacheEventListener");
		wsCacheManager.setCache(cache);

		final int noIndividu = 123456;
		final int noIndividuPapa = 111111;
		final int noIndividuJunior = 222222;
		final RegDate dateNaissance = date(1965, 4, 13);
		final RegDate dateNaissanceJunior = date(2002, 3, 7);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu papa = addIndividu(noIndividuPapa, date(1925, 11, 29), "Papa", "Bolomey", true);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Eric", "Bolomey", true);
				final MockIndividu junior = addIndividu(noIndividuJunior, dateNaissanceJunior, "Junior", "Bolomey", true);
				addLiensFiliation(junior, ind, null, dateNaissanceJunior, null);
				addLiensFiliation(ind, papa, null, dateNaissance, null);
				addOrigine(ind, MockCommune.Neuchatel);
				addOrigine(ind, MockCommune.Orbe);
				ind.setNomNaissance("Bolomey-de-naissance");
				addNationalite(ind, MockPays.Suisse, dateNaissance, null);
				addNationalite(ind, MockPays.France, dateNaissance.addMonths(1), null);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// Un tiers avec avec toutes les parties renseignées
				final PersonnePhysique eric = addHabitant(noIndividu);
				addAdresseSuisse(eric, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
				eric.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH9308440717427290198", null));

				final PersonnePhysique pupille = addNonHabitant("Slobodan", "Pupille", date(1987, 7, 23), Sexe.MASCULIN);
				addTutelle(pupille, eric, null, date(2005, 7, 1), null);

				final SituationFamillePersonnePhysique situation = new SituationFamillePersonnePhysique();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setNombreEnfants(0);
				eric.addSituationFamille(situation);

				final PeriodeFiscale periode = addPeriodeFiscale(2003);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				ch.vd.unireg.declaration.DeclarationImpotOrdinaire di = addDeclarationImpot(eric, periode, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(di, date(2003, 1, 10));
				addDelaiDeclaration(di, date(2003, 1, 10), date(2003, 6, 30), EtatDelaiDocumentFiscal.ACCORDE);

				ids.eric = eric.getNumero();

				// le père et l'enfant
				final PersonnePhysique papa = addHabitant(noIndividuPapa);
				final PersonnePhysique junior = addHabitant(noIndividuJunior);

				addParente(eric, papa, dateNaissance, null);
				addParente(junior, eric, dateNaissanceJunior, null);

				// un débiteur
				final DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.ANNUEL, date(2009, 1, 1));
				ids.debiteur = debiteur.getId();

				// ... avec une LR sur 2009
				addForDebiteur(debiteur, date(2009, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Lausanne);
				final PeriodeFiscale pf2009 = addPeriodeFiscale(2009);
				final ModeleDocument modeleLr = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf2009);
				addListeRecapitulative(debiteur, pf2009, date(2009, 1, 1), date(2009, 12, 31), modeleLr);

				// un rapport de travail entre eric et le débiteur (pour avoir un calcul de PIIS)
				addRapportPrestationImposable(debiteur, eric, date(2009, 1, 1), date(2009, 5, 1), false);

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
				ch.vd.unireg.tiers.MenageCommun mc = ensemble.getMenage();
				mc.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH9308440717427290198", null));

				SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setNombreEnfants(0);
				mc.addSituationFamille(situation);

				addAdresseSuisse(mc, TypeAdresseTiers.COURRIER, date(1989, 5, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(mc, date(1989, 5, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForSecondaire(mc, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne,
				                 MotifRattachement.IMMEUBLE_PRIVE);

				ids.monsieur = monsieur.getNumero();
				ids.madame = madame.getNumero();
				ids.menage = mc.getNumero();
				return null;
			}
		});


		final EFactureServiceProxy eFactureProxy = getBean(EFactureServiceProxy.class, "efactureService");
		eFactureProxy.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ids.eric);
				addEtatDestinataire(ids.eric, DateHelper.getCalendar(2013, 5, 12, 22, 24, 38).getTime(), "C'est maintenant ou jamais", null, TypeEtatDestinataire.INSCRIT, "toto@titi.com", null);
			}
		});
	}

	/**
	 * Construit un objet qui remplit la map des compteurs (clé = nom de la méthode, valeur = paramètres des appels) avant de déléguer l'appel plus loin
	 * @param target à qui déléguer les appels
	 * @param calls map à remplir
	 * @return l'objet qui fait tout ça...
	 */
	private static BusinessWebService buildTracingDelegator(final BusinessWebService target, final Map<String, List<Object[]>> calls) {
		return (BusinessWebService) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
		                                                   new Class[]{BusinessWebService.class},
		                                                   new InvocationHandler() {
			                                                   @Override
			                                                   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				                                                   synchronized (calls) {
					                                                   List<Object[]> c = calls.computeIfAbsent(method.getName(), k -> new ArrayList<>());
					                                                   c.add(args);
				                                                   }
				                                                   return method.invoke(target, args);
			                                                   }
		                                                   });
	}

	private static int getNumberOfCalls(Map<String, List<Object[]>> calls) {
		int counter = 0;
		for (List<Object[]> methodCalls : calls.values()) {
			counter += methodCalls.size();
		}
		return counter;
	}

	private static int getNumberOfCalls(Map<String, List<Object[]>> calls, String methodName) {
		final List<Object[]> methodCalls = calls.get(methodName);
		return methodCalls == null ? 0 : methodCalls.size();
	}

	private static int getNumberOfCallsToGetParties(Map<String, List<Object[]>> calls) {
		return getNumberOfCalls(calls, "getParties");
	}

	private static int getNumberOfCallsToGetParty(Map<String, List<Object[]>> calls) {
		return getNumberOfCalls(calls, "getParty");
	}

	private static Object[] getLastCallParameters(Map<String, List<Object[]>> calls, String methodName) {
		final List<Object[]> methodCalls = calls.get(methodName);
		assertNotNull(methodCalls);
		return methodCalls.get(methodCalls.size() - 1);
	}

	@SuppressWarnings("unchecked")
	private static Pair<List<Integer>, Set<PartyPart>> getLastCallParametersToGetParties(Map<String, List<Object[]>> calls) {
		final Object[] lastCall = getLastCallParameters(calls, "getParties");
		assertEquals(3, lastCall.length);
		return Pair.of((List<Integer>) lastCall[1], (Set<PartyPart>) lastCall[2]);
	}

	@SuppressWarnings("unchecked")
	private static Pair<Integer, Set<PartyPart>> getLastCallParametersToGetParty(Map<String, List<Object[]>> calls) {
		final Object[] lastCall = getLastCallParameters(calls, "getParty");
		assertEquals(3, lastCall.length);
		return Pair.of((Integer) lastCall[1], (Set<PartyPart>) lastCall[2]);
	}

	@Override
	public void onTearDown() throws Exception {
		wsCacheManager.setCache(getBean(BusinessWebServiceCache.class, "wsv5Cache"));
		super.onTearDown();
	}

	/**
	 * [SIFISC-13558] En passant au travers du cache, ces données étaient oubliées
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNomNaissanceEtOrigineEtNationalite() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);
		final int partyNo = ids.eric.intValue();

		final Party party = cache.getParty(userLogin, partyNo, null);
		assertInstanceOf(NaturalPerson.class, party);

		final NaturalPerson np = (NaturalPerson) party;

		// nom de naissance
		{
			assertEquals("Bolomey-de-naissance", np.getBirthName());
		}

		// orgines
		{
			final List<Origin> origines = np.getOrigins();
			assertNotNull(origines);
			assertEquals(2, origines.size());

			// tri dans l'ordre alphabétique des noms de lieux : 1. Neuch', 2. Orbe
			final List<Origin> sortedOrigins = new ArrayList<>(origines);
			Collections.sort(sortedOrigins, new Comparator<Origin>() {
				@Override
				public int compare(Origin o1, Origin o2) {
					return o1.getOriginName().compareTo(o2.getOriginName());
				}
			});

			{
				final Origin origine = sortedOrigins.get(0);
				assertNotNull(origine);
				assertEquals(CantonAbbreviation.NE, origine.getCanton());
				assertEquals(MockCommune.Neuchatel.getNomOfficiel(), origine.getOriginName());
			}
			{
				final Origin origine = sortedOrigins.get(1);
				assertNotNull(origine);
				assertEquals(CantonAbbreviation.VD, origine.getCanton());
				assertEquals(MockCommune.Orbe.getNomOfficiel(), origine.getOriginName());
			}
		}

		// nationalités
		{
			final List<Nationality> nationalites = np.getNationalities();
			assertNotNull(nationalites);
			assertEquals(2, nationalites.size());

			// tri dans l'ordre croissant des dates de début : 1. Suisse, 2. France
			final List<Nationality> sortedNationalities = new ArrayList<>(nationalites);
			Collections.sort(sortedNationalities, new Comparator<Nationality>() {
				@Override
				public int compare(Nationality o1, Nationality o2) {
					return o1.getDateFrom().compareTo(o2.getDateFrom());
				}
			});

			{
				final Nationality nationality = sortedNationalities.get(0);
				assertNotNull(nationality);
				assertEquals(new Date(1965, 4, 13), nationality.getDateFrom());
				assertNull(nationality.getDateTo());
				assertNotNull(nationality.getSwiss());
				assertNull(nationality.getForeignCountry());
				assertNull(nationality.getStateless());
			}
			{
				final Nationality nationality = sortedNationalities.get(1);
				assertNotNull(nationality);
				assertEquals(new Date(1965, 5, 13), nationality.getDateFrom());
				assertNull(nationality.getDateTo());
				assertNull(nationality.getSwiss());
				assertEquals((Integer) MockPays.France.getNoOFS(), nationality.getForeignCountry());
				assertNull(nationality.getStateless());
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetParty() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);
		final int partyNo = ids.eric.intValue();

		// sans parts
		{
			assertNoPart(cache.getParty(userLogin, partyNo, null));

			final GetPartyValue value = getCacheValue(partyNo);
			assertNotNull(value);
			assertEmpty(value.getParts());
		}

		// ajout des adresses
		{
			assertAddressPart(cache.getParty(userLogin, partyNo, EnumSet.of(PartyPart.ADDRESSES)));
			assertNoPart(cache.getParty(userLogin, partyNo, null)); // on vérifie que le tiers sans part fonctionne toujours bien

			final GetPartyValue value = getCacheValue(partyNo);
			assertNotNull(value);
			assertEquals(EnumSet.of(PartyPart.ADDRESSES), value.getParts());
		}

		// ajout des fors
		{
			assertTaxResidenceAndAddressePart(cache.getParty(userLogin, partyNo, EnumSet.of(PartyPart.TAX_RESIDENCES, PartyPart.ADDRESSES)));
			assertTaxResidencePart(cache.getParty(userLogin, partyNo, EnumSet.of(PartyPart.TAX_RESIDENCES))); // on vérifie que le tiers avec seulement les fors est correct
			assertNoPart(cache.getParty(userLogin, partyNo, null)); // on vérifie que le tiers sans part fonctionne toujours bien
			assertAddressPart(cache.getParty(userLogin, partyNo, EnumSet.of(PartyPart.ADDRESSES))); // on vérifie que le tiers avec adresse fonctionne toujours bien

			final GetPartyValue value = getCacheValue(partyNo);
			assertNotNull(value);
			assertEquals(EnumSet.of(PartyPart.TAX_RESIDENCES, PartyPart.ADDRESSES), value.getParts());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyAllParts() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		// on demande tour-à-tour les parties et on vérifie que 1) on les reçoit bien; et 2) qu'on ne reçoit qu'elles.
		for (PartyPart p : PartyPart.values()) {
			assertOnlyPart(p, cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(p)));
		}

		// maintenant que le cache est chaud, on recommence la manipulation pour vérifier que cela fonctionne toujours
		for (PartyPart p : PartyPart.values()) {
			assertOnlyPart(p, cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(p)));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEvictParty() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);
		final int partyNo = ids.eric.intValue();

		// On charge le cache avec des tiers

		assertNotNull(cache.getParty(userLogin, partyNo, null));
		assertNotNull(getCacheValue(partyNo));

		// On evicte les tiers
		cache.evictParty(partyNo);

		// On vérifie que le cache est vide
		assertNull(getCacheValue(partyNo));
	}

	/**
	 * [UNIREG-2588] Vérifie que l'éviction d'un tiers se propage automatiquement à tous les tiers liés par rapport-entre-tiers
	 */
	@Test
	public void testEvictPartyCommonHousehold() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		// On charge le cache avec le ménage commun et ses adresses
		final CommonHousehold menageAvant = (CommonHousehold) cache.getParty(userLogin, ids.menage.intValue(), EnumSet.of(PartyPart.ADDRESSES));
		assertNotNull(menageAvant);

		// On vérifie l'adresse d'envoi
		final List<Address> mailAddressesAvant = menageAvant.getMailAddresses();
		final FormattedAddress adressesAvant = mailAddressesAvant.get(mailAddressesAvant.size() - 1).getFormattedAddress();
		assertEquals("Monsieur et Madame", adressesAvant.getLine1());
		assertEquals("Eric Bolomey", adressesAvant.getLine2());
		assertEquals("Monique Bolomey", adressesAvant.getLine3());
		assertEquals("Avenue de Beaulieu", adressesAvant.getLine4());
		assertEquals("1003 Lausanne", adressesAvant.getLine5());
		assertNull(adressesAvant.getLine6());

		// On modifie le prénom de madame
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final ch.vd.unireg.tiers.MenageCommun mc = hibernateTemplate.get(ch.vd.unireg.tiers.MenageCommun.class, ids.menage);
				assertNotNull(mc);

				final Set<RapportEntreTiers> rapports = mc.getRapportsObjet();

				PersonnePhysique madame = null;
				for (RapportEntreTiers r : rapports) {
					final AppartenanceMenage am = (AppartenanceMenage) r;
					final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, am.getSujetId());
					assertNotNull(pp);

					if (pp.getPrenomUsuel().equals("Monique")) {
						madame = pp;
						break;
					}
				}
				assertNotNull(madame);
				madame.setPrenomUsuel("Gudrun");
				return null;
			}
		});

		// Cette modification va provoquer l'éviction de madame du cache, et par transitivité l'éviction du ménage commun. Si ce n'était pas le cas, les données (périmées) du ménage commun seraient encore dans le cache.
		// On vérifie donc que l'adresse d'envoi du ménage commun est bien mise-à-jour.

		final CommonHousehold menageApres = (CommonHousehold) cache.getParty(userLogin, ids.menage.intValue(), EnumSet.of(PartyPart.ADDRESSES));
		assertNotNull(menageApres);

		// On vérifie l'adresse d'envoi
		final List<Address> mailAddressesApres = menageApres.getMailAddresses();
		final FormattedAddress adressesApres = mailAddressesApres.get(mailAddressesApres.size() - 1).getFormattedAddress();
		assertEquals("Monsieur et Madame", adressesApres.getLine1());
		assertEquals("Eric Bolomey", adressesApres.getLine2());
		assertEquals("Gudrun Bolomey", adressesApres.getLine3());
		assertEquals("Avenue de Beaulieu", adressesApres.getLine4());
		assertEquals("1003 Lausanne", adressesApres.getLine5());
		assertNull(adressesApres.getLine6());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEvictDebtorInfo() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		// au début, il n'y a rien
		assertNull(getCacheValue(ids.debiteur.intValue(), 2009));
		assertNull(getCacheValue(ids.debiteur.intValue(), 2010));

		// On charge le cache avec des tiers

		assertNotNull(cache.getDebtorInfo(userLogin, ids.debiteur.intValue(), 2010));
		assertNotNull(getCacheValue(ids.debiteur.intValue(), 2010));
		assertNull("L'appel a été fait sur 2010, il ne devrait rien y avoir dans le cache pour 2009 !!", getCacheValue(ids.debiteur.intValue(), 2009));       // toujours rien pour le 2009

		// On evicte les tiers
		cache.evictParty(ids.debiteur);

		// On vérifie que le cache est vide
		assertNull(getCacheValue(ids.debiteur.intValue(), 2009));
		assertNull(getCacheValue(ids.debiteur.intValue(), 2010));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyInexistant() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		// Essaie une fois sans part
		assertNull(cache.getParty(userLogin, 1233455, null));
		assertNull(getCacheValue(1233455)); // null -> on ne cache pas la réponse pour un tiers inexistant !

		// Essai une seconde fois avec parts
		assertNull(cache.getParty(userLogin, 1233455, EnumSet.of(PartyPart.ADDRESSES)));
		assertNull(getCacheValue(1233455));
	}

	/**
	 * [UNIREG-2587] Vérifie que le cache fonctionne correctement lorsqu'un tiers est demandé successivement <ol> <li>avec ses fors fiscaux virtuels, puis</li> <li>juste avec ses fors fiscaux, et</li>
	 * <li>finalement de nouveau avec ses fors fiscaux virtuels.</li> </ol>
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartySpecialCaseVirtualTaxResidences() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		// 1. on demande le tiers avec les fors fiscaux virtuels
		{
			final Party tiers = cache.getParty(userLogin, ids.monsieur.intValue(), EnumSet.of(PartyPart.VIRTUAL_TAX_RESIDENCES));
			assertNotNull(tiers);
			assertNotNull(tiers.getMainTaxResidences());
			assertEquals(1, tiers.getMainTaxResidences().size());

			final TaxResidence ffp = tiers.getMainTaxResidences().get(0);
			assertEquals(new Date(1989, 5, 1), ffp.getDateFrom());
			assertNull(ffp.getDateTo());
		}

		// 2. on demande le tiers *sans* les fors fiscaux virtuels
		{
			final Party tiers = cache.getParty(userLogin, ids.monsieur.intValue(), EnumSet.of(PartyPart.TAX_RESIDENCES));
			assertNotNull(tiers);
			assertEmpty(tiers.getMainTaxResidences());
		}

		// 3. on demande de nouveau le tiers avec les fors fiscaux virtuels => le résultat doit être identique à la demande du point 1.
		{
			final Party tiers = cache.getParty(userLogin, ids.monsieur.intValue(), EnumSet.of(PartyPart.VIRTUAL_TAX_RESIDENCES));
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

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		// 1. on demande le tiers avec les déclarations et leurs états
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS, PartyPart.TAX_DECLARATIONS_STATUSES));
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
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS));
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
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS, PartyPart.TAX_DECLARATIONS_STATUSES));
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

	/**
	 * La <i>part</i> non-gérée par le cache doit toujours être récupérée depuis le véritable service
	 */
	@Test
	public void testGetPartyOnNonCacheablePart() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		// état initial -> aucun appel au web-service
		assertEquals(0, getNumberOfCalls(calls));

		// parts to ask for
		final Set<PartyPart> parts = EnumSet.of(PartyPart.TAX_RESIDENCES, PartyPart.EBILLING_STATUSES, PartyPart.ADDRESSES);

		// 1er appel
		{
			final Party party = cache.getParty(userLogin, ids.eric.intValue(), parts);
			assertNotNull(party);
			assertInstanceOf(NaturalPerson.class, party);
			assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
			assertEquals(1, party.getMainTaxResidences().size());
			assertEquals(1, party.getOtherTaxResidences().size());
			assertEquals(1, party.getResidenceAddresses().size());

			assertEquals(1, getNumberOfCallsToGetParty(calls));
			final Pair<Integer, Set<PartyPart>> lastCall = getLastCallParametersToGetParty(calls);
			assertEquals((Integer) ids.eric.intValue(), lastCall.getLeft());
			assertEquals(parts, lastCall.getRight());
		}

		// 2ème appel
		{
			final Party party = cache.getParty(userLogin, ids.eric.intValue(), parts);
			assertNotNull(party);
			assertInstanceOf(NaturalPerson.class, party);
			assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
			assertEquals(1, party.getMainTaxResidences().size());
			assertEquals(1, party.getOtherTaxResidences().size());
			assertEquals(1, party.getResidenceAddresses().size());

			assertEquals(2, getNumberOfCallsToGetParty(calls));
			final Pair<Integer, Set<PartyPart>> lastCall = getLastCallParametersToGetParty(calls);
			assertEquals((Integer) ids.eric.intValue(), lastCall.getLeft());
			assertEquals(EnumSet.of(PartyPart.EBILLING_STATUSES), lastCall.getRight());
		}

		// 3ème appel
		{
			final Party party = cache.getParty(userLogin, ids.eric.intValue(), parts);
			assertNotNull(party);
			assertInstanceOf(NaturalPerson.class, party);
			assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
			assertEquals(1, party.getMainTaxResidences().size());
			assertEquals(1, party.getOtherTaxResidences().size());
			assertEquals(1, party.getResidenceAddresses().size());

			assertEquals(3, getNumberOfCallsToGetParty(calls));
			final Pair<Integer, Set<PartyPart>> lastCall = getLastCallParametersToGetParty(calls);
			assertEquals((Integer) ids.eric.intValue(), lastCall.getLeft());
			assertEquals(EnumSet.of(PartyPart.EBILLING_STATUSES), lastCall.getRight());
		}
	}

	/**
	 * [SIFISC-23048] Quand on demande TAX_DECLARATIONS avec les TAX_DECLARATIONS_STATUSES, puis dans un autre appel les mêmes parts + TAX_DECLARATIONS_DEADLINES
	 * alors les états disparaissent de la deuxième réponse...
	 */
	@Test
	public void testRecompositionDonneesEtatsEtDelaisDeclaration() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		// 1. on demande le tiers avec les déclarations et les états
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS, PartyPart.TAX_DECLARATIONS_STATUSES));
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

			// pas demandés -> pas reçus
			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEmpty(delais);
		}

		// 2. on demande maintenant les déclarations, leurs états et leurs délais
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS, PartyPart.TAX_DECLARATIONS_STATUSES, PartyPart.TAX_DECLARATIONS_DEADLINES));
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

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());
		}

		// 3. et finalement on ne demande plus que les délais
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS, PartyPart.TAX_DECLARATIONS_DEADLINES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(0, etats.size());

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());
		}

		// 4. on demande à nouveau le tout
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS, PartyPart.TAX_DECLARATIONS_STATUSES, PartyPart.TAX_DECLARATIONS_DEADLINES));
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

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());
		}
	}

	/**
	 * [SIFISC-23048] Quand on demande TAX_DECLARATIONS avec les TAX_DECLARATIONS_STATUSES, puis dans un autre appel les mêmes parts + TAX_DECLARATIONS_DEADLINES
	 * alors les états disparaissent de la deuxième réponse... (deuxième test avec la part EBILLING_STATUSES en plus, juste pour être sûr)
	 */
	@Test
	public void testRecompositionDonneesEtatsEtDelaisDeclarationAvecEFacture() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		// 1. on demande le tiers avec les déclarations et les états
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS, PartyPart.TAX_DECLARATIONS_STATUSES));
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

			// pas demandés -> pas reçus
			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEmpty(delais);
		}

		// 2. on demande maintenant les déclarations, leurs états et leurs délais (+ efacture, non-cachable)
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS, PartyPart.TAX_DECLARATIONS_STATUSES, PartyPart.TAX_DECLARATIONS_DEADLINES, PartyPart.EBILLING_STATUSES));
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

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());

			final Taxpayer taxpayer = (Taxpayer) tiers;
			final List<EbillingStatus> efacture = taxpayer.getEbillingStatuses();
			assertNotNull(efacture);
			assertEquals(2, efacture.size());
		}

		// 3. et finalement on ne demande plus que les délais
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS, PartyPart.TAX_DECLARATIONS_DEADLINES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			// les états ont disparu
			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(0, etats.size());

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());
		}

		// 4. on demande à nouveau le tout
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_DECLARATIONS, PartyPart.TAX_DECLARATIONS_STATUSES, PartyPart.TAX_DECLARATIONS_DEADLINES, PartyPart.EBILLING_STATUSES));
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

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());

			final Taxpayer taxpayer = (Taxpayer) tiers;
			final List<EbillingStatus> efacture = taxpayer.getEbillingStatuses();
			assertNotNull(efacture);
			assertEquals(2, efacture.size());
		}
	}

	/**
	 * Pour le renvoi des données en JSON, on modifie la donnée retournée par le cache avant de la renvoyer
	 * --> il faut donc vérifier que cette modification n'impacte pas le contenu du cache (autrement dit :
	 * que le cache nous renvoie toujours une copie de son contenu interne)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyModificationCollectionRenvoyeeEtValeurCachee() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		// 1. on demande le tiers avec les assujettissements
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_LIABILITIES));
			assertNotNull(tiers);
			assertEquals(NaturalPerson.class, tiers.getClass());

			final NaturalPerson np = (NaturalPerson) tiers;
			assertNotNull(np.getTaxLiabilities());
			assertEquals(1, np.getTaxLiabilities().size());

			final TaxLiability tl = np.getTaxLiabilities().get(0);
			assertNotNull(tl);
			assertEquals(OrdinaryResident.class, tl.getClass());
			assertEquals(LiabilityChangeReason.MAJORITY, tl.getStartReason());

			// ok, maintenant on modifie la collection des assujettissements en changeant la classe de l'élément (c'est ce que l'on fait
			// dans la manipulation JSON, au final)
			np.getTaxLiabilities().set(0, new ExpenditureBased(tl.getStartReason(), tl.getEndReason(), tl.getDateTo(), tl.getDateFrom(), null));
		}

		// 2. même appel -> rien ne doit avoir changé
		{
			final Party tiers = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_LIABILITIES));
			assertNotNull(tiers);
			assertEquals(NaturalPerson.class, tiers.getClass());

			final NaturalPerson np = (NaturalPerson) tiers;
			assertNotNull(np.getTaxLiabilities());
			assertEquals(1, np.getTaxLiabilities().size());

			final TaxLiability tl = np.getTaxLiabilities().get(0);
			assertNotNull(tl);
			assertEquals(OrdinaryResident.class, tl.getClass());
			assertEquals(LiabilityChangeReason.MAJORITY, tl.getStartReason());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetParties() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		// Etat initial : aucun appel au web-service
		assertEquals(0, getNumberOfCalls(calls));
		final List<Integer> partyNosMonsieurMadame = Arrays.asList(ids.monsieur.intValue(), ids.madame.intValue());

		// 1er appel
		{
			final Parties parties = cache.getParties(userLogin, partyNosMonsieurMadame, null);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());

			// on vérifique que les données retournées sont correctes
			final Entry batch0 = parties.getEntries().get(0);
			final Entry batch1 = parties.getEntries().get(1);
			assertNotNull(batch0.getParty());
			assertNotNull(batch1.getParty());

			final Party monsieur = (batch0.getPartyNo() == ids.monsieur ? batch0.getParty() : batch1.getParty());
			final Party madame = (batch0.getPartyNo() == ids.madame ? batch0.getParty() : batch1.getParty());
			assertNoPart(monsieur);
			assertNoPart(madame);

			// on vérifie qu'il y a bien eu un appel au web-service
			assertEquals(1, getNumberOfCalls(calls));
			assertEquals(1, getNumberOfCallsToGetParties(calls));
			assertEquals(partyNosMonsieurMadame, getLastCallParametersToGetParties(calls).getLeft());
		}

		// 2ème appel : identique au premier
		{
			final Parties parties = cache.getParties(userLogin, partyNosMonsieurMadame, null);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());

			// on vérifique que les données retournées sont correctes
			final Entry batch0 = parties.getEntries().get(0);
			final Entry batch1 = parties.getEntries().get(1);
			assertNotNull(batch0.getParty());
			assertNotNull(batch1.getParty());

			final Party monsieur = (batch0.getPartyNo() == ids.monsieur ? batch0.getParty() : batch1.getParty());
			final Party madame = (batch0.getPartyNo() == ids.madame ? batch0.getParty() : batch1.getParty());
			assertNoPart(monsieur);
			assertNoPart(madame);

			// on vérifie qu'il n'y a pas de second appel au web-service, c'est-à-dire que toutes les données ont été trouvées dans le cache
			assertEquals(1, getNumberOfCalls(calls));
			assertEquals(1, getNumberOfCallsToGetParties(calls));
		}

		// 3ème appel : avec un tiers de plus
		{
			final Parties parties = cache.getParties(userLogin, Arrays.asList(ids.monsieur.intValue(), ids.madame.intValue(), ids.eric.intValue()), null);
			assertNotNull(parties);
			assertEquals(3, parties.getEntries().size());

			// on vérifique que les données retournées sont correctes
			Party monsieur = null;
			Party madame = null;
			Party eric = null;
			for (Entry entry : parties.getEntries()) {
				final Party party = entry.getParty();
				if (entry.getPartyNo() == ids.monsieur) {
					monsieur = party;
				}
				else if (entry.getPartyNo() == ids.madame) {
					madame = party;
				}
				else if (entry.getPartyNo() == ids.eric) {
					eric = party;
				}
				else {
					fail("Le batch contient un numéro de tiers inconnu = [" + entry.getPartyNo() + ']');
				}
			}
			assertNoPart(monsieur);
			assertNoPart(madame);
			assertNoPart(eric);

			// on vérifie qu'il y a un second appel au web-service, mais qu'il ne concerne que le tiers Eric
			assertEquals(2, getNumberOfCalls(calls));
			assertEquals(2, getNumberOfCallsToGetParties(calls));
			assertEquals(Collections.singletonList(ids.eric.intValue()), getLastCallParametersToGetParties(calls).getLeft());
		}
	}

	/**
	 * Le cache est systématiquemene ignoré pour les appels qui font référence à des <i>parts</i> non-gérées par le cache
	 */
	@Test
	public void testGetPartiesOnNonCacheablePart() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		// état initial -> aucun appel au web-service
		assertEquals(0, getNumberOfCalls(calls));

		// parts to ask for
		final EnumSet<PartyPart> parts = EnumSet.of(PartyPart.TAX_RESIDENCES, PartyPart.EBILLING_STATUSES, PartyPart.ADDRESSES);

		// parties to ask those parts on
		final List<Integer> partyNos = Arrays.asList(ids.menage.intValue(), ids.eric.intValue());

		// 1er appel
		{
			final Parties parties = cache.getParties(userLogin, partyNos, parts);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());

			assertEquals(1, getNumberOfCallsToGetParties(calls));
			final Pair<List<Integer>, Set<PartyPart>> lastCall = getLastCallParametersToGetParties(calls);
			assertEquals(partyNos, lastCall.getLeft());
			assertEquals(parts, lastCall.getRight());

			final List<Entry> sortedEntries = new ArrayList<>(parties.getEntries());
			Collections.sort(sortedEntries, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			// eric d'abord
			{
				final Party party = sortedEntries.get(0).getParty();
				assertNotNull(party);
				assertEquals(ids.eric.intValue(), party.getNumber());

				assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}

			// ménage commun ensuite
			{
				final Party party = sortedEntries.get(1).getParty();
				assertNotNull(party);
				assertEquals(ids.menage.intValue(), party.getNumber());

				assertEquals(0, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}
		}

		// 2ème appel
		{
			final Parties parties = cache.getParties(userLogin, partyNos, parts);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());

			assertEquals(2, getNumberOfCallsToGetParties(calls));
			final Pair<List<Integer>, Set<PartyPart>> lastCall = getLastCallParametersToGetParties(calls);
			assertEquals(partyNos, lastCall.getLeft());
			assertEquals(parts, lastCall.getRight());

			final List<Entry> sortedEntries = new ArrayList<>(parties.getEntries());
			Collections.sort(sortedEntries, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			// eric d'abord
			{
				final Party party = sortedEntries.get(0).getParty();
				assertNotNull(party);
				assertEquals(ids.eric.intValue(), party.getNumber());

				assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}

			// ménage commun ensuite
			{
				final Party party = sortedEntries.get(1).getParty();
				assertNotNull(party);
				assertEquals(ids.menage.intValue(), party.getNumber());

				assertEquals(0, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}
		}

		// 3ème appel
		{
			final Parties parties = cache.getParties(userLogin, partyNos, parts);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());

			assertEquals(3, getNumberOfCallsToGetParties(calls));
			final Pair<List<Integer>, Set<PartyPart>> lastCall = getLastCallParametersToGetParties(calls);
			assertEquals(partyNos, lastCall.getLeft());
			assertEquals(parts, lastCall.getRight());

			final List<Entry> sortedEntries = new ArrayList<>(parties.getEntries());
			Collections.sort(sortedEntries, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			// eric d'abord
			{
				final Party party = sortedEntries.get(0).getParty();
				assertNotNull(party);
				assertEquals(ids.eric.intValue(), party.getNumber());

				assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}

			// ménage commun ensuite
			{
				final Party party = sortedEntries.get(1).getParty();
				assertNotNull(party);
				assertEquals(ids.menage.intValue(), party.getNumber());

				assertEquals(0, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}
		}

		// appel sans la part non-gérée -> là le cache intervient sans problème
		{
			final Set<PartyPart> cachedParts = EnumSet.copyOf(parts);
			cachedParts.remove(PartyPart.EBILLING_STATUSES);

			final Parties parties = cache.getParties(userLogin, partyNos, cachedParts);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());
			assertEquals(3, getNumberOfCallsToGetParties(calls));       // <- = pas de nouvel appel, car tout ce qui est demandé est déjà dans le cache

			final List<Entry> sortedEntries = new ArrayList<>(parties.getEntries());
			Collections.sort(sortedEntries, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			// eric d'abord
			{
				final Party party = sortedEntries.get(0).getParty();
				assertNotNull(party);
				assertEquals(ids.eric.intValue(), party.getNumber());

				assertEquals(0, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}

			// ménage commun ensuite
			{
				final Party party = sortedEntries.get(1).getParty();
				assertNotNull(party);
				assertEquals(ids.menage.intValue(), party.getNumber());

				assertEquals(0, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}
		}
	}

	/**
	 * [UNIREG-3288] Vérifie que les exceptions levées dans la méthode getBatchParty sont correctement gérées au niveau du cache.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartiesWithExceptionOnTiers() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		// on intercale une implémentation du web-service qui lèvera une exception lors de la récupération de madame
		cache.setTarget(new BusinessWebServiceCrashingWrapper(implementation, ids.madame.intValue()));

		// 1er appel : monsieur est correctement récupéré et une exception est retournée à la place de madame.
		{
			final Parties batch = cache.getParties(userLogin, Arrays.asList(ids.monsieur.intValue(), ids.madame.intValue()), null);
			assertNotNull(batch);
			assertNotNull(batch.getEntries());
			assertEquals(2, batch.getEntries().size());

			// on vérifie que : monsieur est bien retourné et qu'une exception a été levée sur madame
			final Entry batch0 = batch.getEntries().get(0);
			final Entry batch1 = batch.getEntries().get(1);

			final Entry entryMonsieur = (batch0.getPartyNo() == ids.monsieur ? batch0 : batch1);
			assertNotNull(entryMonsieur.getParty());

			final Entry entryMadame = (batch0.getPartyNo() == ids.madame ? batch0 : batch1);
			assertNull(entryMadame.getParty());
			assertEquals("Boom badaboom !!", entryMadame.getError().getErrorMessage());

			// on vérifie que : seul monsieur est stocké dans le cache et que madame n'y est pas (parce qu'une exception a été levée)
			assertNotNull(getCacheValue(ids.monsieur));
			assertNull(getCacheValue(ids.madame));
		}

		// 2ème appel : identique au premier pour vérifier que le cache est dans un état cohérent (provoquait un crash avant la correction de UNIREG-3288)
		{
			final Parties batch = cache.getParties(userLogin, Arrays.asList(ids.monsieur.intValue(), ids.madame.intValue()), null);
			assertNotNull(batch);
			assertEquals(2, batch.getEntries().size());

			// on vérifie que : monsieur est bien retourné et qu'une exception a été levée sur madame
			final Entry batch0 = batch.getEntries().get(0);
			final Entry batch1 = batch.getEntries().get(1);

			final Entry entryMonsieur = (batch0.getPartyNo() == ids.monsieur ? batch0 : batch1);
			assertNotNull(entryMonsieur.getParty());

			final Entry entryMadame = (batch0.getPartyNo() == ids.madame ? batch0 : batch1);
			assertNull(entryMadame.getParty());
			assertEquals("Boom badaboom !!", entryMadame.getError().getErrorMessage());

			// on vérifie que : seul monsieur est stocké dans le cache et que madame n'y est pas (parce qu'une exception a été levée)
			assertNotNull(getCacheValue(ids.monsieur));
			assertNull(getCacheValue(ids.madame));
		}
	}

	/**
	 * Ce n'est pas parce qu'une personne est dans le cache que tout le monde peut la voir !!!
	 */
	@Test
	public void testGetPartiesEtDroitAcces() throws Exception {

		final UserLogin voyeur = new UserLogin(getDefaultOperateurName(), 21);
		final UserLogin toto = new UserLogin("TOTO", 22);

		final class Ids {
			int pp;
			int dpi;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Constantin", "Dugenou", null, Sexe.MASCULIN);
				addDroitAcces("zai1232", pp, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(2014, 1, 1), null);
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));

				final Ids ids = new Ids();
				ids.pp = pp.getNumero().intValue();
				ids.dpi = dpi.getNumero().intValue();
				return ids;
			}
		});

		// premier appel avec celui qui a le droit de tout voir -> mise en cache ok
		{
			final Parties parties = cache.getParties(voyeur, Arrays.asList(ids.pp, ids.dpi), null);
			assertNotNull(parties);
			assertNotNull(parties.getEntries());
			assertEquals(2, parties.getEntries().size());

			assertNotNull(getCacheValue(ids.pp));
		}

		// deuxième appel avec celui qui n'a pas le droit de voir -> devrait être bloqué
		{
			final Parties parties = cache.getParties(toto, Arrays.asList(ids.pp, ids.dpi), null);
			assertNotNull(parties);
			assertNotNull(parties.getEntries());
			assertEquals(2, parties.getEntries().size());

			final List<Entry> entries = new ArrayList<>(parties.getEntries());
			Collections.sort(entries, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			{
				final Entry e = entries.get(0);
				assertEquals(ids.dpi, e.getPartyNo());
				assertNull(e.getError());
				assertNotNull(e.getParty());
				assertEquals(ids.dpi, e.getParty().getNumber());
			}
			{
				final Entry e = entries.get(1);
				assertEquals(ids.pp, e.getPartyNo());
				assertNull(e.getParty());
				assertNotNull(e.getError());
				assertEquals("L'utilisateur UserLogin{userId='TOTO', oid=22} ne possède aucun droit de lecture sur le dossier " + ids.pp, e.getError().getErrorMessage());
			}
		}
	}

	/**
	 * [SIFISC-5508] Vérifie que la date de début d'activité (activityStartDate) est bien cachée correctement.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyActivityStartDate() throws Exception {

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		// 1. on demande le tiers une première fois
		{
			final Party party = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_RESIDENCES));
			assertNotNull(party);
			assertEquals(new Date(1983, 4, 13), party.getActivityStartDate());
			assertNull(party.getActivityEndDate());
		}

		// 2. on demande le tiers une seconde fois
		{
			final Party party = cache.getParty(userLogin, ids.eric.intValue(), EnumSet.of(PartyPart.TAX_RESIDENCES));
			assertNotNull(party);
			assertEquals(new Date(1983, 4, 13), party.getActivityStartDate()); // [SIFISC-5508] cette date était nulle avant la correction du bug
			assertNull(party.getActivityEndDate());
		}
	}

	/**
	 * [SIFISC-20870] Problème vu en production, une java.util.ConcurrentModificationException qui saute pendant la sérialisation CXF de réponse du WS
	 */
	@Test
	public void testConcurrentAccessGetParty() throws Exception {

		// on va construire un contribuable avec toutes ses collections contenant quelque chose,
		// puis on va interroger depuis plusieurs threads le contribuable, avec un nombre de parts aléatoire à chaque fois...

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				// Un tiers avec avec toutes les parties renseignées
				final PersonnePhysique eric = addNonHabitant("Eric", "de Melniboné", date(1965, 4, 13), Sexe.MASCULIN);
				addAdresseSuisse(eric, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
				eric.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH9308440717427290198", null));

				final PersonnePhysique pupille = addNonHabitant("Slobodan", "Pupille", date(1987, 7, 23), Sexe.MASCULIN);
				addTutelle(pupille, eric, null, date(2005, 7, 1), null);

				final SituationFamillePersonnePhysique situation = new SituationFamillePersonnePhysique();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setNombreEnfants(0);
				eric.addSituationFamille(situation);

				final PeriodeFiscale periode = addPeriodeFiscale(2004);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				ch.vd.unireg.declaration.DeclarationImpotOrdinaire di = addDeclarationImpot(eric, periode, date(2004, 1, 1), date(2004, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(di, date(2004, 1, 10));

				return eric.getNumero();
			}
		});

		final Random rnd = new Random();
		final ch.vd.unireg.ws.party.v1.ObjectFactory partyFactory = new ch.vd.unireg.ws.party.v1.ObjectFactory();
		final JAXBContext jaxbContext = JAXBContext.newInstance(NaturalPerson.class);

		// lien direct entre le cache et l'implémentation, sans tracing
		cache.setTarget(getBean(BusinessWebService.class, "wsv5Business"));

		// classe de tâche en parallèle...
		final class Task implements Callable<Object> {

			private final Set<PartyPart> parts = buildRandomPartSet();

			/**
			 * Construction d'un ensemble de parts prises au hasard
			 */
			private Set<PartyPart> buildRandomPartSet() {
				final PartyPart[] all = PartyPart.values();
				final int nb = 3;
				final Set<PartyPart> set = EnumSet.noneOf(PartyPart.class);
				for (int i = 0; i < nb ; ++ i) {
					final int index = rnd.nextInt(all.length);
					set.add(all[index]);
				}
				return set;
			}

			@Override
			public Object call() throws Exception {
				// récupération des données + sérialisation
				final Party party = cache.getParty(userLogin, (int) id, parts);
				final Marshaller marshaller = jaxbContext.createMarshaller();
				try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					marshaller.marshal(partyFactory.createParty(party), out);
				}
				return null;
			}
		}

		// quelques threads, quelques appels
		final int nbThreads = 100;
		final int nbCalls = 5000;

		// executors
		final ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
		try {
			final ExecutorCompletionService<Object> completionService = new ExecutorCompletionService<>(executor);
			for (int i = 0 ; i < nbCalls ; ++ i) {
				completionService.submit(new Task());
			}
			executor.shutdown();
			for (int i = 0 ; i < nbCalls ; ++ i) {
				while (true) {
					final Future<Object> future = completionService.poll(1, TimeUnit.SECONDS);
					if (future != null) {
						future.get();
						break;
					}
				}
			}
		}
		finally {
			executor.shutdownNow();
			while (!executor.isTerminated()) {
				executor.awaitTermination(1, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * [SIFISC-20870] Problème vu en production, une java.util.ConcurrentModificationException qui saute pendant la sérialisation CXF de réponse du WS
	 */
	@Test
	public void testConcurrentAccessGetParties() throws Exception {

		// on va construire un contribuable avec toutes ses collections contenant quelque chose,
		// puis on va interroger depuis plusieurs threads le contribuable, avec un nombre de parts aléatoire à chaque fois...

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 21);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				// Un tiers avec avec toutes les parties renseignées
				final PersonnePhysique eric = addNonHabitant("Eric", "de Melniboné", date(1965, 4, 13), Sexe.MASCULIN);
				addAdresseSuisse(eric, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
				eric.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH9308440717427290198", null));

				final PersonnePhysique pupille = addNonHabitant("Slobodan", "Pupille", date(1987, 7, 23), Sexe.MASCULIN);
				addTutelle(pupille, eric, null, date(2005, 7, 1), null);

				final SituationFamillePersonnePhysique situation = new SituationFamillePersonnePhysique();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setNombreEnfants(0);
				eric.addSituationFamille(situation);

				final PeriodeFiscale periode = addPeriodeFiscale(2004);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				ch.vd.unireg.declaration.DeclarationImpotOrdinaire di = addDeclarationImpot(eric, periode, date(2004, 1, 1), date(2004, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(di, date(2004, 1, 10));

				return eric.getNumero();
			}
		});

		final Random rnd = new Random();
		final JAXBContext jaxbContext = JAXBContext.newInstance(Parties.class);

		// lien direct entre le cache et l'implémentation, sans tracing
		cache.setTarget(getBean(BusinessWebService.class, "wsv5Business"));

		// classe de tâche en parallèle...
		final class Task implements Callable<Object> {

			private final Set<PartyPart> parts = buildRandomPartSet();

			/**
			 * Construction d'un ensemble de parts prises au hasard
			 */
			private Set<PartyPart> buildRandomPartSet() {
				final PartyPart[] all = PartyPart.values();
				final int nb = 3;
				final Set<PartyPart> set = EnumSet.noneOf(PartyPart.class);
				for (int i = 0; i < nb ; ++ i) {
					final int index = rnd.nextInt(all.length);
					set.add(all[index]);
				}
				return set;
			}

			@Override
			public Object call() throws Exception {
				// récupération des données + sérialisation
				final Parties parties = cache.getParties(userLogin, Collections.singletonList((int) id), parts);
				final Marshaller marshaller = jaxbContext.createMarshaller();
				try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					marshaller.marshal(parties, out);
				}
				return null;
			}
		}

		// quelques threads, quelques appels
		final int nbThreads = 100;
		final int nbCalls = 5000;

		// executors
		final ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
		try {
			final ExecutorCompletionService<Object> completionService = new ExecutorCompletionService<>(executor);
			for (int i = 0 ; i < nbCalls ; ++ i) {
				completionService.submit(new Task());
			}
			executor.shutdown();
			for (int i = 0 ; i < nbCalls ; ++ i) {
				while (true) {
					final Future<Object> future = completionService.poll(1, TimeUnit.SECONDS);
					if (future != null) {
						future.get();
						break;
					}
				}
			}
		}
		finally {
			executor.shutdownNow();
			while (!executor.isTerminated()) {
				executor.awaitTermination(1, TimeUnit.SECONDS);
			}
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

	private DebtorInfo getCacheValue(int debtorId, int pf) {
		DebtorInfo value = null;
		final GetDebtorInfoKey key = new GetDebtorInfoKey(debtorId, pf);
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

		boolean checkAddresses = ADDRESSES == p;
		boolean checkTaxLiabilities = TAX_LIABILITIES == p;
		boolean checkSimplifiedTaxLiabilities = SIMPLIFIED_TAX_LIABILITIES == p;
		boolean checkHouseholdMembers = HOUSEHOLD_MEMBERS == p;
		boolean checkBankAccounts = BANK_ACCOUNTS == p;
		boolean checkTaxDeclarations = TAX_DECLARATIONS == p;
		boolean checkTaxDeclarationsStatuses = TAX_DECLARATIONS_STATUSES == p;
		boolean checkTaxDeclarationsDeadlines = TAX_DECLARATIONS_DEADLINES == p;
		boolean checkTaxResidences = TAX_RESIDENCES == p;
		boolean checkVirtualTaxResidences = VIRTUAL_TAX_RESIDENCES == p;
		boolean checkManagingTaxResidences = MANAGING_TAX_RESIDENCES == p;
		boolean checkTaxationPeriods = TAXATION_PERIODS == p;
		boolean checkRelationsBetweenParties = RELATIONS_BETWEEN_PARTIES == p;
		boolean checkFamilyStatuses = FAMILY_STATUSES == p;
		boolean checkCapitals = CAPITALS == p;
		boolean checkCorporationStatuses = CORPORATION_STATUSES == p;
		boolean checkLegalForms = LEGAL_FORMS == p;
		boolean checkTaxSystems = TAX_SYSTEMS == p;
		boolean checkLegalSeats = LEGAL_SEATS == p;
		boolean checkDebtorPeriodicities = DEBTOR_PERIODICITIES == p;
		boolean checkImmovableProperties = IMMOVABLE_PROPERTIES == p; // [SIFISC-26536] la part IMMOVABLE_PROPERTIES est dépréciée et n'a aucun effet
		boolean checkChildren = CHILDREN == p;
		boolean checkParents = PARENTS == p;
		boolean checkWithholdingTaxDeclarationPeriods = WITHHOLDING_TAXATION_PERIODS == p;
		boolean checkEbillingStatuses = EBILLING_STATUSES == p;
		assertTrue("La partie [" + p + "] est inconnue", checkAddresses || checkTaxLiabilities || checkHouseholdMembers || checkBankAccounts || checkTaxDeclarations || checkTaxDeclarationsStatuses || checkTaxDeclarationsDeadlines
				|| checkTaxResidences || checkVirtualTaxResidences || checkManagingTaxResidences || checkTaxationPeriods || checkRelationsBetweenParties || checkFamilyStatuses || checkCapitals
				|| checkCorporationStatuses || checkLegalForms || checkTaxSystems || checkLegalSeats || checkDebtorPeriodicities || checkSimplifiedTaxLiabilities || checkImmovableProperties ||
				checkChildren || checkParents || checkWithholdingTaxDeclarationPeriods || checkEbillingStatuses);

		assertNullOrNotNull(checkAddresses, tiers.getMailAddresses(), "mailAddresses");
		assertNullOrNotNull(checkAddresses, tiers.getResidenceAddresses(), "residenceAddresses");
		assertNullOrNotNull(checkAddresses, tiers.getDebtProsecutionAddresses(), "debtProsecutionAddresses");
		assertNullOrNotNull(checkAddresses, tiers.getRepresentationAddresses(), "representationAddresses");
		assertNullOrNotNull(checkBankAccounts, tiers.getBankAccounts(), "bankAccounts");
		assertNullOrNotNull(checkTaxResidences || checkVirtualTaxResidences, tiers.getMainTaxResidences(), "mainTaxResidences");
		assertNullOrNotNull(checkTaxResidences || checkVirtualTaxResidences, tiers.getOtherTaxResidences(), "otherTaxResidences");
		assertNullOrNotNull(checkManagingTaxResidences, tiers.getManagingTaxResidences(), "managingTaxResidences");
		assertNullOrNotNull(checkRelationsBetweenParties || checkChildren || checkParents, tiers.getRelationsBetweenParties(), "relationsBetweenParties (" + p + ')');

		if (tiers instanceof Taxpayer) {
			final Taxpayer ctb = (Taxpayer) tiers;
			assertNullOrNotNull(checkTaxLiabilities, ctb.getTaxLiabilities(), "taxLiabilities");
			assertNullOrNotNull(checkSimplifiedTaxLiabilities, ctb.getSimplifiedTaxLiabilityVD(), "simplifiedTaxLiabilityVD");
			assertNullOrNotNull(checkSimplifiedTaxLiabilities, ctb.getSimplifiedTaxLiabilityCH(), "simplifiedTaxLiabilityCH");
			assertNullOrNotNull(checkTaxDeclarations || checkTaxDeclarationsStatuses || checkTaxDeclarationsDeadlines, ctb.getTaxDeclarations(), "taxDeclarations");
			assertNullOrNotNull(checkTaxationPeriods, ctb.getTaxationPeriods(), "taxationPeriods");
			assertNullOrNotNull(checkFamilyStatuses, ctb.getFamilyStatuses(), "familyStatuses");
			assertEmpty(ctb.getImmovableProperties());
			assertNullOrNotNull(checkEbillingStatuses, ctb.getEbillingStatuses(), "ebillingStatuses");
		}

		if (tiers instanceof CommonHousehold) {
			final CommonHousehold mc = (CommonHousehold) tiers;
			assertNullOrNotNull(checkHouseholdMembers, mc.getMainTaxpayer(), "mainTaxpayer");
			assertNullOrNotNull(checkHouseholdMembers, mc.getSecondaryTaxpayer(), "secondaryTaxpayer");
		}

		if (tiers instanceof Corporation) {
			final Corporation pm = (Corporation) tiers;
			assertNullOrNotNull(checkCapitals, pm.getCapitals(), "capitals");
			assertNullOrNotNull(checkCorporationStatuses, pm.getStatuses(), "statuses");
			assertNullOrNotNull(checkLegalForms, pm.getLegalForms(), "legalForms");
			assertNullOrNotNull(checkTaxSystems, pm.getTaxSystemsVD(), "taxSystemsVD");
			assertNullOrNotNull(checkTaxSystems, pm.getTaxSystemsCH(), "taxSystemsCH");
			assertNullOrNotNull(checkLegalSeats, pm.getLegalSeats(), "legalSeats");
		}

		if (tiers instanceof NaturalPerson) {
			final NaturalPerson np = (NaturalPerson) tiers;
			assertNullOrNotNull(checkWithholdingTaxDeclarationPeriods, np.getWithholdingTaxationPeriods(), "withholdingTaxDelarationPeriods");
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
