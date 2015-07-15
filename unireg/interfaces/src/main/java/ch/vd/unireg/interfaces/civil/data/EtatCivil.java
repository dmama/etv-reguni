package ch.vd.unireg.interfaces.civil.data;

import ch.vd.registre.base.date.RegDate;

public interface EtatCivil {

    /**
     * @return la date de début de l'état civil.
     */
    RegDate getDateDebut();

	/**
     * Retourne le type de l'état civil.
     *
     * @return le type de l'état civil.
     */
    TypeEtatCivil getTypeEtatCivil();
}
