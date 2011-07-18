package ch.vd.uniregctb.webservices.tiers3.impl;

import org.springmodules.xt.model.generator.support.IllegalArgumentPositionException;

import ch.vd.unireg.webservices.tiers3.AccountNumberFormat;
import ch.vd.unireg.webservices.tiers3.ActivityType;
import ch.vd.unireg.webservices.tiers3.CommunicationMode;
import ch.vd.unireg.webservices.tiers3.DebtorCategory;
import ch.vd.unireg.webservices.tiers3.DocumentType;
import ch.vd.unireg.webservices.tiers3.LiabilityChangeReason;
import ch.vd.unireg.webservices.tiers3.MaritalStatus;
import ch.vd.unireg.webservices.tiers3.NaturalPersonCategory;
import ch.vd.unireg.webservices.tiers3.RelationBetweenPartiesType;
import ch.vd.unireg.webservices.tiers3.SearchMode;
import ch.vd.unireg.webservices.tiers3.Sex;
import ch.vd.unireg.webservices.tiers3.TaxDeclarationStatusType;
import ch.vd.unireg.webservices.tiers3.TaxLiabilityReason;
import ch.vd.unireg.webservices.tiers3.TaxType;
import ch.vd.unireg.webservices.tiers3.TaxationAuthorityType;
import ch.vd.unireg.webservices.tiers3.TaxationMethod;
import ch.vd.unireg.webservices.tiers3.WithholdingTaxDeclarationPeriod;
import ch.vd.unireg.webservices.tiers3.WithholdingTaxDeclarationPeriodicity;
import ch.vd.unireg.webservices.tiers3.WithholdingTaxTariff;
import ch.vd.unireg.webservices.tiers3.address.TariffZone;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.TypePermis;

public abstract class EnumHelper {

