package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypePermis;

public interface Permis extends DateRange {

    /**
     * Retourne la date de début de validité du permis.
     *
     * @return la date de début de validité du permis.
     */
    @Override
    RegDate getDateDebut();

    /**
     * Retourne la date de fin de validité du permis.
     *
     * @return la date de fin de validité du permis.
     */
    @Override
    RegDate getDateFin();

    /**
     * Retourne la date d'annulation du permis.
     *
     * @return la date d'annulation du permis.
     */
    RegDate getDateAnnulation();

    /**
     * Retourne le type du permis.
     *
     * @return le type du permis.
     */
    TypePermis getTypePermis();
}
