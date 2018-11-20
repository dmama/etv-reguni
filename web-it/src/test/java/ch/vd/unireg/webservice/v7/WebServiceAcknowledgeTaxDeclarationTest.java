package ch.vd.unireg.webservice.v7;

import javax.xml.bind.JAXBContext;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.unireg.ws.ack.v7.AckStatus;
import ch.vd.unireg.ws.ack.v7.ObjectFactory;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResult;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationKey;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyPart;

import static ch.vd.unireg.webservice.v7.WebServiceGetPartyItTest.buildUriAndParams;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class WebServiceAcknowledgeTaxDeclarationTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceAcknowledgeTaxDeclarationTest.xml";
	public static final String visaUser = "zaixxx";

	private UserLogin login;

	private static boolean alreadySetUp = false;
	private JAXBContext jaxbContext;

	public WebServiceAcknowledgeTaxDeclarationTest() throws Exception {
	}

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}

		login = new UserLogin();
		login.setUserId(visaUser);
		login.setOid(22);

		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Test
	public void testQuittancerDeclarationContribuableInconnu() throws Exception {

		// Le contribuable est inconnu -> erreur
		final ResponseEntity<OrdinaryTaxDeclarationAckResponse> response = quittancerDeclaration(new Date(2009, 4, 1), new TaxDeclarationKey(90909090, 2008, 1));
		assertNotNull(response);

		final List<OrdinaryTaxDeclarationAckResult> results = response.getBody().getAckResult();
		assertNotNull(results);
		assertEquals(1, results.size());

		final OrdinaryTaxDeclarationAckResult result0 = results.get(0);
		assertNotNull(result0);
		assertEquals(AckStatus.ERROR_UNKNOWN_PARTY, result0.getStatus());
		assertEquals("Le tiers n°909.090.90 n'existe pas", result0.getAdditionalMessage());
	}

	@Test
	public void testQuittancerDeclarationContribuableSansForFiscal() throws Exception {

		// Le contribuable ne possède pas de for fiscal -> erreur
		final ResponseEntity<OrdinaryTaxDeclarationAckResponse> response = quittancerDeclaration(new Date(2009, 4, 1), new TaxDeclarationKey(10501047, 2008, 1));
		assertNotNull(response);

		final List<OrdinaryTaxDeclarationAckResult> results = response.getBody().getAckResult();
		assertNotNull(results);
		assertEquals(2, results.size());

		final OrdinaryTaxDeclarationAckResult result0 = results.get(0);
		assertNotNull(result0);
		assertEquals(AckStatus.ERROR_TAX_LIABILITY, result0.getStatus());
		assertEquals("Aucun for principal.", result0.getAdditionalMessage());

		final OrdinaryTaxDeclarationAckResult result1 = results.get(1);
		assertNotNull(result1);
		assertEquals(AckStatus.ERROR_UNKNOWN_TAX_DECLARATION, result1.getStatus());
		assertNull(result1.getAdditionalMessage());
	}

	@Test
	public void testQuittancerDeclarationContribuableDebiteurInactif() throws Exception {

		// Le contribuable est un débiteur inactif -> erreur
		final ResponseEntity<OrdinaryTaxDeclarationAckResponse> response = quittancerDeclaration(new Date(2009, 4, 1), new TaxDeclarationKey(10582592, 2008, 1));
		assertNotNull(response);

		final List<OrdinaryTaxDeclarationAckResult> results = response.getBody().getAckResult();
		assertNotNull(results);
		assertEquals(1, results.size());

		final OrdinaryTaxDeclarationAckResult result0 = results.get(0);
		assertNotNull(result0);
		assertEquals(AckStatus.ERROR_INACTIVE_DEBTOR, result0.getStatus());
		assertNull(result0.getAdditionalMessage());
	}

	@Test
	public void testQuittancerDeclarationAvantDateEnvoi() throws Exception {

		// Le déclaration a été envoyée le 28 janvier 2009, demander un quittancement au 1er janvier 2009 ne devrait donc pas fonctionner
		final ResponseEntity<OrdinaryTaxDeclarationAckResponse> response = quittancerDeclaration(new Date(2009, 1, 1), new TaxDeclarationKey(28014710, 2008, 1));
		assertNotNull(response);

		final List<OrdinaryTaxDeclarationAckResult> results = response.getBody().getAckResult();
		assertNotNull(results);
		assertEquals(1, results.size());

		final OrdinaryTaxDeclarationAckResult result0 = results.get(0);
		assertNotNull(result0);
		assertEquals(AckStatus.ERROR_INVALID_ACK_DATE, result0.getStatus());
		assertEquals("Date donnée antérieure à la date d'émission de la déclaration.", result0.getAdditionalMessage());
	}

	//
	@Test
	public void testQuittancerDeclarationAnnulee() throws Exception {

		// Le déclaration a été annulée après l'envoi, demander un quittancement ne devrait donc pas fonctionner
		final ResponseEntity<OrdinaryTaxDeclarationAckResponse> response = quittancerDeclaration(new Date(2009, 4, 1), new TaxDeclarationKey(38005301, 2008, 1));
		assertNotNull(response);

		final List<OrdinaryTaxDeclarationAckResult> results = response.getBody().getAckResult();
		assertNotNull(results);
		assertEquals(1, results.size());

		final OrdinaryTaxDeclarationAckResult result0 = results.get(0);
		assertNotNull(result0);
		assertEquals(AckStatus.ERROR_CANCELED_TAX_DECLARATION, result0.getStatus());
		assertNull(result0.getAdditionalMessage());
	}
	@Test
	public void testQuittancerDeclaration() throws Exception {

		// Le contribuable possède une seule déclaration, émise, non-sommée, non-annulée, ..., le cas standard quoi.
		final ResponseEntity<OrdinaryTaxDeclarationAckResponse> response = quittancerDeclaration(new Date(2009, 4, 1), new TaxDeclarationKey(28014710, 2008, 1));
		assertNotNull(response);

		final List<OrdinaryTaxDeclarationAckResult> results = response.getBody().getAckResult();
		assertNotNull(results);
		assertEquals(1, results.size());

		// l'appel est bien passé
		final OrdinaryTaxDeclarationAckResult result0 = results.get(0);
		assertNotNull(result0);
		assertEquals(AckStatus.OK, result0.getStatus());
		assertNull(result0.getAdditionalMessage());

		// la déclaration est bien retournée
		assertQuittancement(new Date(2009, 4, 1), 28014710, 2008, 1);
	}

	@Test
	public void testQuittancerDeclarationDejaSommee() throws Exception {

		// Le déclaration a déjà été sommée, cependant une demande de quittancement devrait quand même être traitée avec succès
		final ResponseEntity<OrdinaryTaxDeclarationAckResponse> response = quittancerDeclaration(new Date(2009, 4, 1), new TaxDeclarationKey(38005401, 2008, 1));
		assertNotNull(response);

		final List<OrdinaryTaxDeclarationAckResult> results = response.getBody().getAckResult();
		assertNotNull(results);
		assertEquals(1, results.size());

		// l'appel est bien passé
		final OrdinaryTaxDeclarationAckResult result0 = results.get(0);
		assertNotNull(result0);
		assertEquals(AckStatus.OK, result0.getStatus());
		assertNull(result0.getAdditionalMessage());
		assertQuittancement(new Date(2009, 4, 1), 38005401, 2008, 1);
	}

	@Test
	public void testQuittancerPlusieursDeclarations() throws Exception {

		// on force le rechargement du fichier dbunits, parce des déclarations ont déjà été sommées par les tests précédents
		loadDatabase(DB_UNIT_DATA_FILE);

		final ResponseEntity<OrdinaryTaxDeclarationAckResponse> response = quittancerDeclaration(new Date(2009, 4, 1),
		                                                                                         // Le déclaration a été annulée après l'envoi, demander un quittancement ne devrait donc pas fonctionner
		                                                                                         new TaxDeclarationKey(38005301, 2008, 1),
		                                                                                         // Le contribuable possède une seule déclaration, émise, non-sommée, non-annulée, ..., le cas standard quoi.
		                                                                                         new TaxDeclarationKey(28014710, 2008, 1),
		                                                                                         // Le déclaration a déjà été sommée, cependant une demande de quittancement devrait quand même être traitée avec succès
		                                                                                         new TaxDeclarationKey(38005401, 2008, 1)
		);
		assertNotNull(response);

		final List<OrdinaryTaxDeclarationAckResult> results = response.getBody().getAckResult();
		assertNotNull(results);
		assertEquals(3, results.size());

		final OrdinaryTaxDeclarationAckResult result0 = results.get(0);
		assertNotNull(result0);
		assertEquals(AckStatus.ERROR_CANCELED_TAX_DECLARATION, result0.getStatus());
		assertNull(result0.getAdditionalMessage());

		final OrdinaryTaxDeclarationAckResult result1 = results.get(1);
		assertNotNull(result1);
		assertEquals(AckStatus.OK, result1.getStatus());
		assertNull(result1.getAdditionalMessage());
		assertQuittancement(new Date(2009, 4, 1), 28014710, 2008, 1);

		final OrdinaryTaxDeclarationAckResult result2 = results.get(2);
		assertNotNull(result2);
		assertEquals(AckStatus.OK, result2.getStatus());
		assertNull(result2.getAdditionalMessage());
		assertQuittancement(new Date(2009, 4, 1), 38005401, 2008, 1);
	}

	@Test
	public void testQuittancerDeclarationDeclarationInexistante() throws Exception {

		// Le tiers ne possède qu'une seule déclaration en 2008, demander la quatrième ne devrait donc pas fonctionner
		final ResponseEntity<OrdinaryTaxDeclarationAckResponse> response = quittancerDeclaration(new Date(2009, 4, 1), new TaxDeclarationKey(28014710, 2008, 4));
		assertNotNull(response);

		final List<OrdinaryTaxDeclarationAckResult> results = response.getBody().getAckResult();
		assertNotNull(results);
		assertEquals(1, results.size());

		final OrdinaryTaxDeclarationAckResult result0 = results.get(0);
		assertNotNull(result0);
		assertEquals(AckStatus.ERROR_UNKNOWN_TAX_DECLARATION, result0.getStatus());
		assertNull(result0.getAdditionalMessage());
	}

	private ResponseEntity<OrdinaryTaxDeclarationAckResponse> quittancerDeclaration(Date dateQuittance, TaxDeclarationKey... declarationKey) {

		final OrdinaryTaxDeclarationAckRequest request = new OrdinaryTaxDeclarationAckRequest();
		request.setSource("CEDI");
		request.setDate(dateQuittance);
		Arrays.stream(declarationKey).forEach(key -> request.getDeclaration().add(key));

		final HashMap<String, Object> params = new HashMap<>();
		params.put("user", login.getUserId());
		params.put("oid", login.getOid());

		return post(OrdinaryTaxDeclarationAckResponse.class,
		            "/ackOrdinaryTaxDeclarations?user={user}/{oid}",
		            params,
		            new ObjectFactory().createOrdinaryTaxDeclarationAckRequest(request),
		            MediaType.APPLICATION_XML,
		            jaxbContext);
	}

	private void assertQuittancement(Date date, int partyNo, int year, int sequenceNumber) {
		final Pair<String, Map<String, ?>> params = buildUriAndParams(partyNo, EnumSet.of(PartyPart.TAX_DECLARATIONS_STATUSES));
		final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
		assertNotNull(resp);

		final Party party = resp.getBody();
		final TaxDeclaration declaration2008 = party.getTaxDeclarations().stream()
				.filter(t -> t.getTaxPeriod().getYear() == year)
				.filter(OrdinaryTaxDeclaration.class::isInstance)
				.map(OrdinaryTaxDeclaration.class::cast)
				.filter(t -> t.getSequenceNumber() == sequenceNumber)
				.findFirst()
				.orElse(null);
		assertNotNull(declaration2008);

		final TaxDeclarationStatus returnedStatus2008 = declaration2008.getStatuses().stream()
				.filter(s -> s.getType() == TaxDeclarationStatusType.RETURNED)
				.findFirst()
				.orElse(null);
		assertNotNull(returnedStatus2008);
		assertEquals(date, returnedStatus2008.getDateFrom());
	}
}