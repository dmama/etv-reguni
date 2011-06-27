package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * Un fors fiscal (ou domicile fiscal) détermine les autorités bénéficiaires de tout ou partie d’un impôt.
 * <p/>
 * De manière générale, un for fiscal possède une date de début et une date de fin (incluse) qui délimitent une période de validité. En dehors de cette période, le for fiscal n’est pas valide.
 * <p/>
 * Une date de fin de validité nulle indique que le for est toujours actif, et peut être interprétée comme « la fin des temps ». Une date de début de validité nulle n’est pas autorisée
 * <p/>
 * <h1>Fors principaux</h1> Un for principal représente le rattachement personnel à une collectivité (le plus souvent par le domicile) pour les personnes physiques ou le siège de la société pour les
 * personnes morales.
 * <p/>
 * Plusieurs fors fiscaux principaux peuvent exister sur un tiers donné, mais ceux-ci ne peuvent jamais se chevaucher.
 * <p/>
 * <h1>Fors secondaires</h1> Un for secondaire représente déterminé un rattachement économique à une collectivité, par exploitation ou intéressement dans une entreprise ou par propriété d’un
 * immeuble.
 * <p/>
 * Plusieurs fors fiscaux secondaires peuvent exister sur un tiers donnée, et ceux-ci peuvent se chevaucher à l’inverse des fors principaux.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>taxResidenceType</i> (xml) / <i>TaxResidence</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ForFiscal", propOrder = {
		"id", "dateOuverture", "dateFermeture", "dateAnnulation", "genreImpot", "typeAutoriteFiscale", "noOfsAutoriteFiscale",
		"motifRattachement", "motifOuverture", "motifFermeture", "modeImposition", "virtuel"
})
public class ForFiscal implements Range {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>taxTypeType</i> (xml) / <i>TaxType</i> (client java)
	 */
	@XmlType(name = "GenreImpot")
	@XmlEnum(String.class)
	public static enum GenreImpot {
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>INCOME_WEALTH</i>
		 */
		REVENU_FORTUNE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>IMMOVABLE_PROPERTY_GAINS</i>
		 */
		GAIN_IMMOBILIER,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>REAL_ESTATE_TRANSFER</i>
		 */
		DROIT_MUTATION,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>CAPITAL_INCOME</i>
		 */
		PRESTATION_CAPITAL,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>INHERITANCE</i>
		 */
		SUCCESSION,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>REAL_ESTATE</i>
		 */
		FONCIER,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>DEBTOR_TAXABLE_INCOME</i>
		 */
		DEBITEUR_PRESTATION_IMPOSABLE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>GIFTS</i>
		 */
		DONATION,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>DOGS</i>
		 */
		CHIENS,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>TOBACCO_PATENT</i>
		 */
		PATENTE_TABAC,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>PROFITS_CAPITAL</i>
		 */
		BENEFICE_CAPITAL;

		public static GenreImpot fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>taxLiabilityReasonType</i> (xml) / <i>TaxLiabilityReason</i> (client java)
	 */
	@XmlType(name = "MotifRattachement")
	@XmlEnum(String.class)
	public static enum MotifRattachement {
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>RESIDENCE</i>
		 */
		DOMICILE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>PRIVATE_IMMOVABLE_PROPERTY</i>
		 */
		IMMEUBLE_PRIVE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>SWISS_DIPLOMAT</i>
		 */
		DIPLOMATE_SUISSE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>INDEPENDANT_ACTIVITY</i>
		 */
		ACTIVITE_INDEPENDANTE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>SEASONAL_JOURNEY</i>
		 */
		SEJOUR_SAISONNIER,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>COMPANY_LEADER</i>
		 */
		DIRIGEANT_SOCIETE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>GAINFUL_ACTIVITY_SAS</i>
		 */
		ACTIVITE_LUCRATIVE_CAS,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>ADMINISTRATOR</i>
		 */
		ADMINISTRATEUR,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>MORTGAGE_CREDITORS</i>
		 */
		CREANCIER_HYPOTHECAIRE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>PENSION</i>
		 */
		PRESTATION_PREVOYANCE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>FOREIGN_DIPLOMAT</i>
		 */
		DIPLOMATE_ETRANGER,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>LAW_ON_UNDECLARED_WORK</i>
		 */
		LOI_TRAVAIL_AU_NOIR,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>STABLE_ESTABLISHMENT</i>
		 */
		ETABLISSEMENT_STABLE;

