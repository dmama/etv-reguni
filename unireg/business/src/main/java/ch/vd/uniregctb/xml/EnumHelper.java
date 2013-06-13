package ch.vd.uniregctb.xml;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.infra.data.TypeAffranchissement;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.party.immovableproperty.v1.ImmovablePropertyType;
import ch.vd.unireg.xml.party.immovableproperty.v1.MutationType;
import ch.vd.unireg.xml.party.immovableproperty.v1.OwnershipType;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.rf.TypeImmeuble;
import ch.vd.uniregctb.rf.TypeMutation;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.StatutMenageCommun;
import ch.vd.uniregctb.type.TypePermis;

public abstract class EnumHelper {

	public static ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus coreToXMLv1(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.SINGLE;
		case DIVORCE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.DIVORCED;
		case LIE_PARTENARIAT_ENREGISTRE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.REGISTERED_PARTNER;
		case MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.MARRIED;
		case NON_MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.NOT_MARRIED;
		case PARTENARIAT_DISSOUS_DECES:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH;
		case PARTENARIAT_DISSOUS_JUDICIAIREMENT:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case SEPARE:
		case PARTENARIAT_SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.SEPARATED;
		case VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.WIDOWED;
		default:
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etatCivil + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus coreToXMLv2(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.SINGLE;
		case DIVORCE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.DIVORCED;
		case LIE_PARTENARIAT_ENREGISTRE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.REGISTERED_PARTNER;
		case MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.MARRIED;
		case NON_MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.NOT_MARRIED;
		case PARTENARIAT_DISSOUS_DECES:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH;
		case PARTENARIAT_DISSOUS_JUDICIAIREMENT:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case SEPARE:
		case PARTENARIAT_SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.SEPARATED;
		case VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.WIDOWED;
		default:
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etatCivil + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus coreToXMLv1(TypeEtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.SINGLE;
		case DIVORCE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.DIVORCED;
		case PACS:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.REGISTERED_PARTNER;
		case MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.MARRIED;
		case PACS_TERMINE:
		case PACS_SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case PACS_VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH;
		case SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.SEPARATED;
		case VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.WIDOWED;
		case NON_MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v1.MaritalStatus.NOT_MARRIED;
		default:
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etatCivil + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus coreToXMLv2(TypeEtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.SINGLE;
		case DIVORCE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.DIVORCED;
		case PACS:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.REGISTERED_PARTNER;
		case MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.MARRIED;
		case PACS_TERMINE:
		case PACS_SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case PACS_VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH;
		case SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.SEPARATED;
		case VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.WIDOWED;
		case NON_MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus.NOT_MARRIED;
		default:
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etatCivil + ']');
		}
	}

	public static final Set<CategorieImpotSource> CIS_SUPPORTEES_V1 = Collections.unmodifiableSet(EnumSet.of(CategorieImpotSource.ADMINISTRATEURS, CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS,
	                                                                                                         CategorieImpotSource.CREANCIERS_HYPOTHECAIRES, CategorieImpotSource.LOI_TRAVAIL_AU_NOIR,
	                                                                                                         CategorieImpotSource.PRESTATIONS_PREVOYANCE, CategorieImpotSource.REGULIERS));

	public static final Set<CategorieImpotSource> CIS_SUPPORTEES_V2 = Collections.unmodifiableSet(EnumSet.of(CategorieImpotSource.ADMINISTRATEURS, CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS,
	                                                                                                         CategorieImpotSource.CREANCIERS_HYPOTHECAIRES, CategorieImpotSource.LOI_TRAVAIL_AU_NOIR,
	                                                                                                         CategorieImpotSource.PRESTATIONS_PREVOYANCE, CategorieImpotSource.REGULIERS,
	                                                                                                         CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, CategorieImpotSource.EFFEUILLEUSES));

	private static void checkCategorieImpotSourceSupportee(Set<CategorieImpotSource> allowed, CategorieImpotSource candidate) {
		if (!allowed.contains(candidate)) {
			throw new IllegalArgumentException("Type de catégorie impôt source non supporté dans cette version du service");
		}
	}

	public static ch.vd.unireg.xml.party.debtor.v1.DebtorCategory coreToXMLv1(CategorieImpotSource categorieImpotSource) {
		if (categorieImpotSource == null) {
			return null;
		}

		checkCategorieImpotSourceSupportee(CIS_SUPPORTEES_V1, categorieImpotSource);

		switch (categorieImpotSource) {
		case ADMINISTRATEURS:
			return ch.vd.unireg.xml.party.debtor.v1.DebtorCategory.ADMINISTRATORS;
		case CONFERENCIERS_ARTISTES_SPORTIFS:
			return ch.vd.unireg.xml.party.debtor.v1.DebtorCategory.SPEAKERS_ARTISTS_SPORTSMEN;
		case CREANCIERS_HYPOTHECAIRES:
			return ch.vd.unireg.xml.party.debtor.v1.DebtorCategory.MORTGAGE_CREDITORS;
		case LOI_TRAVAIL_AU_NOIR:
			return ch.vd.unireg.xml.party.debtor.v1.DebtorCategory.LAW_ON_UNDECLARED_WORK;
		case PRESTATIONS_PREVOYANCE:
			return ch.vd.unireg.xml.party.debtor.v1.DebtorCategory.PENSION_FUND;
		case REGULIERS:
			return ch.vd.unireg.xml.party.debtor.v1.DebtorCategory.REGULAR;
		default:
			throw new IllegalArgumentException("Type de catégorie impôt source inconnu = [" + categorieImpotSource + ']');
		}
	}

	public static ch.vd.unireg.xml.party.debtor.v2.DebtorCategory coreToXMLv2(CategorieImpotSource categorieImpotSource) {
		if (categorieImpotSource == null) {
			return null;
		}

		checkCategorieImpotSourceSupportee(CIS_SUPPORTEES_V2, categorieImpotSource);

		switch (categorieImpotSource) {
		case ADMINISTRATEURS:
			return ch.vd.unireg.xml.party.debtor.v2.DebtorCategory.ADMINISTRATORS;
		case CONFERENCIERS_ARTISTES_SPORTIFS:
			return ch.vd.unireg.xml.party.debtor.v2.DebtorCategory.SPEAKERS_ARTISTS_SPORTSMEN;
		case CREANCIERS_HYPOTHECAIRES:
			return ch.vd.unireg.xml.party.debtor.v2.DebtorCategory.MORTGAGE_CREDITORS;
		case LOI_TRAVAIL_AU_NOIR:
			return ch.vd.unireg.xml.party.debtor.v2.DebtorCategory.LAW_ON_UNDECLARED_WORK;
		case PRESTATIONS_PREVOYANCE:
			return ch.vd.unireg.xml.party.debtor.v2.DebtorCategory.PENSION_FUND;
		case REGULIERS:
			return ch.vd.unireg.xml.party.debtor.v2.DebtorCategory.REGULAR;
		case PARTICIPATIONS_HORS_SUISSE:
			return ch.vd.unireg.xml.party.debtor.v2.DebtorCategory.PROFIT_SHARING_FOREIGN_COUNTRY_TAXPAYERS;
		case EFFEUILLEUSES:
			return ch.vd.unireg.xml.party.debtor.v2.DebtorCategory.SEASONAL_WORKERS;
		default:
			throw new IllegalArgumentException("Type de catégorie impôt source inconnu = [" + categorieImpotSource + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatusType coreToXMLv1(ch.vd.uniregctb.type.TypeEtatDeclaration type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case ECHUE:
			return ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatusType.EXPIRED;
		case EMISE:
			return ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatusType.SENT;
		case SOMMEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatusType.SUMMONS_SENT;
		case RETOURNEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationStatusType.RETURNED;
		default:
			throw new IllegalArgumentException("Type d'état de déclaration inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclarationStatusType coreToXMLv2(ch.vd.uniregctb.type.TypeEtatDeclaration type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case ECHUE:
			return ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclarationStatusType.EXPIRED;
		case EMISE:
			return ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclarationStatusType.SENT;
		case SOMMEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclarationStatusType.SUMMONS_SENT;
		case RETOURNEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclarationStatusType.RETURNED;
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

	public static ch.vd.unireg.xml.party.debtor.v1.CommunicationMode coreToXMLv1(ch.vd.uniregctb.type.ModeCommunication mode) {
		if (mode == null) {
			return null;
		}

		switch (mode) {
		case ELECTRONIQUE:
			return ch.vd.unireg.xml.party.debtor.v1.CommunicationMode.UPLOAD;
		case PAPIER:
			return ch.vd.unireg.xml.party.debtor.v1.CommunicationMode.PAPER;
		case SITE_WEB:
			return ch.vd.unireg.xml.party.debtor.v1.CommunicationMode.WEB_SITE;
		default:
			throw new IllegalArgumentException("Mode de communicaiton inconnu = [" + mode + ']');
		}
	}

	public static ch.vd.unireg.xml.party.debtor.v2.CommunicationMode coreToXMLv2(ch.vd.uniregctb.type.ModeCommunication mode) {
		if (mode == null) {
			return null;
		}

		switch (mode) {
		case ELECTRONIQUE:
			return ch.vd.unireg.xml.party.debtor.v2.CommunicationMode.UPLOAD;
		case PAPIER:
			return ch.vd.unireg.xml.party.debtor.v2.CommunicationMode.PAPER;
		case SITE_WEB:
			return ch.vd.unireg.xml.party.debtor.v2.CommunicationMode.WEB_SITE;
		default:
			throw new IllegalArgumentException("Mode de communicaiton inconnu = [" + mode + ']');
		}
	}

	public static ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod coreToXMLv1(ch.vd.uniregctb.type.PeriodeDecompte periodeDecompte) {
		if (periodeDecompte == null) {
			return null;
		}

		switch (periodeDecompte) {
		case M01:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_01;
		case M02:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_02;
		case M03:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_03;
		case M04:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_04;
		case M05:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_05;
		case M06:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_06;
		case M07:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_07;
		case M08:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_08;
		case M09:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_09;
		case M10:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_10;
		case M11:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_11;
		case M12:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.M_12;
		case T1:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.Q_1;
		case T2:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.Q_2;
		case T3:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.Q_3;
		case T4:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.Q_4;
		case S1:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.H_1;
		case S2:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.H_2;
		case A:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriod.Y;
		default:
			throw new IllegalArgumentException("Type de période de décompte inconnu = [" + periodeDecompte + ']');
		}
	}

	public static ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod coreToXMLv2(ch.vd.uniregctb.type.PeriodeDecompte periodeDecompte) {
		if (periodeDecompte == null) {
			return null;
		}

		switch (periodeDecompte) {
		case M01:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_01;
		case M02:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_02;
		case M03:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_03;
		case M04:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_04;
		case M05:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_05;
		case M06:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_06;
		case M07:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_07;
		case M08:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_08;
		case M09:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_09;
		case M10:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_10;
		case M11:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_11;
		case M12:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.M_12;
		case T1:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.Q_1;
		case T2:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.Q_2;
		case T3:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.Q_3;
		case T4:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.Q_4;
		case S1:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.H_1;
		case S2:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.H_2;
		case A:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod.Y;
		default:
			throw new IllegalArgumentException("Type de période de décompte inconnu = [" + periodeDecompte + ']');
		}
	}

	public static ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriodicity coreToXMLv1(ch.vd.uniregctb.type.PeriodiciteDecompte periodiciteDecompte) {
		if (periodiciteDecompte == null) {
			return null;
		}

		switch (periodiciteDecompte) {
		case ANNUEL:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriodicity.YEARLY;
		case MENSUEL:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriodicity.MONTHLY;
		case SEMESTRIEL:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriodicity.HALF_YEARLY;
		case TRIMESTRIEL:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriodicity.QUARTERLY;
		case UNIQUE:
			return ch.vd.unireg.xml.party.debtor.v1.WithholdingTaxDeclarationPeriodicity.ONCE;
		default:
			throw new IllegalArgumentException("Type de périodicité décompte inconnu = [" + periodiciteDecompte + ']');
		}
	}

	public static ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriodicity coreToXMLv2(ch.vd.uniregctb.type.PeriodiciteDecompte periodiciteDecompte) {
		if (periodiciteDecompte == null) {
			return null;
		}

		switch (periodiciteDecompte) {
		case ANNUEL:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriodicity.YEARLY;
		case MENSUEL:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriodicity.MONTHLY;
		case SEMESTRIEL:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriodicity.HALF_YEARLY;
		case TRIMESTRIEL:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriodicity.QUARTERLY;
		case UNIQUE:
			return ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriodicity.ONCE;
		default:
			throw new IllegalArgumentException("Type de périodicité décompte inconnu = [" + periodiciteDecompte + ']');
		}
	}

	public static ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory coreToXMLv1(ch.vd.uniregctb.type.CategorieEtranger categorie) {
		if (categorie == null) {
			return null;
		}

		switch (categorie) {
		case _02_PERMIS_SEJOUR_B:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_02_B_PERMIT;
		case _03_ETABLI_C:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_03_C_PERMIT;
		case _04_CONJOINT_DIPLOMATE_CI:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_04_CI_PERMIT;
		case _05_ETRANGER_ADMIS_PROVISOIREMENT_F:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_05_F_PERMIT;
		case _06_FRONTALIER_G:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_06_G_PERMIT;
		case _07_PERMIS_SEJOUR_COURTE_DUREE_L:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_07_L_PERMIT;
		case _08_REQUERANT_ASILE_N:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_08_N_PERMIT;
		case _09_A_PROTEGER_S:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_09_S_PERMIT;
		case _10_TENUE_DE_S_ANNONCER:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_10_OBLIGED_TO_ANNOUNCE;
		case _11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_11_DIPLOMAT;
		case _12_FONCT_INTER_SANS_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case _13_NON_ATTRIBUEE:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_13_NOT_ASSIGNED;
		default:
			throw new IllegalArgumentException("Catégorie d'étranger inconnue = [" + categorie + ']');
		}
	}

	public static ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory coreToXMLv2(ch.vd.uniregctb.type.CategorieEtranger categorie) {
		if (categorie == null) {
			return null;
		}

		switch (categorie) {
		case _02_PERMIS_SEJOUR_B:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_02_B_PERMIT;
		case _03_ETABLI_C:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_03_C_PERMIT;
		case _04_CONJOINT_DIPLOMATE_CI:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_04_CI_PERMIT;
		case _05_ETRANGER_ADMIS_PROVISOIREMENT_F:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_05_F_PERMIT;
		case _06_FRONTALIER_G:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_06_G_PERMIT;
		case _07_PERMIS_SEJOUR_COURTE_DUREE_L:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_07_L_PERMIT;
		case _08_REQUERANT_ASILE_N:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_08_N_PERMIT;
		case _09_A_PROTEGER_S:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_09_S_PERMIT;
		case _10_TENUE_DE_S_ANNONCER:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_10_OBLIGED_TO_ANNOUNCE;
		case _11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_11_DIPLOMAT;
		case _12_FONCT_INTER_SANS_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case _13_NON_ATTRIBUEE:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_13_NOT_ASSIGNED;
		default:
			throw new IllegalArgumentException("Catégorie d'étranger inconnue = [" + categorie + ']');
		}
	}

	public static ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory coreToXMLv1(TypePermis permis) {
		if (permis == null) {
			return null;
		}
		switch (permis) {
		case SAISONNIER:
			throw new IllegalArgumentException("Type de permis illégal = [" + permis + ']');
		case SEJOUR:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_02_B_PERMIT;
		case ETABLISSEMENT:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_03_C_PERMIT;
		case CONJOINT_DIPLOMATE:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_04_CI_PERMIT;
		case ETRANGER_ADMIS_PROVISOIREMENT:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_05_F_PERMIT;
		case FRONTALIER:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_06_G_PERMIT;
		case COURTE_DUREE:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_07_L_PERMIT;
		case REQUERANT_ASILE:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_08_N_PERMIT;
		case PERSONNE_A_PROTEGER:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_09_S_PERMIT;
		case PERSONNE_TENUE_DE_S_ANNONCER:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_10_OBLIGED_TO_ANNOUNCE;
		case DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_11_DIPLOMAT;
		case FONCT_INTER_SANS_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case PAS_ATTRIBUE:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_13_NOT_ASSIGNED;
		case PROVISOIRE:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.C_13_NOT_ASSIGNED;
		case SUISSE_SOURCIER:
			return ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory.SWISS;
		default:
			throw new IllegalArgumentException("Type de permis inconnu = [" + permis + ']');
		}
	}

	public static ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory coreToXMLv2(TypePermis permis) {
		if (permis == null) {
			return null;
		}
		switch (permis) {
		case SAISONNIER:
			throw new IllegalArgumentException("Type de permis illégal = [" + permis + ']');
		case SEJOUR:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_02_B_PERMIT;
		case ETABLISSEMENT:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_03_C_PERMIT;
		case CONJOINT_DIPLOMATE:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_04_CI_PERMIT;
		case ETRANGER_ADMIS_PROVISOIREMENT:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_05_F_PERMIT;
		case FRONTALIER:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_06_G_PERMIT;
		case COURTE_DUREE:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_07_L_PERMIT;
		case REQUERANT_ASILE:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_08_N_PERMIT;
		case PERSONNE_A_PROTEGER:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_09_S_PERMIT;
		case PERSONNE_TENUE_DE_S_ANNONCER:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_10_OBLIGED_TO_ANNOUNCE;
		case DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_11_DIPLOMAT;
		case FONCT_INTER_SANS_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case PAS_ATTRIBUE:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_13_NOT_ASSIGNED;
		case PROVISOIRE:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.C_13_NOT_ASSIGNED;
		case SUISSE_SOURCIER:
			return ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory.SWISS;
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

	public static ch.vd.unireg.xml.party.taxpayer.v1.WithholdingTaxTariff coreToXMLv1(ch.vd.uniregctb.type.TarifImpotSource tarif) {
		if (tarif == null) {
			return null;
		}

		switch (tarif) {
		case DOUBLE_GAIN:
			return ch.vd.unireg.xml.party.taxpayer.v1.WithholdingTaxTariff.DOUBLE_REVENUE;
		case NORMAL:
			return ch.vd.unireg.xml.party.taxpayer.v1.WithholdingTaxTariff.NORMAL;
		default:
			throw new IllegalArgumentException("Type de tarif inconnu = [" + tarif + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxpayer.v2.WithholdingTaxTariff coreToXMLv2(ch.vd.uniregctb.type.TarifImpotSource tarif) {
		if (tarif == null) {
			return null;
		}

		switch (tarif) {
		case DOUBLE_GAIN:
			return ch.vd.unireg.xml.party.taxpayer.v2.WithholdingTaxTariff.DOUBLE_REVENUE;
		case NORMAL:
			return ch.vd.unireg.xml.party.taxpayer.v2.WithholdingTaxTariff.NORMAL;
		default:
			throw new IllegalArgumentException("Type de tarif inconnu = [" + tarif + ']');
		}
	}



	public static ch.vd.unireg.xml.party.taxdeclaration.v1.DocumentType coreToXMLv1(ch.vd.uniregctb.type.TypeDocument type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case DECLARATION_IMPOT_VAUDTAX:
			return ch.vd.unireg.xml.party.taxdeclaration.v1.DocumentType.VAUDTAX_TAX_DECLARATION;
		case DECLARATION_IMPOT_COMPLETE_BATCH:
		case DECLARATION_IMPOT_COMPLETE_LOCAL:
			return ch.vd.unireg.xml.party.taxdeclaration.v1.DocumentType.FULL_TAX_DECLARATION;
		case DECLARATION_IMPOT_DEPENSE:
			return ch.vd.unireg.xml.party.taxdeclaration.v1.DocumentType.EXPENDITURE_BASED_TAX_DECLARATION;
		case DECLARATION_IMPOT_HC_IMMEUBLE:
			return ch.vd.unireg.xml.party.taxdeclaration.v1.DocumentType.IMMOVABLE_PROPERTY_OTHER_CANTON_TAX_DECLARATION;
		default:
			throw new IllegalArgumentException("Type de document inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxdeclaration.v2.DocumentType coreToXMLv2(ch.vd.uniregctb.type.TypeDocument type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case DECLARATION_IMPOT_VAUDTAX:
			return ch.vd.unireg.xml.party.taxdeclaration.v2.DocumentType.VAUDTAX_TAX_DECLARATION;
		case DECLARATION_IMPOT_COMPLETE_BATCH:
		case DECLARATION_IMPOT_COMPLETE_LOCAL:
			return ch.vd.unireg.xml.party.taxdeclaration.v2.DocumentType.FULL_TAX_DECLARATION;
		case DECLARATION_IMPOT_DEPENSE:
			return ch.vd.unireg.xml.party.taxdeclaration.v2.DocumentType.EXPENDITURE_BASED_TAX_DECLARATION;
		case DECLARATION_IMPOT_HC_IMMEUBLE:
			return ch.vd.unireg.xml.party.taxdeclaration.v2.DocumentType.IMMOVABLE_PROPERTY_OTHER_CANTON_TAX_DECLARATION;
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

	public static ch.vd.unireg.xml.party.person.v1.Sex coreToXMLv1(ch.vd.uniregctb.type.Sexe sexe) {
		if (sexe == null) {
			return null;
		}

		switch (sexe) {
		case FEMININ:
			return ch.vd.unireg.xml.party.person.v1.Sex.FEMALE;
		case MASCULIN:
			return ch.vd.unireg.xml.party.person.v1.Sex.MALE;
		default:
			throw new IllegalArgumentException("Type de sexe inconnu = [" + sexe + ']'); // certainement qu'il vient d'une autre dimension
		}
	}

	public static ch.vd.unireg.xml.party.person.v2.Sex coreToXMLv2(ch.vd.uniregctb.type.Sexe sexe) {
		if (sexe == null) {
			return null;
		}

		switch (sexe) {
		case FEMININ:
			return ch.vd.unireg.xml.party.person.v2.Sex.FEMALE;
		case MASCULIN:
			return ch.vd.unireg.xml.party.person.v2.Sex.MALE;
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

	public static TariffZone coreToXML(TypeAffranchissement t) {
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

	public static ch.vd.unireg.xml.party.v1.AccountNumberFormat coreToXMLv1(CompteBancaire.Format format) {
		if (format == null) {
			return null;
		}
		switch (format) {
		case SPECIFIQUE_CH:
			return ch.vd.unireg.xml.party.v1.AccountNumberFormat.SWISS_SPECIFIC;
		case IBAN:
			return ch.vd.unireg.xml.party.v1.AccountNumberFormat.IBAN;
		default:
			throw new IllegalArgumentException("Format de compte bancaire inconnu = [" + format + ']');
		}
	}

	public static ch.vd.unireg.xml.party.v2.AccountNumberFormat coreToXMLv2(CompteBancaire.Format format) {
		if (format == null) {
			return null;
		}
		switch (format) {
		case SPECIFIQUE_CH:
			return ch.vd.unireg.xml.party.v2.AccountNumberFormat.SWISS_SPECIFIC;
		case IBAN:
			return ch.vd.unireg.xml.party.v2.AccountNumberFormat.IBAN;
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

	public static CategorieImpotSource xmlToCore(ch.vd.unireg.xml.party.debtor.v1.DebtorCategory category) {
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

	public static CategorieImpotSource xmlToCore(ch.vd.unireg.xml.party.debtor.v2.DebtorCategory category) {
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
		case PROFIT_SHARING_FOREIGN_COUNTRY_TAXPAYERS:
			return CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE;
		case SEASONAL_WORKERS:
			return CategorieImpotSource.EFFEUILLEUSES;
		default:
			throw new IllegalArgumentException("Catégorie de débiteur inconnue = [" + category + "]");
		}
	}

	public static TiersCriteria.TypeTiers xmlToCore(ch.vd.unireg.xml.party.v1.PartyType type) {
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

	public static TiersCriteria.TypeTiers xmlToCore(ch.vd.unireg.xml.party.v2.PartyType type) {
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

	public static CategorieEtranger xmlToCore(ch.vd.unireg.xml.party.person.v1.NaturalPersonCategory category) {
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

	public static CategorieEtranger xmlToCore(ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory category) {
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

	public static Sexe xmlToCore(ch.vd.unireg.xml.party.person.v1.Sex sex) {
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

	public static Sexe xmlToCore(ch.vd.unireg.xml.party.person.v2.Sex sex) {
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

	public static StatutMenageCommun xmlToCore(ch.vd.unireg.xml.party.person.v1.CommonHouseholdStatus chs) {
		if (chs == null) {
			return null;
		}
		switch (chs) {
		case ACTIVE:
			return StatutMenageCommun.EN_VIGUEUR;
		case ENDED_BY_DEATH:
			return StatutMenageCommun.TERMINE_SUITE_DECES;
		case SEPARATED_DIVORCED:
			return StatutMenageCommun.TERMINE_SUITE_SEPARATION;
		default:
			throw new IllegalArgumentException("unknown CommonHouseholdStatus = [" + chs + ']');
		}
	}

	public static StatutMenageCommun xmlToCore(ch.vd.unireg.xml.party.person.v2.CommonHouseholdStatus chs) {
		if (chs == null) {
			return null;
		}
		switch (chs) {
		case ACTIVE:
			return StatutMenageCommun.EN_VIGUEUR;
		case ENDED_BY_DEATH:
			return StatutMenageCommun.TERMINE_SUITE_DECES;
		case SEPARATED_DIVORCED:
			return StatutMenageCommun.TERMINE_SUITE_SEPARATION;
		default:
			throw new IllegalArgumentException("unknown CommonHouseholdStatus = [" + chs + ']');
		}
	}

	public static ch.vd.unireg.xml.party.person.v1.CommonHouseholdStatus coreToXMLv1(StatutMenageCommun setc) {
		if (setc == null) {
			return null;
		}
		switch (setc) {
		case EN_VIGUEUR:
			return ch.vd.unireg.xml.party.person.v1.CommonHouseholdStatus.ACTIVE;
		case TERMINE_SUITE_DECES:
			return ch.vd.unireg.xml.party.person.v1.CommonHouseholdStatus.ENDED_BY_DEATH;
		case TERMINE_SUITE_SEPARATION:
			return ch.vd.unireg.xml.party.person.v1.CommonHouseholdStatus.SEPARATED_DIVORCED;
		default:
			throw new IllegalArgumentException("unknown StatutMenageCommun = [" + setc + ']');
		}
	}

	public static ch.vd.unireg.xml.party.person.v2.CommonHouseholdStatus coreToXMLv2(StatutMenageCommun setc) {
		if (setc == null) {
			return null;
		}
		switch (setc) {
		case EN_VIGUEUR:
			return ch.vd.unireg.xml.party.person.v2.CommonHouseholdStatus.ACTIVE;
		case TERMINE_SUITE_DECES:
			return ch.vd.unireg.xml.party.person.v2.CommonHouseholdStatus.ENDED_BY_DEATH;
		case TERMINE_SUITE_SEPARATION:
			return ch.vd.unireg.xml.party.person.v2.CommonHouseholdStatus.SEPARATED_DIVORCED;
		default:
			throw new IllegalArgumentException("unknown StatutMenageCommun = [" + setc + ']');
		}
	}



}
