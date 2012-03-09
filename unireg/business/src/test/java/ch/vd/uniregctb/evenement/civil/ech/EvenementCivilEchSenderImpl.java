package ch.vd.uniregctb.evenement.civil.ech;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;

import ch.vd.evd0006.v1.EventIdentification;
import ch.vd.evd0006.v1.EventNotification;
import ch.vd.evd0006.v1.ObjectFactory;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.AbstractEsbJmsTemplate;
import ch.vd.technical.esb.util.ESBXMLValidator;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.XmlUtils;

/**
 * Implémentation de la fonctionalité de test d'envoi d'un événement civil
 */
public class EvenementCivilEchSenderImpl implements EvenementCivilEchSender, InitializingBean {

	private final ObjectFactory objectFactory = new ObjectFactory();
	private EsbMessageFactory esbMessageFactory;

	private AbstractEsbJmsTemplate esbTemplate;
	private String outputQueue;
	private String serviceDestination;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbTemplate(AbstractEsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final ESBXMLValidator esbValidator = new ESBXMLValidator();
		esbValidator.setResourceResolver(new ClasspathCatalogResolver());
		esbValidator.setSources(new Resource[]{new ClassPathResource("eVD-0009-1-0.xsd"), new ClassPathResource("eVD-0001-3-0.xsd"), new ClassPathResource("eVD-0006-1-0.xsd")});
		esbMessageFactory = new EsbMessageFactory();
		esbMessageFactory.setValidator(esbValidator);
	}

	@Override
	public void sendEvent(EvenementCivilEch evt, String businessUser) throws Exception {

		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Marshaller marshaller = context.createMarshaller();

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.newDocument();

		final EventIdentification eventIdentification = objectFactory.createEventIdentificationType();
		eventIdentification.setAction(String.valueOf(evt.getAction().getEchCode()));
		eventIdentification.setDate(XmlUtils.regdate2xmlcal(evt.getDateEvenement()));
		eventIdentification.setMessageId(evt.getId());
		eventIdentification.setReferenceMessageId(evt.getRefMessageId());
		eventIdentification.setType(String.valueOf(evt.getType().getCodeECH()));

		final EventNotification message = objectFactory.createEventNotification();
		message.setIdentification(eventIdentification);

		marshaller.marshal(message, doc);

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setServiceDestination(serviceDestination);
		m.setBusinessId(String.valueOf(evt.getId()));
		m.setBusinessUser(businessUser);
		m.setContext("evenementCivilEch");
		m.setBody(doc);
		if (outputQueue != null) {
			// testing seulement!
			m.setServiceDestination(outputQueue);
		}

//		System.err.println("Message envoyé : " + m.getBodyAsString());

		esbTemplate.send(m);
	}
}
