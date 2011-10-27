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
     * Retourne le nom de naissance de l'historique individu.
     *
     * @return le nom de naissance de l'historique individu.
     */
    String getNomNaissance();

    /**
     * Retourne le prénom de l'historique individu.
     *
     * @return le prénom de l'historique individu.
     */
    String getPrenom();
}