		public static MotifRattachement fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>taxationAuthorityTypeType</i> (xml) / <i>TaxationAuthorityType</i> (client java)
	 */
	@XmlType(name = "TypeAutoriteFiscale")
	@XmlEnum(String.class)
	public static enum TypeAutoriteFiscale {
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>VAUD_MUNICIPALITY</i>
		 */
		COMMUNE_OU_FRACTION_VD,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>OTHER_CANTON_MUNICIPALITY</i>
		 */
		COMMUNE_HC,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>FOREIGN_COUNTRY</i>
		 */
		PAYS_HS;

		public static TypeAutoriteFiscale fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>taxationMethodType</i> (xml) / <i>TaxationMethod</i> (client java)
	 */
	@XmlType(name = "ModeImposition")
	@XmlEnum(String.class)
	public static enum ModeImposition {
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>ORDINARY</i>
		 */
		ORDINAIRE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>WITHHOLDING</i>
		 */
		SOURCE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>EXPENDITURE_BASED</i>
		 */
		DEPENSE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>MIXED_137_1</i>
		 */
		MIXTE_137_1,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>MIXED_137_2</i>
		 */
		MIXTE_137_2,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>INDIGENT</i>
		 */
		INDIGENT;

		public static ModeImposition fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>liabilityChangeReasonType</i> (xml) / <i>LiabilityChangeReason</i> (client java)
	 */
	@XmlType(name = "MotifFor")
	@XmlEnum(String.class)
	public static enum MotifFor {
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>MOVE_VD</i>
		 */
		DEMENAGEMENT_VD,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>WIDOWHOOD_DEATH</i>
		 */
		VEUVAGE_DECES,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>MARRIAGE_PARTNERSHIP_END_OF_SEPARATION</i>
		 */
		MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION</i>
		 */
		SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>C_PERMIT_SWISS</i>
		 */
		PERMIS_C_SUISSE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>MAJORITY</i>
		 */
		MAJORITE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>MOVE_IN_FROM_FOREIGN_COUNTRY</i>
		 */
		ARRIVEE_HS,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>MOVE_IN_FROM_OTHER_CANTON</i>
		 */
		ARRIVEE_HC,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>MERGE_OF_MUNICIPALITIES</i>
		 */
		FUSION_COMMUNES,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>PURCHASE_REAL_ESTATE</i>
		 */
		ACHAT_IMMOBILIER,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>SALE_REAL_ESTATE</i>
		 */
		VENTE_IMMOBILIER,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>START_COMMERCIAL_EXPLOITATION</i>
		 */
		DEBUT_EXPLOITATION,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>END_COMMERCIAL_EXPLOITATION</i>
		 */
		FIN_EXPLOITATION,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>DEPARTURE_TO_FOREIGN_COUNTRY</i>
		 */
		DEPART_HS,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>DEPARTURE_TO_OTHER_CANTON</i>
		 */
		DEPART_HC,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>UNDETERMINED</i>
		 */
		INDETERMINE,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>SEASONAL_JOURNEY</i>
		 */
		SEJOUR_SAISONNIER,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>CHANGE_OF_TAXATION_METHOD</i>
		 */
		CHGT_MODE_IMPOSITION,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>CANCELLATION</i>
		 */
		ANNULATION,
		/**
		 * <b>Dans la version 3 du web-service :</b> <i>REACTIVATION</i>
		 */
		REACTIVATION;

		public static MotifFor fromValue(String v) {
			// [UNIREG-911] pour des raisons de compatibilité ascendante, les motifs de début/fin d'activité diplomatiques sont mappés comme indéterminés
			if ("DEBUT_ACTIVITE_DIPLOMATIQUE".equals(v) || "FIN_ACTIVITE_DIPLOMATIQUE".equals(v)) {
				return INDETERMINE;
			}
			return valueOf(v);
		}
	}

	/**
	 * L'id technique (= clé primaire)
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> supprimé.
	 */
	@XmlElement(required = true)
	public Long id;

	/**
	 * La date d'ouverture du for fiscal.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateFrom</i>
	 */
	@XmlElement(required = true)
	public Date dateOuverture;

	/**
	 * La date de fermeture du for fiscal. Si le for est toujours actif, cette date n'est pas renseignée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateTo</i>
	 */
	@XmlElement(required = false)
	public Date dateFermeture;

	/**
	 * Date à laquelle le for fiscal a été annulé, ou <b>null</b> s'il n'est pas annulé.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>CANCELLATION</i>
	 */
	@XmlElement(required = false)
	public Date dateAnnulation;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>taxType</i>
	 */
	@XmlElement(required = true)
	public GenreImpot genreImpot;

