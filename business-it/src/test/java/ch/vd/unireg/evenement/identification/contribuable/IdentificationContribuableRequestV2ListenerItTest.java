package ch.vd.unireg.evenement.identification.contribuable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.xml.event.identification.request.v2.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.request.v2.ObjectFactory;
import ch.vd.unireg.xml.event.identification.response.v2.IdentificationContribuableResponse;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class IdentificationContribuableRequestV2ListenerItTest extends IdentificationContribuableRequestListenerItTest {

	private static String requestToString(IdentificationContribuableRequest request) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		marshaller.marshal(new ObjectFactory().createIdentificationContribuableRequest(request), out);
		return out.toString();
	}

	@Override
	protected String getRequestXSD() {
		return "event/identification/identification-contribuable-request-2.xsd";
	}

	@Override
	protected String getResponseXSD() {
		return "event/identification/identification-contribuable-response-2.xsd";
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiqueOK() throws Exception {

		final RegDate dateNaissance = RegDate.get(1982, 6);     // date partielle, pour le faire au moins une fois

		final long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier Vallard", dateNaissance, Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});
		globalTiersIndexer.sync();

		final IdentificationContribuableRequest request = new IdentificationContribuableRequest();
		request.setNAVS13(7569396525489L);
		request.setNom("Monnier");
		request.setPrenoms("Christophe");

		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);
		final IdentificationContribuableResponse response = parseResponse(message);
		if (response.getErreur() != null) {
			fail(response.getErreur().toString());
		}

		final IdentificationContribuableResponse.Contribuable infoCtb = response.getContribuable();
		assertNotNull(infoCtb);
		assertEquals(id, infoCtb.getNumeroContribuableIndividuel());
		assertEquals("Monnier Vallard", infoCtb.getNom());
		assertEquals("Christophe", infoCtb.getPrenom());
		assertEquals(dateNaissance, DataHelper.xmlToCore(infoCtb.getDateNaissance()));
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiquePlusieurs() throws Exception {

		final Long id1 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe","Monnier",date(1982,6,29), Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});
		final Long id2 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe","Monnier",date(1964,6,29), Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});

		globalTiersIndexer.sync();

		final IdentificationContribuableRequest request = new IdentificationContribuableRequest();
		request.setNom("Monnier");
		request.setPrenoms("Christophe");

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);
		IdentificationContribuableResponse response = parseResponse(message);
		assertNotNull(response.getErreur());
		assertNull(response.getContribuable());
		assertNotNull(response.getErreur().getPlusieurs());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiqueAucun() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe","Monnier",date(1982,6,29), Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});

		globalTiersIndexer.sync();

		final IdentificationContribuableRequest request = new IdentificationContribuableRequest();
		request.setNom("Adam");
		request.setPrenoms("Raphaël");

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);
		IdentificationContribuableResponse response = parseResponse(message);
		assertNotNull(response.getErreur());
		assertNull(response.getContribuable());
		assertNotNull(response.getErreur().getAucun());
	}

	private IdentificationContribuableResponse parseResponse(EsbMessage message) throws Exception {
		final JAXBContext context = JAXBContext.newInstance(ch.vd.unireg.xml.event.identification.response.v2.ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(new ClasspathCatalogResolver());
		Schema schema = sf.newSchema(
				new Source[]{new StreamSource(new ClassPathResource(getRequestXSD()).getURL().toExternalForm()),
						new StreamSource(new ClassPathResource(getResponseXSD()).getURL().toExternalForm())});
		u.setSchema(schema);

		final JAXBElement element = (JAXBElement) u.unmarshal(message.getBodyAsSource());
		final IdentificationContribuableResponse reponse = (IdentificationContribuableResponse)element.getValue();
		return reponse;
	}
}
