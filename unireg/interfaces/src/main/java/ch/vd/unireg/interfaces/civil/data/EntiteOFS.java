package ch.vd.unireg.interfaces.civil.data;

public interface EntiteOFS {

    /**
     * @return le nom court de l'entité OFS. Par exemple : "Suisse".
     */
    String getNomCourt();

    /**
     * @return le nom officiel de l'entité OFS. Par exemple : "Confédération suisse".
     */
    String getNomOfficiel();

    /**
     * @return le numéro de l'entité OFS.
     */
    int getNoOFS();

    /**
     * @return le sigle de l'entité OFS.
     */
    String getSigleOFS();
}
