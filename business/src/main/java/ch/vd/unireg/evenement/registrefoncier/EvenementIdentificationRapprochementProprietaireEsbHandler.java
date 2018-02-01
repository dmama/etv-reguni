package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.Reponse;
import ch.vd.uniregctb.evenement.identification.contribuable.XmlEntityAdapter;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.registrefoncier.RapprochementManuelTiersRFService;

/**
 * Handler qui reçoit les réponses d'identification manuelle des rapprochements propriétaire
 */
public class EvenementIdentificationRapprochementProprietaireEsbHandler implements EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementIdentificationRapprochementProprietaireEsbHandler.class);

	private HibernateTemplate hibernateTemplate;
	private RapprochementProprietaireHandler handler;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setHandler(RapprochementProprietaireHandler handler) {
		this.handler = handler;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Arrivée d'une réponse d'identification manuelle de rapprochement propriétaire (BusinessID='%s')", message.getBusinessId()));
		}

		final IdentificationCTBDocument doc = IdentificationCTBDocument.Factory.parse(message.getBodyAsString());

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
			final IdentificationContribuable identification = XmlEntityAdapter.xml2entity(doc.getIdentificationCTB());
			if (identification == null || identification.getReponse() == null || identification.getReponse().getNoContribuable() == null) {
				LOGGER.error("Réponse d'identification non-positive reçue, apparemment...");
				throw new EsbBusinessException(EsbBusinessCode.MESSAGE_NON_SUPPORTE, "Seuls sont supportés ici les réponses positives à une identification manuelle de rapprochement propriétaire", null);
			}

			final Reponse reponse = identification.getReponse();
			final long idTiersRF = extractIdTiersRF(message);

			// on délègue le vrai boulot plus loin
			AuthenticationHelper.pushPrincipal(message.getBusinessUser());
			try {
				handler.addRapprochement(reponse.getNoContribuable(), idTiersRF);

				// flush de la session (pour les données de LOG_*USER assignées dans le contexte du principal
				hibernateTemplate.flush();
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	private static long extractIdTiersRF(EsbMessage message) throws EsbBusinessException {
		final String strIdTiersRF = message.getHeader(RapprochementManuelTiersRFService.ID_TIERS_RF);
		if (strIdTiersRF == null || !StringUtils.isNumeric(strIdTiersRF)) {
			throw new EsbBusinessException(EsbBusinessCode.MESSAGE_NON_SUPPORTE, "L'attribut " + RapprochementManuelTiersRFService.ID_TIERS_RF + " du header est absent ou invalide", null);
		}

		try {
			return Long.parseLong(strIdTiersRF);
		}
		catch (NumberFormatException e) {
			throw new EsbBusinessException(EsbBusinessCode.MESSAGE_NON_SUPPORTE, "L'attribut " + RapprochementManuelTiersRFService.ID_TIERS_RF + " du header est invalide", e);
		}
	}
}
