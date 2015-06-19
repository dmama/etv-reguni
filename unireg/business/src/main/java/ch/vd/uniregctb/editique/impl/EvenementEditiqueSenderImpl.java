package ch.vd.uniregctb.editique.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Bean qui permet d'envoyer des documents à l'éditique.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementEditiqueSenderImpl implements EvenementEditiqueSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEditiqueSenderImpl.class);

	private EsbJmsTemplate esbTemplate;         // ESB template standard
	private EsbJmsTemplate noTxEsbTemplate;     // ESB template non-rattaché au transaction manager
	private String serviceDestinationImpression;
	private String serviceDestinationCopieConforme;
	private String serviceReplyTo;

	private interface HeaderCustomFiller {
		void addHeaders(EsbMessage msg) throws Exception;
	}

	@Override
	public String envoyerDocumentImmediatement(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, FormatDocumentEditique typeFormat, boolean archive) throws EditiqueException {
		return envoyerImpression(nomDocument, typeDocument, document, TypeImpressionEditique.DIRECT, typeFormat, archive, noTxEsbTemplate);
	}

	@Override
	public String envoyerDocument(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, FormatDocumentEditique typeFormat, boolean archive) throws EditiqueException {
		return envoyerImpression(nomDocument, typeDocument, document, TypeImpressionEditique.BATCH, typeFormat, archive, esbTemplate);
	}

	private String envoyerImpression(final String nomDocument, final TypeDocumentEditique typeDocument, XmlObject document, final TypeImpressionEditique typeImpression, final FormatDocumentEditique typeFormat, final boolean archive, EsbJmsTemplate esbTemplate) throws EditiqueException {

		// tant que les documents éditiques n'ont pas de namespace, ils ne peuvent pas être validés par le framework
		// de la message factory de l'ESB. D'après les guidelines de l'ESB, il faut donc les valider à la main...
		validate(document);

		try {

			final String body = XmlUtils.xmlbeans2string(document);
			final boolean reponseAttendue = typeImpression == TypeImpressionEditique.DIRECT;
			final EsbMessage msg = buildEsbMessage(nomDocument, typeDocument, body, serviceDestinationImpression, reponseAttendue, new HeaderCustomFiller() {
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
			});

			return send(esbTemplate, msg);
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi d'un document au service Editique JMS";
			LOGGER.error(message, e);

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

	private EsbMessage buildEsbMessage(String businessId, TypeDocumentEditique typeDocument, String body, String serviceDestination, boolean reponseAttendue, HeaderCustomFiller headerFiller) throws Exception {
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
		m.setBody(body);
		return m;
	}

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
}
