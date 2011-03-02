package ch.vd.uniregctb.evenement.civil.interne.veuvage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitement métier pour événements de veuvage.
 *
 * @author Pavel BLANCO
 *
 */
public class VeuvageHandler extends EvenementCivilHandlerBase {

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

		final Veuvage veuvage = (Veuvage) target;

		final Individu individu = veuvage.getIndividu();

		// [UNIREG-2241] au traitement d'un événement civil de veuvage, on doit contrôler l'état civil de l'individu
		final EtatCivil etatCivil = individu.getEtatCivil(veuvage.getDate());
		if (etatCivil == null || etatCivil.getTypeEtatCivil() != TypeEtatCivil.VEUF) {
			erreurs.add(new EvenementCivilExterneErreur(String.format("L'individu %d n'est pas veuf dans le civil au %s", individu.getNoTechnique(), RegDateHelper.dateToDisplayString(veuvage.getDate()))));
		}
		else {
			final long numeroIndividu = individu.getNoTechnique();
			final PersonnePhysique veuf = getService().getPersonnePhysiqueByNumeroIndividu(numeroIndividu);

			/*
			 * Validations métier
			 */
			final ValidationResults validationResults = getMetier().validateVeuvage(veuf, veuvage.getDate());
			addValidationResults(erreurs, warnings, validationResults);
		}
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		/*
		 * Cas de veuvage
		 */
		Veuvage veuvage = (Veuvage) evenement;

		/*
		 * Obtention du tiers correspondant au veuf.
		 */
		PersonnePhysique veuf = getService().getPersonnePhysiqueByNumeroIndividu(veuvage.getNoIndividu());

		/*
		 * Traitement de l'événement
		 */
		getMetier().veuvage(veuf, veuvage.getDateVeuvage(), null, veuvage.getNumeroEvenement());
		return null;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new VeuvageAdapter(event, context, this);
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.VEUVAGE);
		return types;
	}

}
