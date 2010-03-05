package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Loggable;
import ch.vd.uniregctb.interfaces.model.AdresseAvecCommune;

/**
 * Interface générique permettant de représenter aussi bien une adresses civile et qu'une adresse fiscale.
 *
 * @see AdresseCivileAdapter
 * @see AdresseSupplementaireAdapter
 */
public interface AdresseGenerique extends DateRange, Loggable, AdresseAvecCommune {

	public static enum Source {
		CIVILE,
		FISCALE,
		REPRESENTATION,
		TUTELLE,
		CURATELLE,
		/* = cas d'un ménage commun avec principal sous tutelle (l'adresse du conjoint prime sur celle du tuteur) */
		CONJOINT,
		CONSEIL_LEGAL,
		PM,
		/**
		 * Cas du contribuable associé à un débiteur
		 */
		CONTRIBUABLE;
	}

	/**
	 * Retourne l'id de AdresseTiers quand il s'agit d'une AdresseTiers
	 */
	Long getId();

	/**
	 * Retourne la source de l'adresse courante.
	 */
	Source getSource();

	/**
	 * @return vrai si l'adresse représente une valeur par défaut (= copie d'une adresse réellement définie ailleurs)
	 */
	boolean isDefault();

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
     * @return le nom abrégé de la localité (p.a. "Belmont-Lausanne")
     */
    String getLocalite();

    /**
     * @return le nom complet de la localité (p.a. "Belmont-sur-Lausanne")
     */
    String getLocaliteComplete();

    /**
     * Retourne le numéro de l'adresse.
     *
     * @return le numéro de l'adresse.
     */
    String getNumero();

    /**
     * Retourne le numéro d'ordre postal de l'adresse.
     *
     * @return le numéro d'ordre postal de l'adresse; ou <b>0</b> si la localité n'est pas renseignée.
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
     * @return le numéro Ofs du pays de l'adresse.
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
    String getComplement();

    /**
     * @return si l'adresse est annulée ou pas.
     */
    boolean isAnnule();
}
