package ch.vd.uniregctb.evenement.civil.interne.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class VeuvageFromEch extends Veuvage {

	public VeuvageFromEch(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final Individu individu = getIndividu();

		// [UNIREG-2241] au traitement d'un événement civil de veuvage, on doit contrôler l'état civil de l'individu
		final RegDate dateVeuvage = getDate();
		final EtatCivil etatCivil = individu.getEtatCivil(dateVeuvage);
		if (etatCivil == null || !EtatCivilHelper.estVeuf(etatCivil)) {
			erreurs.addErreur(String.format("L'individu %d n'est pas veuf dans le civil au %s", individu.getNoTechnique(), RegDateHelper.dateToDisplayString(dateVeuvage)));
		}
		else {
			final PersonnePhysique veuf = getPrincipalPP();

			/*
			 * Validations métier
			 */

			final EnsembleTiersCouple couple = context.getTiersService().getEnsembleTiersCouple(veuf, dateVeuvage);
			if (couple == null) {
				erreurs.addErreur(String.format("Aucun ménage commun trouvé pour la personne physique %s valide à la date du veuvage (%s)",
				                                FormatNumeroHelper.numeroCTBToDisplay(veuf.getNumero()), RegDateHelper.dateToDisplayString(dateVeuvage)));
			}
			else {
				final ValidationResults validationResults = new ValidationResults();
				context.getMetierService().validateForOfVeuvage(veuf, dateVeuvage, couple, validationResults);
				addValidationResults(erreurs, warnings, validationResults);
			}
		}
	}
}
