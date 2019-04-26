package ch.vd.unireg.evenement.entreprise.interne;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Evénement de réindexation à créer lorsqu'aucune autre opération n'est effectuée dans le cadre du traitement.
 * Cet événement rapporte cet état de fait dans le commentaire de traitement et renvoie une statut TRAITE.
 *
 * @author Raphaël Marmier, 2015-09-04
 */
public class IndexationPure extends Indexation {

	private static final String MESSAGE_PAS_INDEXEE = "Événement traité sans impact Unireg. L'entité visée n'est pas significative pour Unireg. Pas d'indexation.";
	private static final String MESSAGE_INDEXATION_PURE = "Événement traité sans impact Unireg. L'entité a été réindexée.";

	public IndexationPure(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                      EvenementEntrepriseContext context, EvenementEntrepriseOptions options) throws
			EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		super.doHandle(warnings, suivis);
		if (getEntreprise() != null) {
			if (!StringUtils.isBlank(event.getCommentaireTraitement())) {
				event.setCommentaireTraitement(event.getCommentaireTraitement() + " " + MESSAGE_INDEXATION_PURE);
			}
			else {
				event.setCommentaireTraitement(MESSAGE_INDEXATION_PURE);
			}
			getContext().audit.info(event.getNoEvenement(), MESSAGE_INDEXATION_PURE);
		} else {
			getContext().audit.info(event.getNoEvenement(), MESSAGE_PAS_INDEXEE);
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		super.validateSpecific(erreurs, warnings, suivis);
		// rien à valider
	}
}
