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
 * <p>
 * De manière générale, un for fiscal possède une date de début et une date de fin (incluse) qui délimitent une période de validité. En
 * dehors de cette période, le for fiscal n’est pas valide.
 * <p>
 * Une date de fin de validité nulle indique que le for est toujours actif, et peut être interprétée comme « la fin des temps ». Une date de
 * début de validité nulle n’est pas autorisée
 *
 * <h1>Fors principaux</h1>
 * Un for principal représente le rattachement personnel à une collectivité (le plus souvent par le domicile) pour les personnes physiques
 * ou le siège de la société pour les personnes morales.
 * <p>
 * Plusieurs fors fiscaux principaux peuvent exister sur un tiers donné, mais ceux-ci ne peuvent jamais se chevaucher.
 *
 * <h1>Fors secondaires</h1> Un for secondaire représente déterminé un rattachement économique à une collectivité, par exploitation ou
 * intéressement dans une entreprise ou par propriété d’un immeuble.
 * <p>
 * Plusieurs fors fiscaux secondaires peuvent exister sur un tiers donnée, et ceux-ci peuvent se chevaucher à l’inverse des fors principaux.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ForFiscal", propOrder = {
		"id", "dateOuverture", "dateFermeture", "dateAnnulation", "genreImpot", "typeAutoriteFiscale", "noOfsAutoriteFiscale",
		"motifRattachement", "motifOuverture", "motifFermeture", "modeImposition", "virtuel"
})
public class ForFiscal implements Range {

	@XmlType(name = "GenreImpot")
	@XmlEnum(String.class)
	public static enum GenreImpot {
		REVENU_FORTUNE,
		GAIN_IMMOBILIER,
		DROIT_MUTATION,
		PRESTATION_CAPITAL,
		SUCCESSION,
		FONCIER,
		DEBITEUR_PRESTATION_IMPOSABLE,
		DONATION,
		CHIENS,
		PATENTE_TABAC,
		BENEFICE_CAPITAL;

		public static GenreImpot fromValue(String v) {
			return valueOf(v);
		}
	}

	@XmlType(name = "MotifRattachement")
	@XmlEnum(String.class)
	public static enum MotifRattachement {
		DOMICILE,
		IMMEUBLE_PRIVE,
		DIPLOMATE_SUISSE,
		ACTIVITE_INDEPENDANTE,
		SEJOUR_SAISONNIER,
		DIRIGEANT_SOCIETE,
		ACTIVITE_LUCRATIVE_CAS,
		ADMINISTRATEUR,
		CREANCIER_HYPOTHECAIRE,
		PRESTATION_PREVOYANCE,
		DIPLOMATE_ETRANGER,
		LOI_TRAVAIL_AU_NOIR,
		ETABLISSEMENT_STABLE;

		public static MotifRattachement fromValue(String v) {
			return valueOf(v);
		}
	}

	@XmlType(name = "TypeAutoriteFiscale")
	@XmlEnum(String.class)
	public static enum TypeAutoriteFiscale {
		COMMUNE_OU_FRACTION_VD,
		COMMUNE_HC,
		PAYS_HS;

		public static TypeAutoriteFiscale fromValue(String v) {
			return valueOf(v);
		}
	}

	@XmlType(name = "ModeImposition")
	@XmlEnum(String.class)
	public static enum ModeImposition {
		ORDINAIRE,
		SOURCE,
		DEPENSE,
		MIXTE_137_1,
		MIXTE_137_2,
		INDIGENT;

		public static ModeImposition fromValue(String v) {
			return valueOf(v);
		}
	}

	@XmlType(name = "MotifFor")
	@XmlEnum(String.class)
	public static enum MotifFor {
		DEMENAGEMENT_VD,
		VEUVAGE_DECES,
		MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
		SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
		PERMIS_C_SUISSE,
		MAJORITE,
		ARRIVEE_HS,
		ARRIVEE_HC,
		FUSION_COMMUNES,
		ACHAT_IMMOBILIER,
		VENTE_IMMOBILIER,
		DEBUT_EXPLOITATION,
		FIN_EXPLOITATION,
		DEPART_HS,
		DEPART_HC,
		INDETERMINE,
		SEJOUR_SAISONNIER,
		CHGT_MODE_IMPOSITION,
		ANNULATION,
		REACTIVATION;

		public static MotifFor fromValue(String v) {
			return valueOf(v);
		}
	}

	/** L'id technique (= clé primaire) */
	@XmlElement(required = true)
	public Long id;

	/** La date d'ouverture du for fiscal. */
	@XmlElement(required = true)
	public Date dateOuverture;

	/** La date de fermeture du for fiscal. Si le for est toujours actif, cette date n'est pas renseignée. */
	@XmlElement(required = false)
	public Date dateFermeture;

	/** Date à laquelle le for fiscal a été annulé, ou <b>null</b> s'il n'est pas annulé. */
	@XmlElement(required = false)
	public Date dateAnnulation;

	@XmlElement(required = true)
	public GenreImpot genreImpot;

	/** Cet enum permet d'interpréter le numéro OFS contenu dans noOfsAutoriteFiscale */
	@XmlElement(required = true)
	public TypeAutoriteFiscale typeAutoriteFiscale;

	/** Numéro OFS étendu de la commune, du canton ou du pays de l'autorité fiscale du fors */
	@XmlElement(required = true)
	public int noOfsAutoriteFiscale;

	/** Le motif de rattachement est uniquement renseignée sur les fors fiscaux de genre = REVENU_FORTUNE */
	@XmlElement(required = false)
	public MotifRattachement motifRattachement;

	/** Le motif d'ouverture est uniquement renseignée sur les fors fiscaux de genre = REVENU_FORTUNE */
	@XmlElement(required = false)
	public MotifFor motifOuverture;

	/** Le motif de fermeture est uniquement renseignée sur les fors fiscaux de genre = REVENU_FORTUNE */
	@XmlElement(required = false)
	public MotifFor motifFermeture;

	/**
	 * Le mode d'imposition est uniquement renseignée sur les fors fiscaux de genre = REVENU_FORTUNE et de rattachement = {DOMICILE;
	 * DIPLOMATE}
	 */
	@XmlElement(required = false)
	public ModeImposition modeImposition;

	/**
	 * Si <b>vrai</b>, le for fiscal n'existe pas en tel que tel, mais est une vue construite en fonction de règles métier.
	 * <p>
	 * <b>Exemple:</b> les fors fiscaux principaux individuels des sourciers appartenant à des ménages-communs sont virtuels, car les fors
	 * fiscaux réels sont définis sur les contribuables ménages.
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

	public Date getDateDebut() {
		return dateOuverture;
	}

	public Date getDateFin() {
		return dateFermeture;
	}

	public void setDateDebut(Date v) {
		dateOuverture = v;
	}

	public void setDateFin(Date v) {
		dateFermeture = v;
	}
}
