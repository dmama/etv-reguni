package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;

public interface Permis {

    /**
     * Retourne la date de début de validité du permis.
     *
     * @return la date de début de validité du permis.
     */
    RegDate getDateDebutValidite();

    /**
     * Retourne la date de fin de validité du permis.
     *
     * @return la date de fin de validité du permis.
     */
    RegDate getDateFinValidite();

    /**
     * Retourne la date d'annulation du permis.
     *
     * @return la date d'annulation du permis.
     */
    RegDate getDateAnnulation();

    /**
     * Retourne le numéro de séquence technique du permis.
     *
     * @return le numéro de séquence technique du permis.
     */
    int getNoSequence();

    /**
     * Retourne le type du permis.
     *
     * @return le type du permis.
     */
    EnumTypePermis getTypePermis();
}
