package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.base.date.RegDate;

public interface CollectiviteAdministrative {

    /**
     * Retourne l'adresse de la collectivité administrative.
     *
     * @return l'adresse de la collectivité administrative.
     */
    Adresse getAdresse();

    /**
     * Retourne l'adresse email par DEFAUT de la collectivité administrative.
     * Si la collectivité ne possede pas d'email pour le code usage STANDARD, null est retourné.
     *
     * @return l'adresse email de la collectivité administrative pour le sigle usage STANDARD.
     */
    String getAdresseEmail();

    /**
     * Retourne la date de fin de validité de la collectivité administrative.
     *
     * @return la date de fin de validité de la collectivité administrative.
     */
    RegDate getDateFinValidite();

    /**
     * Retourne le numéro du CCP de la collectivité administrative.
     *
     * @return le numéro du CCP de la collectivité administrative.
     */
    String getNoCCP();

    /**
     * Retourne le numéro de la collectivité administrative.
     *
     * @return le numéro de la collectivité administrative.
     */
    int getNoColAdm();

    /**
     * Retourne le numéro de fax de la collectivité administrative.
     *
     * @return le numéro de fax de la collectivité administrative.
     */
    String getNoFax();

    /**
     * Retourne la première partie du nom complet de la collectivité administrative.
     *
     * @return la première partie du nom complet de la collectivité administrative.
     */
    String getNomComplet1();

    /**
     * Retourne la deuxième partie du nom complet de la collectivité administrative.
     *
     * @return la deuxième partie du nom complet de la collectivité administrative.
     */
    String getNomComplet2();

    /**
     * Retourne la troisième partie du nom complet de la collectivité administrative.
     *
     * @return la troisième partie du nom complet de la collectivité administrative.
     */
    String getNomComplet3();

    /**
     * Retourne le nom court de la collectivité administrative.
     *
     * @return le nom court de la collectivité administrative.
     */
    String getNomCourt();

    /**
     * Retourne le numéro de téléphone de la collectivité administrative.
     *
     * @return le numéro de téléphone de la collectivité administrative.
     */
    String getNoTelephone();

    /**
     * Retourne le sigle de la collectivité administrative.
     *
     * @return le sigle de la collectivité administrative.
     */
    String getSigle();

    /**
     * Retourne le sigle du canton de la collectivité administrative.
     *
     * @return le sigle du canton de la collectivité administrative.
     */
    String getSigleCanton();

    /**
     * Indique si la collectivité administrative est l'Administration Cantonale des Impôts (ACI).
     *
     * @return <code>true</code> si la collectivité administrative est l'Administration Cantonale des Impôts.
     */
    boolean isACI();

    /**
     * Indique si la collectivité administrative est un Office d'Impôts de District (OID).
     *
     * @return <code>true</code> si la collectivité administrative est un Office d'Impôts de District
     */
    boolean isOID();

    /**
     * Indique si la collectivité administrative est valide.
     *
     * @return <code>true</code> si la date de fin de validité de la collectivité est absente ou si celle-ci est égale
     *         ou postérieure à la date du jour.
     */
    boolean isValide();}
