package ch.vd.uniregctb.evenement.civil.interne.separation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour la séparation.
 * 
 * @author Pavel BLANCO
 */
public class Separation extends SeparationOuDivorce {

	protected Separation(EvenementCivilExterne evenement, EvenementCivilContext context, SeparationTranslationStrategy handler, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Separation(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.SEPARATION, date, numeroOfsCommuneAnnonce, conjoint, context);
	}
}
