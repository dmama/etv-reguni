package ch.vd.unireg.evenement.identification.contribuable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.event.identification.request.v2.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.request.v2.ObjectFactory;
import ch.vd.unireg.xml.event.identification.response.v2.IdentificationContribuableResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class IdentificationContribuableRequestV2ListenerItTest extends IdentificationContribuableRequestListenerItTest {

	private static String requestToString(IdentificationContribuableRequest request) {
		try {
			JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			Marshaller marshaller = context.createMarshaller();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			marshaller.marshal(new ObjectFactory().createIdentificationContribuableRequest(request), out);
			return out.toString();
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	@NotNull
	@Override
	protected String getHandlerName() {
		return "identificationContribuableRequestHandlerV2";
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiqueOK() throws Exception {

		final RegDate dateNaissance = RegDate.get(1982, 6);     // date partielle, pour le faire au moins une fois

		final long id = doInNewTransaction(status -> {
			final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier Vallard", dateNaissance, Sexe.MASCULIN);
			return christophe.getNumero();
		});
		globalTiersIndexer.sync();

		final IdentificationContribuableRequest request = new IdentificationContribuableRequest();
		request.setNAVS13(7569396525489L);
		request.setNom("Monnier");
		request.setPrenoms("Christophe");

		// Envoie le message
		doInNewTransaction(status -> {
			sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
			return null;
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

		final Long id1 = doInNewTransaction(status -> {
			final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier", date(1982, 6, 29), Sexe.MASCULIN);
			return christophe.getNumero();
		});
		final Long id2 = doInNewTransaction(status -> {
			final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier", date(1964, 6, 29), Sexe.MASCULIN);
			return christophe.getNumero();
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

		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier", date(1982, 6, 29), Sexe.MASCULIN);
			return christophe.getNumero();
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
		final Schema schema = sf.newSchema(XmlUtils.toSourcesArray(xsdPathes));
		u.setSchema(schema);

		final JAXBElement element = (JAXBElement) u.unmarshal(message.getBodyAsSource());
		final IdentificationContribuableResponse reponse = (IdentificationContribuableResponse)element.getValue();
		return reponse;
	}
}
