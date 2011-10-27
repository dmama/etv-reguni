package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

import java.util.Date;

/**
 * Informations sur l'état d'une personne morale.
 * 
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EtatPM {

	/**
	 * @return la date de début de validité de l'état.
	 */
	RegDate getDateDebut();

	/**
	 * @return La date de fin de validité de l'état; ou <i>null</i> s'il est toujours valide.
	 */
	RegDate getDateFin();

	/**
	 * @return Le code de l'état de la PM.
	 */
	String getCode();
}
