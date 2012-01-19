package ch.vd.uniregctb.evenement.civil.interne.changement.nationalite;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public abstract class RemiseBlancDateFinNationalite extends EvenementCivilInterne {

	protected RemiseBlancDateFinNationalite(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected RemiseBlancDateFinNationalite(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, boolean suisse, EvenementCivilContext context) {
		super(individu, conjoint, (suisse ? TypeEvenementCivil.ANNUL_DATE_FIN_NATIONALITE_SUISSE : TypeEvenementCivil.ANNUL_DATE_FIN_NATIONALITE_NON_SUISSE), date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		// rien à faire
		return null;
	}
}
