package ch.vd.unireg.evenement.civil.ech;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

/**
 * Façade de consultation / traitement sur les événements civils eCH (= RCPers)
 */
public interface EvenementCivilEchFacade {

	/**
	 * @return le visa de l'utilisateur à la source de l'événement civil
	 */
	String getLogCreationUser();

	/**
	 * @return l'identifiant technique de l'événement civil
	 */
	Long getId();

	/**
	 * Fournit l'identifiant technique de l'événement civil à considérer pour retrouver les données après correction (différent de l'identifiant
	 * de l'événement lui-même ({@link #getId()}) dans le cas d'une grape de corrections traitée en bloc)
	 * @return l'identifiant d'événement à utiliser
	 */
	Long getIdForDataAfterEvent();

	/**
	 * @return l'identifiant technique de l'événement civil référencé par celui-ci, obligatoire dans le cas d'une annulation ou d'une correction
	 */
	Long getRefMessageId();

	/**
	 * @param refMessageId l'identifiant technique de l'événement civil référencé (cas de rattrapage)
	 */
	void setRefMessageId(Long refMessageId);

	/**
	 * @return le type de l'événement civil
	 */
	TypeEvenementCivilEch getType();

	/**
	 * @return l'action de l'événement civil
	 */
	ActionEvenementCivilEch getAction();

	/**
	 * @return l'état de traitement de l'événement civil
	 */
	EtatEvenementCivil getEtat();

	/**
	 * @return la date de validité de l'événement
	 */
	RegDate getDateEvenement();

	/**
	 * @return le numéro de l'individu concerné par cet événement
	 */
	Long getNumeroIndividu();

	/**
	 * @return le commentaire de traitement associé à l'événement civil
	 */
	String getCommentaireTraitement();

	/**
	 * @param commentaire nouveau commentaire à assigner à l'événement civil
	 */
	void setCommentaireTraitement(String commentaire);

	/**
	 * @return les erreurs/avertissements associés à l'événement civil lors d'un traitement (ou d'une tentative de traitement) précédent
	 */
	Set<EvenementCivilEchErreur> getErreurs();
}
