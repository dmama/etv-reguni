package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.fiscalite.registre.identificationContribuable.IdentificationCTBDocument;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.Reponse;
import ch.vd.uniregctb.evenement.identification.contribuable.XmlEntityAdapter;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.registrefoncier.RapprochementManuelTiersRFService;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.dao.RapprochementRFDAO;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeRapprochementRF;

/**
 * Handler qui reçoit les réponses d'identification manuelle des rapprochements propriétaire
 */
public class EvenementIdentificationRapprochementProprietaireEsbHandler implements EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementIdentificationRapprochementProprietaireEsbHandler.class);

	private HibernateTemplate hibernateTemplate;
	private RapprochementRFDAO rapprochementRFDAO;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setRapprochementRFDAO(RapprochementRFDAO rapprochementRFDAO) {
		this.rapprochementRFDAO = rapprochementRFDAO;
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
			final TiersRF tiersRF = hibernateTemplate.get(TiersRF.class, idTiersRF);
			if (tiersRF == null) {
				LOGGER.error(String.format("TiersRF inconnu avec l'identifiant %d", idTiersRF));
				throw new EsbBusinessException(EsbBusinessCode.IDENTIFICATION_DONNEES_INVALIDES, "Pas de tiers RF connu avec l'identifiant donné.", null);
			}

			final Contribuable contribuable = hibernateTemplate.get(Contribuable.class, reponse.getNoContribuable());
			if (contribuable == null) {
				LOGGER.error(String.format("Pas de contribuable avec l'identifiant %d", reponse.getNoContribuable()));
				throw new EsbBusinessException(EsbBusinessCode.IDENTIFICATION_DONNEES_INVALIDES, "Pas de contribuable connu avec le numéro annoncé.", null);
			}

			// on crée maintenant le rapprochement qui va bien

			// recherche des plages de valeur disponibles
			final List<DateRange> periodesCouvertes = DateRangeHelper.merge(rapprochementRFDAO.findByTiersRF(idTiersRF, false).stream()
					                                                                .filter(AnnulableHelper::nonAnnule)
					                                                                .sorted(DateRangeComparator::compareRanges)
					                                                                .collect(Collectors.toList()));
			final DateRange eternity = new DateRangeHelper.Range(null, null);
			final List<DateRange> periodesLibres = DateRangeHelper.subtract(eternity, periodesCouvertes);
			if (periodesLibres.isEmpty()) {
				LOGGER.error(String.format("Le tiersRF %d n'a aucune période libre pour créer un nouveau rapprochement avec le contribuable %s", idTiersRF, FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero())));
				throw new EsbBusinessException(EsbBusinessCode.IDENTIFICATION_DONNEES_INVALIDES, "Le tiers RF indiqué n'a plus de période disponible pour un nouveau rapprochement.", null);
			}

			AuthenticationHelper.pushPrincipal(message.getBusinessUser());
			try {
				for (DateRange range : periodesLibres) {
					final RapprochementRF rapprochement = new RapprochementRF();
					rapprochement.setDateDebut(range.getDateDebut());
					rapprochement.setDateFin(range.getDateFin());
					rapprochement.setTiersRF(tiersRF);
					rapprochement.setContribuable(contribuable);
					rapprochement.setTypeRapprochement(TypeRapprochementRF.MANUEL);

					contribuable.addRapprochementRF(hibernateTemplate.merge(rapprochement));

					LOGGER.info(String.format("Généré rapprochement manuel entre le contribuable %s et le tiers RF %d (numéro RF %d) pour la période %s.",
					                          FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()),
					                          idTiersRF,
					                          tiersRF.getNoRF(),
					                          DateRangeHelper.toDisplayString(rapprochement)));
				}
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	private long extractIdTiersRF(EsbMessage message) throws EsbBusinessException {
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
