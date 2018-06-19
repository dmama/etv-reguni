package ch.vd.unireg.indexer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseCivile;
import ch.vd.unireg.adresse.AdresseEtrangere;
import ch.vd.unireg.adresse.AdresseServiceImpl;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.adresse.LocaliteInvalideMatcherService;
import ch.vd.unireg.adresse.LocaliteInvalideMatcherServiceImpl;
import ch.vd.unireg.avatar.AvatarServiceImpl;
import ch.vd.unireg.cache.ServiceCivilCacheWarmerImpl;
import ch.vd.unireg.common.Constants;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.indexer.tiers.DebiteurPrestationImposableIndexable;
import ch.vd.unireg.indexer.tiers.HabitantIndexable;
import ch.vd.unireg.indexer.tiers.MenageCommunIndexable;
import ch.vd.unireg.indexer.tiers.NonHabitantIndexable;
import ch.vd.unireg.indexer.tiers.TiersIndexable;
import ch.vd.unireg.indexer.tiers.TiersIndexableData;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.organisation.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceEntreprise;
import ch.vd.unireg.metier.assujettissement.AssujettissementServiceImpl;
import ch.vd.unireg.regimefiscal.RegimeFiscalServiceImpl;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.ContactImpotSource;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.MockTiersDAO;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.TiersServiceImpl;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.ValidationServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ContribuableIndexableTest extends WithoutSpringTest {

	//private static final Logger LOGGER = LoggerFactory.getLogger(ContribuableIndexableTest.class);

	private static final long noIndPhilippeMaillard = 3333L;
	private static final long noIndGladysMaillard = 3334L;

	private AssujettissementServiceImpl assujettissementService;
	private AdresseServiceImpl adresseService;
	private TiersServiceImpl tiersService;
	private AvatarServiceImpl avatarService;
	private ProxyServiceCivil serviceCivil;
	private ProxyServiceEntreprise serviceEntreprise;
	private ServiceInfrastructureService serviceInfra;
	private MockTiersDAO tiersDAO;
	private LocaliteInvalideMatcherService localiteInvalideMatcherService;
	private RegimeFiscalServiceImpl regimeFiscalService;

	@Override
	public void onSetUp() throws Exception {

		serviceCivil = new ProxyServiceCivil(serviceInfra);
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu charlesPoncet = addIndividu(7643L, RegDate.get(1965, 3, 12), "Poncet", "Charles", true);
				addFieldsIndividu(charlesPoncet, "01234567", "123.43.765.543", "");

				final MockIndividu marcelMeignier = addIndividu(1234L, RegDate.get(1972, 1, 27), "MEIGNIER", "Marcel", true);
				addAdresse(marcelMeignier, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, RegDate.get(1964, 12, 2), null);
				addAdresse(marcelMeignier, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, RegDate.get(1964, 12, 2), null);

				final MockIndividu philippeMaillard = addIndividu(noIndPhilippeMaillard, RegDate.get(1956, 1, 21), "Maillard", "Philippe", true);
				philippeMaillard.setNoAVS11("123.45.678");

				final MockIndividu gladysMaillard = addIndividu(noIndGladysMaillard, RegDate.get(1967, 12, 3), "Maillard-Gallet", "Gladys", false);
				gladysMaillard.setNoAVS11("987.65.432");

				addIndividu(123L, RegDate.get(1970, 1, 1), "Dupont", "Philippe", true);
				addIndividu(4567L, RegDate.get(1970, 1, 1), "Dupond", "Arnold", true);
			}
		});

		serviceEntreprise = new ProxyServiceEntreprise(serviceInfra);
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createSimpleEntrepriseRC(784512L, 7845121001L, "Pittet Levage S.A.R.L", date(1924, 4, 1), null,
				                                                                                FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, MockCommune.Lausanne, "CHE123456788");
				addEntreprise(ent);

				addEntreprise(MockEntrepriseFactory.BCV);
			}
		});

		tiersDAO = new MockTiersDAO();

		final ServiceCivilCacheWarmerImpl warmer = new ServiceCivilCacheWarmerImpl();
		warmer.setServiceCivilService(serviceCivil);
		warmer.setTiersDAO(tiersDAO);

		assujettissementService = new AssujettissementServiceImpl();
		assujettissementService.setTiersService(tiersService);
		assujettissementService.setValidationService(new ValidationServiceImpl());
		assujettissementService.afterPropertiesSet();

		serviceInfra = new ServiceInfrastructureImpl(new DefaultMockServiceInfrastructureService(), tiersDAO);

		regimeFiscalService = new RegimeFiscalServiceImpl();
		regimeFiscalService.setServiceInfra(serviceInfra);

		tiersService = new TiersServiceImpl();
		tiersService.setServiceInfra(serviceInfra);
		tiersService.setServiceCivilService(serviceCivil);
		tiersService.setServiceCivilCacheWarmer(warmer);
		tiersService.setTiersDAO(tiersDAO);
		tiersService.setServiceEntreprise(serviceEntreprise);
		tiersService.setValidationService(null);
		tiersService.setAssujettissementService(assujettissementService);
		tiersService.setRegimeFiscalService(regimeFiscalService);

		avatarService = new AvatarServiceImpl();
		avatarService.setTiersService(tiersService);

		localiteInvalideMatcherService = new LocaliteInvalideMatcherServiceImpl();

		adresseService = new AdresseServiceImpl();
		adresseService.setServiceInfra(serviceInfra);
		adresseService.setServiceCivilService(serviceCivil);
		adresseService.setTiersService(tiersService);
		adresseService.setServiceEntreprise(serviceEntreprise);
		adresseService.setLocaliteInvalideMatcherService(localiteInvalideMatcherService);
	}


	@Test
	public void testMenageCommunMarieSeul() throws Exception {

		PersonnePhysique hab1 = new PersonnePhysique(true);
		hab1.setNumero(123L);
		hab1.setNumeroIndividu(noIndPhilippeMaillard);

		MenageCommun mc = new MenageCommun();
		mc.setNumero(2345L);
		addTiers(mc, hab1, RegDate.get(2001, 2, 23));

		MenageCommunIndexable indexable = new MenageCommunIndexable(adresseService, tiersService, assujettissementService, serviceCivil, serviceInfra, avatarService, mc);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// Search
		//assertContains(numCtb1.toString(), values.get(TiersIndexableData.NUMEROS));
		assertContains("Maillard", values.getNomRaison());
		assertEquals(Collections.singletonList(date(1956, 1, 21)), values.getDatesNaissanceInscriptionRC()); // [UNIREG-2633]
		assertContains(FormatNumeroHelper.formatAncienNumAVS("123.45.678"), values.getNavs11());
		// Display
		assertContains("Maillard", values.getNom1());
		assertContains("Philippe", values.getNom1());
		assertEquals(null, values.getNom2());
	}

	@Test
	public void testNonHabitantIndexable() throws Exception {

		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNumero(1234L);
		nonHab.setDateNaissance(RegDate.get(1965, 3, 12));
		nonHab.setNom("Poncet");
		nonHab.setPrenomUsuel("Charles");
		nonHab.setNumeroAssureSocial("432.23.654.345");
		nonHab.setAdresseCourrierElectronique(null); // Volontaire, on teste que on recoit chaine vide pour une valeur null

		AdresseSuisse a = new AdresseSuisse();
		a.setDateDebut(RegDate.get(2002, 2, 23));
		a.setNumeroOrdrePoste(283);
		a.setUsage(TypeAdresseTiers.COURRIER);
		Set<AdresseTiers> adresses = new HashSet<>();
		adresses.add(a);
		nonHab.setAdressesTiers(adresses);

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, nonHab);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(NonHabitantIndexable.SUB_TYPE, indexable.getSubType());

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertEquals(Collections.singletonList(nonHab.getDateNaissance()), values.getDatesNaissanceInscriptionRC());
		assertEquals("Suisse", values.getPays());
		assertEquals(Collections.singletonList(date(1965, 3, 12)), values.getDatesNaissanceInscriptionRC());
		assertEquals(IndexerFormatHelper.nullValue(), values.getDateDeces());
	}

	@Test
	public void testTiersACIIndexable() throws Exception {
		Individu individu = serviceCivil.getIndividu(7643L, RegDate.get(2007, 12, 31));

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(12348L);
		hab.setNumeroIndividu(individu.getNoTechnique());

		HabitantIndexable indexable = new HabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, hab, individu);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(HabitantIndexable.SUB_TYPE, indexable.getSubType());

		// Ctb
		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertContains(hab.getNumero().toString(), values.getNumeros());
		assertContains("Contribuable PP", values.getRoleLigne1());

		// Individu
		assertEquals(Collections.singletonList(individu.getDateNaissance()), values.getDatesNaissanceInscriptionRC());
		assertEquals(String.format("%s %s", individu.getPrenomUsuel(), individu.getNom()), values.getNom1());
		assertContains(IndexerFormatHelper.noAvsToString(individu.getNouveauNoAVS()), values.getNavs13());
	}

	@Test
	public void testHabitantIndexable() throws Exception {
		Individu individu = serviceCivil.getIndividu(7643L, RegDate.get(2007, 12, 31));

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(12348L);
		hab.setNumeroIndividu(individu.getNoTechnique());
		ForFiscalPrincipalPP ff = new ForFiscalPrincipalPP();
		ff.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ff.setMotifRattachement(MotifRattachement.DOMICILE);
		ff.setModeImposition(ModeImposition.SOURCE);
		ff.setDateDebut(RegDate.get(2005, 2, 2));
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(5586);
		hab.addForFiscal(ff);

		HabitantIndexable indexable = new HabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, hab, individu);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(HabitantIndexable.SUB_TYPE, indexable.getSubType());

		// Ctb
		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertContains(hab.getNumero().toString(), values.getNumeros());
		assertContains("Contribuable PP", values.getRoleLigne1());
		assertContains("source", values.getRoleLigne2());

		// Individu
		assertEquals(Collections.singletonList(individu.getDateNaissance()), values.getDatesNaissanceInscriptionRC());
		assertEquals(String.format("%s %s", individu.getPrenomUsuel(), individu.getNom()), values.getNom1());
		assertContains(IndexerFormatHelper.noAvsToString(individu.getNouveauNoAVS()), values.getNavs13());
	}

	@Test
	public void testDebiteurImpotSourceHabitantIndexable() throws Exception {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(1234L);
		hab.setNumeroIndividu(7643L);
		tiersDAO.save(hab);
		Individu ind = serviceCivil.getIndividu(hab.getNumeroIndividu(), null);

		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(23348L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi.setComplementNom("Service bidon");
		tiersDAO.save(dpi);

		ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, hab, dpi);
		hab.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, assujettissementService, serviceCivil, serviceEntreprise, serviceInfra, avatarService, dpi);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(DebiteurPrestationImposableIndexable.SUB_TYPE, indexable.getSubType());

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		assertContains(dpi.getNumero().toString(), values.getNumeros());
		// On ne doit pas pouvoir rechercher sur le NO_IND
		assertNotContains(hab.getNumeroIndividu().toString(), values.getNumeros());
		assertContains("Débiteur IS", values.getRoleLigne1());
		assertContains("Réguliers", values.getRoleLigne2());
		assertContains(ind.getNom(), values.getNomRaison());
		assertContains(ind.getNom(), values.getAutresNom());
		assertContains(ind.getPrenomUsuel(), values.getAutresNom());

		// Display (quel que soit le nom1 et nom2, si le débiteur a un contact impôt source, sa raison sociale est tirée de là)
		assertEquals(String.format("%s %s", ind.getPrenomUsuel(), ind.getNom()), values.getNom1());
		assertNull(values.getNom2());
	}

	@Test
	public void testDebiteurImpotSourceNonHabitantIndexable() throws Exception {

		PersonnePhysique nhab = new PersonnePhysique(false);
		nhab.setNumero(1234L);
		nhab.setNom("Bli");
		nhab.setPrenomUsuel("Bla");
		tiersDAO.save(nhab);

		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(23348L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		tiersDAO.save(dpi);

		ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, nhab, dpi);
		nhab.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, assujettissementService, serviceCivil, serviceEntreprise, serviceInfra, avatarService, dpi);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(DebiteurPrestationImposableIndexable.SUB_TYPE, indexable.getSubType());

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		// Search
		assertContains(dpi.getNumero().toString(), values.getNumeros());
		assertContains("Débiteur IS", values.getRoleLigne1());
		assertContains("Réguliers", values.getRoleLigne2());
		assertContains(nhab.getNom(), values.getNomRaison());
		assertContains(nhab.getNom(), values.getAutresNom());
		assertContains(nhab.getPrenomUsuel(), values.getAutresNom());

		// Display (quel que soit le nom1 et nom2, si le débiteur a un contact impôt source, sa raison sociale est tirée de là)
		assertEquals("Bla Bli", values.getNom1());
		assertNull(values.getNom2());
	}

	@Test
	public void testDebiteurImpotSourceEntrepriseIndexable() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.setNumeroEntreprise(MockEntrepriseFactory.BCV.getNumeroEntreprise());
		entreprise.setNumero(20222L);
		tiersDAO.save(entreprise);

		final DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(1500003L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi.setComplementNom("Service bidon");
		tiersDAO.save(dpi);

		final ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, entreprise, dpi);
		entreprise.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		final DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, assujettissementService, serviceCivil, serviceEntreprise, serviceInfra, avatarService, dpi);
		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(DebiteurPrestationImposableIndexable.SUB_TYPE, indexable.getSubType());

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// Search
		assertContains(dpi.getNumero().toString(), values.getNumeros());
		assertContains("Débiteur IS", values.getRoleLigne1());
		assertContains("Réguliers", values.getRoleLigne2());
		assertContains("Banque Cantonale Vaudoise", values.getNomRaison());

		// Display (quel que soit le nom1 et nom2, si le débiteur a un contact impôt source, sa raison sociale est tirée de là)
		assertEquals("Banque Cantonale Vaudoise", values.getNom1());
		assertNull(values.getNom2());
	}

	@Test
	public void testDebiteurImpotSourceAutreCommuncauteIndexable() throws Exception {

		AutreCommunaute ac = new AutreCommunaute();
		ac.setNumero(1234L);
		ac.setNom("Nestle SA");
		ac.setComplementNom("Filiale de Orbe");
		tiersDAO.save(ac);

		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(23348L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi.setComplementNom("Service bidon");
		tiersDAO.save(dpi);

		ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ac, dpi);
		ac.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, assujettissementService, serviceCivil, serviceEntreprise, serviceInfra, avatarService, dpi);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(DebiteurPrestationImposableIndexable.SUB_TYPE, indexable.getSubType());

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// Search
		assertContains(dpi.getNumero().toString(), values.getNumeros());
		assertContains("Débiteur IS", values.getRoleLigne1());
		assertContains("Réguliers", values.getRoleLigne2());
		assertContains(ac.getNom(), values.getNomRaison());
		assertContains(ac.getComplementNom(), values.getNomRaison());

		// Display (quel que soit le nom1 et nom2, si le débiteur a un contact impôt source, sa raison sociale est tirée de là)
		assertEquals("Nestle SA", values.getNom1());
		assertEquals("Filiale de Orbe", values.getNom2());
	}

	@Test
	public void testContribuableAvecForOuvert() throws Exception {

		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNumero(1234L);
		nonHab.setNom("Poulain");

		// For fiscaux

		RegDate dateOuverture = RegDate.get(2004, 2, 1);
		// Principal 1 fermé
		{
			ForFiscalPrincipalPP forF = new ForFiscalPrincipalPP();
			forF.setDateDebut(RegDate.get(1998, 3, 1));
			forF.setDateFin(RegDate.get(2004, 1, 31));
			forF.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forF.setNumeroOfsAutoriteFiscale(5586); // Lausanne
			forF.setModeImposition(ModeImposition.ORDINAIRE);
			forF.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forF.setMotifRattachement(MotifRattachement.DOMICILE);
			nonHab.addForFiscal(forF);
		}
		// Principal 2 ouvert
		{
			ForFiscalPrincipalPP forF = new ForFiscalPrincipalPP();
			forF.setNumeroOfsAutoriteFiscale(8001);
			forF.setDateDebut(dateOuverture);
			forF.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forF.setNumeroOfsAutoriteFiscale(8001); // Le Brassus
			forF.setModeImposition(ModeImposition.ORDINAIRE);
			forF.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forF.setMotifRattachement(MotifRattachement.DOMICILE);
			nonHab.addForFiscal(forF);
		}
		// Secondaire 1 fermé
		{
			ForFiscalSecondaire forF = new ForFiscalSecondaire();
			forF.setDateDebut(RegDate.get(1999, 11, 29));
			forF.setDateFin(RegDate.get(2003, 2, 28));
			forF.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forF.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forF.setNumeroOfsAutoriteFiscale(5761); // Romainmotier-Envy
			forF.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
			nonHab.addForFiscal(forF);
		}
		// Secondaire 2 ouvert
		{
			ForFiscalSecondaire forF = new ForFiscalSecondaire();
			forF.setDateDebut(RegDate.get(2003, 3, 1));
			forF.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forF.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forF.setNumeroOfsAutoriteFiscale(5873); // Le Lieu
			forF.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
			nonHab.addForFiscal(forF);
		}

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, nonHab);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// Search
		assertEquals("8001", values.getNoOfsForPrincipal());
		assertContainsNoCase("5586", values.getNosOfsAutresFors());
		assertContainsNoCase("5873", values.getNosOfsAutresFors());
		assertContainsNoCase("5761", values.getNosOfsAutresFors());
		assertContainsNoCase("8001", values.getNosOfsAutresFors());

		// Display
		assertEquals("Le Brassus", values.getForPrincipal());
		assertEquals(RegDateHelper.toIndexString(dateOuverture), values.getDateOuvertureFor());
		assertEquals(IndexerFormatHelper.nullValue(), values.getDateFermetureFor());
	}

	@Test
	public void testContribuableAvecForFerme() throws Exception {

		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNumero(1234L);
		nonHab.setNom("Poulain");

		// For fiscaux

		RegDate dateOuverture = RegDate.get(1998, 3, 1);
		RegDate dateFermeture = RegDate.get(2004, 1, 31);

		// Principal 1 fermé
		{
			ForFiscalPrincipalPP forF = new ForFiscalPrincipalPP();
			forF.setDateDebut(dateOuverture);
			forF.setDateFin(dateFermeture);
			forF.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forF.setNumeroOfsAutoriteFiscale(5477);
			forF.setModeImposition(ModeImposition.ORDINAIRE);
			forF.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forF.setMotifRattachement(MotifRattachement.DOMICILE);
			nonHab.addForFiscal(forF);
		}

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, nonHab);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// for principal fermé -> tout null

		// Search
		assertNull(values.getNoOfsForPrincipal());
		// Display
		assertEquals("Cossonay", values.getForPrincipal());
		assertEquals(RegDateHelper.toIndexString(dateOuverture), values.getDateOuvertureFor());
		assertEquals(RegDateHelper.toIndexString(dateFermeture), values.getDateFermetureFor());
	}

	@Test
	public void testContribuableAvecAdressesSuisse() throws Exception {

		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNumero(1234L);
		nonHab.setNom("Poulain");

		// Ajout des adresses
		Set<AdresseTiers> adrs = new HashSet<>();

		// Adresse domicile
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setNumeroMaison("12");
			adresse.setNumeroOrdrePoste(528); // Cossonay
			adresse.setNumeroRue(1131419);
			adresse.setUsage(TypeAdresseTiers.DOMICILE);
			adresse.setDateDebut(RegDate.get(2001, 6, 21));
			adresse.setTiers(nonHab);
			adrs.add(adresse);
		}
		// Ancienne adresse courrier
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setNumeroMaison("12");
			adresse.setNumeroOrdrePoste(528); // Cossonay
			adresse.setNumeroRue(1131419);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setDateDebut(RegDate.get(2001, 6, 21));
			adresse.setDateFin(RegDate.get(2005, 11, 30));
			adresse.setTiers(nonHab);
			adrs.add(adresse);
		}
		// Adresse courrier courante
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setNumeroMaison("17b");
			adresse.setNumeroOrdrePoste(152); // 1005 Lausanne
			adresse.setNumeroRue(1133753);
			adresse.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setDateDebut(RegDate.get(2005, 12, 1));
			adresse.setTiers(nonHab);
			adrs.add(adresse);
		}
		// Adresse poursuite
		{
			AdresseEtrangere adresse = new AdresseEtrangere();
			adresse.setNumeroMaison("17b");
			adresse.setNumeroOfsPays(8000);
			adresse.setNumeroPostalLocalite("65123");
			adresse.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
			adresse.setUsage(TypeAdresseTiers.REPRESENTATION);
			adresse.setDateDebut(RegDate.get(2002, 1, 15));
			adresse.setTiers(nonHab);
			adrs.add(adresse);
		}

		nonHab.setAdressesTiers(adrs);

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, nonHab);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		assertEquals("1005", values.getNpaCourrier());
		assertEquals("1005 1304", values.getNpaTous());      // courrier, domicile, poursuite (absent car étranger), représentation (absent car défaut)
		assertContains("Lausanne", values.getLocaliteEtPays());
		assertContains("Lausanne", values.getLocalite());
		assertContains(Constants.OUI, values.getDomicileVd());

	}

	/**
	 * msi/tdq 3.6.09 : on ne doit pas tenir compte des adresses de domicile par défaut car elles n'ont pas de valeur pour déterminer si un
	 * contribuable est dans le canton
	 */
	@Test
	public void testContribuableAvecAdresseDomicileParDefaut() throws Exception {

		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNumero(1234L);
		nonHab.setNom("Poulain");

		// Ajout des adresses
		Set<AdresseTiers> adrs = new HashSet<>();

		// Ancienne adresse courrier
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setNumeroMaison("12");
			adresse.setNumeroOrdrePoste(528); // Cossonay
			adresse.setNumeroRue(1131419);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setDateDebut(RegDate.get(2001, 6, 21));
			adresse.setDateFin(RegDate.get(2005, 11, 30));
			adresse.setTiers(nonHab);
			adrs.add(adresse);
		}
		// Adresse courrier courante
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setNumeroMaison("17b");
			adresse.setNumeroOrdrePoste(152); // Lausanne
			adresse.setNumeroRue(1133753);
			adresse.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setDateDebut(RegDate.get(2005, 12, 1));
			adresse.setTiers(nonHab);
			adrs.add(adresse);
		}
		// Adresse poursuite
		{
			AdresseEtrangere adresse = new AdresseEtrangere();
			adresse.setNumeroMaison("17b");
			adresse.setNumeroOfsPays(8000);
			adresse.setNumeroPostalLocalite("65123");
			adresse.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
			adresse.setUsage(TypeAdresseTiers.REPRESENTATION);
			adresse.setDateDebut(RegDate.get(2002, 1, 15));
			adresse.setTiers(nonHab);
			adrs.add(adresse);
		}

		nonHab.setAdressesTiers(adrs);

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, nonHab);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		assertEquals("1005", values.getNpaCourrier());
		assertEquals("1005", values.getNpaTous());      // courrier, domicile (absent car défaut), poursuite (absent car étranger), représentation (absent car défaut)
		assertContains("Lausanne", values.getLocaliteEtPays());
		assertContains("Lausanne", values.getLocalite());
		assertEquals(IndexerFormatHelper.nullValue(), values.getDomicileVd()); // adresse de domicile par défaut -> pas de détermination possible

	}

	@Test
	public void testContribuableAvecAdresseEtrangere() throws Exception {

		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNumero(1234L);
		nonHab.setNom("Poulain");

		// Ajout des adresses
		Set<AdresseTiers> adrs = new HashSet<>();

		// Adresse domicile
		{
			AdresseEtrangere adresse = new AdresseEtrangere();
			adresse.setNumeroMaison("17b");
			adresse.setRue("Ch des abeilles");
			adresse.setNumeroOfsPays(8212);
			adresse.setNumeroPostalLocalite("65123 Paris cedex 16");
			adresse.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
			adresse.setUsage(TypeAdresseTiers.DOMICILE);
			adresse.setDateDebut(RegDate.get(2002, 1, 15));
			adresse.setTiers(nonHab);
			adrs.add(adresse);
		}
		// Adresse courrier
		{
			AdresseEtrangere adresse = new AdresseEtrangere();
			adresse.setNumeroMaison("17b");
			adresse.setRue("Ch des abeilles");
			adresse.setNumeroOfsPays(8212);
			adresse.setNumeroPostalLocalite("65123 Paris cedex 16");
			adresse.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setDateDebut(RegDate.get(2002, 1, 15));
			adresse.setTiers(nonHab);
			adrs.add(adresse);
		}
		nonHab.setAdressesTiers(adrs);

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, nonHab);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		//String s1 = values.getLocaliteEtPays();
		assertContains("abeilles", values.getRue());
		assertEquals("", values.getNpaCourrier());
		assertEquals("NULL", values.getNpaTous());      // courrier (absent car étranger), domicile (absent car étranger), poursuite (absent car défaut), représentation (absent car défaut)
		assertContains("France", values.getLocaliteEtPays());
		assertContains("France", values.getLocaliteEtPays());
		assertContains("Paris", values.getLocalite());
		assertContains("France", values.getPays());
		assertContains(Constants.NON, values.getDomicileVd());
	}

	@Test
	public void testContribuableAvecAdressesCivile() throws Exception {

		Individu individu = serviceCivil.getIndividu(1234L, RegDate.get(2007, 12, 31));
		//HistoriqueIndividu histo = individu.getDernierHistoriqueIndividu();

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(1234L);
		hab.setNumeroIndividu(individu.getNoTechnique());

		// Ajout des adresses
		Set<AdresseTiers> adrs = new HashSet<>();
		// Ancienne adresse courrier
		{
			AdresseCivile adresse = new AdresseCivile();
			adresse.setType(TypeAdresseCivil.COURRIER);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setDateDebut(RegDate.get(2001, 6, 21));
			//util.setDateFin(RegDate.get(2005, 11, 30));
			adresse.setTiers(hab);
			adrs.add(adresse);
		}
		hab.setAdressesTiers(adrs);

		HabitantIndexable indexable = new HabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, hab, individu);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		//String s1 = values.getLocaliteEtPays();
		assertEquals("1880", values.getNpaCourrier());
		assertEquals("1880 1880 1880 1880", values.getNpaTous());      // courrier, domicile, poursuite, représentation
		assertContains("Bex", values.getLocaliteEtPays());
		assertContains("Bex", values.getLocalite());
		assertContains(Constants.OUI, values.getDomicileVd());

	}

	@Test
	public void testMenageCommunHabitants() throws Exception {

		// Les maillards
		PersonnePhysique hab1 = new PersonnePhysique(true);
		hab1.setNumero(123L);
		hab1.setNumeroIndividu(noIndPhilippeMaillard);
		PersonnePhysique hab2 = new PersonnePhysique(true);
		hab2.setNumero(456L);
		hab2.setNumeroIndividu(noIndGladysMaillard);

		MenageCommun mc = new MenageCommun();
		mc.setNumero(2345L);
		addTiers(mc, hab1, RegDate.get(2001, 2, 23));
		addTiers(mc, hab2, RegDate.get(2001, 2, 23));

		MenageCommunIndexable indexable = new MenageCommunIndexable(adresseService, tiersService, assujettissementService, serviceCivil, serviceInfra, avatarService, mc);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// Search
		//assertContains(numCtb1.toString(), values.getNumeros());
		//assertContains(numCtb2.toString(), values.getNumeros());
		assertContains("Maillard", values.getNomRaison());
		assertContains("Gallet", values.getNomRaison());
		assertEquals(Arrays.asList(date(1956, 1, 21), date(1967, 12, 3)), values.getDatesNaissanceInscriptionRC()); // [UNIREG-2633]
		assertContains(FormatNumeroHelper.formatAncienNumAVS("123.45.678"), values.getNavs11());
		assertContains(FormatNumeroHelper.formatAncienNumAVS("987.65.432"), values.getNavs11());
		// Display
		assertContains("Maillard", values.getNom1());
		assertContains("Philippe", values.getNom1());
		assertContains("Gallet", values.getNom2());
		assertContains("Gladys", values.getNom2());

		//assertContains("", values.getDateNaissance());
	}

	@Test
	public void testMenageCommunNonHabitants() throws Exception {

		RegDate dateN1 = RegDate.get(1956, 1, 21);
		RegDate dateN2 = RegDate.get(1967, 12, 3);
		String noAVS1 = "7560000000001";
		String noAVS2 = "7560000000002";
		Long numCtb1 = 123L;
		Long numCtb2 = 456L;

		PersonnePhysique nhab1 = new PersonnePhysique(false);
		nhab1.setNumero(numCtb1);
		nhab1.setNom("Maillard");
		nhab1.setPrenomUsuel("Philippe");
		nhab1.setNumeroAssureSocial(noAVS1);
		nhab1.setDateNaissance(dateN1);
		nhab1.setSexe(Sexe.MASCULIN);
		PersonnePhysique nhab2 = new PersonnePhysique(false);
		nhab2.setNumero(numCtb2);
		nhab2.setNom("Maillard-Gallet");
		nhab2.setPrenomUsuel("Gladys");
		nhab2.setNumeroAssureSocial(noAVS2);
		nhab2.setDateNaissance(dateN2);
		nhab2.setSexe(Sexe.FEMININ);

		MenageCommun mc = new MenageCommun();
		mc.setNumero(2345L);
		addTiers(mc, nhab1, RegDate.get(2001, 2, 23));
		addTiers(mc, nhab2, RegDate.get(2001, 2, 23));

		MenageCommunIndexable indexable = new MenageCommunIndexable(adresseService, tiersService, assujettissementService, serviceCivil, serviceInfra, avatarService, mc);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// Search
		//assertContains(numCtb1.toString(), values.getNumeros());
		//assertContains(numCtb2.toString(), values.getNumeros());
		assertContains("Maillard", values.getNomRaison());
		assertContains("Gallet", values.getNomRaison());
		assertEquals(Arrays.asList(dateN1, dateN2), values.getDatesNaissanceInscriptionRC()); // [UNIREG-2633]
		assertContains(noAVS1, values.getNavs13());
		assertContains(noAVS2, values.getNavs13());
		// Display
		assertContains("Maillard", values.getNom1());
		assertContains("Philippe", values.getNom1());
		assertContains("Gallet", values.getNom2());
		assertContains("Gladys", values.getNom2());

		//assertContains("", values.getDateNaissance());
	}

	/**
	 * [UNIREG-2142] vérifie qu'on prend la localité abrégée comme donnée d'indexation
	 */
	@Test
	public void testHabitantRomanelSurLausanne() {

		PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNumero(1234L);
		pp.setNom("Ruth");
		pp.setPrenomUsuel("Laurent");

		// Ajout des adresses
		AdresseSuisse adresse = new AdresseSuisse();
		adresse.setNumeroOrdrePoste(MockLocalite.RomanelSurLausanne.getNoOrdre());
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setDateDebut(RegDate.get(2002, 1, 15));
		adresse.setTiers(pp);
		pp.addAdresseTiers(adresse);

		final NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, pp);
		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertEquals("1032", values.getNpaCourrier());
		assertEquals("1032", values.getNpaTous());      // courrier, domicile (absent car défaut), poursuite (absent car défaut), représentation (absent car défaut)
		assertEquals("Romanel-sur-Lausanne", values.getLocaliteEtPays());
		assertEquals("Romanel-sur-Lausanne", values.getLocalite());
		assertEquals("Suisse", values.getPays());
	}

	public RapportEntreTiers addTiers(MenageCommun menage, PersonnePhysique tiers, RegDate dateDebut) {

		tiersDAO.save(tiers);
		tiersDAO.save(menage);

		final RapportEntreTiers appartenance = new AppartenanceMenage();
		appartenance.setDateDebut(dateDebut);
		appartenance.setDateFin(null);
		appartenance.setObjet(menage);
		appartenance.setSujet(tiers);

		menage.addRapportObjet(appartenance);
		tiers.addRapportSujet(appartenance);

		return appartenance;
	}

	@Test
	public void testIndexationTousPrenomsHabitant() throws Exception {

		final long noIndividu = 4378546L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Beretta", "Tim", Sexe.MASCULIN);
				individu.setTousPrenoms("Timothée Philibert");
			}
		});

		final PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(1234L);
		hab.setNumeroIndividu(noIndividu);

		final HabitantIndexable indexable = new HabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, hab, serviceCivil.getIndividu(noIndividu, null));
		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertEquals("Beretta", values.getNomRaison());
		assertEquals("Tim Timothée Philibert Beretta", values.getAutresNom());  // prénom usuel, tous prénoms, nom, nom de naissance (absent)
	}

	@Test
	public void testIndexationTousPrenomsNonHabitant() throws Exception {

		final PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNumero(1234L);
		pp.setNom("Ruth");
		pp.setPrenomUsuel("Lolo");
		pp.setTousPrenoms("Laurent Philippe");

		final NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, pp);
		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertEquals("Ruth", values.getNomRaison());
		assertEquals("Lolo Laurent Philippe Ruth", values.getAutresNom());  // prénom usuel, tous prénoms, nom
	}

	/**
	 * [SIFISC-21631] drôle de date de début de premier for vaudois au 31.12.2399 (= late date) pour un sourcier au pays inconnu
	 */
	@Test
	public void testIndexationDatesForsVaudoisSurSourcierAuPaysInconnu() {

		final PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNumero(10552389L);
		pp.setNom("Invisible");
		pp.setPrenomUsuel("L'Homme");
		pp.setTousPrenoms("Albert André");

		final ForFiscalPrincipalPP ffp = new ForFiscalPrincipalPP(date(2009, 12, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, null, MockPays.PaysInconnu.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
		pp.setForsFiscaux(Collections.singleton(ffp));

		final NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, pp);
		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertNotNull(values);
		assertEquals("NULL", values.getDateOuvertureForVd());
		assertEquals("NULL", values.getDateFermetureForVd());
		assertEquals("20091201", values.getDateOuvertureFor());
		assertEquals("NULL", values.getDateFermetureFor());
	}
}
