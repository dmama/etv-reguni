package ch.vd.uniregctb.evenement.civil.interne.testing;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

public class Testing extends EvenementCivilInterne {

	public static final long NoExceptionDansHandle = 20000L;
	public static final long NoTraiteAvecWarningDansHandle = 20001L;
	public static final long NoRedondantAvecWarningDansHandle = 20002L;
	public static final long NoTraiteSansWarning = 20003L;
	public static final long NoRedondantSansWarning = 20004L;

	protected Testing(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected Testing(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		if (getNumeroEvenement().equals(121L)) {
			// On ne fait rien
		}
		if (getNumeroEvenement().equals(122L)) {
			// a faire
		}
		if (getNumeroEvenement().equals(123L)) {
			// On throw une Exception
			throw new RuntimeException("L'événement n'est pas complet");
		}
		if (getNumeroEvenement().equals(124L)) {
			erreurs.addErreur("Check completeness erreur");
			erreurs.addErreur("Again");
		}
		if (getNumeroEvenement().equals(125L)) {
			warnings.addWarning("Check completeness warn");
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		if (getNumeroEvenement().equals(NoExceptionDansHandle)) {
			throw new EvenementCivilException("Exception de test");
		}
		else if (getNumeroEvenement().equals(NoTraiteAvecWarningDansHandle)) {
			warnings.addWarning("Warning de test");
			return HandleStatus.TRAITE;
		}
		else if (getNumeroEvenement().equals(NoRedondantAvecWarningDansHandle)) {
			warnings.addWarning("Warning de test");
			return HandleStatus.REDONDANT;
		}
		else if (getNumeroEvenement().equals(NoRedondantSansWarning)) {
			return HandleStatus.REDONDANT;
		}
		else {
			return HandleStatus.TRAITE;
		}
	}
}
