package ch.vd.uniregctb.evenement.civil.interne.changement.nationalite;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.Individu;

public class RemiseBlancDateFinNationaliteSuisse extends RemiseBlancDateFinNationalite {

	protected RemiseBlancDateFinNationaliteSuisse(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour les tests seulement
	 */
	@SuppressWarnings({"JavaDoc"})
	protected RemiseBlancDateFinNationaliteSuisse(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, true, context);
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		Audit.info(getNumeroEvenement(), "Remise à blanc de la date de fin de la nationalité suisse : passage en traitement manuel");
		erreurs.add(new EvenementCivilExterneErreur("La remise à blanc de la date de fin de la nationalité suisse doit être traitée manuellement"));
	}
}
