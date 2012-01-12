package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

public interface Nationalite {

    /**
     * Retourne la date de début de validité de la nationalité.
     *
     * @return la date de début de validité de la nationalité.
     */
    RegDate getDateDebutValidite();

    /**
     * Retourne la date de fin de validité de la nationalité.
     *
     * @return la date de fin de validité de la nationalité.
     */
    RegDate getDateFinValidite();

    /**
     * Retourne le Pays de la nationalité.
     *
     * @return le Pays de la nationalité.
     */
    Pays getPays();
}
