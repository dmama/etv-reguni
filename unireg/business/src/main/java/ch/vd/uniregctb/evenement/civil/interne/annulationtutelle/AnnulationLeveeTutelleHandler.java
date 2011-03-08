package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.tutelle.AbstractTutelleHandler;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitements métier des événements d'annulation de levée de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationLeveeTutelleHandler extends AbstractTutelleHandler {

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ANNUL_LEVEE_TUTELLE);
		return types;
	}

	@Override
	public EvenementCivilInterne createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new AnnulationLeveeTutelle(event, context);
	}

}