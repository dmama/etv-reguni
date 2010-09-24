package ch.vd.uniregctb.evenement.divorce;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.separation.SeparationOuDivorceHandler;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitement métier des événements divorce.
 * 
 * @author Pavel BLANCO
 *
 */
public class DivorceHandler extends SeparationOuDivorceHandler {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.DIVORCE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new DivorceAdapter();
	}
}
