package ch.vd.uniregctb.evenement.civil.interne.changement.nationalite;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.CivilHandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

public class CorrectionDateFinNationaliteSuisse extends CorrectionDateFinNationalite {

	protected CorrectionDateFinNationaliteSuisse(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	@NotNull
	@Override
	public CivilHandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		throw new EvenementCivilException("Veuillez effectuer cette opération manuellement");
	}
}
