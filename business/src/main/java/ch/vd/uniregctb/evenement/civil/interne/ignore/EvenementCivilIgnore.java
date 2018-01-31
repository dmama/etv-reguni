package ch.vd.uniregctb.evenement.civil.interne.ignore;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

public class EvenementCivilIgnore extends EvenementCivilInterne {

	public EvenementCivilIgnore(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected EvenementCivilIgnore(EvenementCivilContext context) {
		super(null, null, null, null, context);
	}

	@Override
	public boolean isContribuablePresentBefore() {
		return false;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// pas de validation
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// simplement ignoré
		return HandleStatus.TRAITE;
	}
}
