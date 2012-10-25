package ch.vd.uniregctb.xml;

import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.party.debtor.v1.CommunicationMode;
import ch.vd.unireg.xml.party.debtor.v1.DebtorCategory;
import ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod;
import ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriodicity;
import ch.vd.unireg.xml.party.immovableproperty.v1.ImmovablePropertyType;
import ch.vd.unireg.xml.party.immovableproperty.v1.MutationType;
import ch.vd.unireg.xml.party.immovableproperty.v1.OwnershipType;
import ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v1.Sex;
import ch.vd.unireg.xml.party.relation.v1.ActivityType;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.unireg.xml.party.taxdeclaration.v1.DocumentType;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus;
import ch.vd.unireg.xml.party.taxpayer.v1.WithholdingTaxTariff;
import ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod;
import ch.vd.unireg.xml.party.v1.AccountNumberFormat;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.rf.TypeImmeuble;
import ch.vd.uniregctb.rf.TypeMutation;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypePermis;

public abstract class EnumHelper {

	public static MaritalStatus coreToXML(ch.vd.uniregctb.type.EtatCivil etatCivil) {
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
		case PARTENARIAT_SEPARE:
			return MaritalStatus.SEPARATED;
		case VEUF:
			return MaritalStatus.WIDOWED;
		default:
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etatCivil + ']');
		}
	}

	public static MaritalStatus coreToXML(TypeEtatCivil etatCivil) {
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
		case PACS_TERMINE:
		case PACS_SEPARE:
			return MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case PACS_VEUF:
			return MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH;
		case SEPARE:
			return MaritalStatus.SEPARATED;
		case VEUF:
			return MaritalStatus.WIDOWED;
		case NON_MARIE:
			return MaritalStatus.NOT_MARRIED;
		default:
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etatCivil + ']');
		}
	}

	public static DebtorCategory coreToXML(CategorieImpotSource categorieImpotSource) {
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
			throw new IllegalArgumentException("Type de catégorie impôt source inconnu = [" + categorieImpotSource + ']');
		}
	}

	public static TaxDeclarationStatusType coreToXML(ch.vd.uniregctb.type.TypeEtatDeclaration type) {
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
			throw new IllegalArgumentException("Type d'état de déclaration inconnu = [" + type + ']');
		}
	}

	public static TaxType coreToXML(ch.vd.uniregctb.type.GenreImpot genreImpot) {
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
			throw new IllegalArgumentException("Genre d'impôt inconnu = [" + genreImpot + ']');
		}
	}

	public static TaxLiabilityReason coreToXML(ch.vd.uniregctb.type.MotifRattachement rattachement) {
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
			throw new IllegalArgumentException("Motif de rattachement inconnu = [" + rattachement + ']');
		}
	}

	public static TaxationAuthorityType coreToXML(ch.vd.uniregctb.type.TypeAutoriteFiscale typeForFiscal) {
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
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu = [" + typeForFiscal + ']');
		}
	}

	public static TaxationMethod coreToXML(ch.vd.uniregctb.type.ModeImposition mode) {
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
			throw new IllegalArgumentException("Mode d'imposition inconnu = [" + mode + ']');
		}
	}

	public static CommunicationMode coreToXML(ch.vd.uniregctb.type.ModeCommunication mode) {
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
			throw new IllegalArgumentException("Mode de communicaiton inconnu = [" + mode + ']');
		}
	}

	public static WithholdingTaxDeclarationPeriod coreToXML(ch.vd.uniregctb.type.PeriodeDecompte periodeDecompte) {
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
			throw new IllegalArgumentException("Type de période de décompte inconnu = [" + periodeDecompte + ']');
		}
	}

	public static WithholdingTaxDeclarationPeriodicity coreToXML(ch.vd.uniregctb.type.PeriodiciteDecompte periodiciteDecompte) {
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
			throw new IllegalArgumentException("Type de périodicité décompte inconnu = [" + periodiciteDecompte + ']');
		}
	}

	public static NaturalPersonCategory coreToXML(ch.vd.uniregctb.type.CategorieEtranger categorie) {
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
		case _11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return NaturalPersonCategory.C_11_DIPLOMAT;
		case _12_FONCT_INTER_SANS_IMMUNITE:
			return NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case _13_NON_ATTRIBUEE:
			return NaturalPersonCategory.C_13_NOT_ASSIGNED;
		default:
			throw new IllegalArgumentException("Catégorie d'étranger inconnue = [" + categorie + ']');
		}
	}

	public static NaturalPersonCategory coreToXML(TypePermis permis) {
		if (permis == null) {
			return null;
		}
		switch (permis) {
		case SAISONNIER:
			throw new IllegalArgumentException("Type de permis illégal = [" + permis + ']');
		case SEJOUR:
			return NaturalPersonCategory.C_02_B_PERMIT;
		case ETABLISSEMENT:
			return NaturalPersonCategory.C_03_C_PERMIT;
		case CONJOINT_DIPLOMATE:
			return NaturalPersonCategory.C_04_CI_PERMIT;
		case ETRANGER_ADMIS_PROVISOIREMENT:
			return NaturalPersonCategory.C_05_F_PERMIT;
		case FRONTALIER:
			return NaturalPersonCategory.C_06_G_PERMIT;
		case COURTE_DUREE:
			return NaturalPersonCategory.C_07_L_PERMIT;
		case REQUERANT_ASILE:
			return NaturalPersonCategory.C_08_N_PERMIT;
		case PERSONNE_A_PROTEGER:
			return NaturalPersonCategory.C_09_S_PERMIT;
		case PERSONNE_TENUE_DE_S_ANNONCER:
			return NaturalPersonCategory.C_10_OBLIGED_TO_ANNOUNCE;
		case DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return NaturalPersonCategory.C_11_DIPLOMAT;
		case FONCT_INTER_SANS_IMMUNITE:
			return NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case PAS_ATTRIBUE:
			return NaturalPersonCategory.C_13_NOT_ASSIGNED;
		case PROVISOIRE:
			return NaturalPersonCategory.C_13_NOT_ASSIGNED;
		case SUISSE_SOURCIER:
			return NaturalPersonCategory.SWISS;
		default:
			throw new IllegalArgumentException("Type de permis inconnu = [" + permis + ']');
		}
	}

	public static RelationBetweenPartiesType coreToXML(ch.vd.uniregctb.type.TypeRapportEntreTiers type) {
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
			throw new IllegalArgumentException("Type de rapport-entre-tiers inconnu = [" + type + ']');
		}
	}

	public static WithholdingTaxTariff coreToXML(ch.vd.uniregctb.type.TarifImpotSource tarif) {
		if (tarif == null) {
			return null;
		}

		switch (tarif) {
		case DOUBLE_GAIN:
			return WithholdingTaxTariff.DOUBLE_REVENUE;
		case NORMAL:
			return WithholdingTaxTariff.NORMAL;
		default:
			throw new IllegalArgumentException("Type de tarif inconnu = [" + tarif + ']');
		}
	}

	public static ActivityType coreToXML(ch.vd.uniregctb.type.TypeActivite typeActivite) {
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
			throw new IllegalArgumentException("Type d'activité inconnu = [" + typeActivite + ']');
		}
	}

	public static DocumentType coreToXML(ch.vd.uniregctb.type.TypeDocument type) {
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
			throw new IllegalArgumentException("Type de document inconnu = [" + type + ']');
		}
	}

	public static LiabilityChangeReason coreToXML(ch.vd.uniregctb.type.MotifFor ouverture) {
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
			throw new IllegalArgumentException("Motif de for inconnu = [" + ouverture + ']');
		}
	}

	public static Sex coreToXML(ch.vd.uniregctb.type.Sexe sexe) {
		if (sexe == null) {
			return null;
		}

		switch (sexe) {
		case FEMININ:
			return Sex.FEMALE;
		case MASCULIN:
			return Sex.MALE;
		default:
			throw new IllegalArgumentException("Type de sexe inconnu = [" + sexe + ']'); // certainement qu'il vient d'une autre dimension
		}
	}

