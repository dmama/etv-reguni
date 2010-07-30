package ch.vd.uniregctb.evenement.common;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.tiers.PersonnePhysique;

import java.util.List;

/**
 * Classe de base des handlers d'événements civils qui ne nous intéressent pas (complètement ignorés)
 */
public abstract class EvenementCivilIgnoreHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// pas de validation
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// pas de validation
	}

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		// simplement ignoré
		return null;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new EvenementCivilIgnoreAdapter();
	}
}