	public static MaritalStatus coreToWeb(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return MaritalStatus.SINGLE;
		case DIVORCE:
			return MaritalStatus.DIVORCED;
		case LIE_PARTENARIAT_ENREGISTRE:
			return MaritalStatus.REGISTERED_PARTNER;
		case MARIE:
			return MaritalStatus.MARRIED;
		case NON_MARIE:
			return MaritalStatus.NOT_MARRIED;
		case PARTENARIAT_DISSOUS_DECES:
			return MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH;
		case PARTENARIAT_DISSOUS_JUDICIAIREMENT:
			return MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case SEPARE:
			return MaritalStatus.SEPARATED;
		case VEUF:
			return MaritalStatus.WIDOWED;
		default:
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etatCivil + "]");
		}
	}

	public static MaritalStatus coreToWeb(ch.vd.uniregctb.interfaces.model.TypeEtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return MaritalStatus.SINGLE;
		case DIVORCE:
			return MaritalStatus.DIVORCED;
		case PACS:
			return MaritalStatus.REGISTERED_PARTNER;
		case MARIE:
			return MaritalStatus.MARRIED;
		case PACS_ANNULE:
		case PACS_INTERROMPU:
			return MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case SEPARE:
			return MaritalStatus.SEPARATED;
		case VEUF:
			return MaritalStatus.WIDOWED;
		default:
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etatCivil + "]");
		}
	}

	public static DebtorCategory coreToWeb(ch.vd.uniregctb.type.CategorieImpotSource categorieImpotSource) {
		if (categorieImpotSource == null) {
			return null;
		}

		switch (categorieImpotSource) {
		case ADMINISTRATEURS:
			return DebtorCategory.ADMINISTRATORS;
		case CONFERENCIERS_ARTISTES_SPORTIFS:
			return DebtorCategory.SPEAKERS_ARTISTS_SPORTSMEN;
		case CREANCIERS_HYPOTHECAIRES:
			return DebtorCategory.MORTGAGE_CREDITORS;
		case LOI_TRAVAIL_AU_NOIR:
			return DebtorCategory.LAW_ON_UNDECLARED_WORK;
		case PRESTATIONS_PREVOYANCE:
			return DebtorCategory.PENSION_FUND;
		case REGULIERS:
			return DebtorCategory.REGULAR;
		default:
			throw new IllegalArgumentException("Type de catégorie impôt source inconnu = [" + categorieImpotSource + "]");
		}
	}

	public static TaxDeclarationStatusType coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case ECHUE:
			return TaxDeclarationStatusType.EXPIRED;
		case EMISE:
			return TaxDeclarationStatusType.SENT;
		case SOMMEE:
			return TaxDeclarationStatusType.SUMMONS_SENT;
		case RETOURNEE:
			return TaxDeclarationStatusType.RETURNED;
		default:
			throw new IllegalArgumentException("Type d'état de déclaration inconnu = [" + type + "]");
		}
	}

	public static TaxType coreToWeb(ch.vd.uniregctb.type.GenreImpot genreImpot) {
		if (genreImpot == null) {
			return null;
		}

		switch (genreImpot) {
		case BENEFICE_CAPITAL:
			return TaxType.PROFITS_CAPITAL;
		case CHIENS:
			return TaxType.DOGS;
		case DEBITEUR_PRESTATION_IMPOSABLE:
			return TaxType.DEBTOR_TAXABLE_INCOME;
		case DONATION:
			return TaxType.GIFTS;
		case DROIT_MUTATION:
			return TaxType.REAL_ESTATE_TRANSFER;
		case FONCIER:
			return TaxType.REAL_ESTATE;
		case GAIN_IMMOBILIER:
			return TaxType.IMMOVABLE_PROPERTY_GAINS;
		case PATENTE_TABAC:
			return TaxType.TOBACCO_PATENT;
		case PRESTATION_CAPITAL:
			return TaxType.CAPITAL_INCOME;
		case REVENU_FORTUNE:
			return TaxType.INCOME_WEALTH;
		case SUCCESSION:
			return TaxType.INHERITANCE;
		default:
			throw new IllegalArgumentException("Genre d'impôt inconnu = [" + genreImpot + "]");
		}
	}

	public static TaxLiabilityReason coreToWeb(ch.vd.uniregctb.type.MotifRattachement rattachement) {
		if (rattachement == null) {
			return null;
		}

		switch (rattachement) {
		case ACTIVITE_INDEPENDANTE:
			return TaxLiabilityReason.INDEPENDANT_ACTIVITY;
		case ACTIVITE_LUCRATIVE_CAS:
			return TaxLiabilityReason.GAINFUL_ACTIVITY_SAS;
		case ADMINISTRATEUR:
			return TaxLiabilityReason.ADMINISTRATOR;
		case CREANCIER_HYPOTHECAIRE:
			return TaxLiabilityReason.MORTGAGE_CREDITORS;
		case DIPLOMATE_ETRANGER:
			return TaxLiabilityReason.FOREIGN_DIPLOMAT;
		case DIPLOMATE_SUISSE:
			return TaxLiabilityReason.SWISS_DIPLOMAT;
		case DIRIGEANT_SOCIETE:
			return TaxLiabilityReason.COMPANY_LEADER;
		case DOMICILE:
			return TaxLiabilityReason.RESIDENCE;
		case ETABLISSEMENT_STABLE:
			return TaxLiabilityReason.STABLE_ESTABLISHMENT;
		case IMMEUBLE_PRIVE:
			return TaxLiabilityReason.PRIVATE_IMMOVABLE_PROPERTY;
		case LOI_TRAVAIL_AU_NOIR:
			return TaxLiabilityReason.LAW_ON_UNDECLARED_WORK;
		case PRESTATION_PREVOYANCE:
			return TaxLiabilityReason.PENSION;
		case SEJOUR_SAISONNIER:
			return TaxLiabilityReason.SEASONAL_JOURNEY;
		default:
			throw new IllegalArgumentException("Motif de rattachement inconnu = [" + rattachement + "]");
		}
	}

	public static TaxationAuthorityType coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale typeForFiscal) {
		if (typeForFiscal == null) {
			return null;
		}

		switch (typeForFiscal) {
		case COMMUNE_OU_FRACTION_VD:
			return TaxationAuthorityType.VAUD_MUNICIPALITY;
		case COMMUNE_HC:
			return TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY;
		case PAYS_HS:
			return TaxationAuthorityType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu = [" + typeForFiscal + "]");
		}
	}

	public static TaxationMethod coreToWeb(ch.vd.uniregctb.type.ModeImposition mode) {
		if (mode == null) {
			return null;
		}

		switch (mode) {
		case ORDINAIRE:
			return TaxationMethod.ORDINARY;
		case SOURCE:
			return TaxationMethod.WITHHOLDING;
		case DEPENSE:
			return TaxationMethod.EXPENDITURE_BASED;
		case INDIGENT:
			return TaxationMethod.INDIGENT;
		case MIXTE_137_1:
			return TaxationMethod.MIXED_137_1;
		case MIXTE_137_2:
			return TaxationMethod.MIXED_137_2;
		default:
			throw new IllegalArgumentException("Mode d'imposition inconnu = [" + mode + "]");
		}
	}

	public static CommunicationMode coreToWeb(ch.vd.uniregctb.type.ModeCommunication mode) {
		if (mode == null) {
			return null;
		}

		switch (mode) {
		case ELECTRONIQUE:
			return CommunicationMode.UPLOAD;
		case PAPIER:
			return CommunicationMode.PAPER;
		case SITE_WEB:
			return CommunicationMode.WEB_SITE;
		default:
			throw new IllegalArgumentException("Mode de communicaiton inconnu = [" + mode + "]");
		}
	}

	public static WithholdingTaxDeclarationPeriod coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte periodeDecompte) {
		if (periodeDecompte == null) {
			return null;
		}

		switch (periodeDecompte) {
		case M01:
			return WithholdingTaxDeclarationPeriod.M_01;
		case M02:
			return WithholdingTaxDeclarationPeriod.M_02;
		case M03:
			return WithholdingTaxDeclarationPeriod.M_03;
		case M04:
			return WithholdingTaxDeclarationPeriod.M_04;
		case M05:
			return WithholdingTaxDeclarationPeriod.M_05;
		case M06:
			return WithholdingTaxDeclarationPeriod.M_06;
		case M07:
			return WithholdingTaxDeclarationPeriod.M_07;
		case M08:
			return WithholdingTaxDeclarationPeriod.M_08;
		case M09:
			return WithholdingTaxDeclarationPeriod.M_09;
		case M10:
			return WithholdingTaxDeclarationPeriod.M_10;
		case M11:
			return WithholdingTaxDeclarationPeriod.M_11;
		case M12:
			return WithholdingTaxDeclarationPeriod.M_12;
		case T1:
			return WithholdingTaxDeclarationPeriod.Q_1;
		case T2:
			return WithholdingTaxDeclarationPeriod.Q_2;
		case T3:
			return WithholdingTaxDeclarationPeriod.Q_3;
		case T4:
			return WithholdingTaxDeclarationPeriod.Q_4;
		case S1:
			return WithholdingTaxDeclarationPeriod.H_1;
		case S2:
			return WithholdingTaxDeclarationPeriod.H_2;
		case A:
			return WithholdingTaxDeclarationPeriod.Y;
		default:
			throw new IllegalArgumentException("Type de période de décompte inconnu = [" + periodeDecompte + "]");
		}
	}

	public static WithholdingTaxDeclarationPeriodicity coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte periodiciteDecompte) {
		if (periodiciteDecompte == null) {
			return null;
		}

		switch (periodiciteDecompte) {
		case ANNUEL:
			return WithholdingTaxDeclarationPeriodicity.YEARLY;
		case MENSUEL:
			return WithholdingTaxDeclarationPeriodicity.MONTHLY;
		case SEMESTRIEL:
			return WithholdingTaxDeclarationPeriodicity.HALF_YEARLY;
		case TRIMESTRIEL:
			return WithholdingTaxDeclarationPeriodicity.QUARTERLY;
		case UNIQUE:
			return WithholdingTaxDeclarationPeriodicity.ONCE;
		default:
			throw new IllegalArgumentException("Type de périodicité décompte inconnu = [" + periodiciteDecompte + "]");
		}
	}

	public static NaturalPersonCategory coreToWeb(ch.vd.uniregctb.type.CategorieEtranger categorie) {
		if (categorie == null) {
			return null;
		}

		switch (categorie) {
		case _02_PERMIS_SEJOUR_B:
			return NaturalPersonCategory.C_02_B_PERMIT;
		case _03_ETABLI_C:
			return NaturalPersonCategory.C_03_C_PERMIT;
		case _04_CONJOINT_DIPLOMATE_CI:
			return NaturalPersonCategory.C_04_CI_PERMIT;
		case _05_ETRANGER_ADMIS_PROVISOIREMENT_F:
			return NaturalPersonCategory.C_05_F_PERMIT;
		case _06_FRONTALIER_G:
			return NaturalPersonCategory.C_06_G_PERMIT;
		case _07_PERMIS_SEJOUR_COURTE_DUREE_L:
			return NaturalPersonCategory.C_07_L_PERMIT;
		case _08_REQUERANT_ASILE_N:
			return NaturalPersonCategory.C_08_N_PERMIT;
		case _09_A_PROTEGER_S:
			return NaturalPersonCategory.C_09_S_PERMIT;
		case _10_TENUE_DE_S_ANNONCER:
			return NaturalPersonCategory.C_10_OBLIGED_TO_ANNOUNCE;
		case _11_DIPLOMATE:
			return NaturalPersonCategory.C_11_DIPLOMAT;
		case _12_FONCTIONNAIRE_INTERNATIONAL:
			return NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case _13_NON_ATTRIBUEE:
			return NaturalPersonCategory.C_13_NOT_ASSIGNED;
		default:
			throw new IllegalArgumentException("Catégorie d'étranger inconnue = [" + categorie + "]");
		}
	}

	public static NaturalPersonCategory coreToWeb(TypePermis permis) {
		if (permis == null) {
			return null;
		}

		switch (permis) {
		case ANNUEL:
			return NaturalPersonCategory.C_02_B_PERMIT;
		case COURTE_DUREE:
			return NaturalPersonCategory.C_07_L_PERMIT;
		case DIPLOMATE:
			return NaturalPersonCategory.C_11_DIPLOMAT;
		case ETABLISSEMENT:
			return NaturalPersonCategory.C_03_C_PERMIT;
		case FONCTIONNAIRE_INTERNATIONAL:
			return NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case FRONTALIER:
			return NaturalPersonCategory.C_06_G_PERMIT;
		case PERSONNE_A_PROTEGER:
			return NaturalPersonCategory.C_09_S_PERMIT;
		case PROVISOIRE:
			return NaturalPersonCategory.C_05_F_PERMIT;
		case REQUERANT_ASILE_AVANT_DECISION:
			return NaturalPersonCategory.C_08_N_PERMIT;
		case REQUERANT_ASILE_REFUSE:
			return NaturalPersonCategory.C_05_F_PERMIT;
		case SUISSE_SOURCIER:
			return NaturalPersonCategory.SWISS;
		default:
			throw new IllegalArgumentPositionException("Type de permis inconnu = [" + permis + "]");
		}
	}

	public static RelationBetweenPartiesType coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case APPARTENANCE_MENAGE:
			return RelationBetweenPartiesType.HOUSEHOLD_MEMBER;
		case ANNULE_ET_REMPLACE:
			return RelationBetweenPartiesType.CANCELS_AND_REPLACES;
		case CONSEIL_LEGAL:
			return RelationBetweenPartiesType.LEGAL_ADVISER;
		case CONTACT_IMPOT_SOURCE:
			return RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT;
		case CURATELLE:
			return RelationBetweenPartiesType.WELFARE_ADVOCATE;
		case PRESTATION_IMPOSABLE:
			return RelationBetweenPartiesType.TAXABLE_REVENUE;
		case REPRESENTATION:
			return RelationBetweenPartiesType.REPRESENTATIVE;
		case TUTELLE:
			return RelationBetweenPartiesType.GUARDIAN;
		default:
			throw new IllegalArgumentException("Type de rapport-entre-tiers inconnu = [" + type + "]");
		}
	}

	public static WithholdingTaxTariff coreToWeb(ch.vd.uniregctb.type.TarifImpotSource tarif) {
		if (tarif == null) {
			return null;
		}

		switch (tarif) {
		case DOUBLE_GAIN:
			return WithholdingTaxTariff.DOUBLE_REVENUE;
		case NORMAL:
			return WithholdingTaxTariff.NORMAL;
		default:
			throw new IllegalArgumentException("Type de tarif inconnu = [" + tarif + "]");
		}
	}

	public static ActivityType coreToWeb(ch.vd.uniregctb.type.TypeActivite typeActivite) {
		if (typeActivite == null) {
			return null;
		}

		switch (typeActivite) {
		case ACCESSOIRE:
			return ActivityType.ACCESSORY;
		case COMPLEMENTAIRE:
			return ActivityType.COMPLEMENTARY;
		case PRINCIPALE:
			return ActivityType.MAIN;
		default:
			throw new IllegalArgumentException("Type d'activité inconnu = [" + typeActivite + "]");
		}
	}

	public static DocumentType coreToWeb(ch.vd.uniregctb.type.TypeDocument type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case DECLARATION_IMPOT_VAUDTAX:
			return DocumentType.VAUDTAX_TAX_DECLARATION;
		case DECLARATION_IMPOT_COMPLETE_BATCH:
		case DECLARATION_IMPOT_COMPLETE_LOCAL:
			return DocumentType.FULL_TAX_DECLARATION;
		case DECLARATION_IMPOT_DEPENSE:
			return DocumentType.EXPENDITURE_BASED_TAX_DECLARATION;
		case DECLARATION_IMPOT_HC_IMMEUBLE:
			return DocumentType.IMMOVABLE_PROPERTY_OTHER_CANTON_TAX_DECLARATION;
		default:
			throw new IllegalArgumentException("Type de document inconnu = [" + type + "]");
		}
	}

	public static LiabilityChangeReason coreToWeb(ch.vd.uniregctb.type.MotifFor ouverture) {
		if (ouverture == null) {
			return null;
		}

		switch (ouverture) {
		case ACHAT_IMMOBILIER:
			return LiabilityChangeReason.PURCHASE_REAL_ESTATE;
		case ANNULATION:
			return LiabilityChangeReason.CANCELLATION;
		case ARRIVEE_HC:
			return LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON;
		case ARRIVEE_HS:
			return LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY;
		case CHGT_MODE_IMPOSITION:
			return LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD;
		case DEBUT_ACTIVITE_DIPLOMATIQUE:
			return LiabilityChangeReason.START_DIPLOMATIC_ACTVITY;
		case DEBUT_EXPLOITATION:
			return LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION;
		case DEMENAGEMENT_VD:
			return LiabilityChangeReason.MOVE_VD;
		case DEPART_HC:
			return LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON;
		case DEPART_HS:
			return LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY;
		case FIN_ACTIVITE_DIPLOMATIQUE:
			return LiabilityChangeReason.END_DIPLOMATIC_ACTVITY;
		case FIN_EXPLOITATION:
			return LiabilityChangeReason.END_COMMERCIAL_EXPLOITATION;
		case FUSION_COMMUNES:
			return LiabilityChangeReason.MERGE_OF_MUNICIPALITIES;
		case INDETERMINE:
			return LiabilityChangeReason.UNDETERMINED;
		case MAJORITE:
			return LiabilityChangeReason.MAJORITY;
		case MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION:
			return LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION;
		case PERMIS_C_SUISSE:
			return LiabilityChangeReason.C_PERMIT_SWISS;
		case REACTIVATION:
			return LiabilityChangeReason.REACTIVATION;
		case SEJOUR_SAISONNIER:
			return LiabilityChangeReason.SEASONAL_JOURNEY;
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			return LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION;
		case VENTE_IMMOBILIER:
			return LiabilityChangeReason.SALE_REAL_ESTATE;
		case VEUVAGE_DECES:
			return LiabilityChangeReason.WIDOWHOOD_DEATH;
		default:
			throw new IllegalArgumentException("Motif de for inconnu = [" + ouverture + "]");
		}
	}

	public static SearchMode coreToWeb(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case CONTIENT:
			return SearchMode.CONTAINS;
		case EST_EXACTEMENT:
			return SearchMode.IS_EXACTLY;
		case PHONETIQUE:
			return SearchMode.PHONETIC;
		default:
			throw new IllegalArgumentException("Type de recherche inconnu = [" + type + "]");
		}
	}

	public static ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche webToCore(SearchMode type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case CONTAINS:
			return TiersCriteria.TypeRecherche.CONTIENT;
		case IS_EXACTLY:
			return TiersCriteria.TypeRecherche.EST_EXACTEMENT;
		case PHONETIC:
			return TiersCriteria.TypeRecherche.PHONETIQUE;
		default:
			throw new IllegalArgumentException("Type de recherche inconnu = [" + type + "]");
		}
	}

	public static Sex coreToWeb(ch.vd.uniregctb.type.Sexe sexe) {
		if (sexe == null) {
			return null;
		}

		switch (sexe) {
		case FEMININ:
			return Sex.FEMALE;
		case MASCULIN:
			return Sex.MALE;
		default:
			throw new IllegalArgumentException("Type de sexe inconnu = [" + sexe + "]"); // certainement qu'il vient d'une autre dimension
		}
	}

