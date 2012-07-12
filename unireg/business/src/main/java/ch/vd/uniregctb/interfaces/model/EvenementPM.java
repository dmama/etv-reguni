package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

import java.util.Date;

/**
 * Evénement d'une personne morale.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EvenementPM {

	/**
	 * @return la date de l'événement
	 */
	RegDate getDate();

	/**
	 * @return le numéro de la PM
	 */
	Long getNumeroPM();

	/**
	 * @return le code de l'événement.
	 */
	String getCode();
}
