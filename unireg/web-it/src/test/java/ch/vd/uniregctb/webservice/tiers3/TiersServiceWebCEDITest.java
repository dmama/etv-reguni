package ch.vd.uniregctb.webservice.tiers3;

import java.util.List;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers3.BusinessExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.CodeQuittancement;
import ch.vd.uniregctb.webservices.tiers3.Date;
import ch.vd.uniregctb.webservices.tiers3.Declaration;
import ch.vd.uniregctb.webservices.tiers3.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.webservices.tiers3.DeclarationImpotOrdinaireKey;
import ch.vd.uniregctb.webservices.tiers3.DemandeQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers3.EtatDeclaration;
import ch.vd.uniregctb.webservices.tiers3.GetTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.QuittancerDeclarationsRequest;
import ch.vd.uniregctb.webservices.tiers3.QuittancerDeclarationsResponse;
import ch.vd.uniregctb.webservices.tiers3.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.tiers3.UserLogin;
import ch.vd.uniregctb.webservices.tiers3.WebServiceExceptionInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test d'intégration des fonctions du web-service Tiers v2 utilisées par le CEDI.
 */
@SuppressWarnings({"JavaDoc"})
public class TiersServiceWebCEDITest extends AbstractTiersServiceWebTest {

	// private static final Logger LOGGER = Logger.getLogger(TiersServiceWebTest.class);

	private static final String DB_UNIT_DATA_FILE = "TiersServiceWebCEDITest.xml";

	private UserLogin login;

	private static boolean alreadySetUp = false;

	public TiersServiceWebCEDITest() throws Exception {
	}

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}

		login = new UserLogin();
		login.setUserId("[UT] TiersServiceWebCEDITest");
		login.setOid(22);
	}

	@Test
	public void testQuittancerDeclarationContribuableInconnu() throws Exception {

		QuittancerDeclarationsRequest params = new QuittancerDeclarationsRequest();
		params.setLogin(login);

		// Le contribuable est inconnu -> erreur
		final DemandeQuittancementDeclaration demande = newDemande(newDate(2009, 4, 1), 90909090L, 1, 2008);
		params.getDemandes().add(demande);

		final QuittancerDeclarationsResponse array = service.quittancerDeclarations(params);
		assertNotNull(array);

		final List<ReponseQuittancementDeclaration> reponses = array.getItem();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final ReponseQuittancementDeclaration reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(CodeQuittancement.ERREUR_CTB_INCONNU, reponse.getCode());

		final WebServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("Le contribuable est inconnu.", exceptionInfo.getMessage());
	}

	@Test
	public void testQuittancerDeclarationContribuableSansForFiscal() throws Exception {

		QuittancerDeclarationsRequest params = new QuittancerDeclarationsRequest();
		params.setLogin(login);

		// Le contribuable ne possède pas de for fiscal -> erreur
		final DemandeQuittancementDeclaration demande = newDemande(newDate(2009, 4, 1), 10501047L, 1, 2008);
		params.getDemandes().add(demande);

		final QuittancerDeclarationsResponse array = service.quittancerDeclarations(params);
		assertNotNull(array);

		final List<ReponseQuittancementDeclaration> reponses = array.getItem();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final ReponseQuittancementDeclaration reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(CodeQuittancement.ERREUR_ASSUJETTISSEMENT_CTB, reponse.getCode());

		final WebServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("Le contribuable ne possède aucun for principal : il n'aurait pas dû recevoir de déclaration d'impôt.", exceptionInfo.getMessage());
	}

	@Test
	public void testQuittancerDeclarationContribuableDebiteurInactif() throws Exception {

		QuittancerDeclarationsRequest params = new QuittancerDeclarationsRequest();
		params.setLogin(login);

		// Le contribuable est un débiteur inactif -> erreur
		final DemandeQuittancementDeclaration demande = newDemande(newDate(2009, 4, 1), 10582592L, 1, 2008);
		params.getDemandes().add(demande);

		final QuittancerDeclarationsResponse array = service.quittancerDeclarations(params);
		assertNotNull(array);

		final List<ReponseQuittancementDeclaration> reponses = array.getItem();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final ReponseQuittancementDeclaration reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(CodeQuittancement.ERREUR_CTB_DEBITEUR_INACTIF, reponse.getCode());

		final WebServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("Le contribuable est un débiteur inactif : impossible de quittancer la déclaration.", exceptionInfo.getMessage());
	}

	@Test
	public void testQuittancerDeclarationDeclarationInexistante() throws Exception {

		QuittancerDeclarationsRequest params = new QuittancerDeclarationsRequest();
		params.setLogin(login);

		// Le tiers ne possède qu'une seule déclaration en 2008, demander la quatrième ne devrait donc pas fonctionner
		final DemandeQuittancementDeclaration demande = newDemande(newDate(2009, 4, 1), 28014710L, 4, 2008);
		params.getDemandes().add(demande);

		final QuittancerDeclarationsResponse array = service.quittancerDeclarations(params);
		assertNotNull(array);

		final List<ReponseQuittancementDeclaration> reponses = array.getItem();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final ReponseQuittancementDeclaration reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(CodeQuittancement.ERREUR_DECLARATION_INEXISTANTE, reponse.getCode());

		final WebServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("La déclaration n'existe pas.", exceptionInfo.getMessage());
	}


	@Test
	public void testQuittancerDeclarationAvantDateEnvoi() throws Exception {

		QuittancerDeclarationsRequest params = new QuittancerDeclarationsRequest();
		params.setLogin(login);

		// Le déclaration a été envoyée le 28 janvier 2009, demander un quittancement au 1er janvier 2009 ne devrait donc pas fonctionner
		final DemandeQuittancementDeclaration demande = newDemande(newDate(2009, 1, 1), 28014710L, 1, 2008);
		params.getDemandes().add(demande);

		final QuittancerDeclarationsResponse array = service.quittancerDeclarations(params);
		assertNotNull(array);

		final List<ReponseQuittancementDeclaration> reponses = array.getItem();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final ReponseQuittancementDeclaration reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(CodeQuittancement.ERREUR_DATE_RETOUR_INVALIDE, reponse.getCode());

		final WebServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("La date de retour spécifiée (2009.01.01) est avant la date d'envoi de la déclaration (2009.01.28).", exceptionInfo.getMessage());
	}

	@Test
	public void testQuittancerDeclarationAnnulee() throws Exception {

		QuittancerDeclarationsRequest params = new QuittancerDeclarationsRequest();
		params.setLogin(login);

		// Le déclaration a été annulée après l'envoi, demander un quittancement ne devrait donc pas fonctionner
		final DemandeQuittancementDeclaration demande = newDemande(newDate(2009, 4, 1), 38005301L, 1, 2008);
		params.getDemandes().add(demande);

		final QuittancerDeclarationsResponse array = service.quittancerDeclarations(params);
		assertNotNull(array);

		final List<ReponseQuittancementDeclaration> reponses = array.getItem();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final ReponseQuittancementDeclaration reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(CodeQuittancement.ERREUR_DECLARATION_ANNULEE, reponse.getCode());

		final WebServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("La déclaration a été annulée entre-temps.", exceptionInfo.getMessage());
	}

	@Test
	public void testQuittancerDeclaration() throws Exception {

		QuittancerDeclarationsRequest params = new QuittancerDeclarationsRequest();
		params.setLogin(login);

		// Le contribuable possède une seule déclaration, émise, non-sommée, non-annulée, ..., le cas standard quoi.
		final DemandeQuittancementDeclaration demande = newDemande(newDate(2009, 4, 1), 28014710L, 1, 2008);
		params.getDemandes().add(demande);

		final QuittancerDeclarationsResponse array = service.quittancerDeclarations(params);
		assertNotNull(array);

		final List<ReponseQuittancementDeclaration> reponses = array.getItem();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final ReponseQuittancementDeclaration reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(CodeQuittancement.OK, reponse.getCode());
		assertNull(reponse.getExceptionInfo());
		assertQuittancement(newDate(2009, 4, 1), 28014710L, 2008, 1);
	}

	@Test
	public void testQuittancerDeclarationDejaSommee() throws Exception {

		QuittancerDeclarationsRequest params = new QuittancerDeclarationsRequest();
		params.setLogin(login);

		// Le déclaration a déjà été sommée, cependant une demande de quittancement devrait quand même être traitée avec succès
		final DemandeQuittancementDeclaration demande = newDemande(newDate(2009, 4, 1), 38005401L, 1, 2008);
		params.getDemandes().add(demande);

		final QuittancerDeclarationsResponse array = service.quittancerDeclarations(params);
		assertNotNull(array);

		final List<ReponseQuittancementDeclaration> reponses = array.getItem();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final ReponseQuittancementDeclaration reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(CodeQuittancement.OK, reponse.getCode());
		assertNull(reponse.getExceptionInfo());
		assertQuittancement(newDate(2009, 4, 1), 38005401, 2008, 1);
	}

	@Test
	public void testQuittancerPlusieursDeclarations() throws Exception {

		// on force le rechargement du fichier dbunits, parce des déclarations ont déjà été sommées par les tests précédents
		loadDatabase(DB_UNIT_DATA_FILE);

		QuittancerDeclarationsRequest params = new QuittancerDeclarationsRequest();
		params.setLogin(login);

		// Le déclaration a été annulée après l'envoi, demander un quittancement ne devrait donc pas fonctionner
		final DemandeQuittancementDeclaration demande0 = newDemande(newDate(2009, 4, 1), 38005301L, 1, 2008);
		params.getDemandes().add(demande0);

		// Le contribuable possède une seule déclaration, émise, non-sommée, non-annulée, ..., le cas standard quoi.
		final DemandeQuittancementDeclaration demande1 = newDemande(newDate(2009, 4, 1), 28014710L, 1, 2008);
		params.getDemandes().add(demande1);

		// Le déclaration a déjà été sommée, cependant une demande de quittancement devrait quand même être traitée avec succès
		final DemandeQuittancementDeclaration demande2 = newDemande(newDate(2009, 4, 1), 38005401L, 1, 2008);
		params.getDemandes().add(demande2);

		final QuittancerDeclarationsResponse array = service.quittancerDeclarations(params);
		assertNotNull(array);

		final List<ReponseQuittancementDeclaration> reponses = array.getItem();
		assertNotNull(reponses);
		assertEquals(3, reponses.size());

		final ReponseQuittancementDeclaration reponse0 = reponses.get(0);
		assertNotNull(reponse0);
		assertEquals(CodeQuittancement.ERREUR_DECLARATION_ANNULEE, reponse0.getCode());

		final WebServiceExceptionInfo exceptionInfo0 = reponse0.getExceptionInfo();
		assertNotNull(exceptionInfo0);
		assertTrue(exceptionInfo0 instanceof BusinessExceptionInfo);
		assertEquals("La déclaration a été annulée entre-temps.", exceptionInfo0.getMessage());

		final ReponseQuittancementDeclaration reponse1 = reponses.get(1);
		assertNotNull(reponse1);
		assertEquals(CodeQuittancement.OK, reponse1.getCode());
		assertNull(reponse1.getExceptionInfo());
		assertQuittancement(newDate(2009, 4, 1), 28014710L, 2008, 1);

		final ReponseQuittancementDeclaration reponse2 = reponses.get(2);
		assertNotNull(reponse2);
		assertEquals(CodeQuittancement.OK, reponse2.getCode());
		assertNull(reponse2.getExceptionInfo());
		assertQuittancement(newDate(2009, 4, 1), 38005401, 2008, 1);
	}

	/**
	 * Asserte que la déclaration du contribuable spécifié, pour la période fiscale spécifiée est bien quittancée à la date spécifiée.
	 *
	 * @param dateObtention    la date d'obtention (= de retour) de l'état RETOURNEE de la déclaration
	 * @param ctbID            un numéro de contribuable
	 * @param periodeFiscale   un période fiscale
	 * @param numeroSequenceDI le numéro de séquence de la déclaration
	 * @throws Exception en cas d'exception
	 */
	private void assertQuittancement(Date dateObtention, long ctbID, int periodeFiscale, int numeroSequenceDI) throws Exception {

		GetTiersRequest params = new GetTiersRequest();
		params.setLogin(login);
		params.setTiersNumber(ctbID);
		params.getParts().add(TiersPart.DECLARATIONS);
		params.getParts().add(TiersPart.ETATS_DECLARATIONS);

		final Tiers tiers = service.getTiers(params);
		assertNotNull(tiers);
		final List<Declaration> declarations = tiers.getDeclarations();
		assertNotNull(declarations);

		DeclarationImpotOrdinaire declaration = null;
		for (Declaration d : declarations) {
			final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
			if (di.getPeriodeFiscale().getAnnee() == periodeFiscale && di.getNumero() == numeroSequenceDI) {
				declaration = di;
				break;
			}
		}
		assertNotNull(declaration);
		final List<EtatDeclaration> etats = declaration.getEtats();
		assertNotNull(etats);

		EtatDeclaration etat = null;
		for (EtatDeclaration e : etats) {
			if (e.getDateAnnulation() == null && e.getEtat() == TypeEtatDeclaration.RETOURNEE) {
				etat = e;
				break;
			}
		}
		assertNotNull(etat);
		assertSameDay(dateObtention, etat.getDateObtention());
	}

	private static DemandeQuittancementDeclaration newDemande(Date dateRetour, long ctbId, int numeroSequenceDI, int periodeFiscale) {

		final DemandeQuittancementDeclaration demande = new DemandeQuittancementDeclaration();
		demande.setDateRetour(dateRetour);

		final DeclarationImpotOrdinaireKey di0 = new DeclarationImpotOrdinaireKey();
		di0.setCtbId(ctbId);
		di0.setNumeroSequenceDI(numeroSequenceDI);
		di0.setPeriodeFiscale(periodeFiscale);
		demande.setKey(di0);

		return demande;
	}
}