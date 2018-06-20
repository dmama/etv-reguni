package ch.vd.unireg.evenement.entreprise.interne;

import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatEvenementEntreprise;

/**
 * Capping de l'état final de l'événement entreprise à l'état {@link EtatEvenementEntreprise#A_VERIFIER A_VERIFIER}
 */
public class CappingAVerifier extends EvenementEntrepriseInterneDeTraitement {

	public CappingAVerifier(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                        EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(evenement, entrepriseCivile, entreprise, context, options);
	}

	@Override
	public String describe() {
		return null;        // On ne veut pas de message descriptif sur cet événement qui n'en est pas un.
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		// rien de spécial à faire
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		// on émet un warning pour faire passer l'événement dans cet état de toute façon
		// (rien à faire s'il y a déjà des warnings, qui placeraient déjà l'événement dans cet état...)
		if (!warnings.hasWarnings()) {
			warnings.addWarning("Evénement explicitement placé 'à vérifier' par configuration applicative.");
		}
	}
}
