package ch.vd.uniregctb.evenement.civil.interne.depart;


import ch.vd.uniregctb.evenement.civil.interne.mouvement.Mouvement;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Pays;

/**
 * Modèlise le départ d'un individu
 *
 * @author xsilbn
 *
 */
public interface Depart extends Mouvement {
	/**messages d'erreur
	 *
	 */


	/**
	 * Renvoie l'adresse principale de l'individu après le départ.
	 */
	public abstract Adresse getNouvelleAdressePrincipale();

	/**
	 * Renvoie l'adresse principale de l'individu avant le départ.
	 */
	public abstract Adresse getAncienneAdressePrincipale();

	/**
	 * Renvoie la commune de l'individu après le départ.
	 */
	public abstract CommuneSimple getNouvelleCommunePrincipale();

	/**
	 * Renvoie la commune de l'individu avant le départ.
	 */
	public abstract CommuneSimple getAncienneCommunePrincipale();

	public abstract Adresse getAncienneAdresseCourrier();

	public abstract Adresse getNouvelleAdresseCourrier();
	/**
	 * Renvoie l'adresse secondaire de l'individu avant le départ si elle existe.
	 */
	public abstract Adresse getAncienneAdresseSecondaire();
	/**
	 * Renvoie la commune de l'adresse secondaire de l'individu avant le départ si elle existe.
	 */
	public abstract CommuneSimple getAncienneCommuneSecondaire();

	/**
	 * retourne le pays inconnue
	 * */
	public abstract Pays getPaysInconnu();

	/**
	 *  Indique si l'evenement est un ancien type de départ, c'est à dire qu'on ne reçoit
	 *  qu'un evenement civil pour un couple
	 */

	public abstract boolean isAncienTypeDepart();



}
