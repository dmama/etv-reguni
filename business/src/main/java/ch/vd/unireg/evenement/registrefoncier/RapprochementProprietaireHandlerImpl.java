package ch.vd.unireg.evenement.registrefoncier;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.registrefoncier.RapprochementRF;
import ch.vd.unireg.registrefoncier.TiersRF;
import ch.vd.unireg.registrefoncier.dao.RapprochementRFDAO;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.type.TypeRapprochementRF;

public class RapprochementProprietaireHandlerImpl implements RapprochementProprietaireHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RapprochementProprietaireHandlerImpl.class);

	private HibernateTemplate hibernateTemplate;
	private RapprochementRFDAO rapprochementRFDAO;
	private EvenementFiscalService evenementFiscalService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setRapprochementRFDAO(RapprochementRFDAO rapprochementRFDAO) {
		this.rapprochementRFDAO = rapprochementRFDAO;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	@Override
	public void addRapprochement(long idContribuable, long idTiersRF) throws EsbBusinessException {

		final TiersRF tiersRF = hibernateTemplate.get(TiersRF.class, idTiersRF);
		if (tiersRF == null) {
			LOGGER.error(String.format("TiersRF inconnu avec l'identifiant %d", idTiersRF));
			throw new EsbBusinessException(EsbBusinessCode.IDENTIFICATION_DONNEES_INVALIDES, "Pas de tiers RF connu avec l'identifiant donné.", null);
		}

		final Contribuable contribuable = hibernateTemplate.get(Contribuable.class, idContribuable);
		if (contribuable == null) {
			LOGGER.error(String.format("Pas de contribuable avec l'identifiant %d", idContribuable));
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

		for (DateRange range : periodesLibres) {
			final RapprochementRF rapprochement = new RapprochementRF();
			rapprochement.setDateDebut(range.getDateDebut());
			rapprochement.setDateFin(range.getDateFin());
			rapprochement.setTiersRF(tiersRF);
			rapprochement.setContribuable(contribuable);
			rapprochement.setTypeRapprochement(TypeRapprochementRF.MANUEL);

			final RapprochementRF persisted = hibernateTemplate.merge(rapprochement);
			contribuable.addRapprochementRF(persisted);

			// on publie l'événement fiscal correspondant
			evenementFiscalService.publierDebutRapprochementTiersRF(rapprochement.getDateDebut(), persisted);

			LOGGER.info(String.format("Généré rapprochement manuel entre le contribuable %s et le tiers RF %d (numéro RF %d) pour la période %s.",
			                          FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()),
			                          idTiersRF,
			                          tiersRF.getNoRF(),
			                          DateRangeHelper.toDisplayString(rapprochement)));
		}
	}
}
