package ch.vd.unireg.data;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.registre.base.date.InstantHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.xml.event.data.v1.DataEvent;
import ch.vd.unireg.xml.event.data.v1.Events;
import ch.vd.unireg.xml.event.data.v1.ObjectFactory;

public class DataEventSenderImpl implements DataEventSender, InitializingBean {

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private String serviceDestination;
	private String businessUser;

	private final ObjectFactory objectFactory = new ObjectFactory();
	private JAXBContext jaxbContext;

	public void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	public void setBusinessUser(String businessUser) {
		this.businessUser = businessUser;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void sendDataEvent(List<DataEvent> batch) throws Exception {
		final Marshaller marshaller = jaxbContext.createMarshaller();
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.newDocument();
		marshaller.marshal(objectFactory.createEvents(new Events(batch)), doc);

		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessId(String.format("DataEvent-%d-%d", InstantHelper.get().getEpochSecond(), ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)));
		m.setBusinessUser(businessUser);
		m.setServiceDestination(serviceDestination);
		m.setContext("dataEvent");
		m.setBody(doc);

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		esbTemplate.sendInternal(m); // [UNIREG-3242] utilisation d'une queue interne

		// Note : code pour unmarshaller un événement
		//		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		//		Unmarshaller u = context.createUnmarshaller();
		//		SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		//		Schema schema = sf.newSchema(new File("mon_beau_xsd.xsd"));
		//		u.setSchema(schema);
		//		JAXBElement element = (JAXBElement) u.unmarshal(message);
		//		evenement = element == null ? null : (EvenementDeclarationImpot) element.getValue();
	}
}
