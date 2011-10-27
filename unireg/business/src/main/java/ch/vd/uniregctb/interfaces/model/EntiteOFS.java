package ch.vd.uniregctb.interfaces.model;

public interface EntiteOFS {

    /**
     * Retourne le nom en majuscule de l'entité OFS.
     *
     * @return le nom en majuscule de l'entité OFS.
     */
    String getNomMajuscule();

    /**
     * Retourne le nom en minuscule de l'entité OFS.
     *
     * @return le nom en minuscule de l'entité OFS.
     */
    String getNomMinuscule();

    /**
     * Retourne le numéro de l'entité OFS.
     *
     * @return le numéro de l'entité OFS.
     */
    int getNoOFS();

    /**
     * Retourne le sigle de l'entité OFS.
     *
     * @return le sigle de l'entité OFS.
     */
    String getSigleOFS();
}
