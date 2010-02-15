package ch.vd.uniregctb.webservice.tiers2;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers2.Adresse;
import ch.vd.uniregctb.webservices.tiers2.AdresseEnvoi;
import ch.vd.uniregctb.webservices.tiers2.Assujettissement;
import ch.vd.uniregctb.webservices.tiers2.Capital;
import ch.vd.uniregctb.webservices.tiers2.CompteBancaire;
import ch.vd.uniregctb.webservices.tiers2.EtatPM;
import ch.vd.uniregctb.webservices.tiers2.EvenementPM;
import ch.vd.uniregctb.webservices.tiers2.ForFiscal;
import ch.vd.uniregctb.webservices.tiers2.FormatNumeroCompte;
import ch.vd.uniregctb.webservices.tiers2.FormeJuridique;
import ch.vd.uniregctb.webservices.tiers2.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers2.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers2.PersonneMoraleHisto;
import ch.vd.uniregctb.webservices.tiers2.RegimeFiscal;
import ch.vd.uniregctb.webservices.tiers2.SearchEvenementsPM;
import ch.vd.uniregctb.webservices.tiers2.Siege;
import ch.vd.uniregctb.webservices.tiers2.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.TypeAutoriteFiscale;
import ch.vd.uniregctb.webservices.tiers2.TypeSiege;
import ch.vd.uniregctb.webservices.tiers2.UserLogin;

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

	@Ignore
	@Test
	public void testFournirForsPM() throws Exception {

		final GetTiersPeriode params = new GetTiersPeriode();
		params.setLogin(login);
		params.setTiersNumber(123456);
		params.setPeriode(2009);
		params.getParts().add(TiersPart.FORS_FISCAUX);
		params.getParts().add(TiersPart.REGIMES_FISCAUX);

		final PersonneMoraleHisto pm = (PersonneMoraleHisto) service.getTiersPeriode(params);
		assertNotNull(pm);
		assertEquals(123456L, pm.getNumero());
		assertEquals("Ma petite entreprise", pm.getDesignationAbregee());

		// Récupération des informations des fors fiscaux

		final List<ForFiscal> forPrincipaux = pm.getForsFiscauxPrincipaux();
		assertNotNull(forPrincipaux);
		assertEquals(1, forPrincipaux.size());

		final ForFiscal ffp0 = forPrincipaux.get(0);
		assertNotNull(ffp0);
		// Note : les communes hors-canton et les pays hors-Suisse sont aussi retourné. C'est à l'appelant de faire le tri si nécessaire.
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp0.getTypeAutoriteFiscale());
		assertEquals(1234, ffp0.getNoOfsAutoriteFiscale());

		final List<ForFiscal> forSecondaires = pm.getAutresForsFiscaux();
		assertNotNull(forSecondaires);
		// etc...

		// Récupération des informations des régimes fiscaux

		final List<RegimeFiscal> regimesICC = pm.getRegimesFiscauxICC();
		assertNotNull(regimesICC);
		assertEquals(1, regimesICC.size());

		final RegimeFiscal rVD0 = regimesICC.get(0);
		assertNotNull(rVD0);
		assertEquals("41C", rVD0.getCode()); // selon table TY_REGIME_FISCAL
		assertEquals(newDate(1996, 3, 15), rVD0.getDateDebut());
		assertNull(rVD0.getDateFin());
		// note : la catégorie de PM se déduit du code

		final List<RegimeFiscal> regimesIFD = pm.getRegimesFiscauxIFD();
		assertNotNull(regimesIFD);
		// etc...

	}

	@Ignore
	@Test
	public void testFournirCapitalLibere() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(123456);
		params.setDate(newDate(2009, 10, 14));
		params.getParts().add(TiersPart.CAPITAUX);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(123456L, pm.getNumero());

		// Récupération du capital libéré

		final Capital capital = pm.getCapital();
		assertNotNull(capital);
		assertEquals(120000, capital.getCapitalLibere());

		// note : il est de la responsabilité de l'appelant de déterminer si l'abscence ou non du capital libéré est normale ou non. Pour
		// rappel, cette abscence justifiée ou non se déduit de la catégorie de PM (= normal pour une APM, d'après le document d'Eric Wyss).
		// Cette catégorie est elle-même déduite du code du régime fiscal.
	}

	@Ignore
	@Test
	public void testFournirEvenementsPMParNumero() throws Exception {

		final SearchEvenementsPM params = new SearchEvenementsPM();
		params.setLogin(login);
		params.setTiersNumber(123456L);

		final List<EvenementPM> events = service.searchEvenementsPM(params).getItem();
		assertNotNull(events);
		assertEquals(3, events.size());

		final EvenementPM ev0 = events.get(0);
		assertNotNull(ev0);
		assertEquals(newDate(1997, 11, 28), ev0.getDateEvenement());
		assertEquals("010", ev0.getCodeEvenement()); // selon table EVENEMENT du host
		assertEquals(Long.valueOf(123456), ev0.getTiersNumber());

		final EvenementPM ev1 = events.get(1);
		assertNotNull(ev1);
		// etc...
	}

	@Ignore
	@Test
	public void testFournirEvenementsPMParCode() throws Exception {

		final SearchEvenementsPM params = new SearchEvenementsPM();
		params.setLogin(login);
		params.setCodeEvenement("010");

		final List<EvenementPM> events = service.searchEvenementsPM(params).getItem();
		assertNotNull(events);
		assertEquals(32321, events.size());

		final EvenementPM ev0 = events.get(0);
		assertNotNull(ev0);
		assertEquals(newDate(1952, 1, 2), ev0.getDateEvenement());
		assertEquals("010", ev0.getCodeEvenement()); // selon table EVENEMENT du host
		assertEquals(Long.valueOf(12), ev0.getTiersNumber());

		final EvenementPM ev1 = events.get(1);
		assertNotNull(ev1);
		// etc...
	}

	@Ignore
	@Test
	public void testFournirCoordonneesFinancieres() throws Exception {

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

	@Ignore
	@Test
	public void testFournirAdresseCourrier() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(123456);
		params.setDate(newDate(2009, 10, 14));
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(123456L, pm.getNumero());

		// Récupération de l'adresse d'envoi de la PM

		final AdresseEnvoi adresseEnvoi = pm.getAdresseEnvoi();
		assertNotNull(adresseEnvoi);
		assertEquals("Ma petite entreprise", adresseEnvoi.getLigne1());
		assertEquals("Michel Dupneu", adresseEnvoi.getLigne2());
		assertEquals("chemin de la jante 12", adresseEnvoi.getLigne3());
		assertEquals("1234 Bussigny", adresseEnvoi.getLigne4());
		assertNull(adresseEnvoi.getLigne5());
		assertNull(adresseEnvoi.getLigne6());

		// Comparaison champ-à-champ de l'adresse courrier avec l'adresse de siège
		final Adresse adresseCourrier = pm.getAdresseCourrier();
		final Adresse adresseSiege = pm.getAdresseDomicile();
		assertEquals(adresseCourrier.getLocalite(), adresseSiege.getLocalite());
		// etc...
	}

	@Ignore
	@Test
	public void testFournirAdresseContentieuxPM() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(123456);
		params.setDate(newDate(2009, 10, 14));
		params.getParts().add(TiersPart.ADRESSES);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);
		assertEquals(123456L, pm.getNumero());

		// Récupération des adresses de domicile (pour le contentieux) et de poursuite

		final Adresse adresseDomicile = pm.getAdresseDomicile();
		assertNull(adresseDomicile.getCasePostale());
		assertEquals(newDate(1990, 1, 1), adresseDomicile.getDateDebut());
		assertNull(adresseDomicile.getDateFin());
		assertNull(adresseDomicile.getTitre());
		assertNull(adresseDomicile.getNumeroAppartement());
		assertEquals("chemin de la jante", adresseDomicile.getRue());
		assertEquals("12", adresseDomicile.getNumeroRue());
		assertEquals("1234", adresseDomicile.getNumeroPostal());
		assertEquals("Bussigny", adresseDomicile.getLocalite());
		assertNull(adresseDomicile.getPays());
		assertEquals(4321, adresseDomicile.getNoOrdrePostal());
		assertEquals(324, adresseDomicile.getNoRue().intValue());

		final Adresse adressePoursuite = pm.getAdressePoursuite();
		assertNull(adressePoursuite.getCasePostale());
		// etc...

		// Unireg n'est pas en mesure de déterminer l'adresse de l'OP. Ce travail est de la responsabilité du service infastructure.

		// Unireg n'est pas en mesure de retourner le nom et de prénom de l'administrateur, cette information n'est pas disponible.
	}

	@Ignore
	@Test
	public void testFournirInformationDeConsultation() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(123456);
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
		assertEquals(123456L, pm.getNumero());
		assertEquals("Ma petite entreprise", pm.getDesignationAbregee());

		// Siege actif
		final Siege siege = pm.getSiege();
		assertNotNull(siege);
		assertEquals(TypeSiege.COMMUNE_CH, siege.getTypeSiege());
		assertEquals(324, siege.getNoOfsSiege());

		// For principal actif
		final ForFiscal ffp = pm.getForFiscalPrincipal();
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		assertEquals(324, ffp.getNoOfsAutoriteFiscale());
		// note : le nom de la commune/pays doit être demandé au service infrastructure

		// Fors secondaires actifs
		final List<ForFiscal> forsSecondaires = pm.getAutresForsFiscaux();
		assertNotNull(forsSecondaires);
		final ForFiscal ffs0 = forsSecondaires.get(0);
		assertNotNull(ffs0);
		assertEquals(136, ffs0.getNoOfsAutoriteFiscale());
		// note : le nom de la commune doit être demandé au service infrastructure

		// Forme juridique
		final FormeJuridique formeJuridique = pm.getFormeJuridique();
		assertNotNull(formeJuridique);
		assertEquals("01", formeJuridique.getCode()); // code selon table FORME_JURIDIQ_ACI

		// Régime fiscal ICC
		final RegimeFiscal regimeICC = pm.getRegimeFiscalICC();
		assertNotNull(regimeICC);
		assertEquals("41C", regimeICC.getCode()); // selon table TY_REGIME_FISCAL

		// Date de fin du dernier exercice commercial
		assertEquals(newDate(2008, 12, 31), pm.getDateFinDernierExerciceCommercial());

		// Date de bouclement future
		assertEquals(newDate(2009, 12, 31), pm.getDateBouclementFutur());

		// Dates de début et de fin de l'assujettissement LIC
		final Assujettissement assujettissementLIC = pm.getAssujettissementLIC();
		assertNotNull(assujettissementLIC);
		assertEquals(newDate(1998, 12, 3), assujettissementLIC.getDateDebut());
		assertNull(assujettissementLIC.getDateFin());

		// Dates de début et de fin de l'assujettissement LIFD
		final Assujettissement assujettissementLIFD = pm.getAssujettissementLIFD();
		assertNotNull(assujettissementLIFD);
		assertEquals(newDate(1998, 12, 3), assujettissementLIFD.getDateDebut());
		assertNull(assujettissementLIFD.getDateFin());

		// Numéro IPMRO
		assertNull(pm.getNumeroIPMRO());

		// Code blocage remboursement automatique
		assertFalse(pm.isBlocageRemboursementAutomatique());

		// Date de validite et code de l'état de la PM
		final EtatPM etat = pm.getEtat();
		assertNotNull(etat);
		assertEquals(newDate(2003, 6, 21), etat.getDateDebut());
		assertEquals("01", etat.getCode()); // selon table ETAT du host
		// note : le libellé du dernier état doit être demandé au service infrastructure

		// Capital libéré + absence normale ou non
		final Capital capital = pm.getCapital();
		assertNotNull(capital);
		assertEquals(120000, capital.getCapitalLibere());
		assertFalse(capital.isAbsenceCapitalLibereNormale());
	}
}
