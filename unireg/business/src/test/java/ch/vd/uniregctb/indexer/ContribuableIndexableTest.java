package ch.vd.uniregctb.indexer;

import javax.persistence.NonUniqueResultException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FlushMode;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.hqlbuilder.AbstractCriteria;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseServiceImpl;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.indexer.tiers.DebiteurPrestationImposableIndexable;
import ch.vd.uniregctb.indexer.tiers.HabitantIndexable;
import ch.vd.uniregctb.indexer.tiers.MenageCommunIndexable;
import ch.vd.uniregctb.indexer.tiers.NonHabitantIndexable;
import ch.vd.uniregctb.indexer.tiers.TiersIndexable;
import ch.vd.uniregctb.indexer.tiers.TiersIndexableData;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockHistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersServiceImpl;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class ContribuableIndexableTest extends WithoutSpringTest {

	//private static final Logger LOGGER = Logger.getLogger(ContribuableIndexableTest.class);

	private static final long noIndPhilippeMaillard = 3333L;
	private static final long noIndGladysMaillard = 3334L;

	private AdresseServiceImpl adresseService;
	private TiersServiceImpl tiersService;
	private ServiceCivilService serviceCivil;
	private ServiceInfrastructureService serviceInfra;
	private ServicePersonneMoraleService servicePM;
	private MockTiersDAO tiersDAO;
	
	@Override
	public void onSetUp() {

		serviceCivil = new MockServiceCivil() {

			@Override
			protected void init() {

				final MockIndividu charlesPoncet = addIndividu(7643L, RegDate.get(1965, 3, 12), "Poncet", "Charles", true);
				addFieldsIndividu(charlesPoncet, "01234567", "123.43.765.543", "");

				final MockIndividu marcelMeignier = addIndividu(1234L, RegDate.get(1972, 1, 27), "MEIGNIER", "Marcel", true);
				addAdresse(marcelMeignier, TypeAdresseCivil.COURRIER, MockRue.Bex.RouteDuBoet, null, RegDate.get(1964, 12, 2), null);
				addAdresse(marcelMeignier, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, RegDate.get(1964, 12, 2), null);

				final MockIndividu philippeMaillard = addIndividu(noIndPhilippeMaillard, RegDate.get(1956, 1, 21), "Maillard", "Philippe", true);
				{
					MockHistoriqueIndividu histo = (MockHistoriqueIndividu) philippeMaillard.getDernierHistoriqueIndividu();
					histo.setNoAVS("123.45.678");
				}

				final MockIndividu gladysMaillard = addIndividu(noIndGladysMaillard, RegDate.get(1967, 12, 3), "Maillard-Gallet", "Gladys", false);
				{
					MockHistoriqueIndividu histo = (MockHistoriqueIndividu) gladysMaillard.getDernierHistoriqueIndividu();
					histo.setNoAVS("987.65.432");
				}

				addIndividu(123L, RegDate.get(1970, 1, 1), "Dupont", "Philippe", true);
				addIndividu(4567L, RegDate.get(1970, 1, 1), "Dupond", "Arnold", true);
			}
		};

		tiersDAO = new MockTiersDAO();

		tiersService = new TiersServiceImpl();
		serviceInfra = new ServiceInfrastructureImpl(new DefaultMockServiceInfrastructureService());
		servicePM = new DefaultMockServicePM();
		tiersService.setServiceInfra(serviceInfra);
		tiersService.setServiceCivilService(serviceCivil);
		tiersService.setTiersDAO(tiersDAO);
		tiersService.setServicePM(servicePM);
		tiersService.setValidationService(null);

		adresseService = new AdresseServiceImpl();
		adresseService.setServiceInfra(new ServiceInfrastructureImpl(new DefaultMockServiceInfrastructureService()));
		adresseService.setServiceCivilService(serviceCivil);
		adresseService.setTiersService(tiersService);
		adresseService.setServicePM(servicePM);
	}


	@Test
	public void testMenageCommunMarieSeul() throws Exception {

		PersonnePhysique hab1 = new PersonnePhysique(true);
		hab1.setNumero(123L);
		hab1.setNumeroIndividu(noIndPhilippeMaillard);

		MenageCommun mc = new MenageCommun();
		mc.setNumero(2345L);
		addTiers(mc, hab1, RegDate.get(2001, 2, 23));

		MenageCommunIndexable indexable = new MenageCommunIndexable(adresseService, tiersService, serviceCivil, serviceInfra, mc);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// Search
		//assertContains(numCtb1.toString(), values.get(TiersIndexableData.NUMEROS));
		assertContains("Maillard", values.getNomRaison());
		assertEquals("19560121", values.getDateNaissance()); // [UNIREG-2633]
		assertContains(FormatNumeroHelper.formatAncienNumAVS("123.45.678"), values.getNumeroAssureSocial());
		// Display
		assertContains("Maillard", values.getNom1());
		assertContains("Philippe", values.getNom1());
		assertEquals(null, values.getNom2());
	}

	@Test
	public void testNonHabitantIndexable() throws Exception {

		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNumero(1234L);
		nonHab.setDateNaissance(RegDate.get(1965,03,12));
		nonHab.setNom("Poncet");
		nonHab.setPrenom("Charles");
		nonHab.setNumeroAssureSocial("432.23.654.345");
		nonHab.setAdresseCourrierElectronique(null); // Volontaire, on teste que on recoit chaine vide pour une valeur null

		AdresseSuisse a = new AdresseSuisse();
		a.setDateDebut(RegDate.get(2002, 2, 23));
		a.setNumeroOrdrePoste(283);
		a.setUsage(TypeAdresseTiers.COURRIER);
		HashSet<AdresseTiers> adresses = new HashSet<AdresseTiers>();
		adresses.add(a);
		nonHab.setAdressesTiers(adresses);

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, nonHab);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(NonHabitantIndexable.SUB_TYPE, indexable.getSubType());

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertEquals(DateHelper.dateToIndexString(nonHab.getDateNaissance().asJavaDate()), values.getDateNaissance());
		assertEquals("Suisse", values.getPays());
		assertEquals("19650312", values.getDateNaissance());
		assertEquals("", values.getDateDeces());
	}

	@Test
	public void testTiersACIIndexable() throws Exception {
		Individu individu = serviceCivil.getIndividu(7643L, 2007);
		HistoriqueIndividu histoInd = individu.getDernierHistoriqueIndividu();

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(12348L);
		hab.setNumeroIndividu(individu.getNoTechnique());

		HabitantIndexable indexable = new HabitantIndexable(adresseService, tiersService, serviceInfra, hab, individu);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(HabitantIndexable.SUB_TYPE, indexable.getSubType());

		// Ctb
		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertContains(hab.getNumero().toString(), values.getNumeros());
		assertContains("Contribuable PP", values.getRoleLigne1());

		// Individu
		assertEquals(IndexerFormatHelper.objectToString(individu.getDateNaissance()), values.getDateNaissance());
		assertEquals(String.format("%s %s", histoInd.getPrenom(), histoInd.getNom()), values.getNom1());
		assertContains(IndexerFormatHelper.formatNumeroAVS(individu.getNouveauNoAVS()), values.getNumeroAssureSocial());
	}

	@Test
	public void testHabitantIndexable() throws Exception {
		Individu individu = serviceCivil.getIndividu(7643L, 2007);
		HistoriqueIndividu histoInd = individu.getDernierHistoriqueIndividu();

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(12348L);
		hab.setNumeroIndividu(individu.getNoTechnique());
		ForFiscalPrincipal ff = new ForFiscalPrincipal();
		ff.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ff.setMotifRattachement(MotifRattachement.DOMICILE);
		ff.setModeImposition(ModeImposition.SOURCE);
		ff.setDateDebut(RegDate.get(2005, 2, 2));
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(5586);
		hab.addForFiscal(ff);

		HabitantIndexable indexable = new HabitantIndexable(adresseService, tiersService, serviceInfra, hab, individu);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(HabitantIndexable.SUB_TYPE, indexable.getSubType());

		// Ctb
		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertContains(hab.getNumero().toString(), values.getNumeros());
		assertContains("Contribuable PP", values.getRoleLigne1());
		assertContains("source", values.getRoleLigne2());

		// Individu
		assertEquals(IndexerFormatHelper.objectToString(individu.getDateNaissance()), values.getDateNaissance());
		assertEquals(String.format("%s %s", histoInd.getPrenom(), histoInd.getNom()), values.getNom1());
		assertContains(IndexerFormatHelper.formatNumeroAVS(individu.getNouveauNoAVS()), values.getNumeroAssureSocial());
	}

	@Test
	public void testDebiteurImpotSourceHabitantIndexable() throws Exception {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(1234L);
		hab.setNumeroIndividu(7643L);
		tiersDAO.addTiers(hab);
		Individu ind = serviceCivil.getIndividu(hab.getNumeroIndividu(), null);
		HistoriqueIndividu histo = ind.getDernierHistoriqueIndividu();

		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(23348L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi.setComplementNom("Service bidon");
		tiersDAO.addTiers(dpi);

		ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, hab, dpi);
		hab.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, serviceCivil, servicePM, serviceInfra, dpi);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(DebiteurPrestationImposableIndexable.SUB_TYPE, indexable.getSubType());

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		assertContains(dpi.getNumero().toString(), values.getNumeros());
		// On ne doit pas pouvoir rechercher sur le NO_IND
		assertNotContains(hab.getNumeroIndividu().toString(), values.getNumeros());
		assertContains("Débiteur IS", values.getRoleLigne1());
		assertContains("Réguliers", values.getRoleLigne2());
		assertContains(histo.getNom(), values.getNomRaison());
		assertContains(histo.getNom(), values.getAutresNom());
		assertContains(histo.getPrenom(), values.getAutresNom());

		// Display (quel que soit le nom1 et nom2, si le débiteur a un contact impôt source, sa raison sociale est tirée de là)
		assertEquals(String.format("%s %s", histo.getPrenom(), histo.getNom()), values.getNom1());
		assertNull(values.getNom2());
	}

	@Test
	public void testDebiteurImpotSourceNonHabitantIndexable() throws Exception {

		PersonnePhysique nhab = new PersonnePhysique(false);
		nhab.setNumero(1234L);
		nhab.setNom("Bli");
		nhab.setPrenom("Bla");
		tiersDAO.addTiers(nhab);

		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(23348L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		tiersDAO.addTiers(dpi);

		ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, nhab, dpi);
		nhab.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, serviceCivil, servicePM, serviceInfra, dpi);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(DebiteurPrestationImposableIndexable.SUB_TYPE, indexable.getSubType());

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		// Search
		assertContains(dpi.getNumero().toString(), values.getNumeros());
		assertContains("Débiteur IS", values.getRoleLigne1());
		assertContains("Réguliers", values.getRoleLigne2());
		assertContains(nhab.getNom(), values.getNomRaison());
		assertContains(nhab.getNom(), values.getAutresNom());
		assertContains(nhab.getPrenom(), values.getAutresNom());

		// Display (quel que soit le nom1 et nom2, si le débiteur a un contact impôt source, sa raison sociale est tirée de là)
		assertEquals("Bla Bli", values.getNom1());
		assertNull(values.getNom2());
	}

	@Test
	public void testDebiteurImpotSourceEntrepriseIndexable() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.setNumero(MockPersonneMorale.BCV.getNumeroEntreprise());
		tiersDAO.addTiers(entreprise);

		final DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(1500003L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi.setComplementNom("Service bidon");
		tiersDAO.addTiers(dpi);

		final ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, entreprise, dpi);
		entreprise.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		final DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, serviceCivil, servicePM, serviceInfra, dpi);
		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(DebiteurPrestationImposableIndexable.SUB_TYPE, indexable.getSubType());

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// Search
		assertContains(dpi.getNumero().toString(), values.getNumeros());
		assertContains("Débiteur IS", values.getRoleLigne1());
		assertContains("Réguliers", values.getRoleLigne2());
		assertContains(MockPersonneMorale.BCV.getRaisonSociale1(), values.getNomRaison());
		if (MockPersonneMorale.BCV.getRaisonSociale2() != null) {
			assertContains(MockPersonneMorale.BCV.getRaisonSociale2(), values.getNomRaison());
		}

		// Display (quel que soit le nom1 et nom2, si le débiteur a un contact impôt source, sa raison sociale est tirée de là)
		assertEquals(MockPersonneMorale.BCV.getRaisonSociale1(), values.getNom1());
		assertEquals(MockPersonneMorale.BCV.getRaisonSociale2(), values.getNom2());
	}

	@Test
	public void testDebiteurImpotSourceAutreCommuncauteIndexable() throws Exception {

		AutreCommunaute ac = new AutreCommunaute();
		ac.setNumero(1234L);
		ac.setNom("Nestle SA");
		ac.setComplementNom("Filiale de Orbe");
		tiersDAO.addTiers(ac);

		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(23348L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi.setComplementNom("Service bidon");
		tiersDAO.addTiers(dpi);

		ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ac, dpi);
		ac.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, serviceCivil, servicePM, serviceInfra, dpi);

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
			ForFiscalPrincipal forF = new ForFiscalPrincipal();
			forF.setDateDebut(RegDate.get(1998, 3, 01));
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
			ForFiscalPrincipal forF = new ForFiscalPrincipal();
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

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, nonHab);

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
		assertEquals("", values.getDateFermtureFor());
	}

	@Test
	public void testContribuableAvecForFerme() throws Exception {

		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNumero(1234L);
		nonHab.setNom("Poulain");

		// For fiscaux

		RegDate dateOuverture = RegDate.get(1998, 3, 01);
		RegDate dateFermeture = RegDate.get(2004, 1, 31);

		// Principal 1 fermé
		{
			ForFiscalPrincipal forF = new ForFiscalPrincipal();
			forF.setDateDebut(dateOuverture);
			forF.setDateFin(dateFermeture);
			forF.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forF.setNumeroOfsAutoriteFiscale(5477);
			forF.setModeImposition(ModeImposition.ORDINAIRE);
			forF.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forF.setMotifRattachement(MotifRattachement.DOMICILE);
			nonHab.addForFiscal(forF);
		}

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, nonHab);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// for principal fermé -> tout null

		// Search
		assertNull(values.getNoOfsForPrincipal());
		// Display
		assertEquals("Cossonay", values.getForPrincipal());
		assertEquals(RegDateHelper.toIndexString(dateOuverture), values.getDateOuvertureFor());
		assertEquals(RegDateHelper.toIndexString(dateFermeture), values.getDateFermtureFor());
	}

	@Test
	public void testContribuableAvecAdressesSuisse() throws Exception {

		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNumero(1234L);
		nonHab.setNom("Poulain");

		// Ajout des adresses
		Set<AdresseTiers> adrs = new HashSet<AdresseTiers>();

		// Adresse domicile
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setNumeroMaison("12");
			adresse.setNumeroOrdrePoste(528); // Cossonay
			adresse.setNumeroRue(32296);
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
			adresse.setNumeroRue(32296);
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
			adresse.setNumeroOrdrePoste(104); // Lausanne
			adresse.setNumeroRue(76437);
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

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, nonHab);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		assertEquals("1000", values.getNpa());
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
		Set<AdresseTiers> adrs = new HashSet<AdresseTiers>();

		// Ancienne adresse courrier
		{
			AdresseSuisse adresse = new AdresseSuisse();
			adresse.setNumeroMaison("12");
			adresse.setNumeroOrdrePoste(528); // Cossonay
			adresse.setNumeroRue(32296);
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
			adresse.setNumeroOrdrePoste(104); // Lausanne
			adresse.setNumeroRue(76437);
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

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, nonHab);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		assertEquals("1000", values.getNpa());
		assertContains("Lausanne", values.getLocaliteEtPays());
		assertContains("Lausanne", values.getLocalite());
		assertEquals("", values.getDomicileVd()); // adresse de domicile par défaut -> pas de détermination possible

	}

	@Test
	public void testContribuableAvecAdresseEtrangere() throws Exception {

		PersonnePhysique nonHab = new PersonnePhysique(false);
		nonHab.setNumero(1234L);
		nonHab.setNom("Poulain");

		// Ajout des adresses
		Set<AdresseTiers> adrs = new HashSet<AdresseTiers>();

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

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, nonHab);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		//String s1 = values.getLocaliteEtPays();
		assertContains("abeilles", values.getRue());
		assertEquals("", values.getNpa());
		assertContains("France", values.getLocaliteEtPays());
		assertContains("France", values.getLocaliteEtPays());
		assertContains("Paris", values.getLocalite());
		assertContains("France", values.getPays());
		assertContains(Constants.NON, values.getDomicileVd());
	}

	@Test
	public void testContribuableAvecAdressesCivile() throws Exception {

		Individu individu = serviceCivil.getIndividu(1234L, 2007);
		//HistoriqueIndividu histo = individu.getDernierHistoriqueIndividu();

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(1234L);
		hab.setNumeroIndividu(individu.getNoTechnique());

		// Ajout des adresses
		Set<AdresseTiers> adrs = new HashSet<AdresseTiers>();
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

		HabitantIndexable indexable = new HabitantIndexable(adresseService, tiersService, serviceInfra, hab, individu);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		//String s1 = values.getLocaliteEtPays();
		assertEquals("1880", values.getNpa());
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
		addTiers(mc, hab1,RegDate.get(2001, 2, 23));
		addTiers(mc, hab2,RegDate.get(2001, 2, 23));

		MenageCommunIndexable indexable = new MenageCommunIndexable(adresseService, tiersService, serviceCivil, serviceInfra, mc);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		
		// Search
		//assertContains(numCtb1.toString(), values.getNumeros());
		//assertContains(numCtb2.toString(), values.getNumeros());
		assertContains("Maillard", values.getNomRaison());
		assertContains("Gallet", values.getNomRaison());
		assertEquals("19560121 19671203", values.getDateNaissance()); // [UNIREG-2633]
		assertContains(FormatNumeroHelper.formatAncienNumAVS("123.45.678"), values.getNumeroAssureSocial());
		assertContains(FormatNumeroHelper.formatAncienNumAVS("987.65.432"), values.getNumeroAssureSocial());
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
		String noAVS1 = "123.45.678";
		String noAVS2 = "987.65.432";
		Long numCtb1 = 123L;
		Long numCtb2 = 456L;

		PersonnePhysique nhab1 = new PersonnePhysique(false);
		nhab1.setNumero(numCtb1);
		nhab1.setNom("Maillard");
		nhab1.setPrenom("Philippe");
		nhab1.setNumeroAssureSocial(noAVS1);
		nhab1.setDateNaissance(dateN1);
		nhab1.setSexe(Sexe.MASCULIN);
		PersonnePhysique nhab2 = new PersonnePhysique(false);
		nhab2.setNumero(numCtb2);
		nhab2.setNom("Maillard-Gallet");
		nhab2.setPrenom("Gladys");
		nhab2.setNumeroAssureSocial(noAVS2);
		nhab2.setDateNaissance(dateN2);
		nhab2.setSexe(Sexe.FEMININ);

		MenageCommun mc = new MenageCommun();
		mc.setNumero(2345L);
		addTiers(mc, nhab1, RegDate.get(2001, 2, 23));
		addTiers(mc, nhab2, RegDate.get(2001, 2, 23));

		MenageCommunIndexable indexable = new MenageCommunIndexable(adresseService, tiersService, serviceCivil, serviceInfra, mc);

		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();

		// Search
		//assertContains(numCtb1.toString(), values.getNumeros());
		//assertContains(numCtb2.toString(), values.getNumeros());
		assertContains("Maillard", values.getNomRaison());
		assertContains("Gallet", values.getNomRaison());
		assertEquals(dateN1.index() + " " + dateN2.index(), values.getDateNaissance()); // [UNIREG-2633]
		assertContains(FormatNumeroHelper.formatAncienNumAVS(noAVS1), values.getNumeroAssureSocial());
		assertContains(FormatNumeroHelper.formatAncienNumAVS(noAVS2), values.getNumeroAssureSocial());
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
		pp.setPrenom("Laurent");

		// Ajout des adresses
		AdresseSuisse adresse = new AdresseSuisse();
		adresse.setNumeroOrdrePoste(MockLocalite.RomanelSurLausanne.getNoOrdre());
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setDateDebut(RegDate.get(2002, 1, 15));
		adresse.setTiers(pp);
		pp.addAdresseTiers(adresse);

		final NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, pp);
		final TiersIndexableData values = (TiersIndexableData) indexable.getIndexableData();
		assertEquals("1032", values.getNpa());
		assertEquals("Romanel-s-Lausanne", values.getLocaliteEtPays());
		assertEquals("Romanel-s-Lausanne", values.getLocalite());
		assertEquals("Suisse", values.getPays());
	}

	public RapportEntreTiers addTiers(MenageCommun menage, PersonnePhysique tiers, RegDate dateDebut) {

		tiersDAO.addTiers(tiers);
		tiersDAO.addTiers(menage);

		RapportEntreTiers appartenance = new AppartenanceMenage();
		appartenance.setDateDebut(dateDebut);
		appartenance.setDateFin(null);
		appartenance.setObjet(menage);
		appartenance.setSujet(tiers);

		menage.addRapportObjet(appartenance);
		tiers.addRapportSujet(appartenance);

		return appartenance;
	}

	private static class MockTiersDAO implements TiersDAO {
		
		private Map<Long, Tiers> map = new HashMap<Long, Tiers>();
		
		public void addTiers(Tiers tiers) {
			final Long id = tiers.getId();
			Assert.notNull(id);
			map.put(id,  tiers);
		}
		
		public Tiers get(long id, boolean doNotAutoFlush) {
			return map.get(id);
		}

		public Map<Class, List<Tiers>> getFirstGroupedByClass(int count) {
			throw new NotImplementedException();
		}

		public List<Tiers> getBatch(Collection<Long> ids, Set<Parts> parts) {
			throw new NotImplementedException();
		}

		public RapportEntreTiers save(RapportEntreTiers object) {
			throw new NotImplementedException();
		}

		public List<Long> getAllIds() {
			throw new NotImplementedException();
		}

		public List<Long> getDirtyIds() {
			throw new NotImplementedException();
		}

		public List<Long> getAllNumeroIndividu() {
			throw new NotImplementedException();
		}

		public Set<Long> getNumerosIndividu(Collection<Long> tiersIds, boolean includesComposantsMenage) {
			throw new NotImplementedException();
		}

		public List<Long> getHabitantsForMajorite(RegDate dateReference) {
			throw new NotImplementedException();
		}

		public List<Long> getTiersInRange(int ctbStart, int ctbEnd) {
			throw new NotImplementedException();
		}

		public Contribuable getContribuableByNumero(Long numeroContribuable) {
			throw new NotImplementedException();
		}

		public DebiteurPrestationImposable getDebiteurPrestationImposableByNumero(Long numeroDPI) {
			throw new NotImplementedException();
		}

		public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu) {
			throw new NotImplementedException();
		}

		public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
			throw new NotImplementedException();
		}

		public Long getNumeroPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
			throw new NotImplementedException();
		}

		public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu) {
			throw new NotImplementedException();
		}

		public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
			throw new NotImplementedException();
		}

		public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
			throw new NotImplementedException();
		}

		public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush) {
			throw new NotImplementedException();
		}

		public List<PersonnePhysique> getSourciers(int noSourcier) {
			throw new NotImplementedException();
		}

		public List<PersonnePhysique> getAllMigratedSourciers() {
			throw new NotImplementedException();
		}

		public Tiers getTiersForIndexation(long id) {
			return map.get(id);
		}

		public List<MenageCommun> getMenagesCommuns(List<Long> ids, Set<Parts> parts) {
			throw new NotImplementedException();
		}

		public List<Tiers> getAll() {
			throw new NotImplementedException();
		}

		public Tiers get(Long id) {
			return map.get(id);
		}

		public boolean exists(Long id) {
			return map.containsKey(id);
		}

		public boolean exists(Long id, FlushMode flushModeOverride) {
			return map.containsKey(id);
		}

		public Tiers save(Tiers object) {
			return object;
		}

		public Object saveObject(Object object) {
			return object;
		}

		public void remove(Long id) {
			map.remove(id);
		}

		public void removeAll() {
			map.clear();
		}

		public HibernateTemplate getHibernateTemplate() {
			throw new NotImplementedException();
		}

		public Iterator<Tiers> iterate(String query) {
			throw new NotImplementedException();
		}

		public int getCount(Class<?> clazz) {
			return 0;
		}

		public void clearSession() {
		}

		public void evict(Object o) {
		}

		public List<Tiers> getResultList(AbstractCriteria aCriteria) {
			throw new NotImplementedException();
		}

		public List<Tiers> getResultList(AbstractCriteria aCriteria, int aMaxResults) {
			throw new NotImplementedException();
		}

		public Tiers getSingleResult(AbstractCriteria aCriteria) throws NonUniqueResultException {
			throw new NotImplementedException();
		}

		public List<Tiers> getDistinctResultList(AbstractCriteria aCriteria) {
			throw new NotImplementedException();
		}

		public List<Tiers> getDistinctResultList(AbstractCriteria aCriteria, int aMaxResults) {
			throw new NotImplementedException();
		}

		public long getRowCountResult(AbstractCriteria aCriteria) {
			throw new NotImplementedException();
		}

		public Contribuable getContribuable(DebiteurPrestationImposable debiteur) {
			final Long ctbId = debiteur.getContribuableId();
			return (Contribuable) get(ctbId);
		}

		public void updateOids(Map<Long, Integer> tiersOidsMapping) {
			throw new NotImplementedException();
		}

		public List<Long> getListeDebiteursSansPeriodicites() {
			throw new NotImplementedException();
		}

		public <T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal) {
			throw new NotImplementedException();
		}
	}
}
