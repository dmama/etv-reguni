package ch.vd.uniregctb.editique.impl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.ObjectFactory;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EvenementEditiqueSender;
import ch.vd.uniregctb.editique.FormatDocumentEditique;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.editique.TypeImpressionEditique;
import ch.vd.uniregctb.jms.EsbMessageValidator;
import ch.vd.uniregctb.utils.LogLevel;

/**
 * Bean qui permet d'envoyer des documents à l'éditique.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementEditiqueSenderImpl implements EvenementEditiqueSender, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEditiqueSenderImpl.class);

	private EsbJmsTemplate esbTemplate;         // ESB template standard
	private EsbJmsTemplate noTxEsbTemplate;     // ESB template non-rattaché au transaction manager
	private String serviceDestinationImpression;
	private String serviceDestinationCopieConforme;
	private String serviceReplyTo;

	private JAXBContext jaxbContext;
	private EsbMessageValidator esbMessageValidator;

	private interface HeaderCustomFiller {
		void addHeaders(EsbMessage msg) throws Exception;
	}

	@Override
	public String envoyerDocumentImmediatement(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, FormatDocumentEditique typeFormat, boolean archive) throws EditiqueException {
		return envoyerImpressionLegacy(nomDocument, typeDocument, document, TypeImpressionEditique.DIRECT, typeFormat, archive, noTxEsbTemplate);
	}

	@Override
	public String envoyerDocumentImmediatement(String nomDocument, TypeDocumentEditique typeDocument, FichierImpression document, FormatDocumentEditique typeFormat, boolean archive) throws EditiqueException {
		return envoyerImpression(nomDocument, typeDocument, document, TypeImpressionEditique.DIRECT, typeFormat, archive, noTxEsbTemplate);
	}

	@Override
	public String envoyerDocument(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, FormatDocumentEditique typeFormat, boolean archive) throws EditiqueException {
		return envoyerImpressionLegacy(nomDocument, typeDocument, document, TypeImpressionEditique.BATCH, typeFormat, archive, esbTemplate);
	}

	@Override
	public String envoyerDocument(String nomDocument, TypeDocumentEditique typeDocument, FichierImpression document, FormatDocumentEditique typeFormat, boolean archive) throws EditiqueException {
		return envoyerImpression(nomDocument, typeDocument, document, TypeImpressionEditique.BATCH, typeFormat, archive, esbTemplate);
	}

	private static boolean isReponseAttendue(TypeImpressionEditique typeImpression) {
		return typeImpression == TypeImpressionEditique.DIRECT;
	}

	/**
	 * Implémentation spécifique du {@link ch.vd.uniregctb.editique.impl.EvenementEditiqueSenderImpl.HeaderCustomFiller} qui traite des demandes d'impression
	 */
	private static final class PrintingHeaderFiller implements HeaderCustomFiller {

		private final TypeImpressionEditique typeImpression;
		private final boolean archive;
		private final TypeDocumentEditique typeDocument;
		private final boolean reponseAttendue;
		private final FormatDocumentEditique typeFormat;
		private final String nomDocument;

		public PrintingHeaderFiller(TypeImpressionEditique typeImpression, boolean archive, TypeDocumentEditique typeDocument, boolean reponseAttendue, FormatDocumentEditique typeFormat, String nomDocument) {
			this.typeImpression = typeImpression;
			this.archive = archive;
			this.typeDocument = typeDocument;
			this.reponseAttendue = reponseAttendue;
			this.typeFormat = typeFormat;
			this.nomDocument = nomDocument;
		}

		@Override
		public void addHeaders(EsbMessage msg) throws Exception {
			msg.addHeader(ConstantesEditique.PRINT_MODE, typeImpression.getCode());
			msg.addHeader(ConstantesEditique.ARCHIVE_FLAG, Boolean.toString(archive));
			msg.addHeader(ConstantesEditique.DOCUMENT_TYPE, typeDocument.getCodeDocumentEditique());
			if (reponseAttendue) {
				msg.addHeader(ConstantesEditique.RETURN_FORMAT, typeFormat.getCode());

				msg.addHeader(ConstantesEditique.UNIREG_DOCUMENT_ID, nomDocument);
				msg.addHeader(ConstantesEditique.UNIREG_TYPE_DOCUMENT, typeDocument.name());
				msg.addHeader(ConstantesEditique.UNIREG_FORMAT_DOCUMENT, typeFormat.name());
			}
		}
	}

	private String envoyerImpressionLegacy(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, TypeImpressionEditique typeImpression, FormatDocumentEditique typeFormat, boolean archive, EsbJmsTemplate esbTemplate) throws EditiqueException {

		// tant que les documents éditiques n'ont pas de namespace, ils ne peuvent pas être validés par le framework
		// de la message factory de l'ESB. D'après les guidelines de l'ESB, il faut donc les valider à la main...
		validate(document);

		try {
			final String body = XmlUtils.xmlbeans2string(document);
			final boolean reponseAttendue = isReponseAttendue(typeImpression);
			final EsbMessage msg = buildEsbMessage(nomDocument, typeDocument, body, serviceDestinationImpression, reponseAttendue, new PrintingHeaderFiller(typeImpression, archive, typeDocument, reponseAttendue, typeFormat, nomDocument));
			return send(esbTemplate, msg);
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi d'un document au service Editique JMS";
			LOGGER.error(message, e);

			throw new EditiqueException(message, e);
		}
	}

	private String envoyerImpression(String nomDocument, TypeDocumentEditique typeDocument, FichierImpression fichierImpression, TypeImpressionEditique typeImpression,
	                                 FormatDocumentEditique typeFormat, boolean archive, EsbJmsTemplate esbTemplate) throws EditiqueException {

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);

		try {
			final Marshaller marshaller = jaxbContext.createMarshaller();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();
			marshaller.marshal(fichierImpression, doc);

			final boolean reponseAttendue = isReponseAttendue(typeImpression);
			final EsbMessage m = buildEsbMessage(nomDocument, typeDocument, doc, serviceDestinationImpression, reponseAttendue, new PrintingHeaderFiller(typeImpression, archive, typeDocument, reponseAttendue, typeFormat, nomDocument));
			esbMessageValidator.validate(m);
			esbTemplate.send(m);
			return m.getMessageId();
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi des données à l'éditique.";
			LogLevel.log(LOGGER, LogLevel.Level.FATAL, message, e);

			throw new EditiqueException(message, e);
		}
	}

	private static String send(EsbJmsTemplate esbTemplate, EsbMessage msg) throws Exception {
		// on envoie l'événement sous forme de message JMS à travers l'ESB
		esbTemplate.send(msg);

		final String messageId = msg.getMessageId();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Message ID JMS :" + messageId + "--");
			LOGGER.trace("ID :" + msg.getBusinessId() + "--");
		}

		return messageId;
	}

	@Override
	public Pair<String, String> envoyerDemandeCopieConforme(final String cleArchivage, final TypeDocumentEditique typeDocument, final long noTiers) throws EditiqueException {
		if (typeDocument.getCodeDocumentArchivage() == null) {
			throw new IllegalArgumentException("Archivage non-supporté pour document de type " + typeDocument);
		}

		try {
			final String body = "<empty/>";
			final String businessId = String.format("copieconforme-%d-%s", noTiers, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
			final EsbMessage msg = buildEsbMessage(businessId, typeDocument, body, serviceDestinationCopieConforme, true, new HeaderCustomFiller() {
				@Override
				public void addHeaders(EsbMessage msg) throws Exception {
					final FormatDocumentEditique pdf = FormatDocumentEditique.PDF;

					msg.addHeader(ConstantesEditique.TYPE_DOSSIER, ConstantesEditique.TYPE_DOSSIER_ARCHIVAGE);
					msg.addHeader(ConstantesEditique.NOM_DOSSIER, FormatNumeroHelper.numeroCTBToDisplay(noTiers));
					msg.addHeader(ConstantesEditique.TYPE_DOCUMENT, typeDocument.getCodeDocumentArchivage());
					msg.addHeader(ConstantesEditique.CLE_ARCHIVAGE, cleArchivage);
					msg.addHeader(ConstantesEditique.TYPE_FORMAT, pdf.getCode());

					msg.addHeader(ConstantesEditique.UNIREG_DOCUMENT_ID, businessId);
					msg.addHeader(ConstantesEditique.UNIREG_TYPE_DOCUMENT, typeDocument.name());
					msg.addHeader(ConstantesEditique.UNIREG_FORMAT_DOCUMENT, pdf.name());
				}
			});

			return Pair.of(send(noTxEsbTemplate, msg), businessId);
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi d'une demande de copie conforme au service Editique JMS";
			LOGGER.error(message, e);

			throw new EditiqueException(message, e);
		}
	}

	/**
	 * Génère un message ESB avec toutes les méta-données mais sans encore inclure le "body"
	 */
	private EsbMessage buildBodylessEsbMessage(String businessId, TypeDocumentEditique typeDocument, String serviceDestination, boolean reponseAttendue, HeaderCustomFiller headerFiller) throws Exception {
		final String principal = AuthenticationHelper.getCurrentPrincipal();
		Assert.notNull(principal);

		final EsbMessage m = EsbMessageFactory.createMessage();

		// méta-information de base
		m.setBusinessId(businessId);
		m.setBusinessUser(principal);
		m.setContext(typeDocument.getContexteImpression());

		m.setServiceDestination(serviceDestination);
		if (reponseAttendue) {
			m.setServiceReplyTo(serviceReplyTo);
		}

		headerFiller.addHeaders(m);
		return m;
	}

	/**
	 * Genère un message ESB à envoyer à l'éditique quand le "body" est connu sous la forme d'un {@link Document}
	 */
	private EsbMessage buildEsbMessage(String businessId, TypeDocumentEditique typeDocument, Document body, String serviceDestination, boolean reponseAttendue, HeaderCustomFiller headerFiller) throws Exception {
		final EsbMessage m = buildBodylessEsbMessage(businessId, typeDocument, serviceDestination, reponseAttendue, headerFiller);
		m.setBody(body);
		return m;
	}

	/**
	 * Genère un message ESB à envoyer à l'éditique quand le "body" est connu sous la forme d'une simple chaîne de caractères
	 */
	private EsbMessage buildEsbMessage(String businessId, TypeDocumentEditique typeDocument, String body, String serviceDestination, boolean reponseAttendue, HeaderCustomFiller headerFiller) throws Exception {
		final EsbMessage m = buildBodylessEsbMessage(businessId, typeDocument, serviceDestination, reponseAttendue, headerFiller);
		m.setBody(body);
		return m;
	}

	/**
	 * Méthode de validation d'un document "XmlBeans"
	 * @param document le document en question
	 */
	private static void validate(XmlObject document) {

		// Endroit où on va récupérer les éventuelles erreurs
		final XmlOptions validateOptions = new XmlOptions();
		final List<XmlError> errorList = new ArrayList<>();
		validateOptions.setErrorListener(errorList);

		// C'est parti pour la validation !
		final boolean isValid = document.validate(validateOptions);

		// si le document n'est pas valide, on va logguer pour avoir de quoi identifier et corriger le bug ensuite
		if (!isValid) {
			final StringBuilder b = new StringBuilder();
			b.append("--------------------------------------------------\n");
			b.append("--------------------------------------------------\n");
			b.append("Erreur de validation du message éditique en sortie\n");
			b.append("--------------------------------------------------\n");
			b.append("Message :\n").append(document).append('\n');
			b.append("--------------------------------------------------\n");
			for (XmlError error : errorList) {
				b.append("Erreur : ").append(error.getMessage()).append('\n');
				b.append("Localisation de l'erreur : ").append(error.getCursorLocation().xmlText()).append('\n');
				b.append("--------------------------------------------------\n");
			}
			b.append("--------------------------------------------------\n");
			Assert.fail(b.toString());
		}
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setNoTxEsbTemplate(EsbJmsTemplate noTxEsbTemplate) {
		this.noTxEsbTemplate = noTxEsbTemplate;
	}

	public void setServiceDestinationImpression(String serviceDestinationImpression) {
		this.serviceDestinationImpression = serviceDestinationImpression;
	}

	public void setServiceDestinationCopieConforme(String serviceDestinationCopieConforme) {
		this.serviceDestinationCopieConforme = serviceDestinationCopieConforme;
	}

	public void setServiceReplyTo(String serviceReplyTo) {
		this.serviceReplyTo = serviceReplyTo;
	}

	public void setEsbMessageValidator(EsbMessageValidator esbMessageValidator) {
		this.esbMessageValidator = esbMessageValidator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}
}
