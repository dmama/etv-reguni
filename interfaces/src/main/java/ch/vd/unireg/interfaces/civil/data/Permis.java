package ch.vd.unireg.interfaces.civil.data;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypePermis;

public interface Permis extends DateRange {

    /**
     * @return la date de début de validité du permis.
     */
    @Override
    RegDate getDateDebut();

    /**
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
	 * @return la date de valeur de l'information de permis (= date dont on peut se servir pour trier les permis entre eux même en l'absence de date de début de validité)
	 */
	RegDate getDateValeur();

    /**
     * Retourne le type du permis.
     *
     * @return le type du permis.
     */
    TypePermis getTypePermis();
}
