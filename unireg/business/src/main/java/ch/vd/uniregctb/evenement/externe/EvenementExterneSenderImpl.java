package ch.vd.uniregctb.evenement.externe;


import ch.vd.fiscalite.taxation.evtQuittanceListeV1.EvtQuittanceListeDocument;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.common.XmlUtils;

/**
 * Bean qui envoie des événements externes JMS.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementExterneSenderImpl implements EvenementExterneSender {

	//private static Logger LOGGER = Logger.getLogger(EvenementExterneSenderImpl.class);

	private String outputQueue;
	private EsbJmsTemplate esbTemplate;
	private EsbMessageFactory esbMessageFactory;
	private String serviceDestination;
	private String businessUser;

	/**
	 * for testing purpose
	 */
	@SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
	protected void setOutputQueue(String outputQueue) {
		this.outputQueue = outputQueue;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setBusinessUser(String businessUser) {
		this.businessUser = businessUser;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	/**
	 * Envoie un événement.
	 *
	 * @param businessId l'id business du message 
	 * @param document le message
	 * @throws Exception exception.
	 */
	public void sendEvent(String businessId, EvtQuittanceListeDocument document) throws Exception {

		final EsbMessage m = esbMessageFactory.createMessage();
		m.setBusinessId(String.valueOf(businessId));
		m.setBusinessUser(businessUser);
		m.setServiceDestination(serviceDestination);
		m.setContext("evenementExterne");
		m.setBody(XmlUtils.xmlbeans2string(document));

		if (outputQueue != null) {
			m.setServiceDestination(outputQueue); // for testing only
		}
		esbTemplate.send(m);
	}
}