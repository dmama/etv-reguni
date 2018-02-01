package ch.vd.unireg.evenement.civil.ech;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;

import ch.vd.evd0001.v5.EventIdentification;
import ch.vd.evd0001.v5.EventNotification;
import ch.vd.evd0001.v5.ObjectFactory;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.AbstractEsbJmsTemplate;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.jms.EsbMessageValidator;

/**
 * Implémentation de la fonctionalité de test d'envoi d'un événement civil
 */
public class EvenementCivilEchSenderImpl implements EvenementCivilEchSender, InitializingBean {

	private final ObjectFactory objectFactory = new ObjectFactory();
	private JAXBContext jaxbContext;

	private EsbMessageValidator esbValidator;

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
		esbValidator = BusinessItTest.buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("eVD-0009-1-0.xsd"),
				new ClassPathResource("eVD-0004-3-0.xsd"),
				new ClassPathResource("eVD-0001-5-0.xsd")
		});

		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void sendEvent(EvenementCivilEch evt, String businessUser) throws Exception {

		final Marshaller marshaller = jaxbContext.createMarshaller();

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.newDocument();

		final EventIdentification eventIdentification = objectFactory.createEventIdentificationType();
		eventIdentification.setAction(String.valueOf(evt.getAction().getEchCode()));
		eventIdentification.setEventDate(XmlUtils.regdate2xmlcal(evt.getDateEvenement()));
		eventIdentification.setMessageId(evt.getId());
		eventIdentification.setReferenceMessageId(evt.getRefMessageId());
		eventIdentification.setEventType(String.valueOf(evt.getType().getCodeECH()));

		final EventNotification message = objectFactory.createEventNotification();
		message.setIdentification(eventIdentification);

		marshaller.marshal(message, doc);

		final EsbMessage m = EsbMessageFactory.createMessage();
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

		esbValidator.validate(m);
		esbTemplate.send(m);
	}
}
