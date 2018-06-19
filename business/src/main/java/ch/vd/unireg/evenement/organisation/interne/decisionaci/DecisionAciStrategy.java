package ch.vd.unireg.evenement.organisation.interne.decisionaci;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Evénements portant sur une entreprise faisant l'objet d'une décision ACI à la
 * date de l'événement.
 *
 * @author Raphaël Marmier, 2016-02-22.
 */
public class DecisionAciStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public DecisionAciStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {
		if (entreprise == null) {
			return null;
		}

		final RegDate dateApres = event.getDateEvenement();

		final boolean hasDecisionAci = context.getTiersService().hasDecisionAciValidAt(entreprise.getNumero(), dateApres);
		if (hasDecisionAci) {
			final String message = String.format("%s est sous le coup d'une décision ACI. Cet événement doit être traité à la main.", entreprise);
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
		}

		return null;
	}
}
