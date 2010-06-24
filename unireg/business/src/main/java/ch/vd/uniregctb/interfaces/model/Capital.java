package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

/**
 * Informations sur le capital à disposition d'une personne morale.
 * 
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface Capital {

	/**
	 * @return la date de début de validité du capital.
	 */
	RegDate getDateDebut();

	/**
	 * @return La date de fin de validité du capital; ou <i>null</i> s'il est toujours valide.
	 */
	RegDate getDateFin();

	/**
	 * @return la valeur du capital action de la PM.
	 */
	Long getCapitalAction();

	/**
	 * @return la valeur du capital libéré de la PM.
	 */
	Long getCapitalLibere();

	/**
	 * @return l'édition de la Feuille officielle suisse du commerce dans laquelle ces informations sont parues
	 */
	EditionFosc getEditionFosc();
}
