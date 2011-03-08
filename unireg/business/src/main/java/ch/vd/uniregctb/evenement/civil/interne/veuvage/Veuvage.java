package ch.vd.uniregctb.evenement.civil.interne.veuvage;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter de l'événement de veuvage.
 * 
 * @author Pavel BLANCO
 *
 */
public class Veuvage extends EvenementCivilInterne {

	protected Veuvage(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Veuvage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.VEUVAGE, date, numeroOfsCommuneAnnonce, context);
	}

	public RegDate getDateVeuvage() {
		return getDate();
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

		final Individu individu = getIndividu();

		// [UNIREG-2241] au traitement d'un événement civil de veuvage, on doit contrôler l'état civil de l'individu
		final EtatCivil etatCivil = individu.getEtatCivil(getDate());
		if (etatCivil == null || etatCivil.getTypeEtatCivil() != TypeEtatCivil.VEUF) {
			erreurs.add(
					new EvenementCivilExterneErreur(String.format("L'individu %d n'est pas veuf dans le civil au %s", individu.getNoTechnique(), RegDateHelper.dateToDisplayString(getDate()))));
		}
		else {
			final long numeroIndividu = individu.getNoTechnique();
			final PersonnePhysique veuf = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(numeroIndividu);

			/*
			 * Validations métier
			 */
			final ValidationResults validationResults = context.getMetierService().validateVeuvage(veuf, getDate());
			addValidationResults(erreurs, warnings, validationResults);
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		/*
		 * Obtention du tiers correspondant au veuf.
		 */
		PersonnePhysique veuf = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());

		/*
		 * Traitement de l'événement
		 */
		context.getMetierService().veuvage(veuf, getDateVeuvage(), null, getNumeroEvenement());
		return null;
	}
}
