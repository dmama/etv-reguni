package ch.vd.uniregctb.evenement.civil.interne.separation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour la séparation.
 * 
 * @author Pavel BLANCO
 */
public class SeparationAdapter extends SeparationOuDivorceAdapter {

	protected SeparationAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, SeparationHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected SeparationAdapter(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.SEPARATION, date, numeroOfsCommuneAnnonce, conjoint, context);
	}
}
