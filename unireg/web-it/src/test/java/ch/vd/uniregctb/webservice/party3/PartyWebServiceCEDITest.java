package ch.vd.uniregctb.webservice.party3;

import java.util.List;

import org.junit.Test;

import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationRequest;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationResponse;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsRequest;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsResponse;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.OrdinaryTaxDeclarationKey;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.TaxDeclarationAcknowledgeCode;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.ServiceExceptionInfo;
import ch.vd.unireg.xml.party.taxdeclaration.v1.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.v1.Party;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test d'intégration des fonctions du web-service Tiers v2 utilisées par le CEDI.
 */
@SuppressWarnings({"JavaDoc"})
public class PartyWebServiceCEDITest extends AbstractPartyWebServiceTest {

	// private static final Logger LOGGER = LoggerFactory.getLogger(PartyWebServiceTest.class);

	private static final String DB_UNIT_DATA_FILE = "PartyWebServiceCEDITest.xml";

	private UserLogin login;

	private static boolean alreadySetUp = false;

	public PartyWebServiceCEDITest() throws Exception {
	}

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}

		login = new UserLogin();
		login.setUserId("[UT] PartyWebServiceCEDITest");
		login.setOid(22);
	}

	@Test
	public void testQuittancerDeclarationContribuableInconnu() throws Exception {

		AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
		params.setLogin(login);

		// Le contribuable est inconnu -> erreur
		final AcknowledgeTaxDeclarationRequest demande = newDemande(newDate(2009, 4, 1), 90909090L, 1, 2008);
		params.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse array = service.acknowledgeTaxDeclarations(params);
		assertNotNull(array);

		final List<AcknowledgeTaxDeclarationResponse> reponses = array.getResponses();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final AcknowledgeTaxDeclarationResponse reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(TaxDeclarationAcknowledgeCode.ERROR_UNKNOWN_TAXPAYER, reponse.getCode());

		final ServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("Le contribuable est inconnu.", exceptionInfo.getMessage());
	}

	@Test
	public void testQuittancerDeclarationContribuableSansForFiscal() throws Exception {

		AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
		params.setLogin(login);

		// Le contribuable ne possède pas de for fiscal -> erreur
		final AcknowledgeTaxDeclarationRequest demande = newDemande(newDate(2009, 4, 1), 10501047L, 1, 2008);
		params.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse array = service.acknowledgeTaxDeclarations(params);
		assertNotNull(array);

		final List<AcknowledgeTaxDeclarationResponse> reponses = array.getResponses();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final AcknowledgeTaxDeclarationResponse reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(TaxDeclarationAcknowledgeCode.ERROR_TAX_LIABILITY, reponse.getCode());

		final ServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("Le contribuable ne possède aucun for principal : il n'aurait pas dû recevoir de déclaration d'impôt.", exceptionInfo.getMessage());
	}

	@Test
	public void testQuittancerDeclarationContribuableDebiteurInactif() throws Exception {

		AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
		params.setLogin(login);

		// Le contribuable est un débiteur inactif -> erreur
		final AcknowledgeTaxDeclarationRequest demande = newDemande(newDate(2009, 4, 1), 10582592L, 1, 2008);
		params.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse array = service.acknowledgeTaxDeclarations(params);
		assertNotNull(array);

		final List<AcknowledgeTaxDeclarationResponse> reponses = array.getResponses();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final AcknowledgeTaxDeclarationResponse reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(TaxDeclarationAcknowledgeCode.ERROR_INACTIVE_DEBTOR, reponse.getCode());

		final ServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("Le contribuable est un débiteur inactif : impossible de quittancer la déclaration.", exceptionInfo.getMessage());
	}

	@Test
	public void testQuittancerDeclarationDeclarationInexistante() throws Exception {

		AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
		params.setLogin(login);

		// Le tiers ne possède qu'une seule déclaration en 2008, demander la quatrième ne devrait donc pas fonctionner
		final AcknowledgeTaxDeclarationRequest demande = newDemande(newDate(2009, 4, 1), 28014710L, 4, 2008);
		params.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse array = service.acknowledgeTaxDeclarations(params);
		assertNotNull(array);

		final List<AcknowledgeTaxDeclarationResponse> reponses = array.getResponses();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final AcknowledgeTaxDeclarationResponse reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(TaxDeclarationAcknowledgeCode.ERROR_UNKNOWN_TAX_DECLARATION, reponse.getCode());

		final ServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("La déclaration n'existe pas.", exceptionInfo.getMessage());
	}


	@Test
	public void testQuittancerDeclarationAvantDateEnvoi() throws Exception {

		AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
		params.setLogin(login);

		// Le déclaration a été envoyée le 28 janvier 2009, demander un quittancement au 1er janvier 2009 ne devrait donc pas fonctionner
		final AcknowledgeTaxDeclarationRequest demande = newDemande(newDate(2009, 1, 1), 28014710L, 1, 2008);
		params.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse array = service.acknowledgeTaxDeclarations(params);
		assertNotNull(array);

		final List<AcknowledgeTaxDeclarationResponse> reponses = array.getResponses();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final AcknowledgeTaxDeclarationResponse reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(TaxDeclarationAcknowledgeCode.ERROR_INVALID_ACKNOWLEDGE_DATE, reponse.getCode());

		final ServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("La date de retour spécifiée (2009.01.01) est avant la date d'envoi de la déclaration (2009.01.28).", exceptionInfo.getMessage());
	}

	@Test
	public void testQuittancerDeclarationAnnulee() throws Exception {

		AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
		params.setLogin(login);

		// Le déclaration a été annulée après l'envoi, demander un quittancement ne devrait donc pas fonctionner
		final AcknowledgeTaxDeclarationRequest demande = newDemande(newDate(2009, 4, 1), 38005301L, 1, 2008);
		params.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse array = service.acknowledgeTaxDeclarations(params);
		assertNotNull(array);

		final List<AcknowledgeTaxDeclarationResponse> reponses = array.getResponses();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final AcknowledgeTaxDeclarationResponse reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(TaxDeclarationAcknowledgeCode.ERROR_CANCELLED_TAX_DECLARATION, reponse.getCode());

		final ServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
		assertNotNull(exceptionInfo);
		assertTrue(exceptionInfo instanceof BusinessExceptionInfo);
		assertEquals("La déclaration a été annulée entre-temps.", exceptionInfo.getMessage());
	}

	@Test
	public void testQuittancerDeclaration() throws Exception {

		AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
		params.setLogin(login);

		// Le contribuable possède une seule déclaration, émise, non-sommée, non-annulée, ..., le cas standard quoi.
		final AcknowledgeTaxDeclarationRequest demande = newDemande(newDate(2009, 4, 1), 28014710L, 1, 2008);
		params.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse array = service.acknowledgeTaxDeclarations(params);
		assertNotNull(array);

		final List<AcknowledgeTaxDeclarationResponse> reponses = array.getResponses();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final AcknowledgeTaxDeclarationResponse reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(TaxDeclarationAcknowledgeCode.OK, reponse.getCode());
		assertNull(reponse.getExceptionInfo());
		assertQuittancement(newDate(2009, 4, 1), 28014710L, 2008, 1);
	}

	@Test
	public void testQuittancerDeclarationDejaSommee() throws Exception {

		AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
		params.setLogin(login);

		// Le déclaration a déjà été sommée, cependant une demande de quittancement devrait quand même être traitée avec succès
		final AcknowledgeTaxDeclarationRequest demande = newDemande(newDate(2009, 4, 1), 38005401L, 1, 2008);
		params.getRequests().add(demande);

		final AcknowledgeTaxDeclarationsResponse array = service.acknowledgeTaxDeclarations(params);
		assertNotNull(array);

		final List<AcknowledgeTaxDeclarationResponse> reponses = array.getResponses();
		assertNotNull(reponses);
		assertEquals(1, reponses.size());

		final AcknowledgeTaxDeclarationResponse reponse = reponses.get(0);
		assertNotNull(reponse);
		assertEquals(TaxDeclarationAcknowledgeCode.OK, reponse.getCode());
		assertNull(reponse.getExceptionInfo());
		assertQuittancement(newDate(2009, 4, 1), 38005401, 2008, 1);
	}

	@Test
	public void testQuittancerPlusieursDeclarations() throws Exception {

		// on force le rechargement du fichier dbunits, parce des déclarations ont déjà été sommées par les tests précédents
		loadDatabase(DB_UNIT_DATA_FILE);

		AcknowledgeTaxDeclarationsRequest params = new AcknowledgeTaxDeclarationsRequest();
		params.setLogin(login);

		// Le déclaration a été annulée après l'envoi, demander un quittancement ne devrait donc pas fonctionner
		final AcknowledgeTaxDeclarationRequest demande0 = newDemande(newDate(2009, 4, 1), 38005301L, 1, 2008);
		params.getRequests().add(demande0);

		// Le contribuable possède une seule déclaration, émise, non-sommée, non-annulée, ..., le cas standard quoi.
		final AcknowledgeTaxDeclarationRequest demande1 = newDemande(newDate(2009, 4, 1), 28014710L, 1, 2008);
		params.getRequests().add(demande1);

		// Le déclaration a déjà été sommée, cependant une demande de quittancement devrait quand même être traitée avec succès
		final AcknowledgeTaxDeclarationRequest demande2 = newDemande(newDate(2009, 4, 1), 38005401L, 1, 2008);
		params.getRequests().add(demande2);

		final AcknowledgeTaxDeclarationsResponse array = service.acknowledgeTaxDeclarations(params);
		assertNotNull(array);

		final List<AcknowledgeTaxDeclarationResponse> reponses = array.getResponses();
		assertNotNull(reponses);
		assertEquals(3, reponses.size());

		final AcknowledgeTaxDeclarationResponse reponse0 = reponses.get(0);
		assertNotNull(reponse0);
		assertEquals(TaxDeclarationAcknowledgeCode.ERROR_CANCELLED_TAX_DECLARATION, reponse0.getCode());

		final ServiceExceptionInfo exceptionInfo0 = reponse0.getExceptionInfo();
		assertNotNull(exceptionInfo0);
		assertTrue(exceptionInfo0 instanceof BusinessExceptionInfo);
		assertEquals("La déclaration a été annulée entre-temps.", exceptionInfo0.getMessage());

		final AcknowledgeTaxDeclarationResponse reponse1 = reponses.get(1);
		assertNotNull(reponse1);
		assertEquals(TaxDeclarationAcknowledgeCode.OK, reponse1.getCode());
		assertNull(reponse1.getExceptionInfo());
		assertQuittancement(newDate(2009, 4, 1), 28014710L, 2008, 1);

		final AcknowledgeTaxDeclarationResponse reponse2 = reponses.get(2);
		assertNotNull(reponse2);
		assertEquals(TaxDeclarationAcknowledgeCode.OK, reponse2.getCode());
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

		GetPartyRequest params = new GetPartyRequest();
		params.setLogin(login);
		params.setPartyNumber((int) ctbID);
		params.getParts().add(PartyPart.TAX_DECLARATIONS);
		params.getParts().add(PartyPart.TAX_DECLARATIONS_STATUSES);

		final Party tiers = service.getParty(params);
		assertNotNull(tiers);
		final List<TaxDeclaration> declarations = tiers.getTaxDeclarations();
		assertNotNull(declarations);

		OrdinaryTaxDeclaration declaration = null;
		for (TaxDeclaration d : declarations) {
			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) d;
			if (di.getTaxPeriod().getYear() == periodeFiscale && di.getSequenceNumber() == numeroSequenceDI) {
				declaration = di;
				break;
			}
		}
		assertNotNull(declaration);
		final List<TaxDeclarationStatus> etats = declaration.getStatuses();
		assertNotNull(etats);

		TaxDeclarationStatus etat = null;
		for (TaxDeclarationStatus e : etats) {
			if (e.getCancellationDate() == null && e.getType() == TaxDeclarationStatusType.RETURNED) {
				etat = e;
				break;
			}
		}
		assertNotNull(etat);
		assertSameDay(dateObtention, etat.getDateFrom());
	}

	private static AcknowledgeTaxDeclarationRequest newDemande(Date dateRetour, long ctbId, int numeroSequenceDI, int periodeFiscale) {

		final AcknowledgeTaxDeclarationRequest demande = new AcknowledgeTaxDeclarationRequest();
		demande.setAcknowledgeDate(dateRetour);

		final OrdinaryTaxDeclarationKey di0 = new OrdinaryTaxDeclarationKey();
		di0.setTaxpayerNumber((int) ctbId);
		di0.setSequenceNumber(numeroSequenceDI);
		di0.setTaxPeriod(periodeFiscale);
		demande.setKey(di0);

		return demande;
	}
}