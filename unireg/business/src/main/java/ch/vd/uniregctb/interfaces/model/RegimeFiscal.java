package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

/**
 * Informations sur un régime fiscal d'une personne morale.
 * 
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface RegimeFiscal {

	/**
	 * @return la date de début de validité du régime fiscal.
	 */
	RegDate getDateDebut();

	/**
	 * @return La date de fin de validité du régime fiscal; ou <i>null</i> s'il est toujours valide.
	 */
	RegDate getDateFin();

	/**
	 * @return Le code du régime fiscal de la PM.
	 */
	String getCode();
}
