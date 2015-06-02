package ch.vd.uniregctb.evenement.reqdes.engine;

import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Interface de base d'un accesseur de valeur d'attribut sur une personne physique
 * @param <T> le type de l'attribut
 */
public interface AttributeAccessor<T> {

	/**
	 * @param pp la personne physique à inspecter
	 * @return la valeur de l'attribut
	 */
	T get(PersonnePhysique pp);

	/**
	 * @param pp la personne physique à modifier
	 * @param value la nouvelle valeur de l'attribut
	 */
	void set(PersonnePhysique pp, T value);

	/**
	 * @return un nom d'affichage pour l'attribut
	 */
	String getAttributeDisplayName();
}
