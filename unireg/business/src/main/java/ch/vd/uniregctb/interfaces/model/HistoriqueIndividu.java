package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

public interface HistoriqueIndividu {

    /**
     * Retourne les autres prénom de l'historique individu.
     *
     * @return les autres prénom de l'historique individu.
     */
    String getAutresPrenoms();

    /**
     * Retourne les données permettant de mieux identifier l'individu (courrier).
     *
     * @return les données permettant de mieux identifier l'individu (courrier).
     */
    String getComplementIdentification();

    /**
     * Retourne la date de début de validité de l'historique individu.
     *
     * @return la date de début de validité de l'historique individu.
     */
    RegDate getDateDebutValidite();

    /**
     * Retourne le numéro AVS de l'historique individu.
     *
     * @return le numéro AVS de l'historique individu.
     */
    String getNoAVS();

    /**
     * Retourne le nom de l'historique individu.
     *
     * @return le nom de l'historique individu.
     */
    String getNom();

    /**
     * Retourne le nom et prénom de l'individu (première ligne d'adresse).
     *
     * @return le nom et prénom de l'individu (première ligne d'adresse).
     */
    String getNomCourrier1();

    /**
     * Retourne les données d'identification de l'individu (deuxième ligne d'adresse).
     *
     * @return les données d'identification de l'individu (deuxième ligne d'adresse).
     */
    String getNomCourrier2();

    /**
     * Retourne le nom de naissance de l'historique individu.
     *
     * @return le nom de naissance de l'historique individu.
     */
    String getNomNaissance();

    /**
     * Retourne le numéro de séquence technique de l'historique individu.
     *
     * @return le numéro de séquence technique de l'historique individu.
     */
    int getNoSequence();

    /**
     * Retourne le prénom de l'historique individu.
     *
     * @return le prénom de l'historique individu.
     */
    String getPrenom();

    /**
     * Retourne la profession de l'historique individu.
     *
     * @return la profession de l'historique individu.
     */
    String getProfession();
}
