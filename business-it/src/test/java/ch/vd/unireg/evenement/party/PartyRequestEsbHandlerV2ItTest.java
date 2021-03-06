package ch.vd.unireg.evenement.party;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.event.party.v2.ExceptionResponse;
import ch.vd.unireg.xml.event.party.v2.ObjectFactory;
import ch.vd.unireg.xml.event.party.v2.Request;
import ch.vd.unireg.xml.event.party.v2.Response;

/**
 * Factorise le code commun pour les autres classes concrètes du package
 */
@SuppressWarnings({"JavaDoc"})
abstract class PartyRequestEsbHandlerV2ItTest extends PartyRequestEsbHandlerItTest {

	protected static String requestToString(Request request) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		marshaller.marshal(new ObjectFactory().createRequest(request), out);
		return out.toString();
	}

	protected Response parseResponse(EsbMessage message) throws Exception {

		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		final Schema schema = sf.newSchema(XmlUtils.toSourcesArray(xsdPathes));
		u.setSchema(schema);

		final JAXBElement element = (JAXBElement) u.unmarshal(message.getBodyAsSource());
		final Object value = element.getValue();
		if (value instanceof ExceptionResponse) {
			throw new ServiceException(((ExceptionResponse) value).getExceptionInfo());
		}
		return (Response) value;
	}
}
