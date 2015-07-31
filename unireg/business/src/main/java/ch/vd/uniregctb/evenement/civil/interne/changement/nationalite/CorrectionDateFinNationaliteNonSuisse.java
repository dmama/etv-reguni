package ch.vd.uniregctb.evenement.civil.interne.changement.nationalite;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

public class CorrectionDateFinNationaliteNonSuisse extends CorrectionDateFinNationalite {

	protected CorrectionDateFinNationaliteNonSuisse(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// rien à faire...
		return HandleStatus.TRAITE;
	}
}
