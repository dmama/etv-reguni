package ch.vd.unireg.evenement.organisation.interne;

import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
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
