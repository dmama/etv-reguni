package ch.vd.uniregctb.webservice.tiers3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservices.tiers3.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test unitaire pour le web service de la recherche.
 */
@SuppressWarnings({"JavaDoc"})
public class TiersServiceWebTest extends AbstractTiersServiceWebTest {

	// private static final Logger LOGGER = Logger.getLogger(TiersServiceWebTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersServiceWebTest.xml";
	private static final int PREMIERE_ANNEE_FISCALE = 2003;

	private UserLogin login;
	private UserLogin zciddo; // Annie Ourliac

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
		login.setOid(22);

		zciddo = new UserLogin();
		zciddo.setUserId("zciddo");
		zciddo.setOid(21);
	}

	@Test
	public void testGetType() throws Exception {

		final GetTiersTypeRequest params = new GetTiersTypeRequest();
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

	private void assertActivite(@Nullable Date debut, @Nullable Date fin, int numero, TiersWebService service) throws Exception {
		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(numero);
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		assertSameDay(debut, tiers.getDateDebutActivite());
		assertSameDay(fin, tiers.getDateFinActivite());
	}

	@Test
	public void testGetDebiteur() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12500001);
		params.getParts().add(TiersPart.PERIODICITES);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);
		assertEquals(Long.valueOf(12100002L), debiteur.getContribuableAssocie());
		assertEquals("Employeur personnel menage", debiteur.getComplementNom());
		assertEquals(CategorieDebiteur.ADMINISTRATEURS, debiteur.getCategorie());
		assertEquals(ModeCommunication.PAPIER, debiteur.getModeCommunication());
		assertTrue(debiteur.isSansRappel());
		assertTrue(debiteur.isSansListeRecapitulative());
		assertEmpty(debiteur.getAdressesCourrier());
		assertEmpty(debiteur.getAdressesRepresentation());
		assertEmpty(debiteur.getAdressesPoursuite());
		assertEmpty(debiteur.getRapportsEntreTiers());
		assertEmpty(debiteur.getDeclarations());

		final List<Periodicite> periodicites = debiteur.getPeriodicites();
		assertEquals(1, periodicites.size());

		final Periodicite periodicite0 = periodicites.get(0);
		assertNotNull(periodicite0);
		assertEquals(PeriodiciteDecompte.MENSUEL, periodicite0.getPeriodiciteDecompte());
		assertNull(periodicite0.getPeriodeDecompte());
	}

	@Test
	public void testGetDebiteurAvecAdresses() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12500001);
		params.getParts().add(TiersPart.ADRESSES);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		final List<Adresse> adressesCourrier = debiteur.getAdressesCourrier();
		assertNotNull(adressesCourrier);
		assertEquals(2, adressesCourrier.size());

		final Adresse courrier0 = adressesCourrier.get(0);
		assertNotNull(courrier0);
		assertNull(courrier0.getDateDebut());
		assertSameDay(newDate(2004, 1, 28), courrier0.getDateFin());
		assertEquals(new Integer(0), courrier0.getNoRue());
		assertNull(courrier0.getNumeroRue());
		assertEquals(283, courrier0.getNoOrdrePostal());
		assertEquals("Villars-sous-Yens", courrier0.getLocalite());

		final Adresse courrier1 = adressesCourrier.get(1);
		assertNotNull(courrier1);
		assertSameDay(newDate(2004, 1, 29), courrier1.getDateDebut());
		assertNull(courrier1.getDateFin());
		assertEquals(new Integer(141554), courrier1.getNoRue());
		assertEquals("12", courrier1.getNumeroRue());
		assertEquals(1000, courrier1.getNoOrdrePostal());
		assertEquals("Matran", courrier1.getLocalite());

		final List<Adresse> adressesRepresentation = debiteur.getAdressesRepresentation();
		assertNotNull(adressesRepresentation);
		assertEquals(2, adressesRepresentation.size());

		final Adresse repres0 = adressesRepresentation.get(0);
		assertNotNull(repres0);
		assertNull(repres0.getDateDebut());
		assertSameDay(newDate(2004, 1, 28), repres0.getDateFin());
		assertEquals(new Integer(0), repres0.getNoRue());
		assertNull(repres0.getNumeroRue());
		assertEquals(283, repres0.getNoOrdrePostal());
		assertEquals("La Tuilière", repres0.getRue());
		assertEquals("Villars-sous-Yens", repres0.getLocalite());

		final Adresse repres1 = adressesRepresentation.get(1);
		assertNotNull(repres1);
		assertSameDay(newDate(2004, 1, 29), repres1.getDateDebut());
		assertNull(repres1.getDateFin());
		assertEquals(new Integer(32296), repres1.getNoRue());
		assertEquals("1", repres1.getNumeroRue());
		assertEquals(528, repres1.getNoOrdrePostal());
		assertEquals("Avenue du Funiculaire", repres1.getRue());
		assertEquals("Cossonay-Ville", repres1.getLocalite());
	}

	@Test
	public void testGetDebiteurAvecDeclarations() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12500001);
		params.getParts().add(TiersPart.DECLARATIONS);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		assertEmpty(debiteur.getAdressesCourrier());
		assertEmpty(debiteur.getAdressesRepresentation());
		assertEmpty(debiteur.getAdressesPoursuite());

		final List<Declaration> declarations = debiteur.getDeclarations();
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		final Declaration declaration = declarations.get(0);
		assertNotNull(declaration);
		assertTrue(declaration instanceof DeclarationImpotSource);

		final DeclarationImpotSource lr = (DeclarationImpotSource) declaration;
		assertSameDay(newDate(2008, 1, 1), lr.getDateDebut());
		assertSameDay(newDate(2008, 1, 31), lr.getDateFin());
		assertEquals(PeriodiciteDecompte.MENSUEL, lr.getPeriodicite());
		assertEquals(ModeCommunication.PAPIER, lr.getModeCommunication());
		assertNull(lr.getDateAnnulation());
	}

	@Test
	public void testGetDebiteurComptesBancaires() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12500001);
		params.getParts().add(TiersPart.COMPTES_BANCAIRES);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		final List<CompteBancaire> comptes = debiteur.getComptesBancaires();
		assertEquals(1, comptes.size());

		final CompteBancaire compte = comptes.get(0);
		assertCompte("PME Duchemolle", "CH1900767000U01234567", FormatNumeroCompte.IBAN, compte);
	}

	@Test
	public void testGetPersonnePhysique() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12100003);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);
		assertEquals(12100003L, personne.getNumero());
	}

	@Test
	public void testGetPersonnePhysiqueAvecDeclarations() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12100003);
		params.getParts().add(TiersPart.DECLARATIONS);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		assertEmpty(personne.getAdressesCourrier());
		assertEmpty(personne.getAdressesRepresentation());
		assertEmpty(personne.getAdressesPoursuite());

		final List<Declaration> declarations = personne.getDeclarations();
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		final Declaration declaration = declarations.get(0);
		assertNotNull(declaration);
		assertTrue(declaration instanceof DeclarationImpotOrdinaire);

		final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) declaration;
		assertSameDay(newDate(2008, 1, 1), di.getDateDebut());
		assertSameDay(newDate(2008, 3, 31), di.getDateFin());
		assertNull(di.getDateAnnulation());
		assertEquals(6789L, di.getNumero());
		assertEquals(TypeDocument.DECLARATION_IMPOT_VAUDTAX, di.getTypeDocument());
		assertEquals(5646L, di.getNumeroOfsForGestion());
	}

	@Test
	public void testGetPersonnePhysiqueAvecAdresseEnvoi() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12100003);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		assertEmpty(personne.getAdressesCourrier());
		assertEmpty(personne.getAdressesRepresentation());
		assertEmpty(personne.getAdressesPoursuite());

		final AdresseEnvoi adresse = personne.getAdresseEnvoi();
		assertNotNull(adresse);

		assertEquals("Madame", adresse.getLigne1());
		assertEquals("Lyah Emery", trimValiPattern(adresse.getLigne2()));
		assertEquals("Chemin du Riau 2A", adresse.getLigne3());
		assertEquals("1162 St-Prex", adresse.getLigne4());
		assertNull(adresse.getLigne5());
		assertNull(adresse.getLigne6());
		assertEquals(TypeAffranchissement.SUISSE, adresse.getTypeAffranchissement());
		assertEquals("Madame", adresse.getSalutations());
		assertEquals("Madame", adresse.getFormuleAppel());
		{
			final List<String> nomsPrenoms = adresse.getNomsPrenoms();
			Assert.assertEquals(1, nomsPrenoms.size());
			assertEquals("Lyah Emery", trimValiPattern(nomsPrenoms.get(0)));
		}
		assertNull(adresse.getComplement());
		assertNull(adresse.getPourAdresse());
		assertEquals("Chemin du Riau 2A", adresse.getRueNumero());
		assertNull(adresse.getCasePostale());
		assertEquals("1162 St-Prex", adresse.getNpaLocalite());
		assertNull(adresse.getPays());
	}

	@Test
	public void testGetPersonnePhysiqueDecedeeAvecAdresseEnvoi() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(20602603); // Delano Boschung
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);
		assertSameDay(newDate(2008, 5, 1), personne.getDateDeces());
		assertEmpty(personne.getAdressesCourrier());
		assertEmpty(personne.getAdressesRepresentation());
		assertEmpty(personne.getAdressesPoursuite());

		final AdresseEnvoi adresse = personne.getAdresseEnvoi();
		assertNotNull(adresse);

		assertEquals("Aux héritiers de", adresse.getLigne1());
		assertEquals("Delano Boschung, défunt", trimValiPattern(adresse.getLigne2()));
		assertEquals("Ch. du Devin 81", adresse.getLigne3());
		assertEquals("1012 Lausanne", adresse.getLigne4());
		assertNull(adresse.getLigne5());
		assertNull(adresse.getLigne6());
		assertEquals(TypeAffranchissement.SUISSE, adresse.getTypeAffranchissement());
		assertEquals("Aux héritiers de", adresse.getSalutations());
		assertEquals("Madame, Monsieur", adresse.getFormuleAppel()); // [UNIREG-1398]
		{
			final List<String> nomsPrenoms = adresse.getNomsPrenoms();
			Assert.assertEquals(1, nomsPrenoms.size());
			assertEquals("Delano Boschung, défunt", trimValiPattern(nomsPrenoms.get(0)));
		}
		assertNull(adresse.getComplement());
		assertNull(adresse.getPourAdresse());
		assertEquals("Ch. du Devin 81", adresse.getRueNumero());
		assertNull(adresse.getCasePostale());
		assertEquals("1012 Lausanne", adresse.getNpaLocalite());
		assertNull(adresse.getPays());
	}

	@Test
	public void testGetPersonnePhysiqueAvecForFiscaux() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600101); // Andrea Conchita
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		final List<ForFiscal> forsFiscauxPrincipaux = personne.getForsFiscauxPrincipaux();
		assertNotNull(forsFiscauxPrincipaux);
		assertEquals(1, forsFiscauxPrincipaux.size());

		final ForFiscal forPrincipal = forsFiscauxPrincipaux.get(0);
		assertNotNull(forPrincipal);
		assertEquals(GenreImpot.REVENU_FORTUNE, forPrincipal.getGenreImpot());
		assertEquals(MotifRattachement.DOMICILE, forPrincipal.getMotifRattachement());
		assertEquals(ModeImposition.SOURCE, forPrincipal.getModeImposition());
		assertSameDay(newDate(2006, 9, 1), forPrincipal.getDateDebut());
		assertNull(forPrincipal.getDateFin());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forPrincipal.getTypeAutoriteFiscale());
		assertEquals(5586L, forPrincipal.getNoOfsAutoriteFiscale());

		assertEmpty(personne.getAutresForsFiscaux());
	}

	@Test
	public void testGetPersonnePhysiqueSituationFamillePersonneSeule() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12100003); // EMERY Lyah
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		final List<SituationFamille> situationsFamille = personne.getSituationsFamille();
		assertNotNull(situationsFamille);
		assertEquals(1, situationsFamille.size());

		final SituationFamille situation = situationsFamille.get(0);
		assertNotNull(situation);
		assertSameDay(newDate(1990, 2, 12), situation.getDateDebut());
		assertNull(situation.getDateFin());
		assertEquals(new Integer(0), situation.getNombreEnfants());
		assertNull(situation.getTarifApplicable()); // seulement renseigné sur un couple
		assertNull(situation.getNumeroContribuablePrincipal()); // seulement renseigné sur un couple
	}

	@Test
	public void testGetPersonnePhysiqueAvecComptesBancaires() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12100003); // EMERY Lyah
		params.getParts().add(TiersPart.COMPTES_BANCAIRES);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		final List<CompteBancaire> comptes = personne.getComptesBancaires();
		assertEquals(1, comptes.size());

		final CompteBancaire compte = comptes.get(0);
		assertCompte("Emery Lyah", "CH1900767000U01234567", FormatNumeroCompte.IBAN, compte);
	}

	@Test
	public void testGetPersonnePhysiqueSansComptesBancaires() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(34777810); // DESCLOUX Pascaline
		params.getParts().add(TiersPart.COMPTES_BANCAIRES);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);
		assertEmpty(personne.getComptesBancaires());
	}

	@Test
	public void testGetPersonnePhysiqueSituationFamilleCouple() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(86116202); // Les Schmidt
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final MenageCommun menage = (MenageCommun) service.getTiers(params);
		assertNotNull(menage);

		final List<SituationFamille> situationsFamille = menage.getSituationsFamille();
		assertNotNull(situationsFamille);
		assertEquals(1, situationsFamille.size());

		final SituationFamille situation = situationsFamille.get(0);
		assertNotNull(situation);
		assertSameDay(newDate(1990, 2, 12), situation.getDateDebut());
		assertNull(situation.getDateFin());
		assertEquals(new Integer(0), situation.getNombreEnfants());
		assertEquals(TarifImpotSource.NORMAL, situation.getTarifApplicable()); // seulement renseigné sur un couple
		assertEquals(Long.valueOf(12100002L), situation.getNumeroContribuablePrincipal()); // seulement renseigné sur un couple
	}

	@Test
	public void testGetMenageCommun() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(86116202);
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		final MenageCommun menage = (MenageCommun) service.getTiers(params);
		assertNotNull(menage);
		assertEquals(86116202L, menage.getNumero());

		final List<RapportEntreTiers> rapports = menage.getRapportsEntreTiers();
		assertEquals(2, rapports.size()); // 2 rapports appartenance ménages

		/* Extrait les différents type de rapports */
		List<RapportEntreTiers> rapportsMenage = new ArrayList<RapportEntreTiers>();
		for (RapportEntreTiers rapport : rapports) {
			assertNotNull(rapport);
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType()) {
				assertTrue("Trouvé plus de 2 rapports de type appartenance ménage", rapportsMenage.size() < 2);
				rapportsMenage.add(rapport);
			}
			else {
				fail("Type de rapport-entre-tiers non attendu [" + rapport.getType().name() + "]");
			}
		}

		/* Trie la collection de rapports appartenance ménage par ordre croissant de numéro de l'autre tiers */
		Collections.sort(rapportsMenage, new Comparator<RapportEntreTiers>() {
			public int compare(RapportEntreTiers r1, RapportEntreTiers r2) {
				return (int) (r1.getAutreTiersNumero() - r2.getAutreTiersNumero());
			}
		});
		assertEquals(2, rapportsMenage.size());

		final RapportEntreTiers rapportMenage0 = rapportsMenage.get(0);
		assertEquals(12100001L, rapportMenage0.getAutreTiersNumero());
		assertSameDay(newDate(1987, 2, 1), rapportMenage0.getDateDebut());
		assertNull(rapportMenage0.getDateFin());

		final RapportEntreTiers rapportMenage1 = rapportsMenage.get(1);
		assertEquals(12100002L, rapportMenage1.getAutreTiersNumero());
		assertSameDay(newDate(1987, 2, 1), rapportMenage1.getDateDebut());
		assertNull(rapportMenage1.getDateFin());
	}

	@Test
	public void testSearchTiersParNumeroZeroResultat() throws Exception {

		final SearchTiersRequest params = new SearchTiersRequest();
		params.setLogin(login);
		params.setNumero("1239876");

		final SearchTiersResponse response = service.searchTiers(params);
		assertNotNull(response);
		assertEquals(0, response.getItem().size());
	}

	@Test
	public void testSearchTiersParNumeroUnResultat() throws Exception {

		final SearchTiersRequest params = new SearchTiersRequest();
		params.setLogin(login);
		params.setNumero("12100003");

		final SearchTiersResponse list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(1, list.getItem().size());

		final TiersInfo info = list.getItem().get(0);
		assertEquals(12100003L, info.getNumero());
		assertEquals("Lyah Emery", trimValiPattern(info.getNom1()));
		assertEquals("", info.getNom2());
		assertEquals(newDate(2005, 8, 29), info.getDateNaissance());
		assertEquals("1162", info.getNpa());
		assertEquals("St-Prex", info.getLocalite());
		assertEquals("Suisse", info.getPays());
		assertEquals("Chemin du Riau 2A", info.getRue());
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
	}

	@Test
	public void testSearchTiersParNumeroPlusieursResultats() throws Exception {

		final SearchTiersRequest params = new SearchTiersRequest();
		params.setLogin(login);
		params.setNumero("12100001 + 12100002"); // Les Schmidt

		final SearchTiersResponse list = service.searchTiers(params);
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

		final SearchTiersRequest params = new SearchTiersRequest();
		params.setLogin(login);
		params.setNomCourrier("GENGIS KHAN");
		params.setTypeRecherche(TypeRecherche.CONTIENT);

		final SearchTiersResponse list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(0, list.getItem().size());
	}

	@Test
	public void testSearchTiersUnResultat() throws Exception {

		final SearchTiersRequest params = new SearchTiersRequest();
		params.setLogin(login);
		params.setNomCourrier("EMERY");
		params.setTypeRecherche(TypeRecherche.CONTIENT);

		final SearchTiersResponse list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(1, list.getItem().size());

		final TiersInfo info = list.getItem().get(0);
		assertEquals(12100003L, info.getNumero());
		assertEquals("Lyah Emery", trimValiPattern(info.getNom1()));
		assertEquals("", info.getNom2());
		assertEquals(newDate(2005, 8, 29), info.getDateNaissance());
		assertEquals("1162", info.getNpa());
		assertEquals("St-Prex", info.getLocalite());
		assertEquals("Suisse", info.getPays());
		assertEquals("Chemin du Riau 2A", info.getRue());
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
	}

	@Test
	public void testSearchTiersPlusieursResultats() throws Exception {

		final SearchTiersRequest params = new SearchTiersRequest();
		params.setLogin(login);
		params.setLocaliteOuPays("Yens");
		params.setTypeRecherche(TypeRecherche.CONTIENT);

		final SearchTiersResponse list = service.searchTiers(params);
		assertNotNull(list);
		assertEquals(5, list.getItem().size());

		// on retrouve les schmidt (couple + 2 tiers), pascaline descloux et un débiteur associé
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
			if (86116202L == info.getNumero()) {
				assertEquals(TypeTiers.MENAGE_COMMUN, info.getType());
				nbFound++;
			}
			if (12500001L == info.getNumero()) {
				assertEquals(TypeTiers.DEBITEUR, info.getType());
				nbFound++;
			}
		}
		assertEquals(5, nbFound);
	}

	@Test
	public void testSearchTiersSurNoOfsFor() throws Exception {

		final SearchTiersRequest params = new SearchTiersRequest();
		params.setLogin(login);
		params.setNoOfsFor(5652);

		final SearchTiersResponse list = service.searchTiers(params);
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

		final SearchTiersRequest params = new SearchTiersRequest();
		params.setLogin(login);
		params.setNoOfsFor(5652);
		params.setForPrincipalActif(true);

		final SearchTiersResponse list = service.searchTiers(params);
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
			final GetTiersRequest params = new GetTiersRequest();
			params.setLogin(login);
			params.setTiersNumber(12100003); // EMERY Lyah

			final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
			assertNotNull(personne);
			assertFalse(personne.isBlocageRemboursementAutomatique());
		}

		/*
		 * Blocage du remboursement automatique
		 */

		{
			final SetTiersBlocRembAutoRequest params = new SetTiersBlocRembAutoRequest();
			params.setLogin(login);
			params.setTiersNumber(12100003); // EMERY Lyah
			params.setBlocage(true);

			service.setTiersBlocRembAuto(params);
		}

		/*
		 * Etat après changement du blocage
		 */

		{
			final GetTiersRequest params = new GetTiersRequest();
			params.setLogin(login);
			params.setTiersNumber(12100003); // EMERY Lyah

			final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
			assertNotNull(personne);
			assertTrue(personne.isBlocageRemboursementAutomatique());
		}
	}

	@Test
	public void testAnnulationFlag() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(77714803); // RAMONI Jean
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);
		params.getParts().add(TiersPart.DECLARATIONS);
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);
		params.getParts().add(TiersPart.FORS_FISCAUX);

		// Personne annulée
		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
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
		final List<Declaration> declarations = personne.getDeclarations();
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

		final GetTiersTypeRequest params = new GetTiersTypeRequest();
		params.setLogin(login);
		params.setTiersNumber(32323232L); // inconnu

		assertNull(service.getTiersType(params));
	}

	/**
	 * [UNIREG-865] le service doit retourner null lorsque le tiers n'est pas connu, et non pas lever une exception.
	 */
	@Test
	public void testGetTiersInconnu() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(32323232L); // inconnu

		assertNull(service.getTiers(params));
	}

	/**
	 * [UNIREG-910] la période d'imposition courante doit être ouverte
	 */
	@Test
	public void testGetTiersPeriodesImposition() throws Exception {

		final int anneeCourante = RegDate.get().year();

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(34777810); // DESCLOUX Pascaline
		params.getParts().add(TiersPart.PERIODES_IMPOSITION);

		// récupération des périodes d'imposition
		final Contribuable ctb = (Contribuable) service.getTiers(params);
		assertNotNull(ctb);
		final List<PeriodeImposition> periodes = ctb.getPeriodesImposition();
		assertNotNull(periodes);

		final int size = periodes.size();
		assertEquals(anneeCourante - PREMIERE_ANNEE_FISCALE + 1, size);

		// année 2002 à année courante - 1
		for (int i = 0; i < size - 1; ++i) {
			final PeriodeImposition p = periodes.get(i);
			assertNotNull(p);
			assertSameDay(newDate(i + PREMIERE_ANNEE_FISCALE, 1, 1), p.getDateDebut());
			assertSameDay(newDate(i + PREMIERE_ANNEE_FISCALE, 12, 31), p.getDateFin());
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

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
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
		assertEquals(TypeAffranchissement.EUROPE, adresseEnvoi.getTypeAffranchissement());
		assertEquals("Madame, Monsieur", adresseEnvoi.getSalutations());
		assertEquals("Madame, Monsieur", adresseEnvoi.getFormuleAppel());
		{
			final List<String> nomsPrenoms = adresseEnvoi.getNomsPrenoms();
			Assert.assertEquals(1, nomsPrenoms.size());
			assertEquals("Tummers-De Wit Wouter", trimValiPattern(nomsPrenoms.get(0)));
		}
		assertEquals("De Wit Tummers Elisabeth", adresseEnvoi.getComplement());
		assertNull(adresseEnvoi.getPourAdresse());
		assertEquals("Olympialaan 17", adresseEnvoi.getRueNumero());
		assertNull(adresseEnvoi.getCasePostale());
		assertEquals("4624 Aa Bergem Op Zoom", adresseEnvoi.getNpaLocalite());
		assertEquals("Pays-Bas", adresseEnvoi.getPays());

		final List<Adresse> adressesCourrier = ctb.getAdressesCourrier();
		assertNotNull(adressesCourrier);
		assertEquals(1, adressesCourrier.size());

		final Adresse adresseCourrier = adressesCourrier.get(0);
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
	 * Vérifie que la méthode ne retourne rien si on lui demande rien.
	 */
	@Test
	public void testGetBatchTiersEmptyList() throws Exception {

		final GetBatchTiersRequest params = new GetBatchTiersRequest();
		params.setLogin(login);

		final BatchTiers batch = service.getBatchTiers(params);
		assertNotNull(batch);
		assertEmpty(batch.getEntries());
	}

	/**
	 * Vérifie que la méthode ne retourne rien si on lui demande un tiers inconnu.
	 */
	@Test
	public void testGetBatchTiersSurTiersInconnu() throws Exception {

		final GetBatchTiersRequest params = new GetBatchTiersRequest();
		params.setLogin(login);
		params.getTiersNumbers().add(32323232L); // inconnu

		final BatchTiers batch = service.getBatchTiers(params);
		assertNotNull(batch);
		assertEmpty(batch.getEntries());
	}

	/**
	 * Vérifie que la méthode retourne bien un tiers.
	 */
	@Test
	public void testGetBatchTiersUnTiers() throws Exception {

		final GetBatchTiersRequest params = new GetBatchTiersRequest();
		params.setLogin(login);
		params.getTiersNumbers().add(12100003L);

		final BatchTiers batch = service.getBatchTiers(params);
		assertNotNull(batch);
		assertEquals(1, batch.getEntries().size());

		final BatchTiersEntry entry = batch.getEntries().get(0);
		assertNotNull(entry);
		assertNull(entry.getExceptionInfo());

		final Tiers tiers = entry.getTiers();
		assertNotNull(tiers);
		assertEquals(12100003L, tiers.getNumero());
	}

	/**
	 * Vérifie que la méthode retourne bien plusieurs tiers.
	 */
	@Test
	public void testGetBatchTiersHistoPlusieursTiers() throws Exception {

		final GetBatchTiersRequest params = new GetBatchTiersRequest();
		params.setLogin(login);
		params.getTiersNumbers().add(77714803L);
		params.getTiersNumbers().add(12100003L);
		params.getTiersNumbers().add(34777810L);
		params.getTiersNumbers().add(12100001L);
		params.getTiersNumbers().add(12100002L);
		params.getTiersNumbers().add(86116202L);
		params.getTiersNumbers().add(12500001L);
		params.getTiersNumbers().add(12600101L);
		params.getTiersNumbers().add(10035633L);
		final int size = params.getTiersNumbers().size();

		final BatchTiers batch = service.getBatchTiers(params);
		assertNotNull(batch);
		assertEquals(size, batch.getEntries().size());

		final Set<Long> ids = new HashSet<Long>();
		for (BatchTiersEntry entry : batch.getEntries()) {
			ids.add(entry.getNumber());
			assertNotNull("Le tiers n°" + entry.getNumber() + " est nul !", entry.getTiers());
			assertNull(entry.getExceptionInfo());
		}
		assertEquals(size, ids.size());
		assertTrue(ids.contains(77714803L));
		assertTrue(ids.contains(12100003L));
		assertTrue(ids.contains(34777810L));
		assertTrue(ids.contains(12100001L));
		assertTrue(ids.contains(12100002L));
		assertTrue(ids.contains(86116202L));
		assertTrue(ids.contains(12500001L));
		assertTrue(ids.contains(12600101L));
		assertTrue(ids.contains(10035633L));
	}

	/**
	 * Vérifie que la méthode n'expose pas un tiers non-autorisé et renseigne correctement la raison de l'exception.
	 */
	@Test
	public void testGetBatchTiersHistoSurTiersNonAutorise() throws Exception {

		final GetBatchTiersRequest params = new GetBatchTiersRequest();
		params.setLogin(zciddo); // Daniel Di Lallo
		params.getTiersNumbers().add(10149508L); // Pascal Broulis

		final BatchTiers batch = service.getBatchTiers(params);
		assertNotNull(batch);
		assertEquals(1, batch.getEntries().size());

		final BatchTiersEntry entry = batch.getEntries().get(0);
		assertNotNull(entry);
		assertNull(entry.getTiers()); // autorisation exclusive pour Francis Perroset

		final WebServiceExceptionInfo exceptionInfo = entry.getExceptionInfo();
		assertTrue(exceptionInfo instanceof AccessDeniedExceptionInfo);
		assertEquals("L'utilisateur spécifié (zciddo/21) n'a pas les droits d'accès en lecture sur le tiers n° 10149508", exceptionInfo.getMessage());
	}

	/**
	 * [UNIREG-1395] vérifie que la catégorie est bien calculée
	 */
	@Test
	public void testGetTiersCategorie() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);

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
			assertEquals(CategoriePersonnePhysique.C_02_PERMIS_SEJOUR_B, pp.getCategorie());
		}
	}

	/**
	 * [UNIREG-1291] teste les fors virtuels
	 */
	@Test
	public void testGetTiersForVirtuels() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.getParts().add(TiersPart.FORS_FISCAUX);
		params.setTiersNumber(12100002); // Laurent Schmidt

		//
		// sans les fors virtuels
		//

		{
			final PersonnePhysique pp = (PersonnePhysique) service.getTiers(params);
			assertNotNull(pp);

			final List<ForFiscal> fors = pp.getForsFiscauxPrincipaux();
			assertNotNull(fors);
			assertEquals(1, fors.size());

			final ForFiscal fp = fors.get(0);
			assertSameDay(newDate(1980, 1, 1), fp.getDateDebut());
			assertSameDay(newDate(1987, 1, 31), fp.getDateFin());
			assertFalse(fp.isVirtuel());
		}

		//
		// avec les fors virtuels
		//

		params.getParts().add(TiersPart.FORS_FISCAUX_VIRTUELS);

		{
			final PersonnePhysique pp = (PersonnePhysique) service.getTiers(params);
			assertNotNull(pp);

			final List<ForFiscal> fors = pp.getForsFiscauxPrincipaux();
			assertNotNull(fors);
			assertEquals(2, fors.size());

			final ForFiscal fp0 = fors.get(0);
			assertSameDay(newDate(1980, 1, 1), fp0.getDateDebut());
			assertSameDay(newDate(1987, 1, 31), fp0.getDateFin());
			assertFalse(fp0.isVirtuel());

			final ForFiscal fp1 = fors.get(1);
			assertSameDay(newDate(1987, 2, 1), fp1.getDateDebut());
			assertNull(fp1.getDateFin());
			assertTrue(fp1.isVirtuel());
		}

		//
		// une nouvelle fois sans les fors virtuels (permet de vérifier que le cache ne nous retourne pas les fors virtuels s'ils ne sont
		// pas demandés)
		//

		params.getParts().remove(TiersPart.FORS_FISCAUX_VIRTUELS);

		{
			final PersonnePhysique pp = (PersonnePhysique) service.getTiers(params);
			assertNotNull(pp);

			final List<ForFiscal> fors = pp.getForsFiscauxPrincipaux();
			assertNotNull(fors);
			assertEquals(1, fors.size());

			final ForFiscal fp = fors.get(0);
			assertSameDay(newDate(1980, 1, 1), fp.getDateDebut());
			assertSameDay(newDate(1987, 1, 31), fp.getDateFin());
			assertFalse(fp.isVirtuel());
		}
	}

	/**
	 * [UNIREG-1517] l'assujettissement courant d'un contribuable encore assujetti doit avoir une date de fin nulle.
	 */
	@Test
	public void getTiersHistoPeriodesAssujettissement() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(87654321); // Alfred Dupneu
		params.getParts().add(TiersPart.PERIODES_ASSUJETTISSEMENT);

		final PersonnePhysique pp = (PersonnePhysique) service.getTiers(params);
		assertNotNull(pp);

		final List<PeriodeAssujettissement> lic = pp.getPeriodesAssujettissementLIC();
		assertEquals(2, lic.size());

		{ // assujettissement passé

			final PeriodeAssujettissement a = lic.get(0);
			assertNotNull(a);
			assertSameDay(newDate(1985, 9, 1), a.getDateDebut());
			assertSameDay(newDate(1990, 2, 15), a.getDateFin());
			assertEquals(TypePeriodeAssujettissement.LIMITE, a.getType()); // Hors-Suisse
		}

		{ // assujettissement courant

			final PeriodeAssujettissement a = lic.get(1);
			assertNotNull(a);
			assertSameDay(newDate(2003, 7, 12), a.getDateDebut());
			assertNull(a.getDateFin());
			assertEquals(TypePeriodeAssujettissement.LIMITE, a.getType()); // Hors-Suisse
		}

		final List<PeriodeAssujettissement> lifd = pp.getPeriodesAssujettissementLIFD();
		assertEquals(2, lifd.size());

		{ // assujettissement passé

			final PeriodeAssujettissement a = lifd.get(0);
			assertNotNull(a);
			assertSameDay(newDate(1985, 9, 1), a.getDateDebut());
			assertSameDay(newDate(1990, 2, 15), a.getDateFin());
			assertEquals(TypePeriodeAssujettissement.LIMITE, a.getType()); // Hors-Suisse
		}

		{ // assujettissement courant

			final PeriodeAssujettissement a = lifd.get(1);
			assertNotNull(a);
			assertSameDay(newDate(2003, 7, 12), a.getDateDebut());
			assertNull(a.getDateFin());
			assertEquals(TypePeriodeAssujettissement.LIMITE, a.getType()); // Hors-Suisse
		}
	}

	/**
	 * [UNIREG-1969] Vérification que le champ "chez" apparaît bien dans l'adresse d'envoi
	 */
	@Test
	public void getTiersPMAdresseEnvoi() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(312); // PLACE CENTRALE, La Sarraz
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);

		final AdresseEnvoi adresse = pm.getAdresseEnvoi();
		assertNotNull(adresse);

		assertEquals("Société immobilière de", trimValiPattern(adresse.getLigne1()));
		assertEquals("Place centrale S.A. Pe", trimValiPattern(adresse.getLigne2()));
		assertEquals("en liquidation", trimValiPattern(adresse.getLigne3()));
		assertEquals("c/o Mme Hugette Grisel", trimValiPattern(adresse.getLigne4()));
		assertEquals("Rue du Chêne 9", trimValiPattern(adresse.getLigne5()));
		assertEquals("1315 La Sarraz", trimValiPattern(adresse.getLigne6()));
		assertEquals(TypeAffranchissement.SUISSE, adresse.getTypeAffranchissement());
	}

	/**
	 * [UNIREG-1974] Vérification du libellé de la rue dans l'adresse d'envoi
	 */
	@Test
	public void getTiersPMAdresseEnvoiNomRue() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1314); // JAL HOLDING
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);

		final AdresseEnvoi adresse = pm.getAdresseEnvoi();
		assertNotNull(adresse);

		assertEquals("Jal holding S.A.", trimValiPattern(adresse.getLigne1()));
		assertEquals("", trimValiPattern(adresse.getLigne2()));
		assertEquals("en liquidation", trimValiPattern(adresse.getLigne3()));
		assertEquals("pa Fidu. Commerce & Industrie", trimValiPattern(adresse.getLigne4()));
		assertEquals("Avenue de la Gare 10", trimValiPattern(adresse.getLigne5()));
		assertEquals("1003 Lausanne", trimValiPattern(adresse.getLigne6()));
		assertEquals(TypeAffranchissement.SUISSE, adresse.getTypeAffranchissement());
	}
}
