package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.tutelle.Tutelle;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;

/**
 * Adapter pour l'annulation de levée de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationLeveeTutelle extends EvenementCivilInterne {

	protected AnnulationLeveeTutelle(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationLeveeTutelle(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// Récupération du tiers habitant correspondant au pupille
		long numeroIndividu = getNoIndividu();
		PersonnePhysique pupille = getPersonnePhysiqueOrThrowException(numeroIndividu);

		// Récupération du rapport entre tiers (de type tutelle)
		RapportEntreTiers rapportEntreTiers = Tutelle.getRapportTutelleOuvert(pupille, getDate());
		if (rapportEntreTiers == null) {
			throw new EvenementCivilException("L'individu " + numeroIndividu + " n'a aucun rapport de type tutelle");
		}

		// Remise à null de la date de fin de tutelle
		rapportEntreTiers.setDateFin(null);
		return null;
	}
}
