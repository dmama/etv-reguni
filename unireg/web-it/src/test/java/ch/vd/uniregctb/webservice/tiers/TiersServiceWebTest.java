package ch.vd.uniregctb.webservice.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservices.tiers.Adresse;
import ch.vd.uniregctb.webservices.tiers.AdresseEnvoi;
import ch.vd.uniregctb.webservices.tiers.Assujettissement;
import ch.vd.uniregctb.webservices.tiers.CategorieDebiteur;
import ch.vd.uniregctb.webservices.tiers.CompteBancaire;
import ch.vd.uniregctb.webservices.tiers.Contribuable;
import ch.vd.uniregctb.webservices.tiers.ContribuableHisto;
import ch.vd.uniregctb.webservices.tiers.Date;
import ch.vd.uniregctb.webservices.tiers.Debiteur;
import ch.vd.uniregctb.webservices.tiers.Declaration;
import ch.vd.uniregctb.webservices.tiers.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.webservices.tiers.DeclarationImpotSource;
import ch.vd.uniregctb.webservices.tiers.EtatDeclaration;
import ch.vd.uniregctb.webservices.tiers.ForFiscal;
import ch.vd.uniregctb.webservices.tiers.FormatNumeroCompte;
import ch.vd.uniregctb.webservices.tiers.GenreImpot;
import ch.vd.uniregctb.webservices.tiers.GetTiers;
import ch.vd.uniregctb.webservices.tiers.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers.GetTiersType;
import ch.vd.uniregctb.webservices.tiers.MenageCommun;
import ch.vd.uniregctb.webservices.tiers.ModeCommunication;
import ch.vd.uniregctb.webservices.tiers.ModeImposition;
import ch.vd.uniregctb.webservices.tiers.MotifRattachement;
import ch.vd.uniregctb.webservices.tiers.PeriodeDecompte;
import ch.vd.uniregctb.webservices.tiers.PeriodeImposition;
import ch.vd.uniregctb.webservices.tiers.PeriodiciteDecompte;
import ch.vd.uniregctb.webservices.tiers.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers.PersonnePhysiqueHisto;
import ch.vd.uniregctb.webservices.tiers.RapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers.SearchTiers;
import ch.vd.uniregctb.webservices.tiers.SetTiersBlocRembAuto;
import ch.vd.uniregctb.webservices.tiers.SituationFamille;
import ch.vd.uniregctb.webservices.tiers.TarifImpotSource;
import ch.vd.uniregctb.webservices.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers.TiersInfo;
import ch.vd.uniregctb.webservices.tiers.TiersInfoArray;
import ch.vd.uniregctb.webservices.tiers.TiersPart;
import ch.vd.uniregctb.webservices.tiers.TiersPort;
import ch.vd.uniregctb.webservices.tiers.TypeAssujettissement;
import ch.vd.uniregctb.webservices.tiers.TypeAutoriteFiscale;
import ch.vd.uniregctb.webservices.tiers.TypeDocument;
import ch.vd.uniregctb.webservices.tiers.TypeRapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers.TypeRecherche;
import ch.vd.uniregctb.webservices.tiers.TypeTiers;
import ch.vd.uniregctb.webservices.tiers.UserLogin;

/**
 * Test unitaire pour le web service de la recherche.
 */
public class TiersServiceWebTest extends AbstractTiersServiceWebTest {

	//private static final Logger LOGGER = Logger.getLogger(TiersServiceWebTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersServiceWebTest.xml";

	private UserLogin login;

	private static boolean alreadySetUp = false;

