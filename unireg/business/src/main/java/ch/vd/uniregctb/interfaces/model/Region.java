package ch.vd.uniregctb.interfaces.model;

public interface Region {

    /**
     * Retourne l'identifiant de la direction générale de la région.
     *
     * @return l'identifiant de la direction générale de la région.
     */
    int getIdDirectionRegionale();

    /**
     * Retourne l'identificant technique de la région.
     *
     * @return l'identificant technique de la région.
     */
    int getIdTechnique();

    /**
     * Retourne le nom de la région.
     *
     * @return le nom de la région.
     */
    String getNomRegion();

    /**
     * Retourne le sigle de la région.
     *
     * @return le sigle de la région.
     */
    String getSigle();}
