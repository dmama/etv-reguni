package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;

public interface EtatCivil {

    /**
     * Retourne la date de début de l'état civil.
     *
     * @return la date de début de l'état civil.
     */
    RegDate getDateDebutValidite();

    /**
     * Retourne le numéro de séquence technique de l'état civil.
     * Prendre toujours le plus grand (voir plus haut scenario de réconcialition)
     *
     * @return le numéro de séquence technique de l'état civil.
     */
    int getNoSequence();

    /**
     * Retourne le type de l'état civil.
     *
     * @return le type de l'état civil.
     */
    EnumTypeEtatCivil getTypeEtatCivil();

	/**
	 * @return le numéro d'individu du conjoint.
	 */
	Long getNumeroConjoint();
}
