package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.uniregctb.common.AuthenticationHelper;

/**
 * Classe technique qui reçoit des événements de demande d'identification de contribuable, et qui permet d'envoyer les réponses.
 * <p>
 * <b>Note:</b> cette classe ne définit aucune action <i>métier</i> : sa responsabilité se limite à faire l'interface entre les messages JMS
 * et les classes métier correspondantes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IdentificationContribuableMessageHandlerImpl extends EsbMessageListener implements IdentificationContribuableMessageHandler {

	private static Logger LOGGER = Logger.getLogger(IdentificationContribuableMessageHandlerImpl.class);

	private String outputQueue;
	private EsbMessageFactory esbMessageFactory;
	private HibernateTemplate hibernateTemplate;
	private DemandeHandler demandeHandler;

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
	 * {@inheritDoc}
	 */
	public void setDemandeHandler(DemandeHandler handler) {
		this.demandeHandler = handler;
	}

	@Override
	public void onEsbMessage(EsbMessage msg) throws Exception {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("message=" + msg.getBodyAsString());
		}

		// Parse le message sous forme XML
		IdentificationCTBDocument doc = IdentificationCTBDocument.Factory.parse(msg.getBodyAsString());

		// Valide le bousin
		XmlOptions validateOptions = new XmlOptions();
		ArrayList<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!doc.validate(validateOptions)) {
			StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append("\n");
				builder.append("Message: ").append(error.getErrorCode()).append(" ").append(error.getMessage()).append("\n");
				builder.append("Location of invalid XML: ").append(error.getCursorLocation().xmlText()).append("\n");
			}
			throw new RuntimeException(builder.toString());
		}

		// Handle le message
		final IdentificationContribuable message = XmlEntityAdapter.xml2entity(doc.getIdentificationCTB());
		final EsbHeader header = new EsbHeader();
		header.setBusinessUser(msg.getBusinessUser());
		header.setBusinessId(msg.getBusinessId());
		header.setReplyTo(msg.getServiceReplyTo());
		message.setHeader(header);

		Assert.notNull(demandeHandler, "Le handler de demandes n'est pas défini");
		try {
			AuthenticationHelper.pushPrincipal("JMS-EvtIdentCtb(" + msg.getMessageId() + ")");
			demandeHandler.handleDemande(message);
			hibernateTemplate.flush(); // on s'assure que la session soit flushée avant de resetter l'authentification
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	/**
	 * @see ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableMessageHandler#sendReponse(ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable)
	 */
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
		m.setDomain("fiscalite"); // selon mail de Giorgio du 08.09.2009
		m.setContext("identificationContribuable");
		m.setApplication("unireg");
		final Node node = identificationCtb.newDomNode();
		m.setBody((Document) node);

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		getEsbTemplate().send(m);
	}

}
