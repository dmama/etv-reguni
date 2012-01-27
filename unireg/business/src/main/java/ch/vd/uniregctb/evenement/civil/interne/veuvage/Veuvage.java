package ch.vd.uniregctb.evenement.civil.interne.veuvage;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Adapter de l'événement de veuvage.
 * 
 * @author Pavel BLANCO
 *
 */
public class Veuvage extends EvenementCivilInterne {

	protected Veuvage(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Veuvage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	public RegDate getDateVeuvage() {
		return getDate();
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final Individu individu = getIndividu();

		// [UNIREG-2241] au traitement d'un événement civil de veuvage, on doit contrôler l'état civil de l'individu
		final EtatCivil etatCivil = individu.getEtatCivil(getDate());
		if (etatCivil == null || etatCivil.getTypeEtatCivil() != TypeEtatCivil.VEUF) {
			erreurs.addErreur(String.format("L'individu %d n'est pas veuf dans le civil au %s", individu.getNoTechnique(), RegDateHelper.dateToDisplayString(getDate())));
		}
		else {
			final PersonnePhysique veuf = getPrincipalPP();

			/*
			 * Validations métier
			 */
			final ValidationResults validationResults = context.getMetierService().validateVeuvage(veuf, getDate());
			addValidationResults(erreurs, warnings, validationResults);
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		/*
		 * Obtention du tiers correspondant au veuf.
		 */
		PersonnePhysique veuf = getPrincipalPP();

		/*
		 * Traitement de l'événement
		 */
		try {
			context.getMetierService().veuvage(veuf, getDateVeuvage(), null, getNumeroEvenement());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return HandleStatus.TRAITE;
	}
}
