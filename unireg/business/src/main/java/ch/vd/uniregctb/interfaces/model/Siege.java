package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

/**
 * Contient les informations du siège d'une personne morale.
 * 
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface Siege {

	/**
	 * @return la date de début de validité du régime fiscal.
	 */
	RegDate getDateDebut();

	/**
	 * @return La date de fin de validité du régime fiscal; ou <i>null</i> s'il est toujours valide.
	 */
	RegDate getDateFin();

	/**
	 * @return le numéro OFS étendu de la commune suisse ou du pays de siège.
	 */
	int getNoOfsSiege();

	/**
	 * @return Le type de siège qui permet d'interpréter le numéro OFS retourné par {@link #getNoOfsSiege}.
	 */
	TypeNoOfs getType();
}
