package ch.vd.uniregctb.evenement.organisation;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * Façade de consultation / traitement sur les événements Organisation (= RCEnt)
 */
public interface EvenementOrganisationFacade {

	/**
	 * @return l'identifiant technique de l'événement organisation
	 */
	long getId();

	/**
	 * @return le type de l'événement organisation
	 */
	TypeEvenementOrganisation getType();

	/**
	 * @return l'état de traitement de l'événement organisation
	 */
	EtatEvenementOrganisation getEtat();

	/**
	 * @return la date de validité de l'événement
	 */
	RegDate getDateEvenement();

	/**
	 * @return le numéro de l'individu concerné par cet événement
	 */
	long getNoOrganisation();

	/**
	 * @return le commentaire de traitement associé à l'événement organisation
	 */
	String getCommentaireTraitement();

	/**
	 * @param commentaire nouveau commentaire à assigner à l'événement organisation
	 */
	void setCommentaireTraitement(String commentaire);

	/**
	 * @return les erreurs/avertissements associés à l'événement organisation lors d'un traitement (ou d'une tentative de traitement) précédent
	 */
	Set<EvenementOrganisationErreur> getErreurs();
}