	public TiersServiceWebTest() throws Exception {
	}

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}

		login = new UserLogin();
		login.setUserId("[UT] TiersServiceWebTest");
		login.setOid(0);
	}

	@Test
	public void testGetType() throws Exception {

		final GetTiersType params = new GetTiersType();
		params.setLogin(login);

		params.setTiersNumber(12100003); // Habitant
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, service.getTiersType(params));

		params.setTiersNumber(34777810); // Habitant
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, service.getTiersType(params));

		params.setTiersNumber(12100001); // Habitant
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, service.getTiersType(params));

		params.setTiersNumber(12100002); // Habitant
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, service.getTiersType(params));

		params.setTiersNumber(86116202); // Menage Commun
		assertEquals(TypeTiers.MENAGE_COMMUN, service.getTiersType(params));

		params.setTiersNumber(12500001); // DebiteurPrestationImposable
		assertEquals(TypeTiers.DEBITEUR, service.getTiersType(params));

		params.setTiersNumber(12700101); // Entreprise
		assertEquals(TypeTiers.PERSONNE_MORALE, service.getTiersType(params));

		params.setTiersNumber(12600101); // NonHabitant
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, service.getTiersType(params));

		params.setTiersNumber(12800101); // AutreCommunaute
		assertEquals(TypeTiers.PERSONNE_MORALE, service.getTiersType(params));
	}

	@Test
	public void testGetDateDebutFinActivite() throws Exception {

		assertActivite(null, null, 12100003, service);
		assertActivite(null, null, 12500001, service);
		assertActivite(newDate(2006, 9, 1), null, 12600101, service);
		assertActivite(null, null, 12100001, service);
		assertActivite(newDate(2002, 9, 1), null, 34777810, service);
		assertActivite(newDate(1987, 2, 1), null, 86116202, service);
		assertActivite(newDate(1980, 1, 1), newDate(1987, 1, 31), 12100002, service);
	}

	private void assertActivite(Date debut, Date fin, int numero, TiersPort service) throws Exception {
		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(numero);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertSameDay(debut, tiers.getDateDebutActivite());
		assertSameDay(fin, tiers.getDateFinActivite());
	}

	@Test
	public void testGetDebiteur() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12500001);
		params.setDate(newDate(2000, 1, 1));

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);
		assertEquals(Long.valueOf(12100002L), debiteur.getContribuableAssocie());
		assertEquals("Employeur personnel menage", debiteur.getComplementNom());
		assertEquals(CategorieDebiteur.ADMINISTRATEURS, debiteur.getCategorie());
		assertEquals(ModeCommunication.PAPIER, debiteur.getModeCommunication());
		assertEquals(PeriodiciteDecompte.MENSUEL, debiteur.getPeriodiciteDecompte());
		assertEquals(PeriodeDecompte.M_09, debiteur.getPeriodeDecompte());
		assertTrue(debiteur.isSansRappel());
		assertTrue(debiteur.isSansListRecapitulative());

		assertNull(debiteur.getAdresseCourrier());
		assertNull(debiteur.getAdresseRepresentation());
		assertNull(debiteur.getAdressePoursuite());
		assertEmpty(debiteur.getRapportsEntreTiers());
		assertNull(debiteur.getDeclaration());
	}

	@Test
	public void testGetDebiteurAvecAdresses() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12500001);
		params.setDate(newDate(2005, 1, 1));
		params.getParts().add(TiersPart.ADRESSES);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		final Adresse courrier = debiteur.getAdresseCourrier();
		assertNotNull(courrier);
		{
			assertSameDay(newDate(2004, 1, 29), courrier.getDateDebut());
			assertEquals(new Integer(141554), courrier.getNoRue());
			assertEquals("12", courrier.getNumeroRue());
			assertEquals(1000, courrier.getNoOrdrePostal());
			assertEquals("Matran", courrier.getLocalite());
		}

		final Adresse repres = debiteur.getAdresseRepresentation();
		assertNotNull(repres);
		{
			assertSameDay(newDate(2004, 1, 29), repres.getDateDebut());
			assertEquals(new Integer(32296), repres.getNoRue());
			assertEquals("1", repres.getNumeroRue());
			assertEquals(528, repres.getNoOrdrePostal());
			assertEquals("Avenue du Funiculaire", repres.getRue());
			assertEquals("Cossonay-Ville", repres.getLocalite());
		}
	}

	@Test
	public void testGetDebiteurAvecDeclarations() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12500001);
		params.setDate(newDate(2008, 1, 20));
		params.getParts().add(TiersPart.DECLARATIONS);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		assertNull(debiteur.getAdresseCourrier());
		assertNull(debiteur.getAdresseRepresentation());
		assertNull(debiteur.getAdressePoursuite());

		final Declaration declaration = debiteur.getDeclaration();
		assertNotNull(declaration);
		assertTrue(declaration instanceof DeclarationImpotSource);

		final DeclarationImpotSource lr = (DeclarationImpotSource) declaration;
		assertSameDay(newDate(2008, 1, 1), lr.getDateDebut());
		assertSameDay(newDate(2008, 1, 31), lr.getDateFin());
		assertEquals(PeriodiciteDecompte.MENSUEL, lr.getPeriodicite());
		assertEquals(ModeCommunication.PAPIER, lr.getModeCommunication());
		assertFalse(lr.isAnnulee());
		assertNull(lr.getDateAnnulation());
	}

	@Test
	public void testGetDebiteurComptesBancaires() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12500001);
		params.setDate(newDate(2008, 1, 20));

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		final List<CompteBancaire> comptes = debiteur.getComptesBancaires();
		assertEquals(1, comptes.size());

		final CompteBancaire compte = comptes.get(0);
		assertCompte("PME Duchemolle", "CH1900767000U01234567", FormatNumeroCompte.IBAN, compte);
	}

	@Test
	public void testGetPersonnePhysique() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12100003);
		params.setDate(newDate(2000, 1, 1));

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);
		assertEquals(12100003L, personne.getNumero());
	}

	@Test
	public void testGetPersonnePhysiqueAvecDeclarations() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12100003);
		params.setDate(newDate(2008, 1, 20));
		params.getParts().add(TiersPart.DECLARATIONS);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		assertNull(personne.getAdresseCourrier());
		assertNull(personne.getAdresseRepresentation());
		assertNull(personne.getAdressePoursuite());

		final Declaration declaration = personne.getDeclaration();
		assertNotNull(declaration);
		assertTrue(declaration instanceof DeclarationImpotOrdinaire);

		final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) declaration;
		assertSameDay(newDate(2008, 1, 1), di.getDateDebut());
		assertSameDay(newDate(2008, 3, 31), di.getDateFin());
		assertFalse(di.isAnnulee());
		assertNull(di.getDateAnnulation());
		assertEquals(6789L, di.getNumero());
		assertEquals(TypeDocument.DECLARATION_IMPOT_VAUDTAX, di.getTypeDocument());
		assertEquals(5646L, di.getNumeroOfsForGestion());
	}

	@Test
	public void testGetPersonnePhysiqueAvecAdresseEnvoi() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12100003);
		params.setDate(newDate(2008, 1, 20));
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		assertNull(personne.getAdresseCourrier());
		assertNull(personne.getAdresseRepresentation());
		assertNull(personne.getAdressePoursuite());

		final AdresseEnvoi adresse = personne.getAdresseEnvoi();
		assertNotNull(adresse);

		assertEquals("Madame", adresse.getLigne1());
		assertEquals("Lyah Emery", trimValiPattern(adresse.getLigne2()));
		assertEquals("Rue Couvaloup 2", adresse.getLigne3());
		assertEquals("1162 St-Prex", adresse.getLigne4());
		assertNull(adresse.getLigne5());
		assertNull(adresse.getLigne6());
	}

	@Test
	public void testGetPersonnePhysiqueAvecForFiscaux() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600101); // Andrea Conchita
		params.setDate(newDate(2008, 1, 20));
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		final ForFiscal forPrincipal = personne.getForFiscalPrincipal();
		assertNotNull(forPrincipal);
		assertEquals(GenreImpot.REVENU_FORTUNE, forPrincipal.getGenreImpot());
		assertEquals(MotifRattachement.DOMICILE, forPrincipal.getMotifRattachement());
		assertEquals(ModeImposition.SOURCE, forPrincipal.getModeImposition());
		assertSameDay(newDate(2006, 9, 1), forPrincipal.getDateOuverture());
		assertNull(forPrincipal.getDateFermeture());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forPrincipal.getTypeAutoriteFiscale());
		assertEquals(5586L, forPrincipal.getNoOfsAutoriteFiscale());

		assertEmpty(personne.getAutresForsFiscaux());
	}

	@Test
	public void testGetPersonnePhysiqueSituationFamillePersonneSeule() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12100003); // EMERY Lyah
		params.setDate(newDate(2008, 1, 20));
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		final SituationFamille situation = personne.getSituationFamille();
		assertNotNull(situation);
		assertSameDay(newDate(1990, 2, 12), situation.getDateDebut());
		assertNull(situation.getDateFin());
		assertEquals(new Integer(0), situation.getNombreEnfants());
		assertNull(situation.getTarifApplicable()); // seulement renseigné sur un couple
		assertNull(situation.getNumeroContribuablePrincipal()); // seulement renseigné sur un couple
	}

	@Test
	public void testGetPersonnePhysiqueAvecComptesBancaires() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12100003); // EMERY Lyah
		params.setDate(newDate(2008, 1, 20));

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		final List<CompteBancaire> comptes = personne.getComptesBancaires();
		assertEquals(1, comptes.size());

		final CompteBancaire compte = comptes.get(0);
		assertCompte("Emery Lyah", "CH1900767000U01234567", FormatNumeroCompte.IBAN, compte);
	}

	@Test
	public void testGetPersonnePhysiqueSansComptesBancaires() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(34777810); // DESCLOUX Pascaline
		params.setDate(newDate(2008, 1, 20));

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);
		assertEmpty(personne.getComptesBancaires());
	}

	@Test
	public void testGetPersonnePhysiqueSituationFamilleCouple() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(86116202); // Les Schmidt
		params.setDate(newDate(2008, 1, 20));
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final MenageCommun menage = (MenageCommun) service.getTiers(params);
		assertNotNull(menage);

		final SituationFamille situation = menage.getSituationFamille();
		assertNotNull(situation);
		assertSameDay(newDate(1990, 2, 12), situation.getDateDebut());
		assertNull(situation.getDateFin());
		assertEquals(new Integer(0), situation.getNombreEnfants());
		assertEquals(TarifImpotSource.NORMAL, situation.getTarifApplicable()); // seulement renseigné sur un couple
		assertEquals(Long.valueOf(12100002L), situation.getNumeroContribuablePrincipal()); // seulement renseigné sur un couple
	}

	@Test
	public void testGetMenageCommun() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(86116202);
		params.setDate(newDate(2008, 1, 20));
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		final MenageCommun menage = (MenageCommun) service.getTiers(params);
		assertNotNull(menage);
		assertEquals(86116202L, menage.getNumero());

		final List<RapportEntreTiers> rapports = menage.getRapportsEntreTiers();
		assertEquals(3, rapports.size()); // 2 rapports appartenance ménages, 1 rapport conseil légal

		/* Extrait les différents type de rapports */
		List<RapportEntreTiers> rapportsMenage = new ArrayList<RapportEntreTiers>();
		RapportEntreTiers rapportConseilLegal = null;

		for (RapportEntreTiers rapport : rapports) {
			assertNotNull(rapport);
			if (TypeRapportEntreTiers.CONSEIL_LEGAL.equals(rapport.getType())) {
				assertNull("Trouvé plus de 1 rapport de type conseil légal", rapportConseilLegal);
				rapportConseilLegal = rapport;
			}
			else if (TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapport.getType())) {
				assertTrue("Trouvé plus de 2 rapports de type appartenance ménage", rapportsMenage.size() < 2);
				rapportsMenage.add(rapport);
			}
			else {
				fail("Type de rapport-entre-tiers non attendu [" + rapport.getType().name() + "]");
			}
		}

		assertNotNull(rapportConseilLegal);
		assertEquals(34777810L, rapportConseilLegal.getAutreTiersNumero());
		assertSameDay(newDate(2005, 3, 1), rapportConseilLegal.getDateDebut());
		assertNull(rapportConseilLegal.getDateFin());

		/* Trie la collection de rapports appartenance ménage par ordre croissant de numéro de l'autre tiers */
		Collections.sort(rapportsMenage, new Comparator<RapportEntreTiers>() {
			public int compare(RapportEntreTiers r1, RapportEntreTiers r2) {
				return (int) (r1.getAutreTiersNumero() - r2.getAutreTiersNumero());
			}
		});
		assertEquals(2, rapportsMenage.size());

		final RapportEntreTiers rapportMenage0 = rapportsMenage.get(0);
		assertEquals(12100001L, rapportMenage0.getAutreTiersNumero());
		assertSameDay(newDate(1985, 11, 12), rapportMenage0.getDateDebut());
		assertNull(rapportMenage0.getDateFin());

		final RapportEntreTiers rapportMenage1 = rapportsMenage.get(1);
		assertEquals(12100002L, rapportMenage1.getAutreTiersNumero());
		assertSameDay(newDate(1985, 11, 12), rapportMenage1.getDateDebut());
		assertNull(rapportMenage1.getDateFin());
	}

	@Test
	public void testSearchTiersParNumeroZeroResultat() throws Exception {

		final SearchTiers params = new SearchTiers();
		params.setLogin(login);
		params.setNumero("1239876");

		final TiersInfoArray list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(0, list.getItem().size());
	}

	@Test
	public void testSearchTiersParNumeroUnResultat() throws Exception {

		final SearchTiers params = new SearchTiers();
		params.setLogin(login);
		params.setNumero("12100003");

		final TiersInfoArray list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(1, list.getItem().size());

		final TiersInfo info = list.getItem().get(0);
		assertEquals(12100003L, info.getNumero());
		assertEquals("Emery Lyah", trimValiPattern(info.getNom1()));
		assertEquals("", info.getNom2());
		assertEquals("20050829", info.getDateNaissance());
		assertEquals("1162", info.getNpa());
		assertEquals("St-Prex", info.getLocalite());
		assertEquals("Suisse", info.getPays());
		assertEquals("Chemin du Riau 2A", info.getRue());
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
	}

	@Test
	public void testSearchTiersParNumeroPlusieursResultats() throws Exception {

		final SearchTiers params = new SearchTiers();
		params.setLogin(login);
		params.setNumero("12100001 + 12100002"); // Les Schmidt

		final TiersInfoArray list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(2, list.getItem().size());

		// on retrouve les schmidt (couple + 2 tiers)
		int nbFound = 0;
		for (int i = 0; i < list.getItem().size(); i++) {
			TiersInfo info = list.getItem().get(i);
			if (12100001L == info.getNumero()) { // Madame
				assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
				nbFound++;
			}
			if (12100002L == info.getNumero()) { // Monsieur
				assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
				nbFound++;
			}
		}
		assertEquals(2, nbFound);
	}

	@Test
	public void testSearchTiersZeroResultat() throws Exception {

		final SearchTiers params = new SearchTiers();
		params.setLogin(login);
		params.setNomCourrier("GENGIS KHAN");
		params.setTypeRecherche(TypeRecherche.CONTIENT);

		final TiersInfoArray list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(0, list.getItem().size());
	}

	@Test
	public void testSearchTiersUnResultat() throws Exception {

		final SearchTiers params = new SearchTiers();
		params.setLogin(login);
		params.setNomCourrier("EMERY");
		params.setTypeRecherche(TypeRecherche.CONTIENT);

		final TiersInfoArray list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(1, list.getItem().size());

		final TiersInfo info = list.getItem().get(0);
		assertEquals(12100003L, info.getNumero());
		assertEquals("Emery Lyah", trimValiPattern(info.getNom1()));
		assertEquals("", info.getNom2());
		assertEquals("20050829", info.getDateNaissance());
		assertEquals("1162", info.getNpa());
		assertEquals("St-Prex", info.getLocalite());
		assertEquals("Suisse", info.getPays());
		assertEquals("Chemin du Riau 2A", info.getRue());
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
	}

	@Test
	public void testSearchTiersPlusieursResultats() throws Exception {

		final SearchTiers params = new SearchTiers();
		params.setLogin(login);
		params.setLocaliteOuPays("Yens");
		params.setTypeRecherche(TypeRecherche.CONTIENT);

		final TiersInfoArray list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(4, list.getItem().size());

		// on retrouve les schmidt (couple + 2 tiers) et un débiteur associé
		int nbFound = 0;
		for (int i = 0; i < list.getItem().size(); i++) {
			TiersInfo info = list.getItem().get(i);
			if (34777810L == info.getNumero()) {
				assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
				nbFound++;
			}
			if (12100001L == info.getNumero()) {
				assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
				nbFound++;
			}
			if (12100002L == info.getNumero()) {
				assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
				nbFound++;
			}
			if (12500001L == info.getNumero()) {
				assertEquals(TypeTiers.DEBITEUR, info.getType());
				nbFound++;
			}
		}
		assertEquals(4, nbFound);
	}

	@Test
	public void testSearchTiersSurNoOfsFor() throws Exception {

		final SearchTiers params = new SearchTiers();
		params.setLogin(login);
		params.setNoOfsFor(5652);

		final TiersInfoArray list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(2, list.getItem().size());

		// on retrouve les schmidt (couple + un composant du ménage)
		int nbFound = 0;
		for (int i = 0; i < list.getItem().size(); i++) {
			TiersInfo info = list.getItem().get(i);
			long numero = info.getNumero();
			if (86116202L == numero) {
				assertEquals(TypeTiers.MENAGE_COMMUN, info.getType());
				nbFound++;
			}
			if (12100002L == numero) {
				assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
				nbFound++;
			}
		}
		assertEquals(2, nbFound);
	}

	@Test
	public void testSearchTiersSurNoOfsForActif() throws Exception {

		final SearchTiers params = new SearchTiers();
		params.setLogin(login);
		params.setNoOfsFor(5652);
		params.setForPrincipalActif(true);

		final TiersInfoArray list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(1, list.getItem().size());

		TiersInfo info = list.getItem().get(0);
		assertEquals(86116202L, info.getNumero());
		assertEquals(TypeTiers.MENAGE_COMMUN, info.getType());
	}

	@Test
	public void testSetBlocageRemboursementAutomatique() throws Exception {

		/*
		 * Etat avant changement du blocage
		 */

		{
			final GetTiers params = new GetTiers();
			params.setLogin(login);
			params.setTiersNumber(12100003); // EMERY Lyah
			params.setDate(newDate(2008, 1, 20));

			final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
			assertNotNull(personne);
			assertFalse(personne.isBlocageRemboursementAutomatique());
		}
		{
			final GetTiersHisto params = new GetTiersHisto();
			params.setLogin(login);
			params.setTiersNumber(12100003); // EMERY Lyah

			final PersonnePhysiqueHisto personne = (PersonnePhysiqueHisto) service.getTiersHisto(params);
			assertNotNull(personne);
			assertFalse(personne.isBlocageRemboursementAutomatique());
		}

		/*
		 * Blocage du remboursement automatique
		 */

		{
			final SetTiersBlocRembAuto params = new SetTiersBlocRembAuto();
			params.setLogin(login);
			params.setTiersNumber(12100003); // EMERY Lyah
			params.setBlocage(true);

			service.setTiersBlocRembAuto(params);
		}

		/*
		 * Etat après changement du blocage
		 */

		{
			final GetTiers params = new GetTiers();
			params.setLogin(login);
			params.setTiersNumber(12100003); // EMERY Lyah
			params.setDate(newDate(2008, 1, 20));

			final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
			assertNotNull(personne);
			assertTrue(personne.isBlocageRemboursementAutomatique());
		}
		{
			final GetTiersHisto params = new GetTiersHisto();
			params.setLogin(login);
			params.setTiersNumber(12100003); // EMERY Lyah

			final PersonnePhysiqueHisto personne = (PersonnePhysiqueHisto) service.getTiersHisto(params);
			assertNotNull(personne);
			assertTrue(personne.isBlocageRemboursementAutomatique());
		}
	}

	@Test
	public void testAnnulationFlag() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(77714803); // RAMONI Jean
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);
		params.getParts().add(TiersPart.DECLARATIONS);
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);
		params.getParts().add(TiersPart.FORS_FISCAUX);

		// Personne annulée
		final PersonnePhysiqueHisto personne = (PersonnePhysiqueHisto) service.getTiersHisto(params);
		assertNotNull(personne);
		assertSameDay(newDate(2009, 3, 4), personne.getDateAnnulation());

		// Rapport entre tiers annulé
		final List<RapportEntreTiers> rapports = personne.getRapportsEntreTiers();
		assertNotNull(rapports);
		assertEquals(1, rapports.size());

		final RapportEntreTiers tutelle = rapports.get(0);
		assertNotNull(tutelle);
		assertEquals(TypeRapportEntreTiers.TUTELLE, tutelle.getType());
		assertSameDay(newDate(2009, 3, 4), tutelle.getDateAnnulation());

		// Déclaration annulée
		final List<Declaration> declarations = personne.getDiOrLr();
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		final Declaration decl = declarations.get(0);
		assertNotNull(decl);
		assertSameDay(newDate(2009, 3, 4), decl.getDateAnnulation());

		// Délai déclaration annulé
		// final List<DelaiDeclaration> delais = decl.getDelais();
		// assertNotNull(delais);
		// assertEquals(1, delais.size());
		//
		// final DelaiDeclaration delai = delais.get(0);
		// assertNotNull(delai);
		// assertSameDay(newDate(2009, 3, 4), delai.getDateAnnulation());

		// Etat déclaration annulé
		final List<EtatDeclaration> etats = decl.getEtats();
		assertNotNull(etats);
		assertEquals(1, etats.size());

		final EtatDeclaration etat = etats.get(0);
		assertNotNull(etat);
		assertSameDay(newDate(2009, 3, 4), etat.getDateAnnulation());

		// Situation de famille annulée
		final List<SituationFamille> situations = personne.getSituationsFamille();
		assertNotNull(situations);
		assertEquals(1, situations.size());

		final SituationFamille situ = situations.get(0);
		assertNotNull(situ);
		assertSameDay(newDate(2009, 3, 4), situ.getDateAnnulation());

		// For fiscal annulé
		final List<ForFiscal> fors = personne.getForsFiscauxPrincipaux();
		assertNotNull(fors);
		assertEquals(1, fors.size());

		final ForFiscal f = fors.get(0);
		assertNotNull(f);
		assertSameDay(newDate(2009, 3, 4), f.getDateAnnulation());
	}

	/**
	 * [UNIREG-865] le service doit retourner null lorsque le tiers n'est pas connu, et non pas lever une exception.
	 */
	@Test
	public void testGetTiersTypeInconnu() throws Exception {

		{
			final GetTiersType params = new GetTiersType();
			params.setLogin(login);
			params.setTiersNumber(32323232L); // inconnu

			assertNull(service.getTiersType(params));
		}
	}

	/**
	 * [UNIREG-865] le service doit retourner null lorsque le tiers n'est pas connu, et non pas lever une exception.
	 */
	@Test
	public void testGetTiersInconnu() throws Exception {

		{
			final GetTiers params = new GetTiers();
			params.setLogin(login);
			params.setDate(newDate(2008, 1, 20));
			params.setTiersNumber(32323232L); // inconnu

			assertNull(service.getTiers(params));
		}
	}

	/**
	 * [UNIREG-865] le service doit retourner null lorsque le tiers n'est pas connu, et non pas lever une exception.
	 */
	@Test
	public void testGetTiersPeriodeInconnu() throws Exception {
		{
			final GetTiersPeriode params = new GetTiersPeriode();
			params.setLogin(login);
			params.setPeriode(2008);
			params.setTiersNumber(32323232L); // inconnu

			assertNull(service.getTiersPeriode(params));
		}
	}

	/**
	 * [UNIREG-865] le service doit retourner null lorsque le tiers n'est pas connu, et non pas lever une exception.
	 */
	@Test
	public void testGetTiersHistoInconnu() throws Exception {
		{
			final GetTiersHisto params = new GetTiersHisto();
			params.setLogin(login);
			params.setTiersNumber(32323232L); // inconnu

			assertNull(service.getTiersHisto(params));
		}
	}

	/**
	 * [UNIREG-910] la période d'imposition courante doit être ouverte
	 */
	@Test
	public void testGetTiersPeriodeImposition() throws Exception {

		final int anneeCourante = RegDate.get().year();

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setDate(newDate(anneeCourante, 7, 1));
		params.setTiersNumber(34777810); // DESCLOUX Pascaline
		params.getParts().add(TiersPart.PERIODE_IMPOSITION);

		final Contribuable ctb = (Contribuable) service.getTiers(params);
		assertNotNull(ctb);

		final PeriodeImposition periode = ctb.getPeriodeImposition();
		assertNotNull(periode);
		assertSameDay(newDate(anneeCourante, 1, 1), periode.getDateDebut());
		assertNull(periode.getDateFin());
	}

	/**
	 * [UNIREG-910] la période d'imposition courante doit être ouverte
	 */
	@Test
	public void testGetTiersHistoPeriodesImposition() throws Exception {

		final int anneeCourante = RegDate.get().year();

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(34777810); // DESCLOUX Pascaline
		params.getParts().add(TiersPart.PERIODE_IMPOSITION);

		// récupération des périodes d'imposition
		final ContribuableHisto ctb = (ContribuableHisto) service.getTiersHisto(params);
		assertNotNull(ctb);
		final List<PeriodeImposition> periodes = ctb.getPeriodesImposition();
		assertNotNull(periodes);

		final int size = periodes.size();
		assertEquals(anneeCourante - 2002 + 1, size);

		// année 2002 à année courante - 1
		for (int i = 0; i < size - 1; ++i) {
			final PeriodeImposition p = periodes.get(i);
			assertNotNull(p);
			assertSameDay(newDate(i + 2002, 1, 1), p.getDateDebut());
			assertSameDay(newDate(i + 2002, 12, 31), p.getDateFin());
		}

		// année courante
		final PeriodeImposition derniere = periodes.get(size - 1);
		assertNotNull(derniere);
		assertSameDay(newDate(anneeCourante, 1, 1), derniere.getDateDebut());
		assertNull(derniere.getDateFin());
	}

	/**
	 * [UNIREG-1133] le nom du pays doit être correct
	 */
	@Test
	public void testGetTiersAdresseHorsSuisse() throws Exception {

		final int anneeCourante = RegDate.get().year();

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setDate(newDate(anneeCourante, 7, 1));
		params.setTiersNumber(10035633); // Tummers-De Wit Wouter
		params.getParts().add(TiersPart.ADRESSES);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final Contribuable ctb = (Contribuable) service.getTiers(params);
		assertNotNull(ctb);

		final AdresseEnvoi adresseEnvoi = ctb.getAdresseEnvoi();
		assertNotNull(adresseEnvoi);
		assertEquals("Madame, Monsieur", adresseEnvoi.getLigne1());
		assertEquals("Tummers-De Wit Wouter", adresseEnvoi.getLigne2());
		assertEquals("De Wit Tummers Elisabeth", adresseEnvoi.getLigne3());
		assertEquals("Olympialaan 17", adresseEnvoi.getLigne4());
		assertEquals("4624 Aa Bergem Op Zoom", adresseEnvoi.getLigne5());
		assertEquals("Pays-Bas", adresseEnvoi.getLigne6());

		final Adresse adresseCourrier = ctb.getAdresseCourrier();
		assertNotNull(adresseCourrier);
		assertNull(adresseCourrier.getCasePostale());
		assertSameDay(newDate(2009, 6, 25), adresseCourrier.getDateDebut());
		assertNull(adresseCourrier.getDateFin());
		assertEquals("4624 Aa Bergem Op Zoom", adresseCourrier.getLocalite());
		assertEquals(0, adresseCourrier.getNoOrdrePostal());
		assertNull(adresseCourrier.getNoRue());
		assertEquals("Olympialaan 17", adresseCourrier.getRue());
		assertNull(adresseCourrier.getNumeroAppartement());
		assertEquals("", adresseCourrier.getNumeroPostal());
		assertEquals("Pays-Bas", adresseCourrier.getPays());
		assertEquals("De Wit Tummers Elisabeth", adresseCourrier.getTitre());
	}

	/**
	 * [UNIREG-1395] vérifie que la catégorie est bien calculée
	 */
	@Test
	public void testGetTiersCategorie() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setDate(newDate(2009, 9, 3));

		{ // catégorie inconnue
			params.setTiersNumber(10035633); // Tummers-De Wit Wouter
			final PersonnePhysique pp = (PersonnePhysique) service.getTiers(params);
			assertNotNull(pp);
			assertNull(pp.getCategorie());
		}

		{ // permis B
			params.setTiersNumber(10174192); // Eudina Mara Alencar Casal
			final PersonnePhysique pp = (PersonnePhysique) service.getTiers(params);
			assertNotNull(pp);
			assertEquals("_02_PERMIS_SEJOUR_B", pp.getCategorie());
		}
	}

	/**
	 * [UNIREG-1517] l'assujettissement courant d'un contribuable encore assujetti doit avoir une date de fin nulle.
	 */
	@Test
	public void getTiersAssujettissement() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(87654321); // Alfred Dupneu
		params.getParts().add(TiersPart.ASSUJETTISSEMENTS);

		{ // assujettissement passé
			params.setDate(newDate(1987, 7, 1));
			final PersonnePhysique pp = (PersonnePhysique) service.getTiers(params);
			assertNotNull(pp);

			final Assujettissement a = pp.getAssujettissement();
			assertNotNull(a);
			assertSameDay(newDate(1985, 9, 1), a.getDateDebut());
			assertSameDay(newDate(1990, 2, 15), a.getDateFin());
			assertEquals(TypeAssujettissement.LIMITE, a.getLIC()); // Hors-Suisse
			assertEquals(TypeAssujettissement.LIMITE, a.getLIFD());
		}

		{ // assujettissement courant
			params.setDate(newDate(2006, 7, 1));
			final PersonnePhysique pp = (PersonnePhysique) service.getTiers(params);
			assertNotNull(pp);

			final Assujettissement a = pp.getAssujettissement();
			assertNotNull(a);
			assertSameDay(newDate(2003, 7, 12), a.getDateDebut());
			assertNull(a.getDateFin());
			assertEquals(TypeAssujettissement.LIMITE, a.getLIC()); // Hors-Suisse
			assertEquals(TypeAssujettissement.LIMITE, a.getLIFD());
		}
	}

	/**
	 * [UNIREG-1517] l'assujettissement courant d'un contribuable encore assujetti doit avoir une date de fin nulle.
	 */
	@Test
	public void getTiersHistoAssujettissement() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(87654321); // Alfred Dupneu
		params.getParts().add(TiersPart.ASSUJETTISSEMENTS);

		final PersonnePhysiqueHisto pp = (PersonnePhysiqueHisto) service.getTiersHisto(params);
		assertNotNull(pp);

		final List<Assujettissement> list = pp.getAssujettissements();
		assertEquals(2, list.size());

		{ // assujettissement passé

			final Assujettissement a = list.get(0);
			assertNotNull(a);
			assertSameDay(newDate(1985, 9, 1), a.getDateDebut());
			assertSameDay(newDate(1990, 2, 15), a.getDateFin());
			assertEquals(TypeAssujettissement.LIMITE, a.getLIC()); // Hors-Suisse
			assertEquals(TypeAssujettissement.LIMITE, a.getLIFD());
		}

		{ // assujettissement courant

			final Assujettissement a = list.get(1);
			assertNotNull(a);
			assertSameDay(newDate(2003, 7, 12), a.getDateDebut());
			assertNull(a.getDateFin());
			assertEquals(TypeAssujettissement.LIMITE, a.getLIC()); // Hors-Suisse
			assertEquals(TypeAssujettissement.LIMITE, a.getLIFD());
		}
	}
}

