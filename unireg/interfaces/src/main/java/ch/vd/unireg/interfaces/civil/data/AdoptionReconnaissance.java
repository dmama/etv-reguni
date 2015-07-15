package ch.vd.unireg.interfaces.civil.data;

import ch.vd.registre.base.date.RegDate;

public interface AdoptionReconnaissance {

    /**
     * Retourne l'individu adopté ou reconnu.
     *
     * @return l'individu adopté ou reconnu.
     */
    Individu getAdopteReconnu();

    /**
     * Retourne la date d'accueil en vue et de l'adoption.
     *
     * @return la date d'accueil en vue et de l'adoption.
     */
    RegDate getDateAccueilAdoption();

    /**
     * Retourne la date effective de l'adoption.
     *
     * @return la date effective de l'adoption.
     */
    RegDate getDateAdoption();

    /**
     * Retourne la date de désaveu de la reconnaissance.
     *
     * @return la date de désaveu de la reconnaissance.
     */
    RegDate getDateDesaveu();

    /**
     * Retourne la date de la reconnaissance.
     *
     * @return la date de la reconnaissance.
     */
    RegDate getDateReconnaissance();}
