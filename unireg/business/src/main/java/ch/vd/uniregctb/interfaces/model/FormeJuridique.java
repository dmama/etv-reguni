package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface FormeJuridique {

	/**
	 * @return la date de début de validité de la forme juridique.
	 */
	RegDate getDateDebut();

	/**
	 * @return La date de fin de validité de la forme juridique; ou <i>null</i> s'elle est toujours valide.
	 */
	RegDate getDateFin();

	/**
	 * @return Le code de la forme juridique de la PM.
	 */
	String getCode();
}
