package ch.vd.unireg.evenement.civil.interne.fin.nationalite;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

public class FinNationaliteSuisse extends FinNationalite {

	protected FinNationaliteSuisse(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected FinNationaliteSuisse(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		Audit.info(getNumeroEvenement(), "Nationalité suisse : passage en traitement manuel");
		erreurs.addErreur("La fin de la nationalité suisse doit être traitée manuellement");
	}
}