	/**
	 * Cet enum permet d'interpréter le numéro OFS contenu dans noOfsAutoriteFiscale
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>taxationAuthorityType</i>
	 */
	@XmlElement(required = true)
	public TypeAutoriteFiscale typeAutoriteFiscale;

	/**
	 * Numéro OFS étendu de la commune, du canton ou du pays de l'autorité fiscale du fors
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>taxationAuthorityFSOId</i>
	 */
	@XmlElement(required = true)
	public int noOfsAutoriteFiscale;

	/**
	 * Le motif de rattachement est uniquement renseignée sur les fors fiscaux de genre = REVENU_FORTUNE
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>taxLiabilityReason</i>
	 */
	@XmlElement(required = false)
	public MotifRattachement motifRattachement;

	/**
	 * Le motif d'ouverture est uniquement renseignée sur les fors fiscaux de genre = REVENU_FORTUNE
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>startReason</i>
	 */
	@XmlElement(required = false)
	public MotifFor motifOuverture;

	/**
	 * Le motif de fermeture est uniquement renseignée sur les fors fiscaux de genre = REVENU_FORTUNE
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>endReason</i>
	 */
	@XmlElement(required = false)
	public MotifFor motifFermeture;

	/**
	 * Le mode d'imposition est uniquement renseignée sur les fors fiscaux de genre = REVENU_FORTUNE et de rattachement = {DOMICILE; DIPLOMATE}
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>taxationMethod</i>
	 */
	@XmlElement(required = false)
	public ModeImposition modeImposition;

	/**
	 * Si <b>vrai</b>, le for fiscal n'existe pas en tel que tel, mais est une vue construite en fonction de règles métier.
	 * <p/>
	 * <b>Exemple:</b> les fors fiscaux principaux individuels des sourciers appartenant à des ménages-communs sont virtuels, car les fors fiscaux réels sont définis sur les contribuables ménages.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>virtual</i>
	 *
	 * @see TiersPart#FORS_FISCAUX_VIRTUELS
	 */
	@XmlElement(required = true)
	public boolean virtuel;

	public ForFiscal() {
	}

	public ForFiscal(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel, Context context) {
		this.id = forFiscal.getId();
		this.dateOuverture = DataHelper.coreToWeb(forFiscal.getDateDebut());
		this.dateFermeture = DataHelper.coreToWeb(forFiscal.getDateFin());
		this.dateAnnulation = DataHelper.coreToWeb(forFiscal.getAnnulationDate());
		this.genreImpot = EnumHelper.coreToWeb(forFiscal.getGenreImpot());
		this.typeAutoriteFiscale = EnumHelper.coreToWeb(forFiscal.getTypeAutoriteFiscale());
		if (this.typeAutoriteFiscale != TypeAutoriteFiscale.PAYS_HS) {
			this.noOfsAutoriteFiscale = context.noOfsTranslator.translateCommune(forFiscal.getNumeroOfsAutoriteFiscale());
		}
		else {
			this.noOfsAutoriteFiscale = forFiscal.getNumeroOfsAutoriteFiscale();
		}
		this.virtuel = virtuel;

		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) {
			final ch.vd.uniregctb.tiers.ForFiscalRevenuFortune forRevenu = (ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) forFiscal;

			this.motifRattachement = EnumHelper.coreToWeb(forRevenu.getMotifRattachement());
			this.motifOuverture = EnumHelper.coreToWeb(forRevenu.getMotifOuverture());
			this.motifFermeture = EnumHelper.coreToWeb(forRevenu.getMotifFermeture());

			if (forRevenu instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipal) {
				final ch.vd.uniregctb.tiers.ForFiscalPrincipal forPrincipal = (ch.vd.uniregctb.tiers.ForFiscalPrincipal) forRevenu;

				this.modeImposition = EnumHelper.coreToWeb(forPrincipal.getModeImposition());
			}
			else {
				this.modeImposition = null;
			}
		}
		else {
			this.motifRattachement = null;
			this.motifOuverture = null;
			this.motifFermeture = null;
			this.modeImposition = null;
		}
	}

	@Override
	public Date getDateDebut() {
		return dateOuverture;
	}

	@Override
	public Date getDateFin() {
		return dateFermeture;
	}

	@Override
	public void setDateDebut(Date v) {
		dateOuverture = v;
	}

	@Override
	public void setDateFin(Date v) {
		dateFermeture = v;
	}
}
