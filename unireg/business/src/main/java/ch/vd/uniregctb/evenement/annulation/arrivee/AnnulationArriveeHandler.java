package ch.vd.uniregctb.evenement.annulation.arrivee;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Gère les événements suivants:
 * <ul>
 * <li>suppression arrivée dans la commune</li>
 * <li>annulation arrivée secondaire</li>
 * </ul>
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationArriveeHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	public void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		// TODO (PBO) SuppressionArriveeHandler.handle
		throw new EvenementCivilHandlerException("Veuillez effectuer cette opération manuellement");
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.SUP_ARRIVEE_DANS_COMMUNE);
		types.add(TypeEvenementCivil.ANNUL_ARRIVEE_SECONDAIRE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new AnnulationArriveeAdapter();
	}
}