//	public static Parts xmlToCore(PartyPart p) {
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

	public static TariffZone coreToXML(ch.vd.uniregctb.interfaces.model.TypeAffranchissement t) {
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
			throw new IllegalArgumentException("Type d'affranchissement inconnu = [" + t + ']');
		}
	}

	public static AccountNumberFormat coreToXML(CompteBancaire.Format format) {
		if (format == null) {
			return null;
		}
		switch (format) {
		case SPECIFIQUE_CH:
			return AccountNumberFormat.SWISS_SPECIFIC;
		case IBAN:
			return AccountNumberFormat.IBAN;
		default:
			throw new IllegalArgumentException("Format de compte bancaire inconnu = [" + format + ']');
		}
	}

	public static OwnershipType coreToXML(GenrePropriete genre) {
		if (genre == null) {
			return null;
		}
		switch (genre) {
		case INDIVIDUELLE:
			return OwnershipType.SOLE_OWNERSHIP;
		case COPROPRIETE:
			return OwnershipType.SIMPLE_CO_OWNERSHIP;
		case COMMUNE:
			return OwnershipType.COLLECTIVE_OWNERSHIP;
		default:
			throw new IllegalArgumentException("Genre de priopriété inconnu = [" + genre + ']');
		}
	}

	public static ImmovablePropertyType coreToXML(TypeImmeuble type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case BIEN_FOND:
			return ImmovablePropertyType.IMMOVABLE_PROPERTY;
		case PPE:
			return ImmovablePropertyType.CONDOMINIUM_OWNERSHIP;
		case DROIT_DISTINCT_ET_PERMANENT:
			return ImmovablePropertyType.DISTINCT_AND_PERMANENT_RIGHT;
		case PART_DE_COPROPRIETE:
			return ImmovablePropertyType.CO_OWNERSHIP_SHARE;
		default:
			throw new IllegalArgumentException("Type de priopriété inconnu = [" + type + ']');
		}
	}

	public static MutationType coreToXML(TypeMutation type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case ACHAT:
			return MutationType.PURCHASE;
		case AUGMENTATION:
			return MutationType.INCREASE;
		case CESSION:
			return MutationType.CESSION;
		case CONSTITUTION_PPE:
			return MutationType.CONDOMINIUM_OWNERSHIP_COMPOSITION;
		case CONSTITUTION_PARTS_PROPRIETE:
			return MutationType.CO_OWNERSHIP_SHARES_COMPOSITION;
		case DIVISION_BIEN_FONDS:
			return MutationType.IMMOVABLE_PROPERTY_DIVISION;
		case DONATION:
			return MutationType.GIFT;
		case DELIVRANCE_LEGS:
			return MutationType.LEGACY_DELIVERY;
		case ECHANGE:
			return MutationType.EXCHANGE;
		case GROUPEMENT_BIEN_FONDS:
			return MutationType.IMMOVABLE_PROPERTY_GROUPING;
		case JUGEMENT:
			return MutationType.JUDGEMENT;
		case PARTAGE:
			return MutationType.SHARING;
		case REMANIEMENT_PARCELLAIRE:
			return MutationType.PLOT_REFORM;
		case REALISATION_FORCEE:
			return MutationType.COMPULSARY_SALE;
		case SUCCESSION:
			return MutationType.INHERITANCE;
		case TRANSFERT:
			return MutationType.TRANSFER;
		case FIN_DE_PROPRIETE:
			return MutationType.END_OF_OWNERSHIP;
		default:
			throw new IllegalArgumentException("Type de mutation inconnu = [" + type + ']');
		}
	}

	public static CategorieImpotSource xmlToCore(DebtorCategory category) {
		if (category == null) {
			return null;
		}
		switch (category) {
		case ADMINISTRATORS:
			return CategorieImpotSource.ADMINISTRATEURS;
		case LAW_ON_UNDECLARED_WORK:
			return CategorieImpotSource.LOI_TRAVAIL_AU_NOIR;
		case MORTGAGE_CREDITORS:
			return CategorieImpotSource.CREANCIERS_HYPOTHECAIRES;
		case PENSION_FUND:
			return CategorieImpotSource.PRESTATIONS_PREVOYANCE;
		case REGULAR:
			return CategorieImpotSource.REGULIERS;
		case SPEAKERS_ARTISTS_SPORTSMEN:
			return CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS;
		default:
			throw new IllegalArgumentException("Catégorie de débiteur inconnue = [" + category + "]");
		}
	}

	public static TiersCriteria.TypeTiers xmlToCore(PartyType type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case DEBTOR:
			return TiersCriteria.TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE;
		case HOUSEHOLD:
			return TiersCriteria.TypeTiers.MENAGE_COMMUN;
		case CORPORATION:
			return TiersCriteria.TypeTiers.ENTREPRISE;
		case NATURAL_PERSON:
			return TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + ']');
		}
	}

	public static CategorieEtranger xmlToCore(NaturalPersonCategory category) {
		if (category== null) {
			return null;
		}
		switch (category) {
		case SWISS:
			return null;
		case C_02_B_PERMIT:
			return CategorieEtranger._02_PERMIS_SEJOUR_B;
		case C_03_C_PERMIT:
			return CategorieEtranger._03_ETABLI_C;
		case C_04_CI_PERMIT:
			return CategorieEtranger._04_CONJOINT_DIPLOMATE_CI;
		case C_05_F_PERMIT:
			return CategorieEtranger._05_ETRANGER_ADMIS_PROVISOIREMENT_F;
		case C_06_G_PERMIT:
			return CategorieEtranger._06_FRONTALIER_G;
		case C_07_L_PERMIT:
			return CategorieEtranger._07_PERMIS_SEJOUR_COURTE_DUREE_L;
		case C_08_N_PERMIT:
			return CategorieEtranger._08_REQUERANT_ASILE_N;
		case C_09_S_PERMIT:
			return CategorieEtranger._09_A_PROTEGER_S;
		case C_10_OBLIGED_TO_ANNOUNCE:
			return CategorieEtranger._10_TENUE_DE_S_ANNONCER;
		case C_11_DIPLOMAT:
			return CategorieEtranger._11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE;
		case C_12_INTERNATIONAL_CIVIL_SERVANT:
			return CategorieEtranger._12_FONCT_INTER_SANS_IMMUNITE;
		case C_13_NOT_ASSIGNED:
			return CategorieEtranger._13_NON_ATTRIBUEE;
		default:
			throw new IllegalArgumentException("unknown NaturalPersonCategory = [" + category + ']');
		}
	}

	public static Sexe xmlToCore(Sex sex) {
		if (sex == null) {
			return null;
		}
		switch (sex) {
		case MALE:
			return Sexe.MASCULIN;
		case FEMALE:
			return Sexe.FEMININ;
		default:
			throw new IllegalArgumentException("unknown Sex = [" + sex + ']');
		}
	}


}
