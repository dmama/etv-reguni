package ch.vd.uniregctb.evenement.di;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.text.SimpleDateFormat;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.xml.event.di.liberation.v1.DemandeLiberation;
import ch.vd.unireg.xml.event.di.liberation.v1.ObjectFactory;
import ch.vd.unireg.xml.event.di.liberation.v1.TypeDeclarationImpot;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.declaration.EvenementDeclarationException;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbMessageValidator;

/**
 * Implémentation du service d'envoi des messages de demande de libération des déclarations d'impôt
 */
public class EvenementLiberationDeclarationImpotSenderImpl implements EvenementLiberationDeclarationImpotSender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementLiberationDeclarationImpotSenderImpl.class);

	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String serviceDestination;
	private boolean enabled = true;

	private JAXBContext jaxbContext;
	private final ObjectFactory objectFactory = new ObjectFactory();

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
	public void demandeLiberationDeclarationImpot(long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull TypeDeclarationLiberee type) throws EvenementDeclarationException {
		if (!enabled) {
			LOGGER.info(String.format("Evénements de demande de libération de déclaration d'impôt désactivés ; la demande de libération de la déclaration %d/%d du contribuable %s n'est donc pas envoyée.",
			                          periodeFiscale,
			                          numeroSequence,
			                          FormatNumeroHelper.numeroCTBToDisplay(numeroContribuable)));
			return;
		}

		// maintenant, il faut constituer et envoyer le message
		final DemandeLiberation demande = buildDemande(numeroContribuable, periodeFiscale, numeroSequence, type);
		sendMessage(demande);
	}

	private void sendMessage(DemandeLiberation demande) throws EvenementDeclarationException {
		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);

		try {
			final Marshaller marshaller = jaxbContext.createMarshaller();

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			marshaller.marshal(objectFactory.createDemandeLiberation(demande), doc);

			final EsbMessage m = EsbMessageFactory.createMessage();
			m.setBusinessId(String.format("LIBERATION-%d-%d-%d-%s",
			                              demande.getNumeroContribuable(),
			                              demande.getPeriodeFiscale(),
			                              demande.getNumeroSequence(),
			                              new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate())));
			m.setBusinessUser(principal);
			m.setServiceDestination(serviceDestination);
			m.setContext("liberationDI");
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

	private static DemandeLiberation buildDemande(long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull TypeDeclarationLiberee type) {
		final DemandeLiberation demande = new DemandeLiberation();
		demande.setNumeroContribuable((int) numeroContribuable);
		demande.setPeriodeFiscale(periodeFiscale);
		demande.setNumeroSequence(numeroSequence);
		demande.setTypeDeclarationImpot(extractTypeDeclarationImpot(type));
		return demande;
	}

	private static TypeDeclarationImpot extractTypeDeclarationImpot(TypeDeclarationLiberee type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case DI_PM:
			return TypeDeclarationImpot.PM;
		case DI_PP:
			return TypeDeclarationImpot.PP;
		default:
			throw new IllegalArgumentException("Value non-supportée : " + type);
		}
	}
}
