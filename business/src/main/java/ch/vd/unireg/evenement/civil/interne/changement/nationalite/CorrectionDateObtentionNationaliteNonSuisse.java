package ch.vd.unireg.evenement.civil.interne.changement.nationalite;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

public class CorrectionDateObtentionNationaliteNonSuisse extends CorrectionDateObtentionNationalite {

	protected CorrectionDateObtentionNationaliteNonSuisse(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// rien à faire
		return HandleStatus.TRAITE;
	}
}
