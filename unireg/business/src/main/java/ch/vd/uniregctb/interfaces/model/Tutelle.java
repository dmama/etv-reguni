package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeTutelle;

public interface Tutelle {

    /**
     * @return la date de début de la tutelle.
     */
    RegDate getDateDebut();

    /**
     * @return la date de fin de la tutelle.
     */
    RegDate getDateFin();

    /**
     * @return le libellé du motif de la tutelle.
     */
    String getLibelleMotif();

    /**
     * @return le nom de la collectivité administrative ayant décidé de la tutelle.
     */
    String getNomAutoriteTutelaire();

	/**
	 * @return le numéro de collectivité administrative de la justice de paix qui a décidé de la tutelle
	 */
	Long getNumeroCollectiviteAutoriteTutelaire();

    /**
     * @return le numéro de séquence technique de la tutelle.
     */
    int getNoSequence();

    /**
     * @return le tuteur en charge de la tutelle.
     */
    Individu getTuteur();

    /**
     * @return le tuteur général de la tutelle.
     */
    TuteurGeneral getTuteurGeneral();

    /**
     * @return le type de la tutelle.
     */
    TypeTutelle getTypeTutelle();
}
