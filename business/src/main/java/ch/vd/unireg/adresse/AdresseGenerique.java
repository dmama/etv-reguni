package ch.vd.unireg.adresse;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Loggable;
import ch.vd.unireg.interfaces.common.AdresseAvecCommune;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.tiers.Tiers;

/**
 * Interface générique permettant de représenter aussi bien une adresses civile et qu'une adresse fiscale.
 *
 * @see AdresseCivileAdapter
 * @see AdresseSupplementaireAdapter
 */
public interface AdresseGenerique extends DateRange, Loggable, AdresseAvecCommune {

	/**
	 * Représente la source (type et tiers) d'une adresse générique
	 */
	class Source {
		private final SourceType type;
		private final Tiers tiers;

		public Source(SourceType type, Tiers tiers) {
			if (type == null) {
				throw new IllegalArgumentException();
			}
			this.type = type;
			this.tiers = tiers;
		}

		/**
		 * @return le type de source (CIVILE, FISCALE, ...) d'une adresse
		 */
		public SourceType getType() {
			return type;
		}

		/**
		 * @return le tiers à la source d'une adresse
		 */
		public Tiers getTiers() {
			return tiers;
		}
	}

	enum SourceType {
		CIVILE_PERS(false),
		CIVILE_ENT(false),
		/**
		 * Adresse en provenance de l'infrastructure fiscale (Fidor).
		 */
		INFRA(false),
		FISCALE(false),
		REPRESENTATION(true),
		TUTELLE(true),
		CURATELLE(true),
		/** = cas d'un ménage commun sans adresse propre (l'adresse du principal est utilisée comme source) */
		PRINCIPAL(false),
		/** = cas d'un ménage commun avec principal sous tutelle (l'adresse du conjoint prime sur celle du tuteur) */
		CONJOINT(true),
		CONSEIL_LEGAL(true),
		/**
		 * Cas du contribuable associé à un débiteur
		 */
		CONTRIBUABLE(false),
		/**
		 * Cas du mandataire
		 */
		MANDATAIRE(false),
		/**
		 * Cas de l'établissement principal de l'entreprise
		 */
		ETABLISSEMENT_PRINCIPAL(false);

		private final boolean representation;

		SourceType(boolean representation) {
			this.representation = representation;
		}

		/**
		 * @return <b>vrai</b> si la source représente une adresse de représentation.
		 */
		public boolean isRepresentation() {
			return representation;
		}
	}

	/**
	 * @return l'id de AdresseTiers quand il s'agit d'une AdresseTiers
	 */
	Long getId();

	/**
	 * @return la source de l'adresse courante.
	 */
	Source getSource();

	/**
	 * @return vrai si l'adresse représente une valeur par défaut (= copie d'une adresse réellement définie ailleurs)
	 */
	boolean isDefault();

    /**
     * @return la case postale de l'adresse.
     */
    CasePostale getCasePostale();

    /**
     * @return la date de fin de validité de l'adresse.
     */
    @Override
    RegDate getDateDebut();

    /**
     * @return la date de début de validité de l'adresse.
     */
    @Override
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
     * @return le numéro de l'adresse. Par exemple : "3bis".
     * @see ch.vd.unireg.adresse.AdresseGenerique#getRue()
     */
    String getNumero();

    /**
     * @return le numéro d'ordre postal de l'adresse; ou <b>null</b> si la localité n'est pas renseignée.
     */
    Integer getNumeroOrdrePostal();

    /**
     * @return le numéro postal de l'adresse.
     */
    String getNumeroPostal();

    /**
     * @return le numéro postal complémentaire de l'adresse.
     */
    String getNumeroPostalComplementaire();

    /**
     * @return le numéro Ofs du pays de l'adresse.
     */
    Integer getNoOfsPays();

    /**
     * @return la rue de l'adresse. Par exemple : "Avenue de Beaulieu".
     * @see ch.vd.unireg.adresse.AdresseGenerique#getNumero()
     */
    String getRue();

    /**
     * @return le numéro technique de la rue.
     */
    Integer getNumeroRue();

    /**
     * @return le numero de l'appartement.
     */
    String getNumeroAppartement();

    /**
     * @return le titre de l'adresse. Par exemple "Chez : ..." .
     */
    String getComplement();

    /**
     * @return si l'adresse est annulée ou pas.
     */
    @Override
    boolean isAnnule();

	/**
	 * @return <b>vrai</b> s'il s'agit d'une adresse tiers ({@link AdresseTiers}) permanente; <b>faux</b> dans tous les autres cas.
	 */
	boolean isPermanente();
}
