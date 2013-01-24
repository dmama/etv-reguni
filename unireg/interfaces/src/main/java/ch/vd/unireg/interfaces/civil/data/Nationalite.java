package ch.vd.unireg.interfaces.civil.data;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

public interface Nationalite extends DateRange {

    /**
     * Retourne la date de début de validité de la nationalité.
     *
     * @return la date de début de validité de la nationalité.
     */
    @Override
    RegDate getDateDebut();

    /**
     * Retourne la date de fin de validité de la nationalité.
     *
     * @return la date de fin de validité de la nationalité.
     */
    @Override
    RegDate getDateFin();

    /**
     * Retourne le Pays de la nationalité.
     *
     * @return le Pays de la nationalité.
     */
    Pays getPays();
}
