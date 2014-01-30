package ch.vd.uniregctb.xml;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.interfaces.infra.data.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSource;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.rf.TypeImmeuble;
import ch.vd.uniregctb.rf.TypeMutation;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.FormeJuridique;
import ch.vd.uniregctb.type.ModeCommunication;
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

	public static ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus coreToXMLv3(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.SINGLE;
		case DIVORCE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.DIVORCED;
		case LIE_PARTENARIAT_ENREGISTRE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.REGISTERED_PARTNER;
		case MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.MARRIED;
		case NON_MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.NOT_MARRIED;
		case PARTENARIAT_DISSOUS_DECES:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH;
		case PARTENARIAT_DISSOUS_JUDICIAIREMENT:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.SEPARATED;
		case PARTENARIAT_SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.PARTNERSHIP_SEPARATED;
		case VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.WIDOWED;
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

	public static ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus coreToXMLv3(TypeEtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.SINGLE;
		case DIVORCE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.DIVORCED;
		case PACS:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.REGISTERED_PARTNER;
		case MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.MARRIED;
		case PACS_TERMINE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case PACS_SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.PARTNERSHIP_SEPARATED;
		case PACS_VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH;
		case SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.SEPARATED;
		case VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.WIDOWED;
		case NON_MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus.NOT_MARRIED;
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

	public static final Set<CategorieImpotSource> CIS_SUPPORTEES_V3 = Collections.unmodifiableSet(EnumSet.of(CategorieImpotSource.ADMINISTRATEURS, CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS,
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

	public static ch.vd.unireg.xml.party.withholding.v1.DebtorCategory coreToXMLv3(CategorieImpotSource categorieImpotSource) {
		if (categorieImpotSource == null) {
			return null;
		}

		checkCategorieImpotSourceSupportee(CIS_SUPPORTEES_V3, categorieImpotSource);

		switch (categorieImpotSource) {
		case ADMINISTRATEURS:
			return ch.vd.unireg.xml.party.withholding.v1.DebtorCategory.ADMINISTRATORS;
		case CONFERENCIERS_ARTISTES_SPORTIFS:
			return ch.vd.unireg.xml.party.withholding.v1.DebtorCategory.SPEAKERS_ARTISTS_SPORTSMEN;
		case CREANCIERS_HYPOTHECAIRES:
			return ch.vd.unireg.xml.party.withholding.v1.DebtorCategory.MORTGAGE_CREDITORS;
		case LOI_TRAVAIL_AU_NOIR:
			return ch.vd.unireg.xml.party.withholding.v1.DebtorCategory.LAW_ON_UNDECLARED_WORK;
		case PRESTATIONS_PREVOYANCE:
			return ch.vd.unireg.xml.party.withholding.v1.DebtorCategory.PENSION_FUND;
		case REGULIERS:
			return ch.vd.unireg.xml.party.withholding.v1.DebtorCategory.REGULAR;
		case PARTICIPATIONS_HORS_SUISSE:
			return ch.vd.unireg.xml.party.withholding.v1.DebtorCategory.PROFIT_SHARING_FOREIGN_COUNTRY_TAXPAYERS;
		case EFFEUILLEUSES:
			return ch.vd.unireg.xml.party.withholding.v1.DebtorCategory.WINE_FARM_SEASONAL_WORKERS;
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

	public static ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatusType coreToXMLv3(ch.vd.uniregctb.type.TypeEtatDeclaration type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case ECHUE:
			return ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatusType.EXPIRED;
		case EMISE:
			return ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatusType.SENT;
		case SOMMEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatusType.SUMMONS_SENT;
		case RETOURNEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatusType.RETURNED;
		default:
			throw new IllegalArgumentException("Type d'état de déclaration inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v1.TaxType coreToXMLv1(ch.vd.uniregctb.type.GenreImpot genreImpot) {
		if (genreImpot == null) {
			return null;
		}

		switch (genreImpot) {
		case BENEFICE_CAPITAL:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.PROFITS_CAPITAL;
		case CHIENS:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.DOGS;
		case DEBITEUR_PRESTATION_IMPOSABLE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.DEBTOR_TAXABLE_INCOME;
		case DONATION:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.GIFTS;
		case DROIT_MUTATION:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.REAL_ESTATE_TRANSFER;
		case FONCIER:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.REAL_ESTATE;
		case GAIN_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.IMMOVABLE_PROPERTY_GAINS;
		case PATENTE_TABAC:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.TOBACCO_PATENT;
		case PRESTATION_CAPITAL:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.CAPITAL_INCOME;
		case REVENU_FORTUNE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.INCOME_WEALTH;
		case SUCCESSION:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxType.INHERITANCE;
		default:
			throw new IllegalArgumentException("Genre d'impôt inconnu = [" + genreImpot + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v2.TaxType coreToXMLv2(ch.vd.uniregctb.type.GenreImpot genreImpot) {
		if (genreImpot == null) {
			return null;
		}

		switch (genreImpot) {
		case BENEFICE_CAPITAL:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.PROFITS_CAPITAL;
		case CHIENS:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.DOGS;
		case DEBITEUR_PRESTATION_IMPOSABLE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.DEBTOR_TAXABLE_INCOME;
		case DONATION:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.GIFTS;
		case DROIT_MUTATION:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.REAL_ESTATE_TRANSFER;
		case FONCIER:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.REAL_ESTATE;
		case GAIN_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.IMMOVABLE_PROPERTY_GAINS;
		case PATENTE_TABAC:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.TOBACCO_PATENT;
		case PRESTATION_CAPITAL:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.CAPITAL_INCOME;
		case REVENU_FORTUNE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.INCOME_WEALTH;
		case SUCCESSION:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxType.INHERITANCE;
		default:
			throw new IllegalArgumentException("Genre d'impôt inconnu = [" + genreImpot + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason coreToXMLv1(ch.vd.uniregctb.type.MotifRattachement rattachement) {
		if (rattachement == null) {
			return null;
		}

		switch (rattachement) {
		case ACTIVITE_INDEPENDANTE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.INDEPENDANT_ACTIVITY;
		case ACTIVITE_LUCRATIVE_CAS:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.GAINFUL_ACTIVITY_SAS;
		case PARTICIPATIONS_HORS_SUISSE:
		case ADMINISTRATEUR:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.ADMINISTRATOR;
		case CREANCIER_HYPOTHECAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.MORTGAGE_CREDITORS;
		case DIPLOMATE_ETRANGER:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.FOREIGN_DIPLOMAT;
		case DIPLOMATE_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.SWISS_DIPLOMAT;
		case DIRIGEANT_SOCIETE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.COMPANY_LEADER;
		case DOMICILE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.RESIDENCE;
		case ETABLISSEMENT_STABLE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.STABLE_ESTABLISHMENT;
		case IMMEUBLE_PRIVE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.PRIVATE_IMMOVABLE_PROPERTY;
		case LOI_TRAVAIL_AU_NOIR:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.LAW_ON_UNDECLARED_WORK;
		case PRESTATION_PREVOYANCE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.PENSION;
		case EFFEUILLEUSES:
		case SEJOUR_SAISONNIER:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason.SEASONAL_JOURNEY;
		default:
			throw new IllegalArgumentException("Motif de rattachement inconnu = [" + rattachement + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason coreToXMLv2(ch.vd.uniregctb.type.MotifRattachement rattachement) {
		if (rattachement == null) {
			return null;
		}

		switch (rattachement) {
		case ACTIVITE_INDEPENDANTE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.INDEPENDANT_ACTIVITY;
		case ACTIVITE_LUCRATIVE_CAS:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.GAINFUL_ACTIVITY_SAS;
		case ADMINISTRATEUR:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.ADMINISTRATOR;
		case CREANCIER_HYPOTHECAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.MORTGAGE_CREDITORS;
		case DIPLOMATE_ETRANGER:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.FOREIGN_DIPLOMAT;
		case DIPLOMATE_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.SWISS_DIPLOMAT;
		case DIRIGEANT_SOCIETE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.COMPANY_LEADER;
		case DOMICILE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.RESIDENCE;
		case ETABLISSEMENT_STABLE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.STABLE_ESTABLISHMENT;
		case IMMEUBLE_PRIVE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.PRIVATE_IMMOVABLE_PROPERTY;
		case LOI_TRAVAIL_AU_NOIR:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.LAW_ON_UNDECLARED_WORK;
		case PRESTATION_PREVOYANCE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.PENSION;
		case SEJOUR_SAISONNIER:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.SEASONAL_JOURNEY;
		case EFFEUILLEUSES:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.WINE_FARM_SEASONAL_WORKER;
		case PARTICIPATIONS_HORS_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason.PROFIT_SHARING_FOREIGN_COUNTRY_TAXPAYER;
		default:
			throw new IllegalArgumentException("Motif de rattachement inconnu = [" + rattachement + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType coreToXMLv1(ch.vd.uniregctb.type.TypeAutoriteFiscale typeForFiscal) {
		if (typeForFiscal == null) {
			return null;
		}

		switch (typeForFiscal) {
		case COMMUNE_OU_FRACTION_VD:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType.VAUD_MUNICIPALITY;
		case COMMUNE_HC:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY;
		case PAYS_HS:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu = [" + typeForFiscal + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v2.TaxationAuthorityType coreToXMLv2(ch.vd.uniregctb.type.TypeAutoriteFiscale typeForFiscal) {
		if (typeForFiscal == null) {
			return null;
		}

		switch (typeForFiscal) {
		case COMMUNE_OU_FRACTION_VD:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxationAuthorityType.VAUD_MUNICIPALITY;
		case COMMUNE_HC:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY;
		case PAYS_HS:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxationAuthorityType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu = [" + typeForFiscal + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod coreToXMLv1(ch.vd.uniregctb.type.ModeImposition mode) {
		if (mode == null) {
			return null;
		}

		switch (mode) {
		case ORDINAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod.ORDINARY;
		case SOURCE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod.WITHHOLDING;
		case DEPENSE:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod.EXPENDITURE_BASED;
		case INDIGENT:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod.INDIGENT;
		case MIXTE_137_1:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod.MIXED_137_1;
		case MIXTE_137_2:
			return ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod.MIXED_137_2;
		default:
			throw new IllegalArgumentException("Mode d'imposition inconnu = [" + mode + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod coreToXMLv2(ch.vd.uniregctb.type.ModeImposition mode) {
		if (mode == null) {
			return null;
		}

		switch (mode) {
		case ORDINAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod.ORDINARY;
		case SOURCE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod.WITHHOLDING;
		case DEPENSE:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod.EXPENDITURE_BASED;
		case INDIGENT:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod.INDIGENT;
		case MIXTE_137_1:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod.MIXED_137_1;
		case MIXTE_137_2:
			return ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod.MIXED_137_2;
		default:
			throw new IllegalArgumentException("Mode d'imposition inconnu = [" + mode + ']');
		}
	}

	public static ch.vd.unireg.xml.party.debtor.v1.CommunicationMode coreToXMLv1(ModeCommunication mode) {
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
			throw new IllegalArgumentException("Mode de communication inconnu = [" + mode + ']');
		}
	}

	public static ch.vd.unireg.xml.party.debtor.v2.CommunicationMode coreToXMLv2(ModeCommunication mode) {
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
			throw new IllegalArgumentException("Mode de communication inconnu = [" + mode + ']');
		}
	}

	public static ch.vd.unireg.xml.party.withholding.v1.CommunicationMode coreToXMLv3(ModeCommunication mode) {
		if (mode == null) {
			return null;
		}

		switch (mode) {
		case ELECTRONIQUE:
			return ch.vd.unireg.xml.party.withholding.v1.CommunicationMode.UPLOAD;
		case PAPIER:
			return ch.vd.unireg.xml.party.withholding.v1.CommunicationMode.PAPER;
		case SITE_WEB:
			return ch.vd.unireg.xml.party.withholding.v1.CommunicationMode.WEB_SITE;
		default:
			throw new IllegalArgumentException("Mode de communication inconnu = [" + mode + ']');
		}
	}

	public static ModeCommunication xmlToCore(ch.vd.unireg.xml.party.withholding.v1.CommunicationMode mode) {
		if (mode == null) {
			return null;
		}

		switch (mode) {
		case PAPER:
			return ModeCommunication.PAPIER;
		case UPLOAD:
			return ModeCommunication.ELECTRONIQUE;
		case WEB_SITE:
			return ModeCommunication.SITE_WEB;
		default:
			throw new IllegalArgumentException("Mode de communication inconnu = [" + mode + ']');
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

	public static ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod coreToXMLv3(ch.vd.uniregctb.type.PeriodeDecompte periodeDecompte) {
		if (periodeDecompte == null) {
			return null;
		}

		switch (periodeDecompte) {
		case M01:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_01;
		case M02:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_02;
		case M03:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_03;
		case M04:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_04;
		case M05:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_05;
		case M06:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_06;
		case M07:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_07;
		case M08:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_08;
		case M09:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_09;
		case M10:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_10;
		case M11:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_11;
		case M12:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.M_12;
		case T1:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.Q_1;
		case T2:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.Q_2;
		case T3:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.Q_3;
		case T4:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.Q_4;
		case S1:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.H_1;
		case S2:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.H_2;
		case A:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod.Y;
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

	public static ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity coreToXMLv3(ch.vd.uniregctb.type.PeriodiciteDecompte periodiciteDecompte) {
		if (periodiciteDecompte == null) {
			return null;
		}

		switch (periodiciteDecompte) {
		case ANNUEL:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity.YEARLY;
		case MENSUEL:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity.MONTHLY;
		case SEMESTRIEL:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity.HALF_YEARLY;
		case TRIMESTRIEL:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity.QUARTERLY;
		case UNIQUE:
			return ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity.ONCE;
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

	public static ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType coreToXMLv3(ch.vd.uniregctb.type.CategorieEtranger categorie) {
		if (categorie == null) {
			return null;
		}

		switch (categorie) {
		case _01_SAISONNIER_A:
			throw new IllegalArgumentException("Catégorie d'étranger illégale = [" + categorie + ']');
		case _02_PERMIS_SEJOUR_B:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_02_B_PERMIT;
		case _03_ETABLI_C:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_03_C_PERMIT;
		case _04_CONJOINT_DIPLOMATE_CI:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_04_CI_PERMIT;
		case _05_ETRANGER_ADMIS_PROVISOIREMENT_F:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_05_F_PERMIT;
		case _06_FRONTALIER_G:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_06_G_PERMIT;
		case _07_PERMIS_SEJOUR_COURTE_DUREE_L:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_07_L_PERMIT;
		case _08_REQUERANT_ASILE_N:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_08_N_PERMIT;
		case _09_A_PROTEGER_S:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_09_S_PERMIT;
		case _10_TENUE_DE_S_ANNONCER:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_10_OBLIGED_TO_ANNOUNCE;
		case _11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_11_DIPLOMAT;
		case _12_FONCT_INTER_SANS_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case _13_NON_ATTRIBUEE:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_13_NOT_ASSIGNED;
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

	public static ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType coreToXMLv3(TypePermis permis) {
		if (permis == null) {
			return null;
		}
		switch (permis) {
		case SAISONNIER:
			throw new IllegalArgumentException("Type de permis illégal = [" + permis + ']');
		case SEJOUR:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_02_B_PERMIT;
		case ETABLISSEMENT:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_03_C_PERMIT;
		case CONJOINT_DIPLOMATE:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_04_CI_PERMIT;
		case ETRANGER_ADMIS_PROVISOIREMENT:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_05_F_PERMIT;
		case FRONTALIER:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_06_G_PERMIT;
		case COURTE_DUREE:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_07_L_PERMIT;
		case REQUERANT_ASILE:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_08_N_PERMIT;
		case PERSONNE_A_PROTEGER:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_09_S_PERMIT;
		case PERSONNE_TENUE_DE_S_ANNONCER:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_10_OBLIGED_TO_ANNOUNCE;
		case DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_11_DIPLOMAT;
		case FONCT_INTER_SANS_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case PAS_ATTRIBUE:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_13_NOT_ASSIGNED;
		case PROVISOIRE:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.C_13_NOT_ASSIGNED;
		case SUISSE_SOURCIER:
			return ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType.SWISS;
		default:
			throw new IllegalArgumentException("Type de permis inconnu = [" + permis + ']');
		}
	}

	public static ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType coreToXMLv1(ch.vd.uniregctb.type.TypeRapportEntreTiers type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case APPARTENANCE_MENAGE:
			return ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType.HOUSEHOLD_MEMBER;
		case ANNULE_ET_REMPLACE:
			return ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType.CANCELS_AND_REPLACES;
		case CONSEIL_LEGAL:
			return ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType.LEGAL_ADVISER;
		case CONTACT_IMPOT_SOURCE:
			return ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT;
		case CURATELLE:
			return ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType.WELFARE_ADVOCATE;
		case PRESTATION_IMPOSABLE:
			return ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType.TAXABLE_REVENUE;
		case REPRESENTATION:
			return ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType.REPRESENTATIVE;
		case TUTELLE:
			return ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType.GUARDIAN;
		default:
			throw new IllegalArgumentException("Type de rapport-entre-tiers inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.relation.v2.RelationBetweenPartiesType coreToXMLv2(ch.vd.uniregctb.type.TypeRapportEntreTiers type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case APPARTENANCE_MENAGE:
			return ch.vd.unireg.xml.party.relation.v2.RelationBetweenPartiesType.HOUSEHOLD_MEMBER;
		case ANNULE_ET_REMPLACE:
			return ch.vd.unireg.xml.party.relation.v2.RelationBetweenPartiesType.CANCELS_AND_REPLACES;
		case CONSEIL_LEGAL:
			return ch.vd.unireg.xml.party.relation.v2.RelationBetweenPartiesType.LEGAL_ADVISER;
		case CONTACT_IMPOT_SOURCE:
			return ch.vd.unireg.xml.party.relation.v2.RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT;
		case CURATELLE:
			return ch.vd.unireg.xml.party.relation.v2.RelationBetweenPartiesType.WELFARE_ADVOCATE;
		case PRESTATION_IMPOSABLE:
			return ch.vd.unireg.xml.party.relation.v2.RelationBetweenPartiesType.TAXABLE_REVENUE;
		case REPRESENTATION:
			return ch.vd.unireg.xml.party.relation.v2.RelationBetweenPartiesType.REPRESENTATIVE;
		case TUTELLE:
			return ch.vd.unireg.xml.party.relation.v2.RelationBetweenPartiesType.GUARDIAN;
		case PARENTE:
			// ce type ne devrait pas être utilisé tel quel, mais plutôt découpé en PARENT/CHILD
			throw new IllegalArgumentException("Erreur de mapping?");
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


	public static ch.vd.unireg.xml.party.taxpayer.v3.WithholdingTaxTariff coreToXMLv3(ch.vd.uniregctb.type.TarifImpotSource tarif) {
		if (tarif == null) {
			return null;
		}

		switch (tarif) {
		case DOUBLE_GAIN:
			return ch.vd.unireg.xml.party.taxpayer.v3.WithholdingTaxTariff.DOUBLE_REVENUE;
		case NORMAL:
			return ch.vd.unireg.xml.party.taxpayer.v3.WithholdingTaxTariff.NORMAL;
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

	public static ch.vd.unireg.xml.party.taxdeclaration.v3.DocumentType coreToXMLv3(ch.vd.uniregctb.type.TypeDocument type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case DECLARATION_IMPOT_VAUDTAX:
			return ch.vd.unireg.xml.party.taxdeclaration.v3.DocumentType.VAUDTAX_TAX_DECLARATION;
		case DECLARATION_IMPOT_COMPLETE_BATCH:
		case DECLARATION_IMPOT_COMPLETE_LOCAL:
			return ch.vd.unireg.xml.party.taxdeclaration.v3.DocumentType.FULL_TAX_DECLARATION;
		case DECLARATION_IMPOT_DEPENSE:
			return ch.vd.unireg.xml.party.taxdeclaration.v3.DocumentType.EXPENDITURE_BASED_TAX_DECLARATION;
		case DECLARATION_IMPOT_HC_IMMEUBLE:
			return ch.vd.unireg.xml.party.taxdeclaration.v3.DocumentType.IMMOVABLE_PROPERTY_OTHER_CANTON_TAX_DECLARATION;
		default:
			throw new IllegalArgumentException("Type de document inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason coreToXMLv1(ch.vd.uniregctb.type.MotifFor ouverture) {
		if (ouverture == null) {
			return null;
		}

		switch (ouverture) {
		case ACHAT_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.PURCHASE_REAL_ESTATE;
		case ANNULATION:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.CANCELLATION;
		case ARRIVEE_HC:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON;
		case ARRIVEE_HS:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY;
		case CHGT_MODE_IMPOSITION:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD;
		case DEBUT_ACTIVITE_DIPLOMATIQUE:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.START_DIPLOMATIC_ACTVITY;
		case DEBUT_EXPLOITATION:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION;
		case DEMENAGEMENT_VD:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.MOVE_VD;
		case DEPART_HC:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON;
		case DEPART_HS:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY;
		case FIN_ACTIVITE_DIPLOMATIQUE:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.END_DIPLOMATIC_ACTVITY;
		case FIN_EXPLOITATION:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.END_COMMERCIAL_EXPLOITATION;
		case FUSION_COMMUNES:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.MERGE_OF_MUNICIPALITIES;
		case DEBUT_PRESTATION_IS:
		case FIN_PRESTATION_IS:
		case CESSATION_ACTIVITE_FUSION_FAILLITE:
		case DEMENAGEMENT_SIEGE:
		case INDETERMINE:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.UNDETERMINED;
		case MAJORITE:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.MAJORITY;
		case MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION;
		case PERMIS_C_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.C_PERMIT_SWISS;
		case REACTIVATION:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.REACTIVATION;
		case SEJOUR_SAISONNIER:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.SEASONAL_JOURNEY;
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION;
		case VENTE_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.SALE_REAL_ESTATE;
		case VEUVAGE_DECES:
			return ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason.WIDOWHOOD_DEATH;
		default:
			throw new IllegalArgumentException("Motif de for inconnu = [" + ouverture + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason coreToXMLv2(ch.vd.uniregctb.type.MotifFor ouverture) {
		if (ouverture == null) {
			return null;
		}

		switch (ouverture) {
		case ACHAT_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.PURCHASE_REAL_ESTATE;
		case ANNULATION:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.CANCELLATION;
		case ARRIVEE_HC:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON;
		case ARRIVEE_HS:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY;
		case CHGT_MODE_IMPOSITION:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD;
		case DEBUT_ACTIVITE_DIPLOMATIQUE:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.START_DIPLOMATIC_ACTVITY;
		case DEBUT_EXPLOITATION:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION;
		case DEMENAGEMENT_VD:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.MOVE_VD;
		case DEPART_HC:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON;
		case DEPART_HS:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY;
		case FIN_ACTIVITE_DIPLOMATIQUE:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.END_DIPLOMATIC_ACTVITY;
		case FIN_EXPLOITATION:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.END_COMMERCIAL_EXPLOITATION;
		case FUSION_COMMUNES:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.MERGE_OF_MUNICIPALITIES;
		case DEBUT_PRESTATION_IS:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.START_WITHHOLDING_ACTIVITY;
		case FIN_PRESTATION_IS:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.END_WITHHOLDING_ACTIVITY;
		case CESSATION_ACTIVITE_FUSION_FAILLITE:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.END_ACTIVITY_MERGER_BANKRUPTCY;
		case DEMENAGEMENT_SIEGE:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.MOVE_HEADQUARTERS;
		case INDETERMINE:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.UNDETERMINED;
		case MAJORITE:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.MAJORITY;
		case MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION;
		case PERMIS_C_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.C_PERMIT_SWISS;
		case REACTIVATION:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.REACTIVATION;
		case SEJOUR_SAISONNIER:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.SEASONAL_JOURNEY;
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION;
		case VENTE_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.SALE_REAL_ESTATE;
		case VEUVAGE_DECES:
			return ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason.WIDOWHOOD_DEATH;
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

	public static ch.vd.unireg.xml.party.person.v3.Sex coreToXMLv3(ch.vd.uniregctb.type.Sexe sexe) {
		if (sexe == null) {
			return null;
		}

		switch (sexe) {
		case FEMININ:
			return ch.vd.unireg.xml.party.person.v3.Sex.FEMALE;
		case MASCULIN:
			return ch.vd.unireg.xml.party.person.v3.Sex.MALE;
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

	public static ch.vd.unireg.xml.party.address.v1.TariffZone coreToXMLv1(TypeAffranchissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case SUISSE:
			return ch.vd.unireg.xml.party.address.v1.TariffZone.SWITZERLAND;
		case EUROPE:
			return ch.vd.unireg.xml.party.address.v1.TariffZone.EUROPE;
		case MONDE:
			return ch.vd.unireg.xml.party.address.v1.TariffZone.OTHER_COUNTRIES;
		default:
			throw new IllegalArgumentException("Type d'affranchissement inconnu = [" + t + ']');
		}
	}

	public static ch.vd.unireg.xml.party.address.v2.TariffZone coreToXMLv2(TypeAffranchissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case SUISSE:
			return ch.vd.unireg.xml.party.address.v2.TariffZone.SWITZERLAND;
		case EUROPE:
			return ch.vd.unireg.xml.party.address.v2.TariffZone.EUROPE;
		case MONDE:
			return ch.vd.unireg.xml.party.address.v2.TariffZone.OTHER_COUNTRIES;
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

	public static ch.vd.unireg.xml.party.v3.AccountNumberFormat coreToXMLv3(CompteBancaire.Format format) {
		if (format == null) {
			return null;
		}
		switch (format) {
		case SPECIFIQUE_CH:
			return ch.vd.unireg.xml.party.v3.AccountNumberFormat.SWISS_SPECIFIC;
		case IBAN:
			return ch.vd.unireg.xml.party.v3.AccountNumberFormat.IBAN;
		default:
			throw new IllegalArgumentException("Format de compte bancaire inconnu = [" + format + ']');
		}
	}

	public static ch.vd.unireg.xml.party.immovableproperty.v1.OwnershipType coreToXMLv1(GenrePropriete genre) {
		if (genre == null) {
			return null;
		}
		switch (genre) {
		case INDIVIDUELLE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.OwnershipType.SOLE_OWNERSHIP;
		case COPROPRIETE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.OwnershipType.SIMPLE_CO_OWNERSHIP;
		case COMMUNE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.OwnershipType.COLLECTIVE_OWNERSHIP;
		default:
			throw new IllegalArgumentException("Genre de priopriété inconnu = [" + genre + ']');
		}
	}

	public static ch.vd.unireg.xml.party.immovableproperty.v2.OwnershipType coreToXMLv2(GenrePropriete genre) {
		if (genre == null) {
			return null;
		}
		switch (genre) {
		case INDIVIDUELLE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.OwnershipType.SOLE_OWNERSHIP;
		case COPROPRIETE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.OwnershipType.SIMPLE_CO_OWNERSHIP;
		case COMMUNE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.OwnershipType.COLLECTIVE_OWNERSHIP;
		default:
			throw new IllegalArgumentException("Genre de priopriété inconnu = [" + genre + ']');
		}
	}

	public static GenrePropriete xmlToCore(ch.vd.unireg.xml.party.immovableproperty.v2.OwnershipType ownershipType) {
		if (ownershipType == null) {
			return null;
		}
		switch (ownershipType) {
		case SOLE_OWNERSHIP:
			return GenrePropriete.INDIVIDUELLE;
		case SIMPLE_CO_OWNERSHIP:
			return GenrePropriete.COPROPRIETE;
		case COLLECTIVE_OWNERSHIP:
		    return GenrePropriete.COMMUNE;
		default:
			throw new IllegalArgumentException("OwnershipType inconnu = [" + ownershipType + ']');
		}
	}

	public static ch.vd.unireg.xml.party.immovableproperty.v1.ImmovablePropertyType coreToXMLv1(TypeImmeuble type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case BIEN_FOND:
			return ch.vd.unireg.xml.party.immovableproperty.v1.ImmovablePropertyType.IMMOVABLE_PROPERTY;
		case PPE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.ImmovablePropertyType.CONDOMINIUM_OWNERSHIP;
		case DROIT_DISTINCT_ET_PERMANENT:
			return ch.vd.unireg.xml.party.immovableproperty.v1.ImmovablePropertyType.DISTINCT_AND_PERMANENT_RIGHT;
		case PART_DE_COPROPRIETE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.ImmovablePropertyType.CO_OWNERSHIP_SHARE;
		default:
			throw new IllegalArgumentException("Type de priopriété inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.immovableproperty.v2.ImmovablePropertyType coreToXMLv2(TypeImmeuble type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case BIEN_FOND:
			return ch.vd.unireg.xml.party.immovableproperty.v2.ImmovablePropertyType.IMMOVABLE_PROPERTY;
		case PPE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.ImmovablePropertyType.CONDOMINIUM_OWNERSHIP;
		case DROIT_DISTINCT_ET_PERMANENT:
			return ch.vd.unireg.xml.party.immovableproperty.v2.ImmovablePropertyType.DISTINCT_AND_PERMANENT_RIGHT;
		case PART_DE_COPROPRIETE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.ImmovablePropertyType.CO_OWNERSHIP_SHARE;
		default:
			throw new IllegalArgumentException("Type de priopriété inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.immovableproperty.v1.MutationType coreToXMLv1(TypeMutation type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case ACHAT:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.PURCHASE;
		case AUGMENTATION:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.INCREASE;
		case CESSION:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.CESSION;
		case CONSTITUTION_PPE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.CONDOMINIUM_OWNERSHIP_COMPOSITION;
		case CONSTITUTION_PARTS_PROPRIETE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.CO_OWNERSHIP_SHARES_COMPOSITION;
		case DIVISION_BIEN_FONDS:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.IMMOVABLE_PROPERTY_DIVISION;
		case DONATION:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.GIFT;
		case DELIVRANCE_LEGS:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.LEGACY_DELIVERY;
		case ECHANGE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.EXCHANGE;
		case GROUPEMENT_BIEN_FONDS:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.IMMOVABLE_PROPERTY_GROUPING;
		case JUGEMENT:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.JUDGEMENT;
		case PARTAGE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.SHARING;
		case REMANIEMENT_PARCELLAIRE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.PLOT_REFORM;
		case REALISATION_FORCEE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.COMPULSARY_SALE;
		case SUCCESSION:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.INHERITANCE;
		case TRANSFERT:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.TRANSFER;
		case FIN_DE_PROPRIETE:
			return ch.vd.unireg.xml.party.immovableproperty.v1.MutationType.END_OF_OWNERSHIP;
		default:
			throw new IllegalArgumentException("Type de mutation inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.immovableproperty.v2.MutationType coreToXMLv2(TypeMutation type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case ACHAT:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.PURCHASE;
		case AUGMENTATION:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.INCREASE;
		case CESSION:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.CESSION;
		case CONSTITUTION_PPE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.CONDOMINIUM_OWNERSHIP_COMPOSITION;
		case CONSTITUTION_PARTS_PROPRIETE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.CO_OWNERSHIP_SHARES_COMPOSITION;
		case DIVISION_BIEN_FONDS:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.IMMOVABLE_PROPERTY_DIVISION;
		case DONATION:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.GIFT;
		case DELIVRANCE_LEGS:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.LEGACY_DELIVERY;
		case ECHANGE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.EXCHANGE;
		case GROUPEMENT_BIEN_FONDS:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.IMMOVABLE_PROPERTY_GROUPING;
		case JUGEMENT:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.JUDGEMENT;
		case PARTAGE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.SHARING;
		case REMANIEMENT_PARCELLAIRE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.PLOT_REFORM;
		case REALISATION_FORCEE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.COMPULSARY_SALE;
		case SUCCESSION:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.INHERITANCE;
		case TRANSFERT:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.TRANSFER;
		case FIN_DE_PROPRIETE:
			return ch.vd.unireg.xml.party.immovableproperty.v2.MutationType.END_OF_OWNERSHIP;
		default:
			throw new IllegalArgumentException("Type de mutation inconnu = [" + type + ']');
		}
	}

	public static TypeMutation xmlToCore(ch.vd.unireg.xml.party.immovableproperty.v2.MutationType type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case PURCHASE:
			return TypeMutation.ACHAT;
		case INCREASE:
			return TypeMutation.AUGMENTATION;
		case CESSION:
			return TypeMutation.CESSION;
		case CONDOMINIUM_OWNERSHIP_COMPOSITION:
			return TypeMutation.CONSTITUTION_PPE;
		case CO_OWNERSHIP_SHARES_COMPOSITION:
			return TypeMutation.CONSTITUTION_PARTS_PROPRIETE;
		case IMMOVABLE_PROPERTY_DIVISION:
			return TypeMutation.DIVISION_BIEN_FONDS;
		case GIFT:
			return TypeMutation.DONATION;
		case LEGACY_DELIVERY:
			return TypeMutation.DELIVRANCE_LEGS;
		case EXCHANGE:
			return TypeMutation.ECHANGE;
		case IMMOVABLE_PROPERTY_GROUPING:
			return TypeMutation.GROUPEMENT_BIEN_FONDS;
		case JUDGEMENT:
			return TypeMutation.JUGEMENT;
		case SHARING:
			return TypeMutation.PARTAGE;
		case PLOT_REFORM:
			return TypeMutation.REMANIEMENT_PARCELLAIRE;
		case COMPULSARY_SALE:
			return TypeMutation.REALISATION_FORCEE;
		case INHERITANCE:
			return  TypeMutation.SUCCESSION;
		case TRANSFER:
			return TypeMutation.TRANSFERT;
		case END_OF_OWNERSHIP:
			return TypeMutation.FIN_DE_PROPRIETE;
		default:
			throw new IllegalArgumentException("Type de mutation incunnu = [" + type + ']');
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

	public static CategorieImpotSource xmlToCore(ch.vd.unireg.xml.party.withholding.v1.DebtorCategory category) {
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
		case WINE_FARM_SEASONAL_WORKERS:
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

	public static TiersCriteria.TypeTiers xmlToCore(ch.vd.unireg.xml.party.v3.PartyType type) {
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
		case ADMINISTRATIVE_AUTHORITY:
			return TiersCriteria.TypeTiers.COLLECTIVITE_ADMINISTRATIVE;
		case OTHER_COMMUNITY:
			return TiersCriteria.TypeTiers.AUTRE_COMMUNAUTE;
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

	public static CategorieEtranger xmlToCore(ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType category) {
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
			throw new IllegalArgumentException("unknown NaturalPersonCategoryType = [" + category + ']');
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

	public static Sexe xmlToCore(ch.vd.unireg.xml.party.person.v3.Sex sex) {
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

	public static StatutMenageCommun xmlToCore(ch.vd.unireg.xml.party.person.v3.CommonHouseholdStatus chs) {
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

	public static ch.vd.unireg.xml.party.person.v3.CommonHouseholdStatus coreToXMLv3(StatutMenageCommun setc) {
		if (setc == null) {
			return null;
		}
		switch (setc) {
		case EN_VIGUEUR:
			return ch.vd.unireg.xml.party.person.v3.CommonHouseholdStatus.ACTIVE;
		case TERMINE_SUITE_DECES:
			return ch.vd.unireg.xml.party.person.v3.CommonHouseholdStatus.ENDED_BY_DEATH;
		case TERMINE_SUITE_SEPARATION:
			return ch.vd.unireg.xml.party.person.v3.CommonHouseholdStatus.SEPARATED_DIVORCED;
		default:
			throw new IllegalArgumentException("unknown StatutMenageCommun = [" + setc + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v2.WithholdingTaxationPeriodType coreToXMLv2(PeriodeImpositionImpotSource.Type setc) {
		if (setc == null) {
			return null;
		}
		switch (setc) {
		case MIXTE:
			return ch.vd.unireg.xml.party.taxresidence.v2.WithholdingTaxationPeriodType.MIXED;
		case SOURCE:
			return ch.vd.unireg.xml.party.taxresidence.v2.WithholdingTaxationPeriodType.PURE;
		default:
			throw new IllegalArgumentException("unknown PeriodeImpositionImpotSource.Type = [" + setc + ']');
		}
	}

	public static ch.vd.unireg.xml.party.othercomm.v1.LegalForm coreToXMLv1(FormeJuridique formeJuridique) {
		if (formeJuridique == null) {
			return null;
		}
		switch (formeJuridique) {
		case ASS:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.ASSOCIATION;
		case COOP:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.COOPERATIVE_SOCIETY;
		case EDP:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.STATUTORY_CORPORATION;
		case EI:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.SOLE_PROPRIETORSHIP;
		case FOND:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.FOUNDATION;
		case IND:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.JOINT_POSSESSION;
		case PRO:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.CORPORATION_WITHOUT_COMPULSORY_REGISTRATION;
		case SA:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.LIMITED_COMPANY;
		case SAEDP:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.PUBLIC_LIMITED_COMPANY;
		case SARL:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.LIMITED_LIABILITY_COMPANY;
		case SC:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.LIMITED_PARTNERSHIP;
		case SCA:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP;
		case SCPC:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS;
		case SEE:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.BRANCH_OF_FOREIGN_BASED_COMPANY;
		case SES:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.BRANCH_OF_SWISS_COMPANY;
		case SICAF:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.CLOSED_END_INVESTMENT_TRUST;
		case SICAV:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.OPEN_ENDED_INVESTMENT_TRUST;
		case SNC:
			return ch.vd.unireg.xml.party.othercomm.v1.LegalForm.GENERAL_PARTNERSHIP;
		default:
			throw new IllegalArgumentException("unknown FormeJuridique = [" + formeJuridique + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType coreToXMLv2(TypeAssujettissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case DIPLOMATE_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType.SWISS_DIPLOMAT;
		case HORS_CANTON:
			return ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType.OTHER_CANTON;
		case HORS_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType.FOREIGN_COUNTRY;
		case INDIGENT:
			return ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType.INDIGENT;
		case MIXTE_137_1:
			return ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_1;
		case MIXTE_137_2:
			return ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_2;
		case NON_ASSUJETTI:
			return ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType.NONE;
		case SOURCE_PURE:
			return ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType.PURE_WITHHOLDING;
		case VAUDOIS_DEPENSE:
			return ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType.EXPENDITURE_BASED;
		case VAUDOIS_ORDINAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType.ORDINARY_RESIDENT;
		default:
			throw new IllegalArgumentException("Type d'assujettissement inconnu = [" + t + ']');
		}
	}

	public static ch.vd.unireg.xml.party.ebilling.v1.EbillingStatusType coreToXMLv1(TypeEtatDestinataire etat) {
		if (etat == null) {
			return null;
		}

		switch (etat) {
		case NON_INSCRIT:
			return ch.vd.unireg.xml.party.ebilling.v1.EbillingStatusType.NOT_REGISTERED;
		case NON_INSCRIT_SUSPENDU:
			return ch.vd.unireg.xml.party.ebilling.v1.EbillingStatusType.NOT_REGISTERED_SUSPENDED;
		case INSCRIT:
			return ch.vd.unireg.xml.party.ebilling.v1.EbillingStatusType.REGISTERED;
		case INSCRIT_SUSPENDU:
			return ch.vd.unireg.xml.party.ebilling.v1.EbillingStatusType.REGISTERED_SUSPENDED;
		case DESINSCRIT:
			return ch.vd.unireg.xml.party.ebilling.v1.EbillingStatusType.UNREGISTERED;
		case DESINSCRIT_SUSPENDU:
			return ch.vd.unireg.xml.party.ebilling.v1.EbillingStatusType.UNREGISTERED_SUSPENDED;
		default:
			throw new IllegalArgumentException("Type d'état de destinataire efacture inconnu = [" + etat + ']');
		}
	}
}
