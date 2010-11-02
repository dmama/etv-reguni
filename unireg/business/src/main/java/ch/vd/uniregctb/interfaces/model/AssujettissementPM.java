package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface AssujettissementPM extends DateRange {

	public enum Type {
		LIFD,
		LILIC
	}

	/**
	 * @return la date de début de l'assujettissement.
	 */
	RegDate getDateDebut();

	/**
	 * @return la date de fin de l'assujettissement.
	 */
	RegDate getDateFin();

	/**
	 * @return le numéro de séquence technique de l'assujettissement.
	 */
	int getNoSequence();

	/**
	 * @return le type de l'assujettissement.
	 */
	Type getType();
}
