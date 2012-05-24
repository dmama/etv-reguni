package ch.vd.uniregctb.evenement.civil.interne.changement.dateEtatCivil;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

public class CorrectionDateEtatCivil extends EvenementCivilInterne {

	protected CorrectionDateEtatCivil(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// TODO (PBO) CorrectionDateEtatCivilTranslationStrategy.validateSpecific
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// TODO (PBO) CorrectionDateEtatCivilTranslationStrategy.handle
		throw new EvenementCivilException("Veuillez effectuer cette opération manuellement");
	}
}
