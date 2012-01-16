package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

public interface EtatCivil extends DateRange {

    /**
     * @return la date de début de l'état civil.
     */
    @Override
    RegDate getDateDebut();

	/**
	 * @return la date de fin de l'état civil.
	 */
	@Override
	RegDate getDateFin();

	/**
     * Retourne le type de l'état civil.
     *
     * @return le type de l'état civil.
     */
    TypeEtatCivil getTypeEtatCivil();

	/**
	 * @return le numéro d'individu du conjoint.
	 */
	Long getNumeroConjoint();
}
