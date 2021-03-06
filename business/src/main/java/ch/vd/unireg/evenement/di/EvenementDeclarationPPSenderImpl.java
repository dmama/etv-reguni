package ch.vd.unireg.evenement.di;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.cybercontexte.EvenementCyberContexteSender;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbMessageValidator;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.event.di.common.v1.EvenementDeclarationImpotContext;
import ch.vd.unireg.xml.event.di.output.v1.EvenementAnnulationDeclarationImpot;
import ch.vd.unireg.xml.event.di.output.v1.EvenementDeclarationImpotOutput;
import ch.vd.unireg.xml.event.di.output.v1.EvenementEmissionDeclarationImpot;
import ch.vd.unireg.xml.event.di.output.v1.ObjectFactory;

public class EvenementDeclarationPPSenderImpl implements EvenementDeclarationPPSender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementDeclarationPPSenderImpl.class);

	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private EvenementCyberContexteSender evenementCyberContexteSender;
	private String serviceDestination;

	private JAXBContext jaxbContext;

	/**
	 * permet d'activer/désactiver l'envoi des événements fiscaux
	 */
	private boolean enabled = true;

	private final ObjectFactory objectFactory = new ObjectFactory();

	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbValidator(EsbMessageValidator esbValidator) {
		this.esbValidator = esbValidator;
	}

	public void setEvenementCyberContexteSender(EvenementCyberContexteSender evenementCyberContexteSender) {
		this.evenementCyberContexteSender = evenementCyberContexteSender;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void sendEmissionEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, RegDate date, String codeControle, String codeRoutage) throws EvenementDeclarationException {

		if (!enabled) {
			LOGGER.info("Evénements de déclarations désactivés: l'événement d'émission de DI sur le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}

		final EvenementEmissionDeclarationImpot emission = objectFactory.createEvenementEmissionDeclarationImpotType();
		emission.setContext(new EvenementDeclarationImpotContext(periodeFiscale, (int) numeroContribuable, regdate2xml(date)));
		emission.setCodeControle(codeControle);
		emission.setCodeRoutage(codeRoutage);

		// on envoie un événement pour ADDI
		sendEvent(emission);
		// [FISCPROJ-499] on envoie un autre événement pour le context Cyber
		evenementCyberContexteSender.sendEmissionDeclarationEvent(numeroContribuable, periodeFiscale, numeroSequence, codeControle, date);
	}

	@Override
	public void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, RegDate date) throws EvenementDeclarationException {

		if (!enabled) {
			LOGGER.info("Evénements de déclarations désactivés: l'événement d'annulation de DI sur le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}

		final EvenementAnnulationDeclarationImpot annulation = objectFactory.createEvenementAnnulationDeclarationImpotType();
		annulation.setContext(new EvenementDeclarationImpotContext(periodeFiscale, (int) numeroContribuable, regdate2xml(date)));

		// on envoie un événement pour ADDI
		sendEvent(annulation);
		// [FISCPROJ-499] on envoie un autre événement pour le context Cyber
		evenementCyberContexteSender.sendAnnulationDeclarationEvent(numeroContribuable, periodeFiscale, numeroSequence, codeControle, date);
	}

	private static Date regdate2xml(RegDate date) {
		return new Date(date.year(), date.month(), date.day());
	}

	private void sendEvent(EvenementDeclarationImpotOutput evenement) throws EvenementDeclarationException {

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		if (principal == null) {
			throw new IllegalArgumentException();
		}

		try {
			final Marshaller marshaller = jaxbContext.createMarshaller();

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			marshaller.marshal(objectFactory.createEvenement(evenement), doc);

			final EsbMessage m = EsbMessageFactory.createMessage();
			m.setBusinessId(String.format("%d-%d-%s",
			                              evenement.getContext().getNumeroContribuable(),
			                              evenement.getContext().getPeriodeFiscale(),
			                              new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate())));
			m.setBusinessUser(principal);
			m.setServiceDestination(serviceDestination);
			m.setContext("declarationEvent");
			m.setBody(doc);

			esbValidator.validate(m);
			esbTemplate.send(m);
		}
		catch (Exception e) {
			throw new EvenementDeclarationException(EsbBusinessCode.REPONSE_IMPOSSIBLE, e);
		}

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
