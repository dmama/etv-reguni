package ch.vd.unireg.evenement.declaration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
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
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbMessageValidator;

public class EvenementDeclarationPMSenderImpl implements EvenementDeclarationPMSender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementDeclarationPMSenderImpl.class);

	private static final String CODE_ROUTAGE_ATTRIBUTE_NAME = "CODE_ROUTAGE";
	private static final String COMMUNE_ATTRIBUTE_NAME = "COMMUNE";
	private static final String PARCELLE_ATTRIBUTE_NAME = "NUM_PARCELLE";
	private static final String DELAI_RETOUR_ATTRIBUTE_NAME = "DATE_ECHEANCE";

	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String serviceDestinationDI;        // pour les DI
	private String serviceDestinationDD;        // pour les demandes de dégrèvement

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

	public void setServiceDestinationDI(String serviceDestinationDI) {
		this.serviceDestinationDI = serviceDestinationDI;
	}

	public void setServiceDestinationDD(String serviceDestinationDD) {
		this.serviceDestinationDD = serviceDestinationDD;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@Override
	public void sendEmissionDIEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException {
		if (!enabled) {
			LOGGER.info("Evénements de déclarations désactivés: l'événement d'émission de DI sur le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}
		sendPublication(numeroContribuable, periodeFiscale, numeroSequence, codeControle, TypeDocument.DI_PM, true, serviceDestinationDI, Collections.singletonMap(CODE_ROUTAGE_ATTRIBUTE_NAME, codeRoutage));
	}

	@Override
	public void sendAnnulationDIEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException {
		if (!enabled) {
			LOGGER.info("Evénements de déclarations désactivés: l'événement d'annulation de DI sur le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}
		sendPublication(numeroContribuable, periodeFiscale, numeroSequence, codeControle, TypeDocument.DI_PM, false, serviceDestinationDI, Collections.singletonMap(CODE_ROUTAGE_ATTRIBUTE_NAME, codeRoutage));
	}

	@Override
	public void sendEmissionQSNCEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException {
		if (!enabled) {
			LOGGER.info("Evénements de déclarations désactivés: l'événement d'émission du questionnaire SNC sur  le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}
		sendPublication(numeroContribuable, periodeFiscale, numeroSequence, codeControle, TypeDocument.QUEST_SNC, true, serviceDestinationDI, Collections.singletonMap(CODE_ROUTAGE_ATTRIBUTE_NAME, codeRoutage));
	}

	@Override
	public void sendAnnulationQSNCEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException {
		if (!enabled) {
			LOGGER.info("Evénements de déclarations désactivés: l'événement d'annulation du questionnaire SNC sur le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}
		sendPublication(numeroContribuable, periodeFiscale, numeroSequence, codeControle, TypeDocument.QUEST_SNC, false, serviceDestinationDI, Collections.singletonMap(CODE_ROUTAGE_ATTRIBUTE_NAME, codeRoutage));
	}

	@Override
	public void sendEmissionDemandeDegrevementICIEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String commune, String numeroParcelle, RegDate delaiRetour) throws EvenementDeclarationException {
		if (!enabled) {
			LOGGER.info("Evénements de déclarations désactivés: l'événement d'émission de formulaire de demande de dégrèvement sur le contribuable n° " + numeroContribuable + " n'est pas envoyé.");
			return;
		}

		final Map<String, String> infosComplementaires = new HashMap<>();
		addToMapIfNotEmpty(infosComplementaires, COMMUNE_ATTRIBUTE_NAME, commune, StringRenderer.DEFAULT);
		addToMapIfNotEmpty(infosComplementaires, PARCELLE_ATTRIBUTE_NAME, numeroParcelle, StringRenderer.DEFAULT);
		addToMapIfNotEmpty(infosComplementaires, DELAI_RETOUR_ATTRIBUTE_NAME, delaiRetour, RegDateHelper.StringFormat.INDEX::toString);

		sendPublication(numeroContribuable, periodeFiscale, numeroSequence, codeControle, TypeDocument.DEM_DEGREV, true, serviceDestinationDD, infosComplementaires);
	}

	private static <T> void addToMapIfNotEmpty(Map<String, String> map, String key, T value, StringRenderer<? super T> renderer) {
		if (value != null) {
			final String stringValue = renderer.toString(value);
			if (StringUtils.isNotBlank(stringValue)) {
				map.put(key, stringValue);
			}
		}
	}

	private void sendPublication(long numeroContribuable,
	                             int periodeFiscale,
	                             int numeroSequence,
	                             String codeControle,
	                             TypeDocument typeDocument,
	                             boolean activation,
	                             String serviceDestination,
	                             @Nullable Map<String, String> infosComplementaires) throws EvenementDeclarationException {

		final EvtPublicationCodeControleCyber evt = new EvtPublicationCodeControleCyber();
		evt.setApplicationEmettrice(CodeApplication.UNIREG);
		evt.setCodeControle(codeControle);
		evt.setHorodatagePublication(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()));
		if (infosComplementaires != null && !infosComplementaires.isEmpty()) {
			final List<InformationComplementaireType.InformationComplementaire> infos = infosComplementaires.entrySet().stream()
					.map(entry -> new InformationComplementaireType.InformationComplementaire(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());
			evt.setInformationsComplementaires(new InformationComplementaireType(infos));
		}
		evt.setNumeroContribuable((int) numeroContribuable);
		evt.setNumeroSequence(BigInteger.valueOf(numeroSequence));
		evt.setPeriodeFiscale(periodeFiscale);
		evt.setStatut(activation ? Statut.ACTIF : Statut.INACTIF);
		evt.setTypeDocument(typeDocument);
		sendEvent(evt, serviceDestination);
	}

	private void sendEvent(EvtPublicationCodeControleCyber evenement, String serviceDestination) throws EvenementDeclarationException {

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
			m.setBusinessId(String.format("%s-%d-%d-%s-%s",
			                              evenement.getTypeDocument(),
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
