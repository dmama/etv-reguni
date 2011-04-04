package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.tutelle.Tutelle;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour l'annulation de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationTutelle extends EvenementCivilInterne {

	protected AnnulationTutelle(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationTutelle(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.ANNUL_MESURE_TUTELLE, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		// Récupération du tiers habitant correspondant au pupille
		long numeroIndividu = getNoIndividu();
		PersonnePhysique pupille = getPersonnePhysiqueOrThrowException(numeroIndividu);

		// Récupération du rapport entre tiers (de type tutelle)
		RapportEntreTiers rapportEntreTiers = Tutelle.getRapportTutelleOuvert(pupille, getDate());
		if (rapportEntreTiers == null) {
			throw new EvenementCivilHandlerException("L'individu " + numeroIndividu + " n'a aucun rapport de type tutelle");
		}

		// Annulation du rapport
		rapportEntreTiers.setAnnule(true);
		return null;
	}
}
