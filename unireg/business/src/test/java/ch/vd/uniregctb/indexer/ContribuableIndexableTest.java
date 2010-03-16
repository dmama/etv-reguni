package ch.vd.uniregctb.indexer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseService;
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
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.indexer.tiers.TiersSearchFields;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockHistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.TiersServiceImpl;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ContribuableIndexableTest extends WithoutSpringTest {

	//private static final Logger LOGGER = Logger.getLogger(ContribuableIndexableTest.class);

	private static final long noIndPhilippeMaillard = 3333L;
	private static final long noIndGladysMaillard = 3334L;

	private AdresseService adresseService;
	private TiersService tiersService;
	private ServiceCivilService serviceCivil;

	@Override
	public void onSetUp() {

		serviceCivil = new MockServiceCivil() {

			@Override
			protected void init() {

				MockIndividu charlesPoncet = addIndividu(7643L, RegDate.get(1965, 3, 12), "Charles", "Poncet", true);
				addFieldsIndividu(charlesPoncet, "01234567", "123.43.765.543", "");

				Individu marcelMeignier = addIndividu(1234L, RegDate.get(1972, 1, 27), "Marcel", "MEIGNIER", true);
				addAdresse(marcelMeignier, EnumTypeAdresse.COURRIER, MockRue.Bex.RouteDuBoet, null, MockLocalite.Bex, RegDate.get(1964, 12, 2), null);
				addAdresse(marcelMeignier, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, MockLocalite.Bex, RegDate.get(1964, 12, 2), null);

				MockIndividu philippeMaillard = addIndividu(noIndPhilippeMaillard, RegDate.get(1956, 1, 21), "Maillard", "Philippe", true);
				{
					MockHistoriqueIndividu histo = (MockHistoriqueIndividu) philippeMaillard.getDernierHistoriqueIndividu();
					histo.setNoAVS("123.45.678");
				}

				MockIndividu gladysMaillard = addIndividu(noIndGladysMaillard, RegDate.get(1967, 12, 3), "Maillard-Gallet", "Gladys", false);
				{
					MockHistoriqueIndividu histo = (MockHistoriqueIndividu) gladysMaillard.getDernierHistoriqueIndividu();
					histo.setNoAVS("987.65.432");
				}

				addIndividu(123L, RegDate.get(1970, 1, 1), "Philippe", "Dupont", true);
				addIndividu(4567L, RegDate.get(1970, 1, 1), "Arnold", "Dupond", true);
			}
		};

		tiersService = new TiersServiceImpl();
		tiersService.setServiceInfra(new DefaultMockServiceInfrastructureService());
		tiersService.setServiceCivilService(serviceCivil);

		adresseService = new AdresseServiceImpl();
		adresseService.setServiceInfra(new DefaultMockServiceInfrastructureService());
		adresseService.setServiceCivilService(serviceCivil);
		adresseService.setTiersService(tiersService);
	}


	@Test
	public void testMenageCommunMarieSeul() throws Exception {

		PersonnePhysique hab1 = new PersonnePhysique(true);
		hab1.setNumero(123L);
		hab1.setNumeroIndividu(noIndPhilippeMaillard);

		MenageCommun mc = new MenageCommun();
		mc.setNumero(2345L);
		addTiers(mc, hab1, RegDate.get(2001, 2, 23));

		MenageCommunIndexable indexable = new MenageCommunIndexable(adresseService, tiersService, mc);

		HashMap<String, String> values = indexable.getKeyValues();

		// Search
		//assertContains(numCtb1.toString(), values.get(TiersSearchFields.NUMEROS));
		assertContains("Maillard", values.get(TiersSearchFields.NOM_RAISON));
		assertContains(RegDateHelper.toIndexString(RegDate.get(1956, 1, 21)), values.get(TiersSearchFields.DATE_NAISSANCE));
		assertContains(FormatNumeroHelper.formatAncienNumAVS("123.45.678"), values.get(TiersSearchFields.NUMERO_ASSURE_SOCIAL));
		// Display
		assertContains("Maillard", values.get(TiersIndexedData.NOM1));
		assertContains("Philippe", values.get(TiersIndexedData.NOM1));
		assertEquals(null, values.get(TiersIndexedData.NOM2));
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

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, nonHab);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(NonHabitantIndexable.SUB_TYPE, indexable.getSubType());

		HashMap<String, String> values = indexable.getKeyValues();
		assertEquals(DateHelper.dateToIndexString(nonHab.getDateNaissance().asJavaDate()), values.get(TiersSearchFields.DATE_NAISSANCE));
		assertEquals("Villars-sous-Yens", values.get(TiersIndexedData.LOCALITE_PAYS));
		assertEquals("Suisse", values.get(TiersIndexedData.PAYS));
		assertEquals("19650312", values.get(TiersIndexedData.DATE_NAISSANCE));
		assertEquals("", values.get(TiersIndexedData.DATE_DECES));
	}

	@Test
	public void testTiersACIIndexable() throws Exception {
		Individu individu = serviceCivil.getIndividu(7643L, 2007);
		HistoriqueIndividu histoInd = individu.getDernierHistoriqueIndividu();

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(12348L);
		hab.setNumeroIndividu(individu.getNoTechnique());

		HabitantIndexable indexable = new HabitantIndexable(adresseService, tiersService, hab, individu);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(HabitantIndexable.SUB_TYPE, indexable.getSubType());

		// Ctb
		HashMap<String, String> values = indexable.getKeyValues();
		assertContains(hab.getNumero().toString(), values.get(TiersSearchFields.NUMEROS));
		assertContains("Contribuable PP", values.get(TiersIndexedData.ROLE_LIGNE1));

		// Individu
		assertEquals(IndexerFormatHelper.objectToString(individu.getDateNaissance()), values.get(TiersSearchFields.DATE_NAISSANCE));
		assertEquals(histoInd.getNom()+" "+histoInd.getPrenom(), values.get(TiersIndexedData.NOM1));
		assertContains(IndexerFormatHelper.formatNumeroAVS(individu.getNouveauNoAVS()), values.get(TiersSearchFields.NUMERO_ASSURE_SOCIAL));
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

		HabitantIndexable indexable = new HabitantIndexable(adresseService, tiersService, hab, individu);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(HabitantIndexable.SUB_TYPE, indexable.getSubType());

		// Ctb
		HashMap<String, String> values = indexable.getKeyValues();
		assertContains(hab.getNumero().toString(), values.get(TiersSearchFields.NUMEROS));
		assertContains("Contribuable PP", values.get(TiersIndexedData.ROLE_LIGNE1));
		assertContains("source", values.get(TiersIndexedData.ROLE_LIGNE2));

		// Individu
		assertEquals(IndexerFormatHelper.objectToString(individu.getDateNaissance()), values.get(TiersSearchFields.DATE_NAISSANCE));
		assertEquals(histoInd.getNom()+" "+histoInd.getPrenom(), values.get(TiersIndexedData.NOM1));
		assertContains(IndexerFormatHelper.formatNumeroAVS(individu.getNouveauNoAVS()), values.get(TiersSearchFields.NUMERO_ASSURE_SOCIAL));
	}

	@Test
	public void testDebiteurImpotSourceHabitantIndexable() throws Exception {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(1234L);
		hab.setNumeroIndividu(7643L);
		Individu ind = serviceCivil.getIndividu(hab.getNumeroIndividu(), DateHelper.getCurrentYear());
		HistoriqueIndividu histo = ind.getDernierHistoriqueIndividu();

		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(23348L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi.setComplementNom("Service bidon");

		ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, hab, dpi);
		hab.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, dpi);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(DebiteurPrestationImposableIndexable.SUB_TYPE, indexable.getSubType());

		HashMap<String, String> values = indexable.getKeyValues();

		assertContains(dpi.getNumero().toString(), values.get(TiersSearchFields.NUMEROS));
		// On ne doit pas pouvoir rechercher sur le NO_IND
		assertNotContains(hab.getNumeroIndividu().toString(), values.get(TiersSearchFields.NUMEROS));
		assertContains("Débiteur IS", values.get(TiersIndexedData.ROLE_LIGNE1));
		assertContains("Réguliers", values.get(TiersIndexedData.ROLE_LIGNE2));
		assertContains(histo.getNom(), values.get(TiersSearchFields.NOM_RAISON));
		assertContains(histo.getNom(), values.get(TiersSearchFields.AUTRES_NOM));
		assertContains(histo.getPrenom(), values.get(TiersSearchFields.AUTRES_NOM));

		// Display
		assertContains("Nom1 débiteur", values.get(TiersIndexedData.NOM1));
		assertEquals("Nom2 débiteur", values.get(TiersIndexedData.NOM2));
	}

	@Test
	public void testDebiteurImpotSourceNonHabitantIndexable() throws Exception {

		PersonnePhysique nhab = new PersonnePhysique(false);
		nhab.setNumero(1234L);
		nhab.setNom("Bli");
		nhab.setPrenom("Bla");

		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(23348L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);

		ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, nhab, dpi);
		nhab.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, dpi);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(DebiteurPrestationImposableIndexable.SUB_TYPE, indexable.getSubType());

		HashMap<String, String> values = indexable.getKeyValues();
		// Search
		assertContains(dpi.getNumero().toString(), values.get(TiersSearchFields.NUMEROS));
		assertContains("Débiteur IS", values.get(TiersIndexedData.ROLE_LIGNE1));
		assertContains("Réguliers", values.get(TiersIndexedData.ROLE_LIGNE2));
		assertContains(nhab.getNom(), values.get(TiersSearchFields.NOM_RAISON));
		assertContains(nhab.getNom(), values.get(TiersSearchFields.AUTRES_NOM));
		assertContains(nhab.getPrenom(), values.get(TiersSearchFields.AUTRES_NOM));

		// Display
		assertContains("Nom1 débiteur", values.get(TiersIndexedData.NOM1));
		assertEquals("Nom2 débiteur", values.get(TiersIndexedData.NOM2));
	}
	@Test
	public void testDebiteurImpotSourceAutreCommuncauteIndexable() throws Exception {

		AutreCommunaute ac = new AutreCommunaute();
		ac.setNumero(1234L);
		ac.setNom("Nestle SA");
		ac.setComplementNom("Filiale de Orbe");

		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(23348L);
		dpi.setNom1("Nom1 débiteur");
		dpi.setNom2("Nom2 débiteur");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi.setComplementNom("Service bidon");

		ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ac, dpi);
		ac.addRapportSujet(contact);
		dpi.addRapportObjet(contact);

		DebiteurPrestationImposableIndexable indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, dpi);

		assertEquals(TiersIndexable.TYPE, indexable.getType());
		assertEquals(DebiteurPrestationImposableIndexable.SUB_TYPE, indexable.getSubType());

		HashMap<String, String> values = indexable.getKeyValues();

		// Search
		assertContains(dpi.getNumero().toString(), values.get(TiersSearchFields.NUMEROS));
		assertContains("Débiteur IS", values.get(TiersIndexedData.ROLE_LIGNE1));
		assertContains("Réguliers", values.get(TiersIndexedData.ROLE_LIGNE2));
		assertContains(ac.getNom(), values.get(TiersSearchFields.NOM_RAISON));
		assertContains(ac.getComplementNom(), values.get(TiersSearchFields.NOM_RAISON));

		// Display
		assertContains("Nom1 débiteur", values.get(TiersIndexedData.NOM1));
		assertContains("Nom2 débiteur", values.get(TiersIndexedData.NOM2));
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

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, nonHab);

		HashMap<String, String> values = indexable.getKeyValues();

		// Search
		assertEquals("8001", values.get(TiersSearchFields.NO_OFS_FOR_PRINCIPAL));
		assertContainsNoCase("5586", values.get(TiersSearchFields.NOS_OFS_AUTRES_FORS));
		assertContainsNoCase("5873", values.get(TiersSearchFields.NOS_OFS_AUTRES_FORS));
		assertContainsNoCase("5761", values.get(TiersSearchFields.NOS_OFS_AUTRES_FORS));
		assertContainsNoCase("8001", values.get(TiersSearchFields.NOS_OFS_AUTRES_FORS));

		// Display
		assertEquals("Le Brassus", values.get(TiersIndexedData.FOR_PRINCIPAL));
		assertEquals(RegDateHelper.toIndexString(dateOuverture), values.get(TiersIndexedData.DATE_OUVERTURE_FOR));
		assertEquals("", values.get(TiersIndexedData.DATE_FERMETURE_FOR));
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

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, nonHab);

		HashMap<String, String> values = indexable.getKeyValues();

		// for principal fermé -> tout null

		// Search
		assertNull(values.get(TiersSearchFields.NO_OFS_FOR_PRINCIPAL));
		// Display
		assertEquals("Cossonay", values.get(TiersIndexedData.FOR_PRINCIPAL));
		assertEquals(RegDateHelper.toIndexString(dateOuverture), values.get(TiersIndexedData.DATE_OUVERTURE_FOR));
		assertEquals(RegDateHelper.toIndexString(dateFermeture), values.get(TiersIndexedData.DATE_FERMETURE_FOR));
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

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, nonHab);

		HashMap<String, String> values = indexable.getKeyValues();

		assertEquals("1000", values.get(TiersIndexedData.NPA));
		assertContains("Lausanne", values.get(TiersSearchFields.LOCALITE_PAYS));
		assertContains("Lausanne", values.get(TiersIndexedData.LOCALITE));
		assertContains(Constants.OUI, values.get(TiersIndexedData.DOMICILE_VD));

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

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, nonHab);

		HashMap<String, String> values = indexable.getKeyValues();

		assertEquals("1000", values.get(TiersIndexedData.NPA));
		assertContains("Lausanne", values.get(TiersSearchFields.LOCALITE_PAYS));
		assertContains("Lausanne", values.get(TiersIndexedData.LOCALITE));
		assertEquals("", values.get(TiersIndexedData.DOMICILE_VD)); // adresse de domicile par défaut -> pas de détermination possible

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

		NonHabitantIndexable indexable = new NonHabitantIndexable(adresseService, tiersService, nonHab);

		HashMap<String, String> values = indexable.getKeyValues();

		//String s1 = values.get(TiersSearchFields.LOCALITE_PAYS);
		assertContains("abeilles", values.get(TiersIndexedData.RUE));
		assertEquals("", values.get(TiersIndexedData.NPA));
		assertContains("France", values.get(TiersSearchFields.LOCALITE_PAYS));
		assertContains("France", values.get(TiersSearchFields.LOCALITE_PAYS));
		assertContains("France", values.get(TiersIndexedData.LOCALITE_PAYS));
		assertContains("Paris", values.get(TiersIndexedData.LOCALITE));
		assertContains("France", values.get(TiersIndexedData.PAYS));
		assertContains(Constants.NON, values.get(TiersIndexedData.DOMICILE_VD));
	}

	@Test
	public void testContribuableAvecAdressesCivile() throws Exception {

		MockIndividu individu = (MockIndividu)serviceCivil.getIndividu(1234L, 2007);
		//HistoriqueIndividu histo = individu.getDernierHistoriqueIndividu();

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(1234L);
		hab.setNumeroIndividu(individu.getNoTechnique());

		// Ajout des adresses
		Set<AdresseTiers> adrs = new HashSet<AdresseTiers>();
		// Ancienne adresse courrier
		{
			AdresseCivile adresse = new AdresseCivile();
			adresse.setType(EnumTypeAdresse.COURRIER);
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			adresse.setDateDebut(RegDate.get(2001, 6, 21));
			//util.setDateFin(RegDate.get(2005, 11, 30));
			adresse.setTiers(hab);
			adrs.add(adresse);
		}
		hab.setAdressesTiers(adrs);

		HabitantIndexable indexable = new HabitantIndexable(adresseService, tiersService, hab, individu);

		HashMap<String, String> values = indexable.getKeyValues();

		//String s1 = values.get(TiersSearchFields.LOCALITE_PAYS);
		assertEquals("1880", values.get(TiersIndexedData.NPA));
		assertContains("Bex", values.get(TiersSearchFields.LOCALITE_PAYS));
		assertContains("Bex", values.get(TiersIndexedData.LOCALITE));
		assertContains(Constants.OUI, values.get(TiersIndexedData.DOMICILE_VD));

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

		MenageCommunIndexable indexable = new MenageCommunIndexable(adresseService, tiersService, mc);

		HashMap<String, String> values = indexable.getKeyValues();

		// Search
		//assertContains(numCtb1.toString(), values.get(TiersSearchFields.NUMEROS));
		//assertContains(numCtb2.toString(), values.get(TiersSearchFields.NUMEROS));
		assertContains("Maillard", values.get(TiersSearchFields.NOM_RAISON));
		assertContains("Gallet", values.get(TiersSearchFields.NOM_RAISON));
		assertContains(RegDateHelper.toIndexString(RegDate.get(1956, 1, 21)), values.get(TiersSearchFields.DATE_NAISSANCE));
		assertContains(RegDateHelper.toIndexString(RegDate.get(1967, 12, 3)), values.get(TiersSearchFields.DATE_NAISSANCE));
		assertContains(FormatNumeroHelper.formatAncienNumAVS("123.45.678"), values.get(TiersSearchFields.NUMERO_ASSURE_SOCIAL));
		assertContains(FormatNumeroHelper.formatAncienNumAVS("987.65.432"), values.get(TiersSearchFields.NUMERO_ASSURE_SOCIAL));
		// Display
		assertContains("Maillard", values.get(TiersIndexedData.NOM1));
		assertContains("Philippe", values.get(TiersIndexedData.NOM1));
		assertContains("Gallet", values.get(TiersIndexedData.NOM2));
		assertContains("Gladys", values.get(TiersIndexedData.NOM2));

		//assertContains("", values.get(TiersIndexedData.DATE_NAISSANCE));
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

		MenageCommunIndexable indexable = new MenageCommunIndexable(adresseService, tiersService, mc);

		HashMap<String, String> values = indexable.getKeyValues();

		// Search
		//assertContains(numCtb1.toString(), values.get(TiersSearchFields.NUMEROS));
		//assertContains(numCtb2.toString(), values.get(TiersSearchFields.NUMEROS));
		assertContains("Maillard", values.get(TiersSearchFields.NOM_RAISON));
		assertContains("Gallet", values.get(TiersSearchFields.NOM_RAISON));
		assertContains(RegDateHelper.toIndexString(dateN1), values.get(TiersSearchFields.DATE_NAISSANCE));
		assertContains(RegDateHelper.toIndexString(dateN2), values.get(TiersSearchFields.DATE_NAISSANCE));
		assertContains(FormatNumeroHelper.formatAncienNumAVS(noAVS1), values.get(TiersSearchFields.NUMERO_ASSURE_SOCIAL));
		assertContains(FormatNumeroHelper.formatAncienNumAVS(noAVS2), values.get(TiersSearchFields.NUMERO_ASSURE_SOCIAL));
		// Display
		assertContains("Maillard", values.get(TiersIndexedData.NOM1));
		assertContains("Philippe", values.get(TiersIndexedData.NOM1));
		assertContains("Gallet", values.get(TiersIndexedData.NOM2));
		assertContains("Gladys", values.get(TiersIndexedData.NOM2));

		//assertContains("", values.get(TiersIndexedData.DATE_NAISSANCE));
	}

	public static RapportEntreTiers addTiers(MenageCommun menage, PersonnePhysique tiers, RegDate dateDebut) {
		RapportEntreTiers appartenance = new AppartenanceMenage();
		appartenance.setDateDebut(dateDebut);
		appartenance.setDateFin(null);
		appartenance.setObjet(menage);
		appartenance.setSujet(tiers);

		menage.addRapportObjet(appartenance);
		tiers.addRapportSujet(appartenance);

		return appartenance;
	}
}
