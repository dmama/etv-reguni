package ch.vd.uniregctb.evenement.di;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.xml.event.di.cyber.codecontrole.v2.CodeApplication;
import ch.vd.unireg.xml.event.di.cyber.codecontrole.v2.EvtPublicationCodeControleCyber;
import ch.vd.unireg.xml.event.di.cyber.codecontrole.v2.InformationComplementaireType;
import ch.vd.unireg.xml.event.di.cyber.codecontrole.v2.ObjectFactory;
import ch.vd.unireg.xml.event.di.cyber.codecontrole.v2.Statut;
import ch.vd.unireg.xml.event.di.cyber.codecontrole.v2.TypeDocument;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbMessageValidator;

public class EvenementDeclarationPMSenderImpl implements EvenementDeclarationPMSender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementDeclarationPMSenderImpl.class);

	private static final String CODE_ROUTAGE_ATTRIBUTE_NAME = "CODE_ROUTAGE";

	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String serviceDestination;

	private JAXBContext jaxbContext;

	/**
	 * permet d'activer/désactiver l'envoi des événements
	 */
	private boolean enabled = true;

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbValidator(EsbMessageValidator esbValidator) {
		this.esbValidator = esbValidator;
	}

	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void sendEmissionEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException {
		if (!enabled) {
			LOGGER.info("Evénements de déclarations désactivés: l'événement d'émission de DI sur le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}

		final EvtPublicationCodeControleCyber evt = new EvtPublicationCodeControleCyber();
		evt.setApplicationEmettrice(CodeApplication.UNIREG);
		evt.setCodeControle(codeControle);
		evt.setHorodatagePublication(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()));
		if (codeRoutage != null) {
			evt.setInformationsComplementaires(new InformationComplementaireType(Collections.singletonList(new InformationComplementaireType.InformationComplementaire(CODE_ROUTAGE_ATTRIBUTE_NAME, codeRoutage))));
		}
		evt.setNumeroContribuable((int) numeroContribuable);
		evt.setNumeroSequence(BigInteger.valueOf(numeroSequence));
		evt.setPeriodeFiscale(periodeFiscale);
		evt.setStatut(Statut.ACTIF);
		evt.setTypeDocument(TypeDocument.DI_PM);
		sendEvent(evt);
	}

	@Override
	public void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle) throws EvenementDeclarationException {
		if (!enabled) {
			LOGGER.info("Evénements de déclarations désactivés: l'événement d'annulation de DI sur le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}

		final EvtPublicationCodeControleCyber evt = new EvtPublicationCodeControleCyber();
		evt.setApplicationEmettrice(CodeApplication.UNIREG);
		evt.setCodeControle(codeControle);
		evt.setHorodatagePublication(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()));
		evt.setNumeroContribuable((int) numeroContribuable);
		evt.setNumeroSequence(BigInteger.valueOf(numeroSequence));
		evt.setPeriodeFiscale(periodeFiscale);
		evt.setStatut(Statut.INACTIF);
		evt.setTypeDocument(TypeDocument.DI_PM);
		sendEvent(evt);
	}

	private void sendEvent(EvtPublicationCodeControleCyber evenement) throws EvenementDeclarationException {

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);

		try {
			final Marshaller marshaller = jaxbContext.createMarshaller();

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			marshaller.marshal(evenement, doc);

			final EsbMessage m = EsbMessageFactory.createMessage();
			m.setBusinessId(String.format("%d-%d-%s-%s",
			                              evenement.getNumeroContribuable(),
			                              evenement.getPeriodeFiscale(),
			                              evenement.getStatut(),
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
