package ch.vd.unireg.evenement.cybercontexte;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.text.SimpleDateFormat;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
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
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbMessageValidator;
import ch.vd.unireg.xml.event.di.cyber.contextprestation.v1.CodeApplication;
import ch.vd.unireg.xml.event.di.cyber.contextprestation.v1.EvtPublicationContextePrestationCyber;
import ch.vd.unireg.xml.event.di.cyber.contextprestation.v1.ObjectFactory;
import ch.vd.unireg.xml.event.di.cyber.contextprestation.v1.Statut;

public class EvenementCyberContexteSenderImpl implements EvenementCyberContexteSender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCyberContexteSenderImpl.class);
	public static final String DELAI_DI_DOC_TYPE = "DELAI-DI";

	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String serviceDestination;
	private JAXBContext jaxbContext;

	/**
	 * permet d'activer/désactiver l'envoi des événements
	 */
	private boolean enabled = true;

	@Override
	public void sendEmissionDeclarationEvent(Long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull String codeControle, @NotNull RegDate dateEvenement) throws EvenementCyberContexteException {

		if (!enabled) {
			LOGGER.info("Publication Cybercontexte désactivé: l'événement d'émission de DI sur le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}

		final String businessId = buildBusinessId(numeroContribuable, periodeFiscale, numeroSequence);
		final EvtPublicationContextePrestationCyber event = buildDeclarationEvent(periodeFiscale, numeroContribuable, numeroSequence, Statut.ACTIF, codeControle, dateEvenement);

		sendEvent(event, businessId);
	}

	@Override
	public void sendAnnulationDeclarationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull String codeControle, @NotNull RegDate dateEvenement) throws EvenementCyberContexteException {

		if (!enabled) {
			LOGGER.info("Publication Cybercontexte désactivé: l'événement d'annulation de DI sur le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}

		final String businessId = buildBusinessId(numeroContribuable, periodeFiscale, numeroSequence);
		final EvtPublicationContextePrestationCyber event = buildDeclarationEvent(periodeFiscale, numeroContribuable, numeroSequence, Statut.INACTIF, codeControle, dateEvenement);

		sendEvent(event, businessId);
	}

	@NotNull
	private EvtPublicationContextePrestationCyber buildDeclarationEvent(int periodeFiscale, long numeroContribuable, int numeroSequence, @NotNull Statut statut, @NotNull String codeControle, @NotNull RegDate dateEvenement) {
		final EvtPublicationContextePrestationCyber event = new EvtPublicationContextePrestationCyber();
		event.setPrestationCode("e-delai");
		event.setAuthentificationHashKey(buildAuthentificationHashKey(numeroContribuable, periodeFiscale, codeControle));
		event.setIdentificationHashKey(buildIdentificationHashKey(numeroContribuable, periodeFiscale));
		event.setEmissionDate(XmlUtils.regdate2xmlcal(dateEvenement));
		event.setSource(CodeApplication.UNIREG);
		event.setStatut(statut);
		event.setDocumentId(periodeFiscale + "-" + numeroContribuable + "-" + numeroSequence);
		event.setDocumentType(DELAI_DI_DOC_TYPE);
		event.setPeriodeFiscale(periodeFiscale);
		event.setNumeroContribuable((int) numeroContribuable);
		return event;
	}

	@NotNull
	private static String buildIdentificationHashKey(long numeroContribuable, int periodeFiscale) {
		final String numeroAsString = String.valueOf(numeroContribuable);
		final char firstChar = numeroAsString.charAt(0);
		final char lastChar = numeroAsString.charAt(numeroAsString.length() - 1);
		return firstChar + DigestUtils.sha512Hex(buildIdentificationString(numeroAsString, periodeFiscale)) + lastChar;
	}

	@NotNull
	private static String buildAuthentificationHashKey(long numeroContribuable, int periodeFiscale, @NotNull String codeControle) {
		final String numeroAsString = String.valueOf(numeroContribuable);
		final char firstChar = numeroAsString.charAt(0);
		final char lastChar = numeroAsString.charAt(numeroAsString.length() - 1);

		return firstChar + DigestUtils.sha512Hex(buildAuthentificationString(numeroAsString, periodeFiscale, codeControle)) + lastChar;
	}

	@NotNull
	private static String buildIdentificationString(@NotNull String numeroContribuable, int periodeFiscale) {
		return periodeFiscale + "-" + numeroContribuable + "-" + DELAI_DI_DOC_TYPE;
	}

	@NotNull
	private static String buildAuthentificationString(@NotNull String numeroContribuable, int periodeFiscale, @NotNull String codeControle) {
		return buildIdentificationString(numeroContribuable, periodeFiscale) + "-" + codeControle;
	}

	@NotNull
	private static String buildBusinessId(long numeroContribuable, int periodeFiscale, int numeroSequence) {
		return String.format("%d-%d-%d-%s",
		                     numeroContribuable,
		                     periodeFiscale,
		                     numeroSequence,
		                     new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
	}

	private void sendEvent(@NotNull EvtPublicationContextePrestationCyber event, String businessId) throws EvenementCyberContexteException {
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

			marshaller.marshal(event, doc);

			final EsbMessage m = EsbMessageFactory.createMessage();
			m.setBusinessId(businessId);
			m.setBusinessUser(principal);
			m.setServiceDestination(serviceDestination);
			m.setContext("cyberContexte");
			m.setBody(doc);

			esbValidator.validate(m);
			esbTemplate.send(m);
		}
		catch (Exception e) {
			throw new EvenementCyberContexteException(EsbBusinessCode.REPONSE_IMPOSSIBLE, e);
		}
	}

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
}
