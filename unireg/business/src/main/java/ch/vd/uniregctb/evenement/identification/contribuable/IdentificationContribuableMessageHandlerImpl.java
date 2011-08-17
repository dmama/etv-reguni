package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

/**
 * Classe technique qui reçoit des événements de demande d'identification de contribuable, et qui permet d'envoyer les réponses.
 * <p>
 * <b>Note:</b> cette classe ne définit aucune action <i>métier</i> : sa responsabilité se limite à faire l'interface entre les messages JMS
 * et les classes métier correspondantes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IdentificationContribuableMessageHandlerImpl extends EsbMessageListener implements IdentificationContribuableMessageHandler, MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(IdentificationContribuableMessageHandlerImpl.class);

	protected static final String DOCUMENT_URL_ATTRIBUTE_NAME = "documentUrl";

	private String outputQueue;
	private EsbMessageFactory esbMessageFactory;
	private HibernateTemplate hibernateTemplate;
	private DemandeHandler demandeHandler;

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	/**
	 * for testing purpose
	 */
	protected void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
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

		// pour la statistique
		nbMessagesRecus.incrementAndGet();

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
		final List<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!doc.validate(validateOptions)) {
			final StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append("\n");
				builder.append("Message: ").append(error.getErrorCode()).append(" ").append(error.getMessage()).append("\n");
				builder.append("Location of invalid XML: ").append(error.getCursorLocation().xmlText()).append("\n");
			}

			final String errorMessage = builder.toString();
			LOGGER.error(errorMessage);
			getEsbTemplate().sendError(msg, errorMessage, null, ErrorType.TECHNICAL, "");
		}
		else {

			// Traitement du message
			try {
				final IdentificationContribuable message = XmlEntityAdapter.xml2entity(doc.getIdentificationCTB());
				final EsbHeader header = new EsbHeader();
				header.setBusinessUser(msg.getBusinessUser());
				header.setBusinessId(msg.getBusinessId());
				header.setReplyTo(msg.getServiceReplyTo());
				header.setDocumentUrl(msg.getHeader(DOCUMENT_URL_ATTRIBUTE_NAME));
				message.setHeader(header);

				Assert.notNull(demandeHandler, "Le handler de demandes n'est pas défini");

				AuthenticationHelper.pushPrincipal("JMS-EvtIdentCtb(" + msg.getMessageId() + ")");
				try {
					demandeHandler.handleDemande(message);
					hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'authentification
				}
				finally {
					AuthenticationHelper.popPrincipal();
				}
			}
			catch (XmlException e) {
				// problème au moment de la conversion de l'XML en entité
				LOGGER.error("Erreur dans le message XML reçu", e);
				getEsbTemplate().sendError(msg, e.getMessage(), e, ErrorType.BUSINESS, "");
			}
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

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessId(String.valueOf(message.getId()));
		m.setBusinessUser(businessUser);
		m.setBusinessCorrelationId(businessId);
		m.setServiceDestination(replyTo);
		m.setContext("identificationContribuable");
		m.setBody(XmlUtils.xmlbeans2string(identificationCtb));

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		getEsbTemplate().send(m);
	}

	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}
