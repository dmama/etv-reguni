package ch.vd.uniregctb.evenement.arrivee;


import ch.vd.uniregctb.evenement.Mouvement;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;

public interface Arrivee extends Mouvement {

	/**
	 * @return l'adresse principale de l'individu avant le déménagement.
	 */
	public abstract Adresse getAncienneAdressePrincipale();

	/**
	 * @return l'adresse secondaire de l'individu avant le déménagement.
	 */
	public abstract Adresse getAncienneAdresseSecondaire();

	/**
	 * @return la commune d'arrivée.
	 */
	public abstract Commune getAncienneCommunePrincipale();

	/**
	 * @return la commune d'arrivée.
	 */
	public abstract Commune getAncienneCommuneSecondaire();

	/**
	 * @return l'adresse principale de l'individu après le déménagement.
	 */
	public abstract Adresse getNouvelleAdressePrincipale();

	/**
	 * @return l'adresse secondaire de l'individu après le déménagement.
	 */
	public abstract Adresse getNouvelleAdresseSecondaire();

	/**
	 * @return la commune d'arrivée.
	 */
	public abstract Commune getNouvelleCommunePrincipale();

	/**
	 * @return la commune d'arrivée.
	 */
	public abstract Commune getNouvelleCommuneSecondaire();
}
