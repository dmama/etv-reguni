package ch.vd.uniregctb.webservice.tiers2;

import ch.vd.uniregctb.webservices.tiers2.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Classe de test et d'exemple d'utilisation du web-service PM à l'usage de SIPF.
 * <p>
 * Basé sur le document de spécification d'Eric Wyss "Echanges Registe - SIPF particularités des personnes morales" v0.96 du 10 octobre
 * 2009.
 * <p>
 * <b>Note:</b> tous les tests sont actuellement désactivés parce que le service n'est pas implémenté.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class TiersServiceWebSIPFTest extends AbstractTiersServiceWebTest {

	// private static final Logger LOGGER = Logger.getLogger(WebitTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersServiceWebSIPFTest.xml";

	private UserLogin login;

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}

		login = new UserLogin();
		login.setUserId("[UT] TiersServiceWebSIPFTest");
		login.setOid(0);
	}

	@Test
	public void testFournirForsPM() throws Exception {

		final GetTiersPeriode params = new GetTiersPeriode();
		params.setLogin(login);
		params.setTiersNumber(222);
		params.setPeriode(2009);
		params.getParts().add(TiersPart.FORS_FISCAUX);
		params.getParts().add(TiersPart.REGIMES_FISCAUX);

		final PersonneMoraleHisto pm = (PersonneMoraleHisto) service.getTiersPeriode(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumero());
		assertEquals("Fiduciaire Turrian SA", trimValiPattern(pm.getPersonneContact()));
		assertEquals("KALESA", trimValiPattern(pm.getDesignationAbregee()));
		assertEquals("Kalesa S.A.", trimValiPattern(pm.getRaisonSociale1()));
		assertEquals("", trimValiPattern(pm.getRaisonSociale2()));
		assertEquals("en liquidation", trimValiPattern(pm.getRaisonSociale3()));

		// Récupération des informations des fors fiscaux

		final List<ForFiscal> forPrincipaux = pm.getForsFiscauxPrincipaux();
		assertNotNull(forPrincipaux);
		assertEquals(1, forPrincipaux.size());

		final ForFiscal ffp0 = forPrincipaux.get(0);
		assertNotNull(ffp0);
		// Note : les communes hors-canton et les pays hors-Suisse sont aussi retourné. C'est à l'appelant de faire le tri si nécessaire.
		assertSameDay(newDate(1979,8,7), ffp0.getDateOuverture());
		assertNull(ffp0.getDateFermeture());
		assertEquals(5413, ffp0.getNoOfsAutoriteFiscale());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp0.getTypeAutoriteFiscale());
		assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp0.getGenreImpot());
		assertEquals(MotifRattachement.DOMICILE, ffp0.getMotifRattachement());

		final List<ForFiscal> forSecondaires = pm.getAutresForsFiscaux();
		assertEmpty(forSecondaires);
		// etc...

		// Récupération des informations des régimes fiscaux

		final List<RegimeFiscal> regimesICC = pm.getRegimesFiscauxICC();
		assertNotNull(regimesICC);
		assertEquals(1, regimesICC.size());

		final RegimeFiscal icc0 = regimesICC.get(0);
		assertNotNull(icc0);
		assertEquals("01", icc0.getCode()); // selon table TY_REGIME_FISCAL
		assertSameDay(newDate(1993, 1, 1), icc0.getDateDebut());
		assertNull(icc0.getDateFin());
		// note : la catégorie de PM se déduit du code

		final List<RegimeFiscal> regimesIFD = pm.getRegimesFiscauxIFD();
		assertNotNull(regimesIFD);
		assertEquals(1, regimesIFD.size());

		final RegimeFiscal ifd0 = regimesIFD.get(0);
		assertNotNull(ifd0);
		assertEquals("01", ifd0.getCode()); // selon table TY_REGIME_FISCAL
		assertSameDay(newDate(1993, 1, 1), ifd0.getDateDebut());
		assertNull(ifd0.getDateFin());
	}

	@Test
	public void testFournirCapital() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(222);
		params.setDate(newDate(2009, 10, 14));
		params.getParts().add(TiersPart.CAPITAUX);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumero());
		assertEquals("Fiduciaire Turrian SA", trimValiPattern(pm.getPersonneContact()));
		assertEquals("KALESA", trimValiPattern(pm.getDesignationAbregee()));
		assertEquals("Kalesa S.A.", trimValiPattern(pm.getRaisonSociale1()));
		assertEquals("", trimValiPattern(pm.getRaisonSociale2()));
		assertEquals("en liquidation", trimValiPattern(pm.getRaisonSociale3()));

		// Récupération du capital

		final Capital capital = pm.getCapital();
		assertNotNull(capital);
		assertEquals(150000, capital.getCapitalLibere());
		assertEquals(150000, capital.getCapitalAction());

		// note : il est de la responsabilité de l'appelant de déterminer si l'abscence ou non du capital libéré est normale ou non. Pour
		// rappel, cette abscence justifiée ou non se déduit de la catégorie de PM (= normal pour une APM, d'après le document d'Eric Wyss).
		// Cette catégorie est elle-même déduite du code du régime fiscal.
	}

	@Test
	public void testFournirEvenementsPMParNumero() throws Exception {

		final SearchEvenementsPM params = new SearchEvenementsPM();
		params.setLogin(login);
		params.setTiersNumber(222L);

		final List<EvenementPM> events = service.searchEvenementsPM(params).getItem();
		assertNotNull(events);
		assertEquals(17, events.size());

		final EvenementPM ev0 = events.get(0);
		assertNotNull(ev0);
		assertSameDay(newDate(1979, 10, 30), ev0.getDateEvenement());
		assertEquals("007", ev0.getCodeEvenement()); // selon table EVENEMENT du host
		assertEquals(Long.valueOf(222), ev0.getTiersNumber());

		final EvenementPM ev1 = events.get(1);
		assertNotNull(ev1);
		assertSameDay(newDate(1979, 10, 30), ev1.getDateEvenement());
		assertEquals("026", ev1.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev1.getTiersNumber());

		final EvenementPM ev2 = events.get(2);
		assertNotNull(ev2);
		assertSameDay(newDate(1980, 1, 1), ev2.getDateEvenement());
		assertEquals(Long.valueOf(222), ev2.getTiersNumber());
		assertEquals("003", ev2.getCodeEvenement());

		final EvenementPM ev3 = events.get(3);
		assertNotNull(ev3);
		assertSameDay(newDate(1992, 1, 1), ev3.getDateEvenement());
		assertEquals("001", ev3.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev3.getTiersNumber());

		final EvenementPM ev4 = events.get(4);
		assertNotNull(ev4);
		assertSameDay(newDate(1992, 1, 1), ev4.getDateEvenement());
		assertEquals("001", ev4.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev4.getTiersNumber());

		final EvenementPM ev5 = events.get(5);
		assertNotNull(ev5);
		assertSameDay(newDate(1992, 11, 6), ev5.getDateEvenement());
		assertEquals("021", ev5.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev5.getTiersNumber());
		
		final EvenementPM ev6 = events.get(6);
		assertNotNull(ev6);
		assertSameDay(newDate(1996, 10, 24), ev6.getDateEvenement());
		assertEquals(Long.valueOf(222), ev6.getTiersNumber());
		assertEquals("020", ev6.getCodeEvenement());

		final EvenementPM ev7 = events.get(7);
		assertNotNull(ev7);
		assertSameDay(newDate(1997, 7, 10), ev7.getDateEvenement());
		assertEquals(Long.valueOf(222), ev7.getTiersNumber());
		assertEquals("008", ev7.getCodeEvenement());

		final EvenementPM ev8 = events.get(8);
		assertNotNull(ev8);
		assertSameDay(newDate(1997, 12, 1), ev8.getDateEvenement());
		assertEquals(Long.valueOf(222), ev8.getTiersNumber());
		assertEquals("020", ev8.getCodeEvenement());

		final EvenementPM ev9 = events.get(9);
		assertNotNull(ev9);
		assertSameDay(newDate(2000, 1, 1), ev9.getDateEvenement());
		assertEquals(Long.valueOf(222), ev9.getTiersNumber());
		assertEquals("020", ev9.getCodeEvenement());

		final EvenementPM ev10 = events.get(10);
		assertNotNull(ev10);
		assertSameDay(newDate(2001, 9, 6), ev10.getDateEvenement());
		assertEquals(Long.valueOf(222), ev10.getTiersNumber());
		assertEquals("037", ev10.getCodeEvenement());

		final EvenementPM ev11 = events.get(11);
		assertNotNull(ev11);
		assertSameDay(newDate(2003, 4, 3), ev11.getDateEvenement());
		assertEquals(Long.valueOf(222), ev11.getTiersNumber());
		assertEquals("003", ev11.getCodeEvenement());

		final EvenementPM ev12 = events.get(12);
		assertNotNull(ev12);
		assertSameDay(newDate(2003, 4, 3), ev12.getDateEvenement());
		assertEquals(Long.valueOf(222), ev12.getTiersNumber());
		assertEquals("003", ev12.getCodeEvenement());

		final EvenementPM ev13 = events.get(13);
		assertNotNull(ev13);
		assertSameDay(newDate(2003, 4, 3), ev13.getDateEvenement());
		assertEquals(Long.valueOf(222), ev13.getTiersNumber());
		assertEquals("016", ev13.getCodeEvenement());

		final EvenementPM ev14 = events.get(14);
		assertNotNull(ev14);
		assertSameDay(newDate(2003, 4, 3), ev14.getDateEvenement());
		assertEquals(Long.valueOf(222), ev14.getTiersNumber());
		assertEquals("023", ev14.getCodeEvenement());

		final EvenementPM ev15 = events.get(15);
		assertNotNull(ev15);
		assertSameDay(newDate(2003, 11, 6), ev15.getDateEvenement());
		assertEquals(Long.valueOf(222), ev15.getTiersNumber());
		assertEquals("002", ev15.getCodeEvenement());

		final EvenementPM ev16 = events.get(16);
		assertNotNull(ev16);
		assertSameDay(newDate(2003, 11, 6), ev16.getDateEvenement());
		assertEquals(Long.valueOf(222), ev16.getTiersNumber());
		assertEquals("002", ev16.getCodeEvenement());
	}

	@Test
	public void testFournirEvenementsPMParCode() throws Exception {

		final SearchEvenementsPM params = new SearchEvenementsPM();
		params.setLogin(login);
		params.setCodeEvenement("001");
		params.setTiersNumber(222L); // pas obligatoire, juste pour limiter le nombre de résultats

		final List<EvenementPM> events = service.searchEvenementsPM(params).getItem();
		assertNotNull(events);
		assertEquals(2, events.size());

		final EvenementPM ev0 = events.get(1);
		assertNotNull(ev0);
		assertSameDay(newDate(1992, 1, 1), ev0.getDateEvenement());
		assertEquals("001", ev0.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev0.getTiersNumber());

		final EvenementPM ev1 = events.get(1);
		assertNotNull(ev1);
		assertSameDay(newDate(1992, 1, 1), ev1.getDateEvenement());
		assertEquals("001", ev1.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev1.getTiersNumber());
	}

	/**
	 * [UNIREG-2039] vérifie que les paramètres date mini et date maxi fonctionnent correctement.
	 */
	@Test
	public void testFournirEvenementsPMDateMiniMaxi() throws Exception {

		final SearchEvenementsPM params = new SearchEvenementsPM();
		params.setLogin(login);
		params.setDateMinimale(newDate(2000, 1, 1));
		params.setDateMaximale(newDate(2003, 7, 1));
		params.setTiersNumber(222L);

		final List<EvenementPM> events = service.searchEvenementsPM(params).getItem();
		assertNotNull(events);
		assertEquals(6, events.size());

		final EvenementPM ev0 = events.get(0);
		assertNotNull(ev0);
		assertSameDay(newDate(2000, 1, 1), ev0.getDateEvenement());
		assertEquals("020", ev0.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev0.getTiersNumber());

		final EvenementPM ev1 = events.get(1);
		assertNotNull(ev1);
		assertSameDay(newDate(2001, 9, 6), ev1.getDateEvenement());
		assertEquals("037", ev1.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev1.getTiersNumber());

		final EvenementPM ev2 = events.get(2);
		assertNotNull(ev2);
		assertSameDay(newDate(2003, 4, 3), ev2.getDateEvenement());
		assertEquals("003", ev2.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev2.getTiersNumber());

		final EvenementPM ev3 = events.get(3);
		assertNotNull(ev3);
		assertSameDay(newDate(2003, 4, 3), ev3.getDateEvenement());
		assertEquals("003", ev3.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev3.getTiersNumber());

		final EvenementPM ev4 = events.get(4);
		assertNotNull(ev4);
		assertSameDay(newDate(2003, 4, 3), ev4.getDateEvenement());
		assertEquals("016", ev4.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev4.getTiersNumber());

		final EvenementPM ev5 = events.get(5);
		assertNotNull(ev5);
		assertSameDay(newDate(2003, 4, 3), ev5.getDateEvenement());
		assertEquals("023", ev5.getCodeEvenement());
		assertEquals(Long.valueOf(222), ev5.getTiersNumber());
	}

	@Test
	public void testFournirCoordonneesFinancieres() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(222);
		params.setDate(newDate(2009, 10, 14));
		params.getParts().add(TiersPart.COMPTES_BANCAIRES);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumero());

		final List<CompteBancaire> comptes = pm.getComptesBancaires();
		assertNotNull(comptes);
		assertEquals(1, comptes.size());

		// Récupération du compte bancaire de la PM

		final CompteBancaire comptePM = comptes.get(0);
		assertNotNull(comptePM);
		assertEquals("18-25277-7", comptePM.getNumero());
		assertNull(comptePM.getClearing());
		assertNull(comptePM.getAdresseBicSwift());
		assertEquals(FormatNumeroCompte.SPECIFIQUE_CH, comptePM.getFormat());
		assertNull(comptePM.getTitulaire());
		assertEquals("La Poste Suisse", comptePM.getNomInstitution());
	}

	@Ignore // exemple fictif
	@Test
	public void testFournirCoordonneesFinancieresAvecMandataire() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(123456);
		params.setDate(newDate(2009, 10, 14));
		params.getParts().add(TiersPart.COMPTES_BANCAIRES);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(123456L, pm.getNumero());

		final List<CompteBancaire> comptes = pm.getComptesBancaires();
		assertNotNull(comptes);
		assertEquals(2, comptes.size());

		final CompteBancaire compte0 = comptes.get(0);
		final CompteBancaire compte1 = comptes.get(0);

		// Récupération du compte bancaire de la PM

		final CompteBancaire comptePM = (compte0.getNumeroTiersTitulaire() == pm.getNumero() ? compte0 : compte1);
		assertNotNull(comptePM);
		assertEquals("<un numéro IBAN>", comptePM.getNumero());
		assertEquals("<un numéro de clearing>", comptePM.getClearing());
		assertNull(comptePM.getAdresseBicSwift());
		assertEquals(FormatNumeroCompte.IBAN, comptePM.getFormat());
		assertEquals("Ma petite entreprise", comptePM.getTitulaire());
		assertEquals("Banque de la place", comptePM.getNomInstitution());

		// Récupération du compte bancaire du mandataire

		final CompteBancaire compteMandataire = (compte0.getNumeroTiersTitulaire() == pm.getNumero() ? compte1 : compte0);
		assertNotNull(compteMandataire);
		assertEquals("<un numéro IBAN>", compteMandataire.getNumero());
		assertEquals("<un numéro de clearing>", compteMandataire.getClearing());
		assertNull(compteMandataire.getAdresseBicSwift());
		assertEquals(FormatNumeroCompte.IBAN, compteMandataire.getFormat());
		assertEquals("Maître George-Edouard Dubeauchapeau", compteMandataire.getTitulaire());
		assertEquals("Banque privée Piquet S.A.", compteMandataire.getNomInstitution());
	}

	/**
	 * [UNIREG-2106] teste que les coordonnées fiscales d'un mandataire de type 'T' sont bien exposées
	 */
	@Test
	public void testFournirCoordonneesFinancieresAvecMandatairePM() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(32592);
		params.setDate(newDate(2010, 3, 17));
		params.getParts().add(TiersPart.COMPTES_BANCAIRES);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(32592L, pm.getNumero());

		final List<CompteBancaire> comptes = pm.getComptesBancaires();
		assertNotNull(comptes);
		assertEquals(1, comptes.size());

		final CompteBancaire compteMandataire = comptes.get(0);
		assertNotNull(compteMandataire);
		assertEquals(426, compteMandataire.getNumeroTiersTitulaire());
		assertEquals("230-575.013.03", compteMandataire.getNumero());
		assertNull(compteMandataire.getClearing());
		assertNull(compteMandataire.getAdresseBicSwift());
		assertEquals(FormatNumeroCompte.SPECIFIQUE_CH, compteMandataire.getFormat());
		assertEquals("Deloitte AG", trimValiPattern(compteMandataire.getTitulaire()));
		assertEquals("UBS AG", trimValiPattern(compteMandataire.getNomInstitution()));
	}

	@Test
	public void testFournirAdresseCourrier() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(222);
		params.setDate(newDate(2009, 10, 14));
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);


		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumero());
		assertEquals("Fiduciaire Turrian SA", trimValiPattern(pm.getPersonneContact()));
		assertEquals("KALESA", trimValiPattern(pm.getDesignationAbregee()));
		assertEquals("Kalesa S.A.", trimValiPattern(pm.getRaisonSociale1()));
		assertEquals("", trimValiPattern(pm.getRaisonSociale2()));
		assertEquals("en liquidation", trimValiPattern(pm.getRaisonSociale3()));

		final Adresse adresseCourrier = pm.getAdresseCourrier();
		assertNotNull(adresseCourrier);
		assertSameDay(newDate(2003,4,3), adresseCourrier.getDateDebut());
		assertNull(adresseCourrier.getDateFin());
		assertEquals("p.a. Office des faillites", adresseCourrier.getTitre());
		assertNull(adresseCourrier.getCasePostale());
		assertNull(adresseCourrier.getNumeroAppartement());
		assertNull(adresseCourrier.getRue());
		assertNull(adresseCourrier.getNumeroRue());
		assertEquals("1860", adresseCourrier.getNumeroPostal());
		assertEquals("Aigle", adresseCourrier.getLocalite());
		assertNull(adresseCourrier.getPays());
		assertEquals(1100, adresseCourrier.getNoOrdrePostal());
		assertNull(adresseCourrier.getNoRue());

		// Récupération de l'adresse d'envoi de la PM

		final AdresseEnvoi adresseEnvoi = pm.getAdresseEnvoi();
		assertNotNull(adresseEnvoi);
		assertEquals("Kalesa S.A.", trimValiPattern(adresseEnvoi.getLigne1()));
		assertEquals("", trimValiPattern(adresseEnvoi.getLigne2()));
		assertEquals("en liquidation", trimValiPattern(adresseEnvoi.getLigne3()));
		assertEquals("p.a. Office des faillites", trimValiPattern(adresseEnvoi.getLigne4()));
		assertEquals("1860 Aigle", trimValiPattern(adresseEnvoi.getLigne5()));
		assertNull(adresseEnvoi.getLigne6());
		assertTrue(adresseEnvoi.isIsSuisse());
		assertEquals(TypeAffranchissement.SUISSE, adresseEnvoi.getTypeAffranchissement());
	}

	/**
	 * [UNIREG-1974]
	 */
	@Test
	public void testFournirAdresseCourrierLocaliteAbregee() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1314);
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);


		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(1314L, pm.getNumero());
		assertEquals("R. Borgo", trimValiPattern(pm.getPersonneContact()));
		assertEquals("JAL HOLDING", trimValiPattern(pm.getDesignationAbregee()));
		assertEquals("Jal holding S.A.", trimValiPattern(pm.getRaisonSociale1()));
		assertEquals("", trimValiPattern(pm.getRaisonSociale2()));
		assertEquals("en liquidation", trimValiPattern(pm.getRaisonSociale3()));

		final Adresse adresseCourrier = pm.getAdresseCourrier();
		assertNotNull(adresseCourrier);
		assertSameDay(newDate(2007, 6, 11), adresseCourrier.getDateDebut());
		assertNull(adresseCourrier.getDateFin());
		assertEquals("pa Fidu. Commerce & Industrie", adresseCourrier.getTitre());
		assertNull(adresseCourrier.getCasePostale());
		assertNull(adresseCourrier.getNumeroAppartement());
		assertEquals("Avenue de la Gare", adresseCourrier.getRue());
		assertEquals("10", adresseCourrier.getNumeroRue());
		assertEquals("1003", adresseCourrier.getNumeroPostal());
		assertEquals("Lausanne", adresseCourrier.getLocalite());
		assertNull(adresseCourrier.getPays());
		assertEquals(150, adresseCourrier.getNoOrdrePostal());
		assertEquals(Integer.valueOf(30317), adresseCourrier.getNoRue());

		// Récupération de l'adresse d'envoi de la PM

		final AdresseEnvoi adresseEnvoi = pm.getAdresseEnvoi();
		assertNotNull(adresseEnvoi);
		assertEquals("Jal holding S.A.", trimValiPattern(adresseEnvoi.getLigne1()));
		assertEquals("", trimValiPattern(adresseEnvoi.getLigne2()));
		assertEquals("en liquidation", trimValiPattern(adresseEnvoi.getLigne3()));
		assertEquals("pa Fidu. Commerce & Industrie", trimValiPattern(adresseEnvoi.getLigne4()));
		assertEquals("Avenue de la Gare 10", trimValiPattern(adresseEnvoi.getLigne5()));
		assertEquals("1003 Lausanne", trimValiPattern(adresseEnvoi.getLigne6()));
		assertTrue(adresseEnvoi.isIsSuisse());
		assertEquals(TypeAffranchissement.SUISSE, adresseEnvoi.getTypeAffranchissement());
	}

	/**
	 * [UNIREG-1973] la personne de contact de la PM ne doit pas apparaître dans l'adresse d'envoi.
	 */
	@Test
	public void testFournirAdresseEnvoi() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(25000);
		params.setDate(newDate(2009, 10, 14));
		params.getParts().add(TiersPart.ADRESSES_ENVOI);


		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);

		// Récupération de l'adresse d'envoi de la PM

		final AdresseEnvoi adresseEnvoi = pm.getAdresseEnvoi();
		assertNotNull(adresseEnvoi);
		assertEquals("Fonds prévoyance en fa", trimValiPattern(adresseEnvoi.getLigne1()));
		assertEquals("personnel Sté électriq", trimValiPattern(adresseEnvoi.getLigne2()));
		assertEquals("intercommunale de la C", trimValiPattern(adresseEnvoi.getLigne3()));
		assertEquals("Rte des Avouillons 2 / CP 321", trimValiPattern(adresseEnvoi.getLigne4()));
		assertEquals("1196 Gland", trimValiPattern(adresseEnvoi.getLigne5()));
		assertNull(adresseEnvoi.getLigne6());
		assertTrue(adresseEnvoi.isIsSuisse());
		assertEquals(TypeAffranchissement.SUISSE, adresseEnvoi.getTypeAffranchissement());
	}

	@Test
	public void testFournirAdresseContentieuxPM() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(37); // passé de la PM 222 à la PM 37 parce quelqu'un s'est amusé à entrer des valeurs bidon en développement...
		params.setDate(newDate(2009, 10, 14));
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(37L, pm.getNumero());
		assertEquals("Fiduciaire Pierre Terrier", trimValiPattern(pm.getPersonneContact()));
		assertEquals("FIBER SEAL ROMANDIE", trimValiPattern(pm.getDesignationAbregee()));
		assertEquals("Fiber Seal (Romandie)", trimValiPattern(pm.getRaisonSociale1()));
		assertEquals("", trimValiPattern(pm.getRaisonSociale2()));
		assertEquals("en liquidation", trimValiPattern(pm.getRaisonSociale3()));

		// Récupération des adresses de domicile (pour le contentieux)

		final Adresse adresseDomicile = pm.getAdresseDomicile();
		assertNotNull(adresseDomicile);
		assertSameDay(newDate(1996, 4, 11), adresseDomicile.getDateDebut());
		assertNull(adresseDomicile.getDateFin());
		assertNull(adresseDomicile.getTitre());
		assertNull(adresseDomicile.getCasePostale());
		assertNull(adresseDomicile.getNumeroAppartement());
		assertEquals("Quai du Seujet", adresseDomicile.getRue());
		assertEquals("28A", adresseDomicile.getNumeroRue());
		assertEquals("1201", adresseDomicile.getNumeroPostal());
		assertEquals("Genève", adresseDomicile.getLocalite());
		assertNull(adresseDomicile.getPays());
		assertEquals(367, adresseDomicile.getNoOrdrePostal());
		assertEquals(Integer.valueOf(46421), adresseDomicile.getNoRue());

		final AdresseEnvoi adresseDomicileFormattee = pm.getAdresseDomicileFormattee();
		assertNotNull(adresseDomicileFormattee);
		assertEquals("Fiber Seal (Romandie)", trimValiPattern(adresseDomicileFormattee.getLigne1()));
		assertEquals("", trimValiPattern(adresseDomicileFormattee.getLigne2()));
		assertEquals("en liquidation", trimValiPattern(adresseDomicileFormattee.getLigne3()));
		assertEquals("Quai du Seujet 28A", trimValiPattern(adresseDomicileFormattee.getLigne4()));
		assertEquals("1201 Genève", trimValiPattern(adresseDomicileFormattee.getLigne5()));
		assertNull(adresseDomicileFormattee.getLigne6());
		assertTrue(adresseDomicileFormattee.isIsSuisse());
		assertEquals(TypeAffranchissement.SUISSE, adresseDomicileFormattee.getTypeAffranchissement());

		// Récupération des adresses de poursuite

		final Adresse adressePoursuite = pm.getAdressePoursuite();
		assertNotNull(adressePoursuite);
		assertSameDay(newDate(1996, 4, 11), adressePoursuite.getDateDebut());
		assertNull(adressePoursuite.getDateFin());
		assertNull(adressePoursuite.getTitre());
		assertNull(adressePoursuite.getCasePostale());
		assertNull(adressePoursuite.getNumeroAppartement());
		assertEquals("Quai du Seujet", adressePoursuite.getRue());
		assertEquals("28A", adressePoursuite.getNumeroRue());
		assertEquals("1201", adressePoursuite.getNumeroPostal());
		assertEquals("Genève", adressePoursuite.getLocalite());
		assertNull(adressePoursuite.getPays());
		assertEquals(367, adressePoursuite.getNoOrdrePostal());
		assertEquals(Integer.valueOf(46421), adressePoursuite.getNoRue());

		final AdresseEnvoi adressePoursuiteFormattee = pm.getAdressePoursuiteFormattee();
		assertNotNull(adressePoursuiteFormattee);
		assertEquals("Fiber Seal (Romandie)", trimValiPattern(adressePoursuiteFormattee.getLigne1()));
		assertEquals("", trimValiPattern(adressePoursuiteFormattee.getLigne2()));
		assertEquals("en liquidation", trimValiPattern(adressePoursuiteFormattee.getLigne3()));
		assertEquals("Quai du Seujet 28A", trimValiPattern(adressePoursuiteFormattee.getLigne4()));
		assertEquals("1201 Genève", trimValiPattern(adressePoursuiteFormattee.getLigne5()));
		assertNull(adressePoursuiteFormattee.getLigne6());
		assertTrue(adressePoursuiteFormattee.isIsSuisse());
		assertEquals(TypeAffranchissement.SUISSE, adressePoursuiteFormattee.getTypeAffranchissement());

		// Unireg n'est pas en mesure de déterminer l'adresse de l'OP. Ce travail est de la responsabilité du service infastructure.

		// Unireg n'est pas en mesure de retourner le nom et de prénom de l'administrateur, cette information n'est pas disponible.
	}

	@Test
	public void testFournirInformationDeConsultation() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(222);
		params.setDate(null); // null -> pour obtenir l'état le plus récent; ou une date pour un état antérieur
		params.getParts().add(TiersPart.SIEGES);
		params.getParts().add(TiersPart.FORS_FISCAUX);
		params.getParts().add(TiersPart.FORMES_JURIDIQUES);
		params.getParts().add(TiersPart.REGIMES_FISCAUX);
		params.getParts().add(TiersPart.ASSUJETTISSEMENTS);
		params.getParts().add(TiersPart.ETATS_PM);
		params.getParts().add(TiersPart.CAPITAUX);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumero());
		assertEquals("Fiduciaire Turrian SA", trimValiPattern(pm.getPersonneContact()));
		assertEquals("KALESA", trimValiPattern(pm.getDesignationAbregee()));
		assertEquals("Kalesa S.A.", trimValiPattern(pm.getRaisonSociale1()));
		assertEquals("", trimValiPattern(pm.getRaisonSociale2()));
		assertEquals("en liquidation", trimValiPattern(pm.getRaisonSociale3()));

		// Siege actif
		final Siege siege = pm.getSiege();
		assertNotNull(siege);
		assertSameDay(newDate(1979,8,7), siege.getDateDebut());
		assertNull(siege.getDateFin());
		assertEquals(TypeSiege.COMMUNE_CH, siege.getTypeSiege());
		assertEquals(5413, siege.getNoOfsSiege());

		// For principal actif
		final ForFiscal ffp = pm.getForFiscalPrincipal();
		assertNull(ffp.getDateFermeture());
		assertEquals(5413, ffp.getNoOfsAutoriteFiscale());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
		assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
		// note : le nom de la commune/pays doit être demandé au service infrastructure

		// Fors secondaires actifs
		final List<ForFiscal> forsSecondaires = pm.getAutresForsFiscaux();
		assertEmpty(forsSecondaires);
		// note : le nom de la commune doit être demandé au service infrastructure

		// Forme juridique
		final FormeJuridique formeJuridique = pm.getFormeJuridique();
		assertNotNull(formeJuridique);
		assertSameDay(newDate(1979, 8, 7), formeJuridique.getDateDebut());
		assertNull(formeJuridique.getDateFin());
		assertEquals("S.A.", formeJuridique.getCode()); // code selon table FORME_JURIDIQ_ACI

		// Régime fiscal ICC
		final RegimeFiscal regimeICC = pm.getRegimeFiscalICC();
		assertNotNull(regimeICC);
		assertSameDay(newDate(1993, 1, 1), regimeICC.getDateDebut());
		assertNull(regimeICC.getDateFin());
		assertEquals("01", regimeICC.getCode()); // selon table TY_REGIME_FISCAL

		// Date de fin du dernier exercice commercial
		assertNull(pm.getDateFinDernierExerciceCommercial());

		// Date de bouclement future
		assertSameDay(newDate(2003, 12, 31), pm.getDateBouclementFutur());

		// Dates de début et de fin de l'assujettissement LIC
		final Assujettissement assujettissementLIC = pm.getAssujettissementLIC();
		assertNull(assujettissementLIC);

		// Dates de début et de fin de l'assujettissement LIFD
		final Assujettissement assujettissementLIFD = pm.getAssujettissementLIFD();
		assertNull(assujettissementLIFD);

		// Numéro IPMRO
		assertEquals("01880", pm.getNumeroIPMRO());

		// Code blocage remboursement automatique
		assertTrue(pm.isBlocageRemboursementAutomatique());

		// Date de validite et code de l'état de la PM
		final EtatPM etat = pm.getEtat();
		assertNotNull(etat);
		assertSameDay(newDate(2003, 11, 6), etat.getDateDebut());
		assertNull(etat.getDateFin());
		assertEquals("06", etat.getCode()); // selon table ETAT du host
		// note : le libellé du dernier état doit être demandé au service infrastructure

		// Capital libéré + absence normale ou non
		final Capital capital = pm.getCapital();
		assertNotNull(capital);
		assertEquals(150000, capital.getCapitalAction());
		assertEquals(150000, capital.getCapitalLibere());
		assertFalse(capital.isAbsenceCapitalLibereNormale());
	}

	@Test
	public void testFournirAssujettissements() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(222);
		params.getParts().add(TiersPart.ASSUJETTISSEMENTS);

		final PersonneMoraleHisto pm = (PersonneMoraleHisto) service.getTiersHisto(params);
		assertNotNull(pm);
		assertEquals(222L, pm.getNumero());
		assertEquals("Fiduciaire Turrian SA", trimValiPattern(pm.getPersonneContact()));
		assertEquals("KALESA", trimValiPattern(pm.getDesignationAbregee()));
		assertEquals("Kalesa S.A.", trimValiPattern(pm.getRaisonSociale1()));
		assertEquals("", trimValiPattern(pm.getRaisonSociale2()));
		assertEquals("en liquidation", trimValiPattern(pm.getRaisonSociale3()));

		final List<Assujettissement> lic = pm.getAssujettissementsLIC();
		assertEquals(1, lic.size());

		final Assujettissement lic0 = lic.get(0);
		assertNotNull(lic0);
		assertSameDay(newDate(1992, 12, 31), lic0.getDateDebut());
		assertSameDay(newDate(2003, 12, 31), lic0.getDateFin());
		assertEquals(TypeAssujettissement.ILLIMITE, lic0.getType());

		final List<Assujettissement> lifd = pm.getAssujettissementsLIFD();
		assertEquals(1, lifd.size());

		final Assujettissement lifd0 = lifd.get(0);
		assertNotNull(lifd0);
		assertSameDay(newDate(1992, 12, 31), lifd0.getDateDebut());
		assertSameDay(newDate(2003, 12, 31), lifd0.getDateFin());
		assertEquals(TypeAssujettissement.ILLIMITE, lifd0.getType());
	}

	/**
	 * [UNIREG-1974] Vérifie que l'adresse de la fiduciaire Jal Holding utilise bien les trois lignes de la raison sociale et non pas la raison sociale abbrégée.
	 */
	@Test
	public void testGetAdresseEnvoiPM() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1314); // Jal Holding
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(1314L, pm.getNumero());

		final AdresseEnvoi adresseCourrier = pm.getAdresseEnvoi();
		assertNotNull(adresseCourrier);
		assertEquals("Jal holding S.A.", trimValiPattern(adresseCourrier.getLigne1())); // <-- raison sociale ligne 1
		assertEquals("", trimValiPattern(adresseCourrier.getLigne2())); // <-- raison sociale ligne 2 (qui ne contient que la pattern de validation)
		assertEquals("en liquidation", trimValiPattern(adresseCourrier.getLigne3())); // <-- raison sociale ligne 3
		assertEquals("pa Fidu. Commerce & Industrie", adresseCourrier.getLigne4());
		assertEquals("Avenue de la Gare 10", adresseCourrier.getLigne5());
		assertEquals("1003 Lausanne", adresseCourrier.getLigne6());
		assertTrue(adresseCourrier.isIsSuisse());
		assertEquals(TypeAffranchissement.SUISSE, adresseCourrier.getTypeAffranchissement());
		assertNull(adresseCourrier.getSalutations());
		assertEquals("Madame, Monsieur", adresseCourrier.getFormuleAppel());

		final AdresseEnvoi adresseDomicile = pm.getAdresseDomicileFormattee();
		assertNotNull(adresseDomicile);
		assertEquals("Jal holding S.A.", trimValiPattern(adresseDomicile.getLigne1())); // <-- raison sociale ligne 1
		assertEquals("", trimValiPattern(adresseDomicile.getLigne2())); // <-- raison sociale ligne 2 (qui ne contient que la pattern de validation)
		assertEquals("en liquidation", trimValiPattern(adresseDomicile.getLigne3())); // <-- raison sociale ligne 3
		assertEquals("Fid.Commerce & Industrie S.A.", adresseDomicile.getLigne4());
		assertEquals("Chemin Messidor 5", adresseDomicile.getLigne5());
		assertEquals("1006 Lausanne", adresseDomicile.getLigne6());
		assertTrue(adresseDomicile.isIsSuisse());
		assertNull(adresseDomicile.getSalutations());
		assertEquals("Madame, Monsieur", adresseDomicile.getFormuleAppel());
	}
}
