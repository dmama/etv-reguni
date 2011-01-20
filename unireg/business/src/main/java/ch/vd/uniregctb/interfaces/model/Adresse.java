package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public interface Adresse extends DateRange, AdresseAvecCommune {

    /**
     * Retourne la case postale de l'adresse.
     *
     * @return la case postale de l'adresse.
     */
    String getCasePostale();

    /**
     * Retourne la date de fin de validité de l'adresse.
     *
     * @return la date de fin de validité de l'adresse.
     */
    RegDate getDateDebut();

    /**
     * Retourne la date de début de validité de l'adresse.
     *
     * @return la date de début de validité de l'adresse.
     */
    RegDate getDateFin();

    /**
     * Retourne la localité de l'adresse.
     *
     * @return la localité de l'adresse.
     */
    String getLocalite();

    /**
     * Retourne le numéro de l'adresse.
     *
     * @return le numéro de l'adresse.
     */
    String getNumero();

    /**
     * Retourne le numéro d'ordre postal de l'adresse.
     *
     * @return le numéro d'ordre postal de l'adresse.
     */
    int getNumeroOrdrePostal();

    /**
     * Retourne le numéro postal de l'adresse.
     *
     * @return le numéro postal de l'adresse.
     */
    String getNumeroPostal();

    /**
     * Retourne le numéro postal complémentaire de l'adresse.
     *
     * @return le numéro postal complémentaire de l'adresse.
     */
    String getNumeroPostalComplementaire();

    /**
     * Retourne le pays de l'adresse. Cette attribut peut être <code>null</code> si l'adresse n'est pas située en
     * Suisse.
     *
     * @return le pays de l'adresse.
     */
    Integer getNoOfsPays();

    /**
     * Retourne la rue de l'adresse.
     *
     * @return la rue de l'adresse.
     */
    String getRue();

    /**
     * @return le numéro technique de la rue.
     */
    Integer getNumeroRue();

    /**
     * Retourne le numero de l'appartement.
     *
     * @return le numero de l'appartement.
     */
    String getNumeroAppartement();

    /**
     * Retourne le titre de l'adresse. Par exemple "Chez : ..." .
     *
     * @return le titre de l'adresse. Par exemple "Chez : ..." .
     */
    String getTitre();

    /**
     * Retourne le type de l'adresse.
     *
     * @return le type de l'adresse.
     */
    TypeAdresseCivil getTypeAdresse();
}
