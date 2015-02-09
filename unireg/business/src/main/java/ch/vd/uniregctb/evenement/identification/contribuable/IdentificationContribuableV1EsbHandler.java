package ch.vd.uniregctb.evenement.identification.contribuable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.jms.EsbMessageHelper;
import ch.vd.uniregctb.jms.EsbMessageValidator;

/**
 * Classe technique qui reçoit des événements de demande d'identification de contribuable, et qui permet d'envoyer les réponses.
 * <p>
 * <b>Note:</b> cette classe ne définit aucune action <i>métier</i> : sa responsabilité se limite à faire l'interface entre les messages JMS
 * et les classes métier correspondantes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IdentificationContribuableV1EsbHandler implements IdentificationContribuableMessageHandler, EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationContribuableV1EsbHandler.class);

	protected static final String DOCUMENT_URL_ATTRIBUTE_NAME = "documentUrl";

	private String outputQueue;
	private EsbMessageValidator esbValidator;
	private HibernateTemplate hibernateTemplate;
	private EsbJmsTemplate esbTemplate;

	private DemandeHandler demandeHandler;

	/**
	 * for testing purpose
	 */
	protected void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	public void setEsbValidator(EsbMessageValidator esbValidator) {
		this.esbValidator = esbValidator;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	/**
	 * Défini le handler qui sera appelé lors de la réception d'une demande d'identification de contribuable. Le handler est responsable
	 * d'entreprendre toutes les actions <i>métier</i> nécessaires au traitement correct du message.
	 *
	 * @param handler le handler <i>métier</i> de demande d'identification de contribuable
	 */
	public void setDemandeHandler(DemandeHandler handler) {
		this.demandeHandler = handler;
	}

	@Override
	public void onEsbMessage(EsbMessage msg) throws Exception {

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Arrivée d'une demande d'identification de contribuable (BusinessID='%s')", msg.getBusinessId()));
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("message=" + msg.getBodyAsString());
		}

		// Parse le message sous forme XML
		final IdentificationCTBDocument doc = IdentificationCTBDocument.Factory.parse(msg.getBodyAsString());

		// Valide le bousin
		final XmlOptions validateOptions = new XmlOptions();
		final List<XmlError> errorList = new ArrayList<>();
		validateOptions.setErrorListener(errorList);
		if (!doc.validate(validateOptions)) {
			final StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append('\n');
				builder.append("Message: ").append(error.getErrorCode()).append(' ').append(error.getMessage()).append('\n');
				builder.append("Location of invalid XML: ").append(error.getCursorLocation().xmlText()).append('\n');
			}

			final String errorMessage = builder.toString();
			LOGGER.error(errorMessage);
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, errorMessage, null);
		}
		else {

			// Traitement du message
			try {
				final IdentificationContribuable message = XmlEntityAdapter.xml2entity(doc.getIdentificationCTB());
				verifierMontantMessage(message, msg.getBusinessId());
				final EsbHeader header = new EsbHeader();
				header.setBusinessUser(msg.getBusinessUser());
				header.setBusinessId(msg.getBusinessId());
				header.setReplyTo(msg.getServiceReplyTo());
				header.setDocumentUrl(msg.getHeader(DOCUMENT_URL_ATTRIBUTE_NAME));
				header.setMetadata(EsbMessageHelper.extractCustomHeaders(msg));
				message.setHeader(header);

				Assert.notNull(demandeHandler, "Le handler de demandes n'est pas défini");

				AuthenticationHelper.pushPrincipal("JMS-EvtIdentCtb(" + msg.getMessageId() + ')');
				try {
					demandeHandler.handleDemande(message);
					hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'authentification
				}
				finally {
					AuthenticationHelper.popPrincipal();
				}
			}
			catch (XmlException | MontantInvalideException e) {
				// problème au moment de la conversion de l'XML en entité
				LOGGER.error("Erreur dans le message XML reçu", e);
				throw new EsbBusinessException(EsbBusinessCode.IDENTIFICATION_DONNEES_INVALIDES, e.getMessage(), e);
			}
			catch (RuntimeException e) {
				// Départ en DLQ, mais on log avant...
				LOGGER.error(e.getMessage(), e);
				throw e;
			}
		}
	}

	private void verifierMontantMessage(IdentificationContribuable message, String businessId) throws MontantInvalideException {
		final Long montant = message.getDemande().getMontant();
		if (montant != null && Math.abs(montant) > 9999999999L) {
			final String cause= String.format("La demande d'identification ayant le business id %S a un montant d'une valeur de %s qui n'est pas acceptée." +
					" Elle sera mise en queue d'erreur.",businessId, montant);
			throw new MontantInvalideException(cause);
		}
	}

	/**
	 * @see ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableMessageHandler#sendReponse(ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable)
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void sendReponse(IdentificationContribuable message) throws Exception {
		
		final EsbHeader header = message.getHeader();
		Assert.notNull(header, "Le header doit être renseigné.");
		final String businessUser = header.getBusinessUser();
		final String businessId = header.getBusinessId();
		final String replyTo = header.getReplyTo();
		Assert.notNull(businessUser, "Le business user doit être renseigné.");
		Assert.notNull(businessId, "Le business id doit être renseigné.");
		Assert.notNull(replyTo, "Le reply-to doit être renseigné.");

		final IdentificationCTBDocument identificationCtb = XmlEntityAdapter.entity2xml(message);

		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessId(String.format("%s-answer-%s", businessId, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate())));
		m.setBusinessUser(businessUser);
		m.setBusinessCorrelationId(businessId);
		m.setServiceDestination(replyTo);
		m.setContext("identificationContribuable");
		m.setBody(XmlUtils.xmlbeans2string(identificationCtb));

		final Map<String,String> metadata = header.getMetadata();
		if (metadata != null && metadata.size() > 0) {
			EsbMessageHelper.setHeaders(m, metadata, false);
		}

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}

		esbValidator.validate(m);
		esbTemplate.send(m);
	}
}
