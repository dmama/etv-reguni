package ch.vd.uniregctb.evenement;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public interface EvenementCivil {

	/**
	 * @return the type
	 */
	TypeEvenementCivil getType();

	/**
	 * @return the date
	 */
	RegDate getDate();

	/**
	 * @return the numeroEvenement
	 */
	Long getNumeroEvenement();

	/**
	 * @return the numeroOfsCommuneAnnonce
	 */
	Integer getNumeroOfsCommuneAnnonce();

	/**
	 * Renvoie vrai (par défaut) si le contribuable est normalement présent avant le traitement.<br/>Ceci est faux pour le cas d'une naissance par exemple.
	 * @return boolean
	 */
	boolean isContribuablePresentBefore();

	/**
	 * @return le numéro de l'individu principal
	 */
	Long getNoIndividu();

	/**
	 * @return the individu
	 */
	Individu getIndividu();

	/**
	 * @return l'id de la personne physique correspondant à l'individu; ou <b>null</b> si aucune personne physique ne correspond à l'individu.
	 */
	Long getPrincipalPPId();

	/**
	 * @return le numéro d'individu du conjoint
	 */
	Long getNoIndividuConjoint();
	
	/**
	 * @return the conjoint
	 */
	Individu getConjoint();

	/**
	 * @return l'id de la personne physique correspondant au conjoint de l'individu; ou <b>null</b> si aucune personne physique ne correspond.
	 */
	Long getConjointPPId();

	/**
	 * Valide que toutes les données nécessaires sur l'événement sont bien présentes.
	 *
	 * @param erreurs  la liste des erreurs trouvées
	 * @param warnings la liste des warnings trouvés
	 */
	void checkCompleteness(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	/**
	 * Valide que l'événement courant est bien cohérent.
	 *
	 * @param erreurs  la liste des erreurs trouvées
	 * @param warnings la liste des warnings trouvés
	 */
	void validate(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings);

	/**
	 * Traite l'événement courant.
	 *
	 * @param warnings la liste des warnings trouvés
	 * @return une pair contenant les habitants créés par cet événement (respectivement le principal et le conjoint), ou <code>null</code> si aucun n'a été nouvellement créé (ou passé habitant)
	 * @throws ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException
	 *          en cas d'erreur lors de l'exécution.
	 */
	Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException;
}
