package ch.vd.unireg.evenement.organisation.interne;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
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

	public IndexationPure(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                      EvenementOrganisationContext context, EvenementOrganisationOptions options) throws
			EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.doHandle(warnings, suivis);
		if (getEntreprise() != null) {
			if (!StringUtils.isBlank(event.getCommentaireTraitement())) {
				event.setCommentaireTraitement(event.getCommentaireTraitement() + " " + MESSAGE_INDEXATION_PURE);
			}
			else {
				event.setCommentaireTraitement(MESSAGE_INDEXATION_PURE);
			}
			Audit.info(event.getNoEvenement(), MESSAGE_INDEXATION_PURE);
		} else {
			Audit.info(event.getNoEvenement(), MESSAGE_PAS_INDEXEE);
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings, suivis);
		// rien à valider
	}
}
