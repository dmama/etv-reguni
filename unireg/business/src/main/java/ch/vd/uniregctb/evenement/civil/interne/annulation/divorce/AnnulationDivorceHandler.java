package ch.vd.uniregctb.evenement.civil.interne.annulation.divorce;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.annulation.separation.AnnulationSeparationOuDivorceHandler;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitements métier pour événements d'annulation de divorce.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationDivorceHandler extends AnnulationSeparationOuDivorceHandler {

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}
	
	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ANNUL_DIVORCE);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new AnnulationDivorceAdapter(event, context, this);
	}

}