//	public static Parts webToCore(PartyPart p) {
//		if (p == null) {
//			return null;
//		}
//
//		switch (p) {
//		case ADDRESSES:
//			return Parts.ADRESSES;
//		case BANK_ACCOUNTS:
//		case CAPITALS:
//		case CORPORATION_STATUSES:
//		case DEBTOR_PERIODICITIES:
//		case FAMILY_STATUSES:
//		case FORMATTED_ADDRESSES:
//		case HOUSEHOLD_MEMBERS:
//		case LEGAL_FORMS:
//		case LEGAL_SEATS:
//		case MANAGING_TAX_RESIDENCES:
//		case ORDINARY_TAX_LIABILITIES:
//		case RELATIONS_BETWEEN_PARTIES:
//		case SIMPLIFIED_TAX_LIABILITIES:
//		case TAX_DECLARATIONS:
//		case TAX_DECLARATIONS_STATUSES:
//		case TAX_RESIDENCES:
//		case TAX_SYSTEMS:
//		case TAXATION_PERIODS:
//		case VIRTUAL_TAX_RESIDENCES:
//		}
//		final Parts part = Parts.fromValue(p.name());
//		Assert.notNull(part);
//		return part;
//	}

	public static TariffZone coreToWeb(ch.vd.uniregctb.interfaces.model.TypeAffranchissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case SUISSE:
			return TariffZone.SWITZERLAND;
		case EUROPE:
			return TariffZone.EUROPE;
		case MONDE:
			return TariffZone.OTHER_COUNTRIES;
		default:
			throw new IllegalArgumentException("Type d'affranchissement inconnu = [" + t + "]");
		}
	}

	public static String coreToEch44(ch.vd.uniregctb.type.Sexe sexe) {
		if (sexe == null) {
			return null;
		}
		switch (sexe) {
		case MASCULIN:
			return "1";
		case FEMININ:
			return "2";
		default:
			throw new IllegalArgumentException("Type de sexe inconnu = [" + sexe + "]");
		}
	}

	public static AccountNumberFormat coreToWeb(CompteBancaire.Format format) {
		if (format == null) {
			return null;
		}
		switch (format) {
		case SPECIFIQUE_CH:
			return AccountNumberFormat.SWISS_SPECIFIC;
		case IBAN:
			return AccountNumberFormat.IBAN;
		default:
			throw new IllegalArgumentException("Format de compte bancaire inconnu = [" + format + "]");
		}
	}
}
