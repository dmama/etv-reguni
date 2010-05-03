package ch.vd.uniregctb.webservice.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers1.Adresse;
import ch.vd.uniregctb.webservices.tiers1.AdresseEnvoi;
import ch.vd.uniregctb.webservices.tiers1.CategorieDebiteur;
import ch.vd.uniregctb.webservices.tiers1.Date;
import ch.vd.uniregctb.webservices.tiers1.Debiteur;
import ch.vd.uniregctb.webservices.tiers1.DebiteurHisto;
import ch.vd.uniregctb.webservices.tiers1.Declaration;
import ch.vd.uniregctb.webservices.tiers1.DeclarationImpotSource;
import ch.vd.uniregctb.webservices.tiers1.EtatCivil;
import ch.vd.uniregctb.webservices.tiers1.ForFiscal;
import ch.vd.uniregctb.webservices.tiers1.GetTiers;
import ch.vd.uniregctb.webservices.tiers1.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers1.MenageCommunHisto;
import ch.vd.uniregctb.webservices.tiers1.ModeCommunication;
import ch.vd.uniregctb.webservices.tiers1.ModeImposition;
import ch.vd.uniregctb.webservices.tiers1.PeriodeImposition;
import ch.vd.uniregctb.webservices.tiers1.PeriodiciteDecompte;
import ch.vd.uniregctb.webservices.tiers1.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers1.PersonnePhysiqueHisto;
import ch.vd.uniregctb.webservices.tiers1.RapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers1.SearchTiers;
import ch.vd.uniregctb.webservices.tiers1.Sexe;
import ch.vd.uniregctb.webservices.tiers1.SituationFamille;
import ch.vd.uniregctb.webservices.tiers1.TarifImpotSource;
import ch.vd.uniregctb.webservices.tiers1.TiersHisto;
import ch.vd.uniregctb.webservices.tiers1.TiersInfo;
import ch.vd.uniregctb.webservices.tiers1.TiersInfoArray;
import ch.vd.uniregctb.webservices.tiers1.TiersPart;
import ch.vd.uniregctb.webservices.tiers1.TypeActivite;
import ch.vd.uniregctb.webservices.tiers1.TypeAutoriteFiscale;
import ch.vd.uniregctb.webservices.tiers1.TypeRapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers1.TypeRecherche;
import ch.vd.uniregctb.webservices.tiers1.TypeTiers;
import ch.vd.uniregctb.webservices.tiers1.UserLogin;

public class TiersServiceWebTAOISTest extends AbstractTiersServiceWebTest {

