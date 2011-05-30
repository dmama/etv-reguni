package ch.vd.uniregctb.webservice.tiers3;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservices.tiers3.Adresse;
import ch.vd.uniregctb.webservices.tiers3.AdresseEnvoi;
import ch.vd.uniregctb.webservices.tiers3.CategorieDebiteur;
import ch.vd.uniregctb.webservices.tiers3.Date;
import ch.vd.uniregctb.webservices.tiers3.Debiteur;
import ch.vd.uniregctb.webservices.tiers3.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers3.Declaration;
import ch.vd.uniregctb.webservices.tiers3.DeclarationImpotSource;
import ch.vd.uniregctb.webservices.tiers3.EtatCivil;
import ch.vd.uniregctb.webservices.tiers3.ForFiscal;
import ch.vd.uniregctb.webservices.tiers3.GetDebiteurInfoRequest;
import ch.vd.uniregctb.webservices.tiers3.GetListeCtbModifiesRequest;
import ch.vd.uniregctb.webservices.tiers3.GetTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.MenageCommun;
import ch.vd.uniregctb.webservices.tiers3.ModeCommunication;
import ch.vd.uniregctb.webservices.tiers3.ModeImposition;
import ch.vd.uniregctb.webservices.tiers3.PeriodeImposition;
import ch.vd.uniregctb.webservices.tiers3.Periodicite;
import ch.vd.uniregctb.webservices.tiers3.PeriodiciteDecompte;
import ch.vd.uniregctb.webservices.tiers3.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers3.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers3.RapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersResponse;
import ch.vd.uniregctb.webservices.tiers3.Sexe;
import ch.vd.uniregctb.webservices.tiers3.SituationFamille;
import ch.vd.uniregctb.webservices.tiers3.TarifImpotSource;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersInfo;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.TypeActivite;
import ch.vd.uniregctb.webservices.tiers3.TypeAffranchissement;
import ch.vd.uniregctb.webservices.tiers3.TypeAutoriteFiscale;
import ch.vd.uniregctb.webservices.tiers3.TypeRapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers3.TypeRecherche;
import ch.vd.uniregctb.webservices.tiers3.TypeTiers;
import ch.vd.uniregctb.webservices.tiers3.UserLogin;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
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
		login.setOid(22);
	}

	@Test
	public void testGetListCtbModifies() throws Exception {

		final GetListeCtbModifiesRequest params = new GetListeCtbModifiesRequest();
		params.setLogin(login);
		RegDate dateDebut = RegDate.get(2008, 1, 1);
		RegDate dateFin = RegDate.get(2008, 1, 2);
		XMLGregorianCalendar calDebut = regdate2xmlcal(dateDebut);
		XMLGregorianCalendar calFin = regdate2xmlcal(dateFin);
		params.setDateDebutRecherche(calDebut);
		params.setDateFinRecherche(calFin);

		Long[] arraytTiersId = service.getListeCtbModifies(params);

		assertNotNull(arraytTiersId);
		assertEquals(9, arraytTiersId.length);
	}


	@Test
	public void testGetDateArriveeSourcier() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);
		assertSameDay(newDate(2008, 1, 29), tiers.getDateArrivee());
	}

	@Test
	public void testGetNumeroAVSSourcier() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);
		assertEquals("7565789312435", tiers.getNouveauNumeroAssureSocial());
	}

	@Test
	public void testGetNomSourcier() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);
		assertEquals("Pirez", tiers.getNom());
	}

	@Test
	public void testGetPrenomSourcier() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);
		assertEquals("Isidor", tiers.getPrenom());
	}

	@Test
	public void testGetDateNaissanceSourcier() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);
		assertSameDay(newDate(1971, 1, 23), tiers.getDateNaissance());
	}

	@Test
	public void testGetTarifApplicableAvecHistorique() throws Exception {

		GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600003); // Mario Gomez
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		// Récupération du tiers
		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Recherche du ménage courant du tiers
		final List<RapportEntreTiers> rapports = tiers.getRapportsEntreTiers();
		assertNotNull(rapports);

		RapportEntreTiers rapportMenage = null;
		for (RapportEntreTiers r : rapports) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType() && r.getDateFin() == null) {
				rapportMenage = r;
				break;
			}
		}
		assertNotNull(rapportMenage);

		// Note: l'information 'tarif applicable' n'a de sens et n'est renseignée que sur un ménage commun
		GetTiersRequest paramsMenage = new GetTiersRequest();
		paramsMenage.setLogin(login);
		paramsMenage.setTiersNumber(rapportMenage.getAutreTiersNumero());
		paramsMenage.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final MenageCommun menage = (MenageCommun) service.getTiers(paramsMenage);
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

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
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

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
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
	public void testGetRapportPrestationsImposablesHistoSourcier() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600003); // Mario Gomez
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Récupération de l'historique des rapport de travail entre le sourcier et son (ses) employeur(s)
		final List<RapportEntreTiers> rapports = new ArrayList<RapportEntreTiers>();
		for (RapportEntreTiers r : tiers.getRapportsEntreTiers()) {
			if (TypeRapportEntreTiers.PRESTATION_IMPOSABLE == r.getType()) {
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

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.PERIODICITES);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);
		assertEquals(CategorieDebiteur.REGULIERS, debiteur.getCategorie());
		assertEquals(ModeCommunication.PAPIER, debiteur.getModeCommunication());
		assertEquals("Café du Commerce", debiteur.getComplementNom());

		final List<Periodicite> periodicites = debiteur.getPeriodicites();
		assertNotNull(periodicites);
		assertEquals(1, periodicites.size());

		final Periodicite periodicite0 = periodicites.get(0);
		assertEquals(PeriodiciteDecompte.TRIMESTRIEL, periodicite0.getPeriodiciteDecompte());
	}

	@Test
	public void testGetActiviteDebiteur() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		final List<ForFiscal> fors = debiteur.getForsFiscauxPrincipaux();
		assertNotNull(fors);
		assertEquals(2, fors.size());

		// Ancienne activité sur Renens
		final ForFiscal for0 = fors.get(0);
		assertNotNull(for0);
		assertEquals(5591, for0.getNoOfsAutoriteFiscale()); // Renens
		assertSameDay(newDate(2002, 7, 1), for0.getDateDebut()); // = date début d'activité
		assertSameDay(newDate(2002, 12, 31), for0.getDateFin()); // = date fin d'activité

		// Nouvelle activité sur Lausanne
		final ForFiscal for1 = fors.get(1);
		assertNotNull(for1);
		assertEquals(5586, for1.getNoOfsAutoriteFiscale()); // Lausanne
		assertSameDay(newDate(2003, 3, 1), for1.getDateDebut());
		assertNull(for1.getDateFin());
	}

	@Test
	public void testGetPeriodiciteDebiteur() throws Exception {
		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.PERIODICITES);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		final List<Periodicite> periodicites = debiteur.getPeriodicites();
		assertNotNull(periodicites);
		assertEquals(1, periodicites.size());
		Periodicite p1 = periodicites.get(0);
		assertSameDay(newDate(2002, 7, 1), p1.getDateDebut());
		assertEquals(PeriodiciteDecompte.TRIMESTRIEL, p1.getPeriodiciteDecompte());
	}

	private static int index(Date date) {
		return date.getYear() * 10000 + date.getMonth() * 100 + date.getDay();
	}

	private static boolean isBeforeOrEquals(Date left, Date right) {
		return index(left) <= index(right);
	}

	private static boolean isTiersActif(Tiers tiers, Date date) {

		boolean estActif = false;
		for (ForFiscal f : tiers.getForsFiscauxPrincipaux()) {
			if (isBeforeOrEquals(f.getDateDebut(), date)
					&& (f.getDateFin() == null || isBeforeOrEquals(date, f.getDateFin()))) {
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

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		assertFalse(isTiersActif(debiteur, _2000_01_01));
		assertTrue(isTiersActif(debiteur, _2002_07_01)); // à Renens
		assertTrue(isTiersActif(debiteur, _2003_04_01)); // à Lausanne
	}

	@Test
	public void testSearchPersonneParNomEtDateNaissance() throws Exception {

		final SearchTiersRequest params = new SearchTiersRequest();
		params.setLogin(login);
		params.setTypeRecherche(TypeRecherche.CONTIENT);
		params.setNomCourrier("Pirez");
		params.setDateNaissance(newDate(1971, 1, 23));

		final SearchTiersResponse resultat = service.searchTiers(params);
		assertNotNull(resultat);
		assertEquals(1, resultat.getItem().size());

		final TiersInfo info = resultat.getItem().get(0);
		assertEquals(12600001L, info.getNumero());
		assertEquals(TypeTiers.PERSONNE_PHYSIQUE, info.getType());
	}

	@Test
	public void testGetContribuablePrincipalAvecHistorique() throws Exception {

		// Récupération du ménage commun à partir du contribuable

		GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600003); // Mario Gomez
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		// Récupération du tiers
		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Recherche du ménage courant du tiers
		final List<RapportEntreTiers> rapports = tiers.getRapportsEntreTiers();
		assertNotNull(rapports);

		RapportEntreTiers rapportMenage = null;
		for (RapportEntreTiers r : rapports) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType() && r.getDateFin() == null) {
				rapportMenage = r;
				break;
			}
		}
		assertNotNull(rapportMenage);

		// Récupération du contribuable principal (au sens 'source') du ménage commun

		// Note: l'information 'contribuable principal' n'a de sens et n'est renseignée que sur un ménage commun
		GetTiersRequest paramsMenage = new GetTiersRequest();
		paramsMenage.setLogin(login);
		paramsMenage.setTiersNumber(rapportMenage.getAutreTiersNumero());
		paramsMenage.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final MenageCommun menage = (MenageCommun) service.getTiers(paramsMenage);
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
		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce

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
		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		assertEquals("0213332211", debiteur.getNumeroTelProf());
		assertEquals("0213332212", debiteur.getNumeroTelPrive());
		assertEquals("0213332213", debiteur.getNumeroTelecopie());
		assertEquals("0790001234", debiteur.getNumeroTelPortable());
		assertEquals("sabri@cafeducommerce.ch", debiteur.getAdresseCourrierElectronique());
	}

	@Test
	public void testGetPeriodiciteDeclarationDebiteurAvecHistorique() throws Exception {

		// Recherche débiteur
		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.DECLARATIONS);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		// Récupère l'historique des déclarations
		final List<Declaration> declarations = debiteur.getDeclarations();
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

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		// Mode communication
		assertEquals(ModeCommunication.PAPIER, debiteur.getModeCommunication());
	}

	@Test
	public void testGetModeCommunicationHistoDebiteur() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.DECLARATIONS);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		final List<Declaration> declarations = debiteur.getDeclarations();
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
		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
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
		assertEquals("Sabri Inanç Ertem", adresseEnvoi.getLigne1());
		assertEquals("Café du Commerce", adresseEnvoi.getLigne2());
		assertEquals("Avenue de Beaulieu 12", adresseEnvoi.getLigne3());
		assertEquals("1004 Lausanne", adresseEnvoi.getLigne4());
		assertNull(adresseEnvoi.getLigne5());
		assertNull(adresseEnvoi.getLigne6());
		assertEquals(TypeAffranchissement.SUISSE, adresseEnvoi.getTypeAffranchissement());
	}

	@Test
	public void testGetAdressesDebiteur() throws Exception {

		// Recherche débiteur
		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.ADRESSES);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		/**
		 * <pre>
		 * Avenue de Beaulieu 12
		 * 1004 Lausanne Secteur de dist.
		 * </pre>
		 */
		final List<Adresse> adressesCourrier = debiteur.getAdressesCourrier();
		assertNotNull(adressesCourrier);
		assertEquals(2, adressesCourrier.size());

		final Adresse adresseCourrier0 = adressesCourrier.get(0);
		assertNotNull(adresseCourrier0);
		assertNull(adresseCourrier0.getDateDebut());
		assertEquals(newDate(2002, 6, 30), adresseCourrier0.getDateFin());
		assertEquals("Rue de Lausanne 59", adresseCourrier0.getRue());
		assertNull(adresseCourrier0.getNumeroRue());
		assertEquals("1028", adresseCourrier0.getNumeroPostal());
		assertEquals("Préverenges", adresseCourrier0.getLocalite());
		assertNull(adresseCourrier0.getCasePostale());
		assertNull(adresseCourrier0.getNumeroAppartement());
		assertEquals(Integer.valueOf(0),adresseCourrier0.getNoRue());
		assertEquals(175, adresseCourrier0.getNoOrdrePostal());

		final Adresse adresseCourrier1 = adressesCourrier.get(1);
		assertNotNull(adresseCourrier1);
		assertEquals(newDate(2002, 7, 1), adresseCourrier1.getDateDebut());
		assertNull(adresseCourrier1.getDateFin());
		assertEquals("Avenue de Beaulieu", adresseCourrier1.getRue());
		assertEquals("12", adresseCourrier1.getNumeroRue());
		assertEquals("1004", adresseCourrier1.getNumeroPostal());
		assertEquals("Lausanne", adresseCourrier1.getLocalite());
		assertNull(adresseCourrier1.getCasePostale());
		assertNull(adresseCourrier1.getNumeroAppartement());
		assertEquals(Integer.valueOf(30387), adresseCourrier1.getNoRue());
		assertEquals(151, adresseCourrier1.getNoOrdrePostal());

		// ... aussi disponibles :
		final List<Adresse> adresseDomicile = debiteur.getAdressesDomicile();
		assertNotNull(adresseDomicile);
		assertFalse(adresseDomicile.isEmpty());

		final List<Adresse> adressePoursuite = debiteur.getAdressesPoursuite();
		assertNotNull(adressePoursuite);
		assertFalse(adressePoursuite.isEmpty());

		final List<Adresse> adresseRepresentation = debiteur.getAdressesRepresentation();
		assertNotNull(adresseRepresentation);
		assertFalse(adresseRepresentation.isEmpty());
	}

	@Test
	public void testGetNumeroOfsCommuneDebiteurAvecHistorique() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		final List<ForFiscal> fors = debiteur.getForsFiscauxPrincipaux();
		assertNotNull(fors);
	}

	@Test
	public void testGetForDebiteur() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(1678432); // Café du Commerce
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		final List<ForFiscal> fors = debiteur.getForsFiscauxPrincipaux();
		assertNotNull(fors);
		assertEquals(2, fors.size());

		final ForFiscal for0 = fors.get(0);
		assertNotNull(for0);
		assertSameDay(newDate(2002, 7, 1), for0.getDateDebut());
		assertSameDay(newDate(2002, 12, 31), for0.getDateFin());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, for0.getTypeAutoriteFiscale());
		assertEquals(5591, for0.getNoOfsAutoriteFiscale()); // Renens VD

		final ForFiscal for1 = fors.get(1);
		assertNotNull(for1);
		assertSameDay(newDate(2003, 3, 1), for1.getDateDebut());
		assertNull(for1.getDateFin());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, for1.getTypeAutoriteFiscale());
		assertEquals(5586, for1.getNoOfsAutoriteFiscale()); // Lausanne
	}

	@Test
	public void testGetModeImpositionSourcier() throws Exception {

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600001); // Isidor Pirez
		params.getParts().add(TiersPart.FORS_FISCAUX);

		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
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

		GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12300003); // Emery Lyah
		params.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		// Récupération du tiers
		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Recherche de la situation de famille courante du tiers
		final List<SituationFamille> situationsFamille = tiers.getSituationsFamille();
		assertNotNull(situationsFamille);
		assertEquals(1, situationsFamille.size());

		final SituationFamille situationFamille = situationsFamille.get(0);
		assertNotNull(situationFamille);
		assertEquals(new Integer(1), situationFamille.getNombreEnfants());
	}

	@Test
	public void testGetNombreEnfantsAvecHistoriqueSurContribuableMarie() throws Exception {

		GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(12600003); // Mario Gomez
		params.getParts().add(TiersPart.RAPPORTS_ENTRE_TIERS);

		// Récupération du tiers
		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
		assertNotNull(tiers);

		// Recherche du ménage courant du tiers
		final List<RapportEntreTiers> rapports = tiers.getRapportsEntreTiers();
		assertNotNull(rapports);

		RapportEntreTiers rapportMenage = null;
		for (RapportEntreTiers r : rapports) {
			if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == r.getType() && r.getDateFin() == null) {
				rapportMenage = r;
				break;
			}
		}
		assertNotNull(rapportMenage);

		GetTiersRequest paramsHisto = new GetTiersRequest();
		paramsHisto.setLogin(login);
		paramsHisto.setTiersNumber(rapportMenage.getAutreTiersNumero());
		paramsHisto.getParts().add(TiersPart.SITUATIONS_FAMILLE);

		final MenageCommun menage = (MenageCommun) service.getTiers(paramsHisto);
		assertNotNull(menage);

		// Recherche de l'historique de la situation de famille du tiers ménage (ici, il n'y en a qu'une)
		final List<SituationFamille> situations = menage.getSituationsFamille();
		assertEquals(1, situations.size());

		final SituationFamille situationFamille = situations.get(0);
		assertNotNull(situationFamille);
		assertEquals(new Integer(2), situationFamille.getNombreEnfants());
	}

	// Récupération de la déclaration d'impôt source suite à la réception de l'événement fiscal OUVERTURE_PERIODE_DECOMPTE_LR
	@Test
	public void testGetDeclarationImpotSourceDepuisEvenementFiscal() throws Exception {

		// Données contenue dans l'événement fiscal reçu
		final Long numeroCtb = 1678432L;
		final Date dateEvenement = newDate(2008, 4, 1);

		final GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(numeroCtb); // Café du Commerce
		params.getParts().add(TiersPart.DECLARATIONS);

		final Debiteur debiteur = (Debiteur) service.getTiers(params);
		assertNotNull(debiteur);

		final List<Declaration> declarations = debiteur.getDeclarations();
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
	public void testGetHistoPeriodeImpositionContribuablePartiHSAvecImmeubleVD() throws Exception {

		GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(10184458); // Michel Dupont
		params.getParts().add(TiersPart.PERIODES_IMPOSITION);

		// Récupération du tiers
		final PersonnePhysique tiers = (PersonnePhysique) service.getTiers(params);
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

	/**
	 * [UNIREG-2110]
	 */
	@Test
	public void testGetDebiteurInfoRequest() throws Exception {

		GetDebiteurInfoRequest params = new GetDebiteurInfoRequest();
		params.setLogin(login);
		params.setNumeroDebiteur(1678432L); // débiteur trimestriel

		// 2008
		params.setPeriodeFiscale(2008);
		final DebiteurInfo info2008 = service.getDebiteurInfo(params);
		assertNotNull(info2008);
		assertEquals(1678432L, info2008.getNumeroDebiteur());
		assertEquals(2008, info2008.getPeriodeFiscale());
		assertEquals(4, info2008.getNbLRsTheorique());
		assertEquals(2, info2008.getNbLRsEmises()); // deux LRs émises : 2008010->20080331 et 20080401->20080630

		// 2009
		params.setPeriodeFiscale(2009);
		final DebiteurInfo info2009 = service.getDebiteurInfo(params);
		assertNotNull(info2009);
		assertEquals(1678432L, info2009.getNumeroDebiteur());
		assertEquals(2009, info2009.getPeriodeFiscale());
		assertEquals(4, info2009.getNbLRsTheorique());
		assertEquals(0, info2009.getNbLRsEmises()); // aucune LR émise
	}

	@Test
	public void testGetDebiteurInfoRequestDebiteurInconnu() throws Exception {

		GetDebiteurInfoRequest params = new GetDebiteurInfoRequest();
		params.setLogin(login);
		params.setNumeroDebiteur(1877222L); // débiteur inconnu
		params.setPeriodeFiscale(2008);

		// 2008
		try {
			service.getDebiteurInfo(params);
			fail("Le débiteur est inconnu, la méthode aurait dû lever une exception");
		}
		catch (WebServiceException e) {
			assertEquals("Le tiers n°1877222 n'existe pas.", e.getMessage());
		}
	}

	/**
	 * [UNIREG-2302]
	 */
	@Test
	public void testGetAdresseEnvoiPersonneMorale() throws Exception {

		GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(20222); // la BCV
		params.getParts().add(TiersPart.ADRESSES_ENVOI);

		final PersonneMorale pm = (PersonneMorale) service.getTiers(params);
		assertNotNull(pm);

		final AdresseEnvoi adresseEnvoi = pm.getAdresseEnvoi();
		assertNotNull(adresseEnvoi);

		// l'adresse d'envoi n'a pas de salutations
		assertNull(adresseEnvoi.getSalutations());
		assertEquals("Banque Cantonale Vaudo", trimValiPattern(adresseEnvoi.getLigne1()));
		assertEquals("", trimValiPattern(adresseEnvoi.getLigne2()));
		assertEquals("", trimValiPattern(adresseEnvoi.getLigne3()));
		assertEquals("pa Comptabilité financière", trimValiPattern(adresseEnvoi.getLigne4()));
		assertEquals("M. Daniel Küffer / CP 300", trimValiPattern(adresseEnvoi.getLigne5()));
		assertEquals("1001 Lausanne", adresseEnvoi.getLigne6());
		assertEquals(TypeAffranchissement.SUISSE, adresseEnvoi.getTypeAffranchissement());
		{
			final List<String> nomsPrenoms = adresseEnvoi.getNomsPrenoms();
			assertEquals(3, nomsPrenoms.size());
			assertEquals("Banque Cantonale Vaudo", trimValiPattern(nomsPrenoms.get(0)));
			assertEquals("", trimValiPattern(nomsPrenoms.get(1)));
			assertEquals("", trimValiPattern(nomsPrenoms.get(2)));
		}
		assertEquals("pa Comptabilité financière", adresseEnvoi.getComplement());
		assertNull(adresseEnvoi.getPourAdresse());
		assertEquals("M. Daniel Küffer / CP 300", adresseEnvoi.getRueNumero());
		assertNull(adresseEnvoi.getCasePostale());
		assertEquals("1001 Lausanne", adresseEnvoi.getNpaLocalite());
		assertNull(adresseEnvoi.getPays());

		// par contre, la formule d'appel est renseignée
		assertEquals("Madame, Monsieur", adresseEnvoi.getFormuleAppel());
	}

	public static XMLGregorianCalendar regdate2xmlcal(RegDate date) {
		if (date == null) {
			return null;
		}
		return getDataTypeFactory().newXMLGregorianCalendar(date.year(), date.month(), date.day(), 0, 0, 0, 0, DatatypeConstants.FIELD_UNDEFINED);
	}

	private static DatatypeFactory getDataTypeFactory() {
		DatatypeFactory datatypeFactory;
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		}
		catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
		return datatypeFactory;
	}
}
