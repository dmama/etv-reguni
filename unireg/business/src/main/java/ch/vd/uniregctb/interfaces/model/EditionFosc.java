package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

/**
 * Informations permettant d'identifier une édition de la Feuille officielle suisse du commerce.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EditionFosc {

	/**
	 * @return l'année de parution
	 */
	public int getAnnee();

	/**
	 * @return le numéro dans l'année de parution
	 */
	public int getNumero();

	/**
	 * @return la date de parution
	 */
	public RegDate getDateParution();
}
