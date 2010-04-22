package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeTutelle;

public interface Tutelle {

    /**
     * Retourne la date de début de la tutelle.
     *
     * @return la date de début de la tutelle.
     */
    RegDate getDateDebut();

    /**
     * Retourne la date de fin de la tutelle.
     *
     * @return la date de fin de la tutelle.
     */
    RegDate getDateFin();

    /**
     * Retourne le libellé du motif de la tutelle.
     *
     * @return le libellé du motif de la tutelle.
     */
    String getLibelleMotif();

    /**
     * Retourne le nom de la collectivité administrative ayant décidé de la tutelle.
     *
     * @return le nom de la collectivité administrative ayant décidé de la tutelle.
     */
    String getNomAutoriteTutelaire();

    /**
     * Retourne le numéro de séquence technique de la tutelle.
     *
     * @return le numéro de séquence technique de la tutelle.
     */
    int getNoSequence();

    /**
     * Retourne le tuteur en charge de la tutelle.
     *
     * @return le tuteur en charge de la tutelle.
     */
    Individu getTuteur();

    /**
     * Retourne le tuteur général de la tutelle.
     *
     * @return le tuteur général de la tutelle.
     */
    TuteurGeneral getTuteurGeneral();

    /**
     * Retourne le type de la tutelle.
     *
     * @return le type de la tutelle.
     */
    EnumTypeTutelle getTypeTutelle();
}
