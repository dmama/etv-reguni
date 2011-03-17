package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

public interface Origine {

	/**
	 * Retourne la commune de l'origine. Cette attribut peut être <code>null</code> si l'origine n'est pas suisse.
	 *
	 * @return la commune de l'origine.
	 */
	Commune getCommune();

	/**
	 * Retourne la date de début de validité de l'origine.
	 *
	 * @return la date de début de validité de l'origine.
	 */
	RegDate getDebutValidite();

	/**
	 * Retourne le pays de l'origine. Cette attribut peut être <code>null</code> si l'origine est suisse.
	 *
	 * @return le pays de l'origine.
	 */
	Pays getPays();
}
