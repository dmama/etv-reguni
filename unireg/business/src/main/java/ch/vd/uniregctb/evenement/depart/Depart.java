package ch.vd.uniregctb.evenement.depart;


import ch.vd.uniregctb.evenement.Mouvement;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
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
	public abstract Commune getNouvelleCommunePrincipale();

	/**
	 * Renvoie la commune de l'individu avant le départ.
	 */
	public abstract Commune getAncienneCommunePrincipale();

	public abstract Adresse getAncienneAdresseCourrier();

	public abstract Adresse getNouvelleAdresseCourrier();
	/**
	 * Renvoie l'adresse secondaire de l'individu avant le départ si elle existe.
	 */
	public abstract Adresse getAncienneAdresseSecondaire();
	/**
	 * Renvoie la commune de l'adresse secondaire de l'individu avant le départ si elle existe.
	 */
	public abstract Commune getAncienneCommuneSecondaire();

	/**
	 * retourne le pays inconnue
	 * */
	public abstract Pays getPaysInconnu();



}
