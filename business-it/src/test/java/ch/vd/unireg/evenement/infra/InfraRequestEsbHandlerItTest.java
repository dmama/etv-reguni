package ch.vd.unireg.evenement.infra;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.xml.sax.SAXException;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.evenement.party.PartyRequestEsbHandlerItTest;
import ch.vd.unireg.xml.event.infra.v1.ObjectFactory;
import ch.vd.unireg.xml.event.infra.v1.Request;
import ch.vd.unireg.xml.event.infra.v1.Response;

abstract class InfraRequestEsbHandlerItTest extends PartyRequestEsbHandlerItTest {

	protected static String requestToString(Request request) {
		ByteArrayOutputStream out;
		try {
			JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			Marshaller marshaller = context.createMarshaller();
			out = new ByteArrayOutputStream();
			marshaller.marshal(new ObjectFactory().createRequest(request), out);
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		return out.toString();
	}

	protected Response parseResponse(EsbMessage message) {

		final JAXBElement element;
		try {
			final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			final Unmarshaller u = context.createUnmarshaller();
			final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			final Schema schema = sf.newSchema(XmlUtils.toSourcesArray(xsdPathes));
			u.setSchema(schema);

			element = (JAXBElement) u.unmarshal(message.getBodyAsSource());
		}
		catch (JAXBException | SAXException | IOException e) {
			throw new RuntimeException(e);
		}
		final Object value = element.getValue();
		return (Response) value;
	}

}