	//private static final Logger LOGGER = Logger.getLogger(WebitTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersServiceWebTAOISTest.xml";

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
		login.setUserId("[UT] TiersServiceWebTAOISTest");
		login.setOid(0);
	}

	@Test
	public void testGetDateArriveeSourcier() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.setDate(null); // Etat courant

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);
		assertSameDay(newDate(2008, 1, 29), tiers.getDateArrivee());
	}

	@Test
	public void testGetNumeroAVSSourcier() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.setDate(null); // Etat courant

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);
		assertEquals("7565789312435", tiers.getNouveauNumeroAssureSocial());
	}

	@Test
	public void testGetNomSourcier() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.setDate(null); // Etat courant

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);
		assertEquals("Pirez", tiers.getNom());
	}

	@Test
	public void testGetPrenomSourcier() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.setDate(null); // Etat courant

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);
		assertEquals("Isidor", tiers.getPrenom());
	}

	@Test
	public void testGetDateNaissanceSourcier() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.setDate(null); // Etat courant

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);
		assertSameDay(newDate(1971, 1, 23), tiers.getDateNaissance());
	}

	@Test
	public void testGetTarifApplicableAvecHistorique() throws Exception {

		GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600003); // Mario Gomez
		params.setDate(null); // Etat courant
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		// Récupération du tiers
		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Recherche du ménage courant du tiers
		final List<RapportEntreTiers> rapports = tiers.getRapportsEntreTiers();
		assertNotNull(rapports);

		RapportEntreTiers rapportMenage = null;
		for (RapportEntreTiers r : rapports) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(r.getType()) && r.getDateFin() == null) {
				rapportMenage = r;
				break;
			}
		}
		assertNotNull(rapportMenage);

		// Note: l'information 'tarif applicable' n'a de sens et n'est renseignée que sur un ménage commun
		GetTiersHisto paramsHisto = new GetTiersHisto();
		paramsHisto.setLogin(login);
		paramsHisto.setTiersNumber(rapportMenage.getAutreTiersNumero());
		paramsHisto.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final MenageCommunHisto menage = (MenageCommunHisto) service.getTiersHisto(paramsHisto);
		assertNotNull(menage);

		// Recherche de l'historique de la situation de famille du tiers ménage (ici, il n'y en a qu'une)
		final List<SituationFamille> situations = menage.getSituationsFamille();
		assertEquals(1, situations.size());

		final SituationFamille situationFamille = situations.get(0);
		assertNotNull(situationFamille);
		assertEquals(TarifImpotSource.NORMAL, situationFamille.getTarifApplicable());
		assertEquals(Long.valueOf(12600003), situationFamille.getNumeroContribuablePrincipal());
	}

	@Test
	public void testGetEtatCivilSourcier() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.setDate(null); // Etat courant
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Recherche de la situation de famille courante du sourcier
		final SituationFamille situationFamille = tiers.getSituationFamille();
		assertEquals(EtatCivil.CELIBATAIRE, situationFamille.getEtatCivil());
	}

	@Test
	public void testGetEtatCivilHistoSourcier() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final PersonnePhysiqueHisto tiers = (PersonnePhysiqueHisto) service.getTiersHisto(params);
		assertNotNull(tiers);
		assertEquals("Pirez", tiers.getNom());
		assertEquals("Isidor", tiers.getPrenom());

		// Recherche de l'historique de la situation de famille du sourcier
		final List<SituationFamille> situations = tiers.getSituationsFamille();
		assertNotNull(situations);
		assertEquals(1, situations.size());

		final SituationFamille situationFamille = situations.get(0);
		assertNotNull(situationFamille);
		assertEquals(EtatCivil.CELIBATAIRE, situationFamille.getEtatCivil());
	}

	@Test
	public void testGetForPrincipalSourcier() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.setDate(null); // Etat courant
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Récupération du for fiscal principal courant du sourcier
		final ForFiscal forPrincipal = tiers.getForFiscalPrincipal();
		assertNotNull(forPrincipal);
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forPrincipal.getTypeAutoriteFiscale());
		assertEquals(5477L, forPrincipal.getNoOfsAutoriteFiscale()); // Cossonay
	}

	@Test
	public void testGetForPrincipalHistoSourcier() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final PersonnePhysiqueHisto tiers = (PersonnePhysiqueHisto) service.getTiersHisto(params);
		assertNotNull(tiers);

		// Récupération de l'historique des fors fiscaux principaux du sourcier
		final List<ForFiscal> forsPrincipaux = tiers.getForsFiscauxPrincipaux();
		assertNotNull(forsPrincipaux);
		assertEquals(1, forsPrincipaux.size());

		final ForFiscal forPrincipal = forsPrincipaux.get(0);
		assertNotNull(forPrincipal);
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forPrincipal.getTypeAutoriteFiscale());
		assertEquals(5477L, forPrincipal.getNoOfsAutoriteFiscale()); // Cossonay
	}

	@Test
	public void testGetRapportPrestationsImposablesSourcier() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600003); // Mario Gomez
		params.setDate(null); // Etat courant
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Récupération du rapport de travail courant entre le sourcier et son employeur
		final List<RapportEntreTiers> rapports = tiers.getRapportsEntreTiers();
		RapportEntreTiers rapport = null;
		for (RapportEntreTiers r : rapports) {
			if (TypeRapportEntreTiers.PRESTATION_IMPOSABLE.equals(r.getType())) {
				rapport = r;
				break;
			}
		}
		assertNotNull(rapport);
		assertEquals(TypeRapportEntreTiers.PRESTATION_IMPOSABLE, rapport.getType()); // autre tiers = débiteur
		assertEquals(1678432L, rapport.getAutreTiersNumero()); // 1678432 = N° débiteur
		assertSameDay(newDate(2008, 1, 29), rapport.getDateDebut());
		assertNull(rapport.getDateFin());
		assertEquals(TypeActivite.PRINCIPALE, rapport.getTypeActivite());
		assertEquals(Integer.valueOf(100), rapport.getTauxActivite()); // 100%
	}

	@Test
	public void testGetRapportPrestationsImposablesHistoSourcier() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(12600003); // Mario Gomez
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		final PersonnePhysiqueHisto tiers = (PersonnePhysiqueHisto) service.getTiersHisto(params);
		assertNotNull(tiers);

		// Récupération de l'historique des rapport de travail entre le sourcier et son (ses) employeur(s)
		final List<RapportEntreTiers> rapports = new ArrayList<RapportEntreTiers>();
		for (RapportEntreTiers r : tiers.getRapportsEntreTiers()) {
			if (TypeRapportEntreTiers.PRESTATION_IMPOSABLE.equals(r.getType())) {
				rapports.add(r);
			}
		}
		assertEquals(1, rapports.size());

		final RapportEntreTiers rapport = rapports.get(0);
		assertNotNull(rapport);
		assertEquals(TypeRapportEntreTiers.PRESTATION_IMPOSABLE, rapport.getType()); // autre tiers = débiteur
		assertEquals(1678432L, rapport.getAutreTiersNumero()); // 1678432 = N° débiteur
		assertSameDay(newDate(2008, 1, 29), rapport.getDateDebut());
		assertNull(rapport.getDateFin());
		assertEquals(TypeActivite.PRINCIPALE, rapport.getTypeActivite());
		assertEquals(Integer.valueOf(100), rapport.getTauxActivite()); // 100%
	}

	@Test
	public void testGetDebiteur() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.setDate(newDate(2008, 1, 1));

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);
		assertEquals(CategorieDebiteur.REGULIERS, debiteur.getCategorie());
		assertEquals(PeriodiciteDecompte.TRIMESTRIEL, debiteur.getPeriodiciteDecompte());
		assertEquals(ModeCommunication.PAPIER, debiteur.getModeCommunication());
		assertEquals("Café du Commerce", debiteur.getComplementNom());
	}

	@Test
	public void testGetActiviteDebiteur() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.setDate(newDate(2008, 1, 1));
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		// Activité courante sur Lausanne
		final ForFiscal forCourant = debiteur.getForFiscalPrincipal();
		assertNotNull(forCourant);
		assertEquals(5586, forCourant.getNoOfsAutoriteFiscale()); // Lausanne
		assertSameDay(newDate(2003, 3, 1), forCourant.getDateOuverture());
		assertNull(forCourant.getDateFermeture());
	}

	@Test
	public void testGetActiviteHistoDebiteur() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final DebiteurHisto debiteur = (DebiteurHisto) service.getTiersHisto(params);
		assertNotNull(debiteur);

		final List<ForFiscal> fors = debiteur.getForsFiscauxPrincipaux();
		assertNotNull(fors);
		assertEquals(2, fors.size());

		// Ancienne activité sur Renens
		final ForFiscal for0 = fors.get(0);
		assertNotNull(for0);
		assertEquals(5591, for0.getNoOfsAutoriteFiscale()); // Renens
		assertSameDay(newDate(2002, 7, 1), for0.getDateOuverture()); // = date début d'activité
		assertSameDay(newDate(2002, 12, 31), for0.getDateFermeture()); // = date fin d'activité

		// Nouvelle activité sur Lausanne
		final ForFiscal for1 = fors.get(1);
		assertNotNull(for1);
		assertEquals(5586, for1.getNoOfsAutoriteFiscale()); // Lausanne
		assertSameDay(newDate(2003, 3, 1), for1.getDateOuverture());
		assertNull(for1.getDateFermeture());
	}

	private static int index(Date date) {
		return date.getYear() * 10000 + date.getMonth() * 100 + date.getDay();
	}

	private static boolean isBeforeOrEquals(Date left, Date right) {
		return index(left) <= index(right);
	}

	private static boolean isTiersActif(TiersHisto tiers, Date date) {

		boolean estActif = false;
		for (ForFiscal f : tiers.getForsFiscauxPrincipaux()) {
			if (isBeforeOrEquals(f.getDateOuverture(), date)
					&& (f.getDateFermeture() == null || isBeforeOrEquals(date, f.getDateFermeture()))) {
				estActif = true;
				break;
			}
		}

		return estActif;
	}

	@Test
	public void testGetActiviteAUneDateDonneeDebiteur() throws Exception {

		final Date _2000_01_01 = new Date();
		_2000_01_01.setYear(2000);
		_2000_01_01.setMonth(1);
		_2000_01_01.setDay(1);

		final Date _2002_07_01 = new Date();
		_2002_07_01.setYear(2002);
		_2002_07_01.setMonth(7);
		_2002_07_01.setDay(1);

		final Date _2003_04_01 = new Date();
		_2003_04_01.setYear(2003);
		_2003_04_01.setMonth(4);
		_2003_04_01.setDay(1);

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final DebiteurHisto debiteur = (DebiteurHisto) service.getTiersHisto(params);
		assertNotNull(debiteur);

		assertFalse(isTiersActif(debiteur, _2000_01_01));
		assertTrue(isTiersActif(debiteur, _2002_07_01)); // à Renens
		assertTrue(isTiersActif(debiteur, _2003_04_01)); // à Lausanne
	}

	@Test
	public void testGetDebiteurHisto() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce

		final DebiteurHisto debiteur = (DebiteurHisto) service.getTiersHisto(params);
		assertNotNull(debiteur);
		assertEquals(CategorieDebiteur.REGULIERS, debiteur.getCategorie());
		assertEquals(PeriodiciteDecompte.TRIMESTRIEL, debiteur.getPeriodiciteDecompte());
		assertEquals(ModeCommunication.PAPIER, debiteur.getModeCommunication());
		assertEquals("Café du Commerce", debiteur.getComplementNom());
	}

	@Test
	public void testSearchPersonneParNomEtDateNaissance() throws Exception {

		final SearchTiers params = new SearchTiers();
		params.setLogin(login);
		params.setTypeRecherche(TypeRecherche.CONTIENT);
		params.setNomCourrier("Pirez");
		params.setDateNaissance(newDate(1971, 1, 23));

		final TiersInfoArray resultat = service.searchTiers(params);
		assertNotNull(resultat);
		assertEquals(1, resultat.getItem().size());

		final TiersInfo info = resultat.getItem().get(0);
		assertEquals(12600001L, info.getNumero());
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
	}

	@Test
	public void testGetContribuablePrincipalAvecHistorique() throws Exception {

		/*
		 * Récupération du ménage commun à partir du contribuable
		 */

		GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600003); // Mario Gomez
		params.setDate(null); // Etat courant
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		// Récupération du tiers
		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Recherche du ménage courant du tiers
		final List<RapportEntreTiers> rapports = tiers.getRapportsEntreTiers();
		assertNotNull(rapports);

		RapportEntreTiers rapportMenage = null;
		for (RapportEntreTiers r : rapports) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(r.getType()) && r.getDateFin() == null) {
				rapportMenage = r;
				break;
			}
		}
		assertNotNull(rapportMenage);

		/*
		 * Récupération du contribuable principal (au sens 'source') du ménage commun
		 */

		// Note: l'information 'contribuable principal' n'a de sens et n'est renseignée que sur un ménage commun
		GetTiersHisto paramsHisto = new GetTiersHisto();
		paramsHisto.setLogin(login);
		paramsHisto.setTiersNumber(rapportMenage.getAutreTiersNumero());
		paramsHisto.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final MenageCommunHisto menage = (MenageCommunHisto) service.getTiersHisto(paramsHisto);
		assertNotNull(menage);

		// Recherche du contribuable principal du ménage commun (avec historique)
		final List<SituationFamille> situations = menage.getSituationsFamille();
		assertNotNull(situations);
		assertEquals(1, situations.size());

		final SituationFamille situation = situations.get(0);
		assertNotNull(situation);
		assertEquals(Long.valueOf(12600003), situation.getNumeroContribuablePrincipal());
	}

	@Test
	public void testGetContribuableAssocieAuDebiteur() throws Exception {

		// Recherche débiteur
		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.setDate(null);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		// Recherche du contribuable associé
		final long contribuableId = debiteur.getContribuableAssocie();
		assertEquals(43308102L, contribuableId);

		params.setTiersNumber(contribuableId);

		final PersonnePhysique personne = (PersonnePhysique) service.getTiers(params);
		assertNotNull(personne);

		assertEquals("Sabri Inanç", personne.getPrenom());
		assertEquals("Ertem", trimValiPattern(personne.getNom()));
		assertEquals(Sexe.MASCULIN, personne.getSexe());
	}

	@Test
	public void testGetNoTelephoneEtEmailPersonneDeContactDebiteur() throws Exception {

		// Recherche débiteur
		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.setDate(null);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		assertEquals("0213332211", debiteur.getNumeroTelProf());
		assertEquals("0213332212", debiteur.getNumeroTelPrive());
		assertEquals("0213332213", debiteur.getNumeroTelecopie());
		assertEquals("0790001234", debiteur.getNumeroTelPortable());
		assertEquals("sabri@cafeducommerce.ch", debiteur.getAdresseCourrierElectronique());
	}

	@Test
	public void testGetPeriodiciteDeclarationDebiteur() throws Exception {

		// Recherche débiteur
		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.setDate(null);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		assertEquals(PeriodiciteDecompte.TRIMESTRIEL, debiteur.getPeriodiciteDecompte());
	}

	@Test
	public void testGetPeriodiciteDeclarationDebiteurAvecHistorique() throws Exception {

		// Recherche débiteur
		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.DECLARATIONS);

		final DebiteurHisto debiteur = (DebiteurHisto) service.getTiersHisto(params);
		assertNotNull(debiteur);

		// Récupère l'historique des déclarations
		final List<Declaration> declarations = debiteur.getDiOrLr();
		assertNotNull(declarations);
		assertEquals(2, declarations.size());

		final DeclarationImpotSource lr0 = (DeclarationImpotSource) declarations.get(0);
		assertNotNull(lr0);
		assertEquals(PeriodiciteDecompte.TRIMESTRIEL, lr0.getPeriodicite());

		final DeclarationImpotSource lr1 = (DeclarationImpotSource) declarations.get(1);
		assertNotNull(lr1);
		assertEquals(PeriodiciteDecompte.TRIMESTRIEL, lr1.getPeriodicite());
	}

	@Test
	public void testGetModeCommunicationDebiteur() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.setDate(newDate(2008, 1, 1));

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		// Mode communication
		assertEquals(ModeCommunication.PAPIER, debiteur.getModeCommunication());
	}

	@Test
	public void testGetModeCommunicationHistoDebiteur() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.DECLARATIONS);

		final DebiteurHisto debiteur = (DebiteurHisto) service.getTiersHisto(params);
		assertNotNull(debiteur);

		final List<Declaration> declarations = debiteur.getDiOrLr();
		assertNotNull(declarations);
		assertEquals(2, declarations.size());

		final DeclarationImpotSource dis_2008_1er_trimestre = (DeclarationImpotSource) declarations.get(0);
		final DeclarationImpotSource dis_2008_2eme_trimestre = (DeclarationImpotSource) declarations.get(1);

		// Mode communication
		assertEquals(ModeCommunication.PAPIER, dis_2008_1er_trimestre.getModeCommunication()); // 1er trimestre 2008
		assertEquals(ModeCommunication.PAPIER, dis_2008_2eme_trimestre.getModeCommunication());// 2ème trimestre 2008
		assertEquals(ModeCommunication.PAPIER, debiteur.getModeCommunication());// état courant
	}

	@Test
	public void testGetAdresseEnvoiDebiteur() throws Exception {

		// Recherche débiteur
		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.setDate(null);
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		/**
		 * <pre>
		 * Monsieur
		 * Sabri Inanç Ertem
		 * Café du Commerce
		 * Avenue de Beaulieu 12
		 * 1004 Lausanne Secteur de dist.
		 * </pre>
		 */
		final AdresseEnvoi adresseEnvoi = debiteur.getAdresseEnvoi();
		assertNotNull(adresseEnvoi);
		assertEquals("Café du Commerce", adresseEnvoi.getLigne1());
		assertEquals("Avenue de Beaulieu 12", adresseEnvoi.getLigne2());
		assertEquals("1004 Lausanne", adresseEnvoi.getLigne3());
		assertNull(adresseEnvoi.getLigne4());
	}

	@Test
	public void testGetAdressesDebiteur() throws Exception {

		// Recherche débiteur
		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.setDate(null);
		params.getParts().add(TiersPart.ADRESSES);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		/**
		 * <pre>
		 * Avenue de Beaulieu 12
		 * 1004 Lausanne Secteur de dist.
		 * </pre>
		 */
		final Adresse adresseCourrier = debiteur.getAdresseCourrier();
		assertNotNull(adresseCourrier);
		assertEquals("Avenue de Beaulieu", adresseCourrier.getRue());
		assertEquals("12", adresseCourrier.getNumeroRue());
		assertEquals("1004", adresseCourrier.getNumeroPostal());
		assertEquals("Lausanne", adresseCourrier.getLocalite());

		assertNull(adresseCourrier.getCasePostale());
		assertNull(adresseCourrier.getNumeroAppartement());

		// données techniques
		assertEquals(Integer.valueOf(30387), adresseCourrier.getNoRue());
		assertEquals(151, adresseCourrier.getNoOrdrePostal());

		// ... aussi disponibles :
		final Adresse adresseDomicile = debiteur.getAdresseDomicile();
		assertNotNull(adresseDomicile);
		final Adresse adressePoursuite = debiteur.getAdressePoursuite();
		assertNotNull(adressePoursuite);
		final Adresse adresseRepresentation = debiteur.getAdresseRepresentation();
		assertNotNull(adresseRepresentation);
	}

	@Test
	public void testGetNumeroOfsCommuneDebiteurAvecHistorique() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final DebiteurHisto debiteur = (DebiteurHisto) service.getTiersHisto(params);
		assertNotNull(debiteur);

		final List<ForFiscal> fors = debiteur.getForsFiscauxPrincipaux();
		assertNotNull(fors);
	}

	@Test
	public void testGetForDebiteur() throws Exception {

		final GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		// retourne le for débiteur courant
		final ForFiscal fors = debiteur.getForFiscalPrincipal();
		assertNotNull(fors);
		assertSameDay(newDate(2003, 3, 1), fors.getDateOuverture());
		assertNull(fors.getDateFermeture());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, fors.getTypeAutoriteFiscale());
		assertEquals(5586, fors.getNoOfsAutoriteFiscale()); // Lausanne
	}

	@Test
	public void testGetForDebiteurHisto() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final DebiteurHisto debiteur = (DebiteurHisto) service.getTiersHisto(params);
		assertNotNull(debiteur);

		final List<ForFiscal> fors = debiteur.getForsFiscauxPrincipaux();
		assertNotNull(fors);
		assertEquals(2, fors.size());

		final ForFiscal for0 = fors.get(0);
		assertNotNull(for0);
		assertSameDay(newDate(2002, 7, 1), for0.getDateOuverture());
		assertSameDay(newDate(2002, 12, 31), for0.getDateFermeture());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, for0.getTypeAutoriteFiscale());
		assertEquals(5591, for0.getNoOfsAutoriteFiscale()); // Renens VD

		final ForFiscal for1 = fors.get(1);
		assertNotNull(for1);
		assertSameDay(newDate(2003, 3, 1), for1.getDateOuverture());
		assertNull(for1.getDateFermeture());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, for1.getTypeAutoriteFiscale());
		assertEquals(5586, for1.getNoOfsAutoriteFiscale()); // Lausanne
	}

	@Test
	public void testGetModeImpositionSourcier() throws Exception {

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final PersonnePhysiqueHisto tiers = (PersonnePhysiqueHisto) service.getTiersHisto(params);
		assertNotNull(tiers);

		// Récupération de l'historique des fors fiscaux principaux du sourcier
		final List<ForFiscal> forsPrincipaux = tiers.getForsFiscauxPrincipaux();
		assertNotNull(forsPrincipaux);
		assertEquals(1, forsPrincipaux.size());

		final ForFiscal forPrincipal = forsPrincipaux.get(0);
		assertNotNull(forPrincipal);
		assertEquals(ModeImposition.MIXTE_137_1, forPrincipal.getModeImposition());
	}

	@Test
	public void testGetNombreEnfantsAvecHistoriqueSurContribuableSeul() throws Exception {

		GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12300003); // Emery Lyah
		params.setDate(null); // Etat courant
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		// Récupération du tiers
		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Recherche de la situation de famille courante du tiers
		final SituationFamille situationFamille = tiers.getSituationFamille();
		assertNotNull(situationFamille);
		assertEquals(new Integer(1), situationFamille.getNombreEnfants());
	}

	@Test
	public void testGetNombreEnfantsAvecHistoriqueSurContribuableMarie() throws Exception {

		GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(12600003); // Mario Gomez
		params.setDate(null); // Etat courant
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		// Récupération du tiers
		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Recherche du ménage courant du tiers
		final List<RapportEntreTiers> rapports = tiers.getRapportsEntreTiers();
		assertNotNull(rapports);

		RapportEntreTiers rapportMenage = null;
		for (RapportEntreTiers r : rapports) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(r.getType()) && r.getDateFin() == null) {
				rapportMenage = r;
				break;
			}
		}
		assertNotNull(rapportMenage);

		GetTiersHisto paramsHisto = new GetTiersHisto();
		paramsHisto.setLogin(login);
		paramsHisto.setTiersNumber(rapportMenage.getAutreTiersNumero());
		paramsHisto.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final MenageCommunHisto menage = (MenageCommunHisto) service.getTiersHisto(paramsHisto);
		assertNotNull(menage);

		// Recherche de l'historique de la situation de famille du tiers ménage (ici, il n'y en a qu'une)
		final List<SituationFamille> situations = menage.getSituationsFamille();
		assertEquals(1, situations.size());

		final SituationFamille situationFamille = situations.get(0);
		assertNotNull(situationFamille);
		assertEquals(new Integer(2), situationFamille.getNombreEnfants());
	}

	/**
	 * Récupération de la déclaration d'impôt source suite à la réception de l'événement fiscal OUVERTURE_PERIODE_DECOMPTE_LR
	 */
	@Test
	public void testGetDeclarationImpotSourceDepuisEvenementFiscal() throws Exception {

		// Données contenue dans l'événement fiscal reçu
		final Long numeroCtb = 1678432L;
		final Date dateEvenement = newDate(2008, 4, 1);

		final GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(numeroCtb); // Café du Commerce
		params.getParts().add(TiersPart.DECLARATIONS);

		final DebiteurHisto debiteur = (DebiteurHisto) service.getTiersHisto(params);
		assertNotNull(debiteur);

		final List<Declaration> declarations = debiteur.getDiOrLr();
		assertNotNull(declarations);

		DeclarationImpotSource declaration = null;
		for (Declaration d : declarations) {
			DeclarationImpotSource dis = (DeclarationImpotSource) d;
			if (within(dateEvenement, dis.getDateDebut(), dis.getDateFin())) {
				declaration = dis;
				break;
			}
		}
		assertNotNull(declaration);
		assertSameDay(newDate(2008, 4, 1), declaration.getDateDebut());
		assertSameDay(newDate(2008, 6, 30), declaration.getDateFin());
	}

	/**
	 * [UNIREG-1253] Test qu'un contribuable vaudois qui part HS en milieu d'année et garde un immeuble sur sol vaudois possède bien une période d'imposition couvrant toute l'année
	 */
	@Test
	public void testGetPeriodeImpositionContribuablePartiHSAvecImmeubleVD() throws Exception {

		GetTiers params = new GetTiers();
		params.setLogin(login);
		params.setTiersNumber(10184458); // Michel Dupont
		params.setDate(newDate(2008, 12, 31)); // état fin 2008
		params.getParts().add(TiersPart.PERIODE_IMPOSITION);

		// Récupération du tiers
		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Vérifie qu'il possède une seul période fiscale pour 2008, malgré son départ HS (car il garde un immeuble)
		final PeriodeImposition periode = tiers.getPeriodeImposition();
		assertNotNull(periode);
		assertSameDay(newDate(2008, 1, 1), periode.getDateDebut());
		assertSameDay(newDate(2008, 12, 31), periode.getDateFin());
		assertEquals(Long.valueOf(6), periode.getIdDI());
	}

	/**
	 * [UNIREG-1253] Test qu'un contribuable vaudois qui part HS en milieu d'année et garde un immeuble sur sol vaudois possède bien une période d'imposition couvrant toute l'année
	 */
	@Test
	public void testGetHistoPeriodeImpositionContribuablePartiHSAvecImmeubleVD() throws Exception {

		GetTiersHisto params = new GetTiersHisto();
		params.setLogin(login);
		params.setTiersNumber(10184458); // Michel Dupont
		params.getParts().add(TiersPart.PERIODE_IMPOSITION);

		// Récupération du tiers
		final PersonnePhysiqueHisto tiers = (PersonnePhysiqueHisto) service.getTiersHisto(params);
		assertNotNull(tiers);

		// Vérifie qu'il possède une seul période fiscale pour 2008, malgré son départ HS (car il garde un immeuble)
		final List<PeriodeImposition> periodes = tiers.getPeriodesImposition();
		assertEquals(3, periodes.size()); // 2006, 2007 et 2008

		final PeriodeImposition p2006 = periodes.get(0);
		assertNotNull(p2006);
		assertSameDay(newDate(2006, 1, 1), p2006.getDateDebut());
		assertSameDay(newDate(2006, 12, 31), p2006.getDateFin());

		final PeriodeImposition p2007 = periodes.get(1);
		assertNotNull(p2007);
		assertSameDay(newDate(2007, 1, 1), p2007.getDateDebut());
		assertSameDay(newDate(2007, 12, 31), p2007.getDateFin());

		final PeriodeImposition p2008 = periodes.get(2);
		assertNotNull(p2008);
		assertSameDay(newDate(2008, 1, 1), p2008.getDateDebut());
		assertSameDay(newDate(2008, 12, 31), p2008.getDateFin());
		assertEquals(Long.valueOf(6), p2008.getIdDI());
	}

	private static boolean within(Date d, Date rangeStart, Date rangeEnd) {
		return beforeOrEqual(rangeStart, d) && (rangeEnd == null || beforeOrEqual(d, rangeEnd));
	}

	private static boolean beforeOrEqual(Date left, Date right) {
		if (left.getYear() == right.getYear()) {
			if (left.getMonth() == right.getMonth()) {
				return left.getDay() <= right.getDay();
			}
			else {
				return left.getMonth() < right.getMonth();
			}
		}
		else {
			return left.getYear() < right.getYear();
		}
	}
}
