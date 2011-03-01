package ch.vd.uniregctb.evenement.veuvage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
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

	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

		final Veuvage veuvage = (Veuvage) target;

		final Individu individu = veuvage.getIndividu();

		// [UNIREG-2241] au traitement d'un événement civil de veuvage, on doit contrôler l'état civil de l'individu
		final EtatCivil etatCivil = individu.getEtatCivil(veuvage.getDate());
		if (etatCivil == null || etatCivil.getTypeEtatCivil() != TypeEtatCivil.VEUF) {
			erreurs.add(new EvenementCivilErreur(String.format("L'individu %d n'est pas veuf dans le civil au %s", individu.getNoTechnique(), RegDateHelper.dateToDisplayString(veuvage.getDate()))));
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

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
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
	public GenericEvenementAdapter createAdapter(EvenementCivilData event, EvenementCivilContext context) throws EvenementAdapterException {
		return new VeuvageAdapter(event, context, this);
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.VEUVAGE);
		return types;
	}

}
