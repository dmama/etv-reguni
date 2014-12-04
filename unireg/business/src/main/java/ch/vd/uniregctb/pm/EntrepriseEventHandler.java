package ch.vd.uniregctb.pm;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.fiscalite.registre.entrepriseEvent.EvtEntrepriseDocument;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Bean qui écoute les messages JMS en provenance du registre des entreprises (PMs).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EntrepriseEventHandler implements EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntrepriseEventHandler.class);

	private GlobalTiersIndexer indexer;
	private DataEventService dataEventService;
	private HibernateTemplate hibernateTemplate;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void onEsbMessage(EsbMessage msg) throws Exception {

		// Parse le message sous forme XML
		final XmlObject doc = XmlObject.Factory.parse(msg.getBodyAsString());

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

			// Traite le message
			AuthenticationHelper.pushPrincipal("JMS-PmEvent(" + msg.getMessageId() + ')');
			try {

				if (doc instanceof EvtEntrepriseDocument) {
					onEvtEntreprise((EvtEntrepriseDocument) doc);
				}
				else {
					LOGGER.error("Type de message inconnu : " + doc.getClass().getName());
				}
			}
			catch (Exception e) {
				LOGGER.error("Erreur lors de la réception du message n°" + msg.getMessageId(), e);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	private void onEvtEntreprise(EvtEntrepriseDocument doc) {
		final EvtEntrepriseDocument.EvtEntreprise event = doc.getEvtEntreprise();
		final int entrepriseId = event.getNoEntreprise();
		onEvtEntreprise(entrepriseId);
	}

	protected void onEvtEntreprise(long entrepriseId) {
		LOGGER.info(String.format("Arrivée d'un événement sur la PM n°%d", entrepriseId));

		// [SIFISC-1526] création de la coquille PM vide, si nécessaire
		if (hibernateTemplate.get(Entreprise.class, entrepriseId) == null) {
			hibernateTemplate.merge(new Entreprise(entrepriseId));
		}

		dataEventService.onPersonneMoraleChange(entrepriseId);
		indexer.schedule(entrepriseId);
	}
}
