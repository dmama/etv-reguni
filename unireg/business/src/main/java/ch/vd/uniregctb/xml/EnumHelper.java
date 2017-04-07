package ch.vd.uniregctb.xml;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.interfaces.infra.data.TypeAffranchissement;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwnersType;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.uniregctb.avatar.TypeAvatar;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSource;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.rf.TypeImmeuble;
import ch.vd.uniregctb.rf.TypeMutation;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.FormeJuridique;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.StatutMenageCommun;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
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

	public static ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus coreToXMLv4(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus.SINGLE;
		case DIVORCE:
			return ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus.DIVORCED;
		case LIE_PARTENARIAT_ENREGISTRE:
			return ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus.REGISTERED_PARTNER;
		case MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus.MARRIED;
		case NON_MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus.NOT_MARRIED;
		case PARTENARIAT_DISSOUS_DECES:
			return ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH;
		case PARTENARIAT_DISSOUS_JUDICIAIREMENT:
			return ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus.SEPARATED;
		case PARTENARIAT_SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus.PARTNERSHIP_SEPARATED;
		case VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v4.MaritalStatus.WIDOWED;
		default:
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etatCivil + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus coreToXMLv5(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}

		switch (etatCivil) {
		case CELIBATAIRE:
			return ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus.SINGLE;
		case DIVORCE:
			return ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus.DIVORCED;
		case LIE_PARTENARIAT_ENREGISTRE:
			return ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus.REGISTERED_PARTNER;
		case MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus.MARRIED;
		case NON_MARIE:
			return ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus.NOT_MARRIED;
		case PARTENARIAT_DISSOUS_DECES:
			return ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH;
		case PARTENARIAT_DISSOUS_JUDICIAIREMENT:
			return ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW;
		case SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus.SEPARATED;
		case PARTENARIAT_SEPARE:
			return ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus.PARTNERSHIP_SEPARATED;
		case VEUF:
			return ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus.WIDOWED;
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

	public static final Set<CategorieImpotSource> CIS_SUPPORTEES_V4 = Collections.unmodifiableSet(EnumSet.of(CategorieImpotSource.ADMINISTRATEURS, CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS,
	                                                                                                         CategorieImpotSource.CREANCIERS_HYPOTHECAIRES, CategorieImpotSource.LOI_TRAVAIL_AU_NOIR,
	                                                                                                         CategorieImpotSource.PRESTATIONS_PREVOYANCE, CategorieImpotSource.REGULIERS,
	                                                                                                         CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, CategorieImpotSource.EFFEUILLEUSES));

	public static final Set<CategorieImpotSource> CIS_SUPPORTEES_V5 = Collections.unmodifiableSet(EnumSet.of(CategorieImpotSource.ADMINISTRATEURS, CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS,
	                                                                                                         CategorieImpotSource.CREANCIERS_HYPOTHECAIRES, CategorieImpotSource.LOI_TRAVAIL_AU_NOIR,
	                                                                                                         CategorieImpotSource.PRESTATIONS_PREVOYANCE, CategorieImpotSource.REGULIERS,
	                                                                                                         CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, CategorieImpotSource.EFFEUILLEUSES));

	public static final Set<TypeAvatar> TA_IGNORES_V1 = Collections.unmodifiableSet(EnumSet.of(TypeAvatar.ETABLISSEMENT));

	public static final Set<TypeAvatar> TA_IGNORES_V2 = Collections.unmodifiableSet(EnumSet.of(TypeAvatar.ETABLISSEMENT));

	public static final Set<TypeAvatar> TA_IGNORES_V3 = Collections.unmodifiableSet(EnumSet.of(TypeAvatar.ETABLISSEMENT));

	public static final Set<TypeAvatar> TA_IGNORES_V4 = Collections.unmodifiableSet(EnumSet.noneOf(TypeAvatar.class));

	public static final Set<TypeAvatar> TA_IGNORES_V5 = Collections.unmodifiableSet(EnumSet.noneOf(TypeAvatar.class));

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

	public static ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatusType coreToXMLv4(ch.vd.uniregctb.type.TypeEtatDeclaration type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case ECHUE:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatusType.EXPIRED;
		case EMISE:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatusType.SENT;
		case SOMMEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatusType.SUMMONS_SENT;
		case RETOURNEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatusType.RETURNED;
		case SUSPENDUE:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatusType.SUSPENDED;
		case RAPPELEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatusType.REMINDER_SENT;
		default:
			throw new IllegalArgumentException("Type d'état de déclaration inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType coreToXMLv5(ch.vd.uniregctb.type.TypeEtatDeclaration type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case ECHUE:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType.EXPIRED;
		case EMISE:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType.SENT;
		case SOMMEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType.SUMMONS_SENT;
		case RETOURNEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType.RETURNED;
		case SUSPENDUE:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType.SUSPENDED;
		case RAPPELEE:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType.REMINDER_SENT;
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

	public static ch.vd.unireg.xml.party.taxresidence.v3.TaxType coreToXMLv3(ch.vd.uniregctb.type.GenreImpot genreImpot) {
		if (genreImpot == null) {
			return null;
		}

		switch (genreImpot) {
		case BENEFICE_CAPITAL:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.PROFITS_CAPITAL;
		case CHIENS:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.DOGS;
		case DEBITEUR_PRESTATION_IMPOSABLE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.DEBTOR_TAXABLE_INCOME;
		case DONATION:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.GIFTS;
		case DROIT_MUTATION:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.REAL_ESTATE_TRANSFER;
		case FONCIER:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.REAL_ESTATE;
		case GAIN_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.IMMOVABLE_PROPERTY_GAINS;
		case PATENTE_TABAC:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.TOBACCO_PATENT;
		case PRESTATION_CAPITAL:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.CAPITAL_INCOME;
		case REVENU_FORTUNE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.INCOME_WEALTH;
		case SUCCESSION:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxType.INHERITANCE;
		default:
			throw new IllegalArgumentException("Genre d'impôt inconnu = [" + genreImpot + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v4.TaxType coreToXMLv4(ch.vd.uniregctb.type.GenreImpot genreImpot) {
		if (genreImpot == null) {
			return null;
		}

		switch (genreImpot) {
		case BENEFICE_CAPITAL:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.PROFITS_CAPITAL;
		case CHIENS:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.DOGS;
		case DEBITEUR_PRESTATION_IMPOSABLE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.DEBTOR_TAXABLE_INCOME;
		case DONATION:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.GIFTS;
		case DROIT_MUTATION:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.REAL_ESTATE_TRANSFER;
		case FONCIER:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.REAL_ESTATE;
		case GAIN_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.IMMOVABLE_PROPERTY_GAINS;
		case PATENTE_TABAC:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.TOBACCO_PATENT;
		case PRESTATION_CAPITAL:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.CAPITAL_INCOME;
		case REVENU_FORTUNE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.INCOME_WEALTH;
		case SUCCESSION:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxType.INHERITANCE;
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

	public static ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason coreToXMLv3(ch.vd.uniregctb.type.MotifRattachement rattachement) {
		if (rattachement == null) {
			return null;
		}

		switch (rattachement) {
		case ACTIVITE_INDEPENDANTE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.INDEPENDANT_ACTIVITY;
		case ACTIVITE_LUCRATIVE_CAS:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.GAINFUL_ACTIVITY_SAS;
		case ADMINISTRATEUR:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.ADMINISTRATOR;
		case CREANCIER_HYPOTHECAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.MORTGAGE_CREDITORS;
		case DIPLOMATE_ETRANGER:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.FOREIGN_DIPLOMAT;
		case DIPLOMATE_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.SWISS_DIPLOMAT;
		case DIRIGEANT_SOCIETE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.COMPANY_LEADER;
		case DOMICILE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.RESIDENCE;
		case ETABLISSEMENT_STABLE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.STABLE_ESTABLISHMENT;
		case IMMEUBLE_PRIVE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.PRIVATE_IMMOVABLE_PROPERTY;
		case LOI_TRAVAIL_AU_NOIR:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.LAW_ON_UNDECLARED_WORK;
		case PRESTATION_PREVOYANCE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.PENSION;
		case SEJOUR_SAISONNIER:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.SEASONAL_JOURNEY;
		case EFFEUILLEUSES:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.WINE_FARM_SEASONAL_WORKER;
		case PARTICIPATIONS_HORS_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxLiabilityReason.PROFIT_SHARING_FOREIGN_COUNTRY_TAXPAYER;
		default:
			throw new IllegalArgumentException("Motif de rattachement inconnu = [" + rattachement + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason coreToXMLv4(ch.vd.uniregctb.type.MotifRattachement rattachement) {
		if (rattachement == null) {
			return null;
		}

		switch (rattachement) {
		case ACTIVITE_INDEPENDANTE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.INDEPENDANT_ACTIVITY;
		case ACTIVITE_LUCRATIVE_CAS:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.GAINFUL_ACTIVITY_SAS;
		case ADMINISTRATEUR:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.ADMINISTRATOR;
		case CREANCIER_HYPOTHECAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.MORTGAGE_CREDITORS;
		case DIPLOMATE_ETRANGER:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.FOREIGN_DIPLOMAT;
		case DIPLOMATE_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.SWISS_DIPLOMAT;
		case DIRIGEANT_SOCIETE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.COMPANY_LEADER;
		case DOMICILE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.RESIDENCE;
		case ETABLISSEMENT_STABLE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.STABLE_ESTABLISHMENT;
		case IMMEUBLE_PRIVE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.PRIVATE_IMMOVABLE_PROPERTY;
		case LOI_TRAVAIL_AU_NOIR:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.LAW_ON_UNDECLARED_WORK;
		case PRESTATION_PREVOYANCE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.PENSION;
		case SEJOUR_SAISONNIER:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.SEASONAL_JOURNEY;
		case EFFEUILLEUSES:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.WINE_FARM_SEASONAL_WORKER;
		case PARTICIPATIONS_HORS_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason.PROFIT_SHARING_FOREIGN_COUNTRY_TAXPAYER;
		default:
			throw new IllegalArgumentException("Motif de rattachement inconnu = [" + rattachement + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType coreToXMLv1(TypeAutoriteFiscale typeForFiscal) {
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

	public static ch.vd.unireg.xml.party.taxresidence.v2.TaxationAuthorityType coreToXMLv2(TypeAutoriteFiscale typeForFiscal) {
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

	public static ch.vd.unireg.xml.party.taxresidence.v3.TaxationAuthorityType coreToXMLv3(TypeAutoriteFiscale typeForFiscal) {
		if (typeForFiscal == null) {
			return null;
		}

		switch (typeForFiscal) {
		case COMMUNE_OU_FRACTION_VD:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxationAuthorityType.VAUD_MUNICIPALITY;
		case COMMUNE_HC:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY;
		case PAYS_HS:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxationAuthorityType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu = [" + typeForFiscal + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v4.TaxationAuthorityType coreToXMLv4(TypeAutoriteFiscale typeForFiscal) {
		if (typeForFiscal == null) {
			return null;
		}

		switch (typeForFiscal) {
		case COMMUNE_OU_FRACTION_VD:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxationAuthorityType.VAUD_MUNICIPALITY;
		case COMMUNE_HC:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY;
		case PAYS_HS:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxationAuthorityType.FOREIGN_COUNTRY;
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

	public static ch.vd.unireg.xml.party.taxresidence.v3.TaxationMethod coreToXMLv3(ch.vd.uniregctb.type.ModeImposition mode) {
		if (mode == null) {
			return null;
		}

		switch (mode) {
		case ORDINAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxationMethod.ORDINARY;
		case SOURCE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxationMethod.WITHHOLDING;
		case DEPENSE:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxationMethod.EXPENDITURE_BASED;
		case INDIGENT:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxationMethod.INDIGENT;
		case MIXTE_137_1:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxationMethod.MIXED_137_1;
		case MIXTE_137_2:
			return ch.vd.unireg.xml.party.taxresidence.v3.TaxationMethod.MIXED_137_2;
		default:
			throw new IllegalArgumentException("Mode d'imposition inconnu = [" + mode + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v4.TaxationMethod coreToXMLv4(ch.vd.uniregctb.type.ModeImposition mode) {
		if (mode == null) {
			return null;
		}

		switch (mode) {
		case ORDINAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxationMethod.ORDINARY;
		case SOURCE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxationMethod.WITHHOLDING;
		case DEPENSE:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxationMethod.EXPENDITURE_BASED;
		case INDIGENT:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxationMethod.INDIGENT;
		case MIXTE_137_1:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxationMethod.MIXED_137_1;
		case MIXTE_137_2:
			return ch.vd.unireg.xml.party.taxresidence.v4.TaxationMethod.MIXED_137_2;
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
		case _01_SAISONNIER_A:
			throw new IllegalArgumentException("Catégorie d'étranger illégale = [" + categorie + ']');
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
		case _01_SAISONNIER_A:
			throw new IllegalArgumentException("Catégorie d'étranger illégale = [" + categorie + ']');
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

	public static ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType coreToXMLv4(ch.vd.uniregctb.type.CategorieEtranger categorie) {
		if (categorie == null) {
			return null;
		}

		switch (categorie) {
		case _01_SAISONNIER_A:
			throw new IllegalArgumentException("Catégorie d'étranger illégale = [" + categorie + ']');
		case _02_PERMIS_SEJOUR_B:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_02_B_PERMIT;
		case _03_ETABLI_C:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_03_C_PERMIT;
		case _04_CONJOINT_DIPLOMATE_CI:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_04_CI_PERMIT;
		case _05_ETRANGER_ADMIS_PROVISOIREMENT_F:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_05_F_PERMIT;
		case _06_FRONTALIER_G:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_06_G_PERMIT;
		case _07_PERMIS_SEJOUR_COURTE_DUREE_L:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_07_L_PERMIT;
		case _08_REQUERANT_ASILE_N:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_08_N_PERMIT;
		case _09_A_PROTEGER_S:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_09_S_PERMIT;
		case _10_TENUE_DE_S_ANNONCER:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_10_OBLIGED_TO_ANNOUNCE;
		case _11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_11_DIPLOMAT;
		case _12_FONCT_INTER_SANS_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case _13_NON_ATTRIBUEE:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_13_NOT_ASSIGNED;
		default:
			throw new IllegalArgumentException("Catégorie d'étranger inconnue = [" + categorie + ']');
		}
	}

	public static ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType coreToXMLv5(ch.vd.uniregctb.type.CategorieEtranger categorie) {
		if (categorie == null) {
			return null;
		}

		switch (categorie) {
		case _01_SAISONNIER_A:
			throw new IllegalArgumentException("Catégorie d'étranger illégale = [" + categorie + ']');
		case _02_PERMIS_SEJOUR_B:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_02_B_PERMIT;
		case _03_ETABLI_C:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_03_C_PERMIT;
		case _04_CONJOINT_DIPLOMATE_CI:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_04_CI_PERMIT;
		case _05_ETRANGER_ADMIS_PROVISOIREMENT_F:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_05_F_PERMIT;
		case _06_FRONTALIER_G:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_06_G_PERMIT;
		case _07_PERMIS_SEJOUR_COURTE_DUREE_L:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_07_L_PERMIT;
		case _08_REQUERANT_ASILE_N:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_08_N_PERMIT;
		case _09_A_PROTEGER_S:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_09_S_PERMIT;
		case _10_TENUE_DE_S_ANNONCER:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_10_OBLIGED_TO_ANNOUNCE;
		case _11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_11_DIPLOMAT;
		case _12_FONCT_INTER_SANS_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case _13_NON_ATTRIBUEE:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_13_NOT_ASSIGNED;
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

	public static ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType coreToXMLv4(TypePermis permis) {
		if (permis == null) {
			return null;
		}
		switch (permis) {
		case SAISONNIER:
			throw new IllegalArgumentException("Type de permis illégal = [" + permis + ']');
		case SEJOUR:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_02_B_PERMIT;
		case ETABLISSEMENT:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_03_C_PERMIT;
		case CONJOINT_DIPLOMATE:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_04_CI_PERMIT;
		case ETRANGER_ADMIS_PROVISOIREMENT:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_05_F_PERMIT;
		case FRONTALIER:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_06_G_PERMIT;
		case COURTE_DUREE:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_07_L_PERMIT;
		case REQUERANT_ASILE:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_08_N_PERMIT;
		case PERSONNE_A_PROTEGER:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_09_S_PERMIT;
		case PERSONNE_TENUE_DE_S_ANNONCER:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_10_OBLIGED_TO_ANNOUNCE;
		case DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_11_DIPLOMAT;
		case FONCT_INTER_SANS_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case PAS_ATTRIBUE:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_13_NOT_ASSIGNED;
		case PROVISOIRE:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.C_13_NOT_ASSIGNED;
		case SUISSE_SOURCIER:
			return ch.vd.unireg.xml.party.person.v4.NaturalPersonCategoryType.SWISS;
		default:
			throw new IllegalArgumentException("Type de permis inconnu = [" + permis + ']');
		}
	}

	public static ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType coreToXMLv5(TypePermis permis) {
		if (permis == null) {
			return null;
		}
		switch (permis) {
		case SAISONNIER:
			throw new IllegalArgumentException("Type de permis illégal = [" + permis + ']');
		case SEJOUR:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_02_B_PERMIT;
		case ETABLISSEMENT:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_03_C_PERMIT;
		case CONJOINT_DIPLOMATE:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_04_CI_PERMIT;
		case ETRANGER_ADMIS_PROVISOIREMENT:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_05_F_PERMIT;
		case FRONTALIER:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_06_G_PERMIT;
		case COURTE_DUREE:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_07_L_PERMIT;
		case REQUERANT_ASILE:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_08_N_PERMIT;
		case PERSONNE_A_PROTEGER:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_09_S_PERMIT;
		case PERSONNE_TENUE_DE_S_ANNONCER:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_10_OBLIGED_TO_ANNOUNCE;
		case DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_11_DIPLOMAT;
		case FONCT_INTER_SANS_IMMUNITE:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_12_INTERNATIONAL_CIVIL_SERVANT;
		case PAS_ATTRIBUE:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_13_NOT_ASSIGNED;
		case PROVISOIRE:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.C_13_NOT_ASSIGNED;
		case SUISSE_SOURCIER:
			return ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType.SWISS;
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
		case ASSUJETTISSEMENT_PAR_SUBSTITUTION:
		case ACTIVITE_ECONOMIQUE:
		case MANDAT:
		case FUSION_ENTREPRISES:
		case ADMINISTRATION_ENTREPRISE:
		case SOCIETE_DIRECTION:
		case SCISSION_ENTREPRISE:
		case TRANSFERT_PATRIMOINE:
			throw new IllegalArgumentException("Erreur de mapping?");
		default:
			throw new IllegalArgumentException("Type de rapport-entre-tiers inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType coreToXMLv3(ch.vd.uniregctb.type.TypeRapportEntreTiers type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case APPARTENANCE_MENAGE:
			return ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType.HOUSEHOLD_MEMBER;
		case ANNULE_ET_REMPLACE:
			return ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType.CANCELS_AND_REPLACES;
		case CONSEIL_LEGAL:
			return ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType.LEGAL_ADVISER;
		case CONTACT_IMPOT_SOURCE:
			return ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT;
		case CURATELLE:
			return ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType.WELFARE_ADVOCATE;
		case PRESTATION_IMPOSABLE:
			return ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType.TAXABLE_REVENUE;
		case REPRESENTATION:
			return ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType.REPRESENTATIVE;
		case TUTELLE:
			return ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType.GUARDIAN;
		case PARENTE:
			// ce type ne devrait pas être utilisé tel quel, mais plutôt découpé en PARENT/CHILD
			throw new IllegalArgumentException("Erreur de mapping?");
		case ACTIVITE_ECONOMIQUE:
			return ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType.ECONOMIC_ACTIVITY;
		case FUSION_ENTREPRISES:
			return ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType.MERGE;
		case ASSUJETTISSEMENT_PAR_SUBSTITUTION:
		case MANDAT:
		case ADMINISTRATION_ENTREPRISE:
		case SOCIETE_DIRECTION:
		case SCISSION_ENTREPRISE:
		case TRANSFERT_PATRIMOINE:
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


	public static ch.vd.unireg.xml.party.taxpayer.v4.WithholdingTaxTariff coreToXMLv4(ch.vd.uniregctb.type.TarifImpotSource tarif) {
		if (tarif == null) {
			return null;
		}

		switch (tarif) {
		case DOUBLE_GAIN:
			return ch.vd.unireg.xml.party.taxpayer.v4.WithholdingTaxTariff.DOUBLE_REVENUE;
		case NORMAL:
			return ch.vd.unireg.xml.party.taxpayer.v4.WithholdingTaxTariff.NORMAL;
		default:
			throw new IllegalArgumentException("Type de tarif inconnu = [" + tarif + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxpayer.v5.WithholdingTaxTariff coreToXMLv5(ch.vd.uniregctb.type.TarifImpotSource tarif) {
		if (tarif == null) {
			return null;
		}

		switch (tarif) {
		case DOUBLE_GAIN:
			return ch.vd.unireg.xml.party.taxpayer.v5.WithholdingTaxTariff.DOUBLE_REVENUE;
		case NORMAL:
			return ch.vd.unireg.xml.party.taxpayer.v5.WithholdingTaxTariff.NORMAL;
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

	public static ch.vd.unireg.xml.party.taxdeclaration.v4.DocumentType coreToXMLv4(ch.vd.uniregctb.type.TypeDocument type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case DECLARATION_IMPOT_VAUDTAX:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.DocumentType.VAUDTAX_TAX_DECLARATION;
		case DECLARATION_IMPOT_COMPLETE_BATCH:
		case DECLARATION_IMPOT_COMPLETE_LOCAL:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.DocumentType.FULL_TAX_DECLARATION;
		case DECLARATION_IMPOT_DEPENSE:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.DocumentType.EXPENDITURE_BASED_TAX_DECLARATION;
		case DECLARATION_IMPOT_HC_IMMEUBLE:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.DocumentType.IMMOVABLE_PROPERTY_OTHER_CANTON_TAX_DECLARATION;
		case DECLARATION_IMPOT_APM_BATCH:
		case DECLARATION_IMPOT_APM_LOCAL:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.DocumentType.OTHER_CORPORATE_ENTITY_TAX_DECLARATION;
		case DECLARATION_IMPOT_PM_BATCH:
		case DECLARATION_IMPOT_PM_LOCAL:
			return ch.vd.unireg.xml.party.taxdeclaration.v4.DocumentType.CORPORATE_ENTITY_TAX_DECLARATION;
		default:
			throw new IllegalArgumentException("Type de document inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclarationType coreToXMLv5(ch.vd.uniregctb.type.TypeDocument type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case DECLARATION_IMPOT_VAUDTAX:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclarationType.VAUDTAX_TAX_DECLARATION;
		case DECLARATION_IMPOT_COMPLETE_BATCH:
		case DECLARATION_IMPOT_COMPLETE_LOCAL:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclarationType.FULL_TAX_DECLARATION;
		case DECLARATION_IMPOT_DEPENSE:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclarationType.EXPENDITURE_BASED_TAX_DECLARATION;
		case DECLARATION_IMPOT_HC_IMMEUBLE:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclarationType.IMMOVABLE_PROPERTY_OTHER_CANTON_TAX_DECLARATION;
		case DECLARATION_IMPOT_APM_BATCH:
		case DECLARATION_IMPOT_APM_LOCAL:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclarationType.OTHER_CORPORATE_ENTITY_TAX_DECLARATION;
		case DECLARATION_IMPOT_PM_BATCH:
		case DECLARATION_IMPOT_PM_LOCAL:
			return ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclarationType.CORPORATE_ENTITY_TAX_DECLARATION;
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
		case FAILLITE:
		case FUSION_ENTREPRISES:
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
		case FUSION_ENTREPRISES:
		case FAILLITE:
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

	public static ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason coreToXMLv3(ch.vd.uniregctb.type.MotifFor ouverture) {
		if (ouverture == null) {
			return null;
		}

		switch (ouverture) {
		case ACHAT_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.PURCHASE_REAL_ESTATE;
		case ANNULATION:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.CANCELLATION;
		case ARRIVEE_HC:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON;
		case ARRIVEE_HS:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY;
		case CHGT_MODE_IMPOSITION:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD;
		case DEBUT_ACTIVITE_DIPLOMATIQUE:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.START_DIPLOMATIC_ACTVITY;
		case DEBUT_EXPLOITATION:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION;
		case DEMENAGEMENT_VD:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.MOVE_VD;
		case DEPART_HC:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON;
		case DEPART_HS:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY;
		case FIN_ACTIVITE_DIPLOMATIQUE:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.END_DIPLOMATIC_ACTVITY;
		case FIN_EXPLOITATION:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.END_COMMERCIAL_EXPLOITATION;
		case FUSION_COMMUNES:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.MERGE_OF_MUNICIPALITIES;
		case DEBUT_PRESTATION_IS:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.START_WITHHOLDING_ACTIVITY;
		case FIN_PRESTATION_IS:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.END_WITHHOLDING_ACTIVITY;
		case CESSATION_ACTIVITE_FUSION_FAILLITE:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.END_ACTIVITY_MERGER_BANKRUPTCY;
		case DEMENAGEMENT_SIEGE:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.MOVE_HEADQUARTERS;
		case FUSION_ENTREPRISES:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.CORPORATION_MERGER;
		case FAILLITE:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.BANKRUPTCY;
		case INDETERMINE:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.UNDETERMINED;
		case MAJORITE:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.MAJORITY;
		case MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION;
		case PERMIS_C_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.C_PERMIT_SWISS;
		case REACTIVATION:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.REACTIVATION;
		case SEJOUR_SAISONNIER:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.SEASONAL_JOURNEY;
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION;
		case VENTE_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.SALE_REAL_ESTATE;
		case VEUVAGE_DECES:
			return ch.vd.unireg.xml.party.taxresidence.v3.LiabilityChangeReason.WIDOWHOOD_DEATH;
		default:
			throw new IllegalArgumentException("Motif de for inconnu = [" + ouverture + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason coreToXMLv4(ch.vd.uniregctb.type.MotifFor ouverture) {
		if (ouverture == null) {
			return null;
		}

		switch (ouverture) {
		case ACHAT_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.PURCHASE_REAL_ESTATE;
		case ANNULATION:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.CANCELLATION;
		case ARRIVEE_HC:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON;
		case ARRIVEE_HS:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY;
		case CHGT_MODE_IMPOSITION:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD;
		case DEBUT_ACTIVITE_DIPLOMATIQUE:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.START_DIPLOMATIC_ACTVITY;
		case DEBUT_EXPLOITATION:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION;
		case DEMENAGEMENT_VD:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.MOVE_VD;
		case DEPART_HC:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON;
		case DEPART_HS:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY;
		case FIN_ACTIVITE_DIPLOMATIQUE:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.END_DIPLOMATIC_ACTVITY;
		case FIN_EXPLOITATION:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.END_COMMERCIAL_EXPLOITATION;
		case FUSION_COMMUNES:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.MERGE_OF_MUNICIPALITIES;
		case DEBUT_PRESTATION_IS:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.START_WITHHOLDING_ACTIVITY;
		case FIN_PRESTATION_IS:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.END_WITHHOLDING_ACTIVITY;
		case CESSATION_ACTIVITE_FUSION_FAILLITE:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.END_ACTIVITY_MERGER_BANKRUPTCY;
		case DEMENAGEMENT_SIEGE:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.MOVE_HEADQUARTERS;
		case FUSION_ENTREPRISES:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.CORPORATION_MERGER;
		case FAILLITE:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.BANKRUPTCY;
		case INDETERMINE:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.UNDETERMINED;
		case MAJORITE:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.MAJORITY;
		case MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION;
		case PERMIS_C_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.C_PERMIT_SWISS;
		case REACTIVATION:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.REACTIVATION;
		case SEJOUR_SAISONNIER:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.SEASONAL_JOURNEY;
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION;
		case VENTE_IMMOBILIER:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.SALE_REAL_ESTATE;
		case VEUVAGE_DECES:
			return ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason.WIDOWHOOD_DEATH;
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

	public static ch.vd.unireg.xml.party.person.v4.Sex coreToXMLv4(ch.vd.uniregctb.type.Sexe sexe) {
		if (sexe == null) {
			return null;
		}

		switch (sexe) {
		case FEMININ:
			return ch.vd.unireg.xml.party.person.v4.Sex.FEMALE;
		case MASCULIN:
			return ch.vd.unireg.xml.party.person.v4.Sex.MALE;
		default:
			throw new IllegalArgumentException("Type de sexe inconnu = [" + sexe + ']'); // certainement qu'il vient d'une autre dimension
		}
	}

	public static ch.vd.unireg.xml.party.person.v5.Sex coreToXMLv5(ch.vd.uniregctb.type.Sexe sexe) {
		if (sexe == null) {
			return null;
		}

		switch (sexe) {
		case FEMININ:
			return ch.vd.unireg.xml.party.person.v5.Sex.FEMALE;
		case MASCULIN:
			return ch.vd.unireg.xml.party.person.v5.Sex.MALE;
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
		case MINE:
			// que retourner d'autre ?
			return ch.vd.unireg.xml.party.immovableproperty.v1.ImmovablePropertyType.IMMOVABLE_PROPERTY;
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
		case MINE:
			// que retourner d'autre ?
			return ch.vd.unireg.xml.party.immovableproperty.v2.ImmovablePropertyType.IMMOVABLE_PROPERTY;
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

	public static ModeImposition xmlToCore(ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod taxationMethod){
		if (taxationMethod == null) {
			return null;
		}
		switch (taxationMethod){
			case EXPENDITURE_BASED:
				return ModeImposition.DEPENSE;
			case INDIGENT:
				return ModeImposition.INDIGENT;
			case MIXED_137_1:
				return ModeImposition.MIXTE_137_1;
			case MIXED_137_2:
				return ModeImposition.MIXTE_137_2;
			case ORDINARY:
				return ModeImposition.ORDINAIRE;
			case WITHHOLDING:
				return ModeImposition.SOURCE;
			default:
				throw new IllegalArgumentException("Mode d'imposition inconnue = [" + taxationMethod + "]");
		}
	}

	public static TypeAssujettissement xmlToCore(ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType taxLiabilityType){
		if (taxLiabilityType == null) {
			return null;
		}
		switch (taxLiabilityType){
		case EXPENDITURE_BASED:
			return TypeAssujettissement.VAUDOIS_DEPENSE;
		case INDIGENT:
			return TypeAssujettissement.INDIGENT;
		case FOREIGN_COUNTRY:
			return TypeAssujettissement.HORS_SUISSE;
		case MIXED_WITHHOLDING_137_1:
			return TypeAssujettissement.MIXTE_137_1;
		case MIXED_WITHHOLDING_137_2:
			return TypeAssujettissement.MIXTE_137_2;
		case NONE:
			return TypeAssujettissement.NON_ASSUJETTI;
		case ORDINARY_RESIDENT:
			return TypeAssujettissement.VAUDOIS_ORDINAIRE;
		case OTHER_CANTON:
			return TypeAssujettissement.HORS_CANTON;
		case PURE_WITHHOLDING:
			return TypeAssujettissement.SOURCE_PURE;
		case SWISS_DIPLOMAT:
			return TypeAssujettissement.DIPLOMATE_SUISSE;

		default:
			throw new IllegalArgumentException("Type d'assujetissement inconnu = [" + taxLiabilityType + "]");
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

	public static ch.vd.unireg.xml.party.person.v4.CommonHouseholdStatus coreToXMLv4(StatutMenageCommun setc) {
		if (setc == null) {
			return null;
		}
		switch (setc) {
		case EN_VIGUEUR:
			return ch.vd.unireg.xml.party.person.v4.CommonHouseholdStatus.ACTIVE;
		case TERMINE_SUITE_DECES:
			return ch.vd.unireg.xml.party.person.v4.CommonHouseholdStatus.ENDED_BY_DEATH;
		case TERMINE_SUITE_SEPARATION:
			return ch.vd.unireg.xml.party.person.v4.CommonHouseholdStatus.SEPARATED_DIVORCED;
		default:
			throw new IllegalArgumentException("unknown StatutMenageCommun = [" + setc + ']');
		}
	}

	public static ch.vd.unireg.xml.party.person.v5.CommonHouseholdStatus coreToXMLv5(StatutMenageCommun setc) {
		if (setc == null) {
			return null;
		}
		switch (setc) {
		case EN_VIGUEUR:
			return ch.vd.unireg.xml.party.person.v5.CommonHouseholdStatus.ACTIVE;
		case TERMINE_SUITE_DECES:
			return ch.vd.unireg.xml.party.person.v5.CommonHouseholdStatus.ENDED_BY_DEATH;
		case TERMINE_SUITE_SEPARATION:
			return ch.vd.unireg.xml.party.person.v5.CommonHouseholdStatus.SEPARATED_DIVORCED;
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

	public static ch.vd.unireg.xml.party.taxresidence.v3.WithholdingTaxationPeriodType coreToXMLv3(PeriodeImpositionImpotSource.Type setc) {
		if (setc == null) {
			return null;
		}
		switch (setc) {
		case MIXTE:
			return ch.vd.unireg.xml.party.taxresidence.v3.WithholdingTaxationPeriodType.MIXED;
		case SOURCE:
			return ch.vd.unireg.xml.party.taxresidence.v3.WithholdingTaxationPeriodType.PURE;
		default:
			throw new IllegalArgumentException("unknown PeriodeImpositionImpotSource.Type = [" + setc + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v4.WithholdingTaxationPeriodType coreToXMLv4(PeriodeImpositionImpotSource.Type setc) {
		if (setc == null) {
			return null;
		}
		switch (setc) {
		case MIXTE:
			return ch.vd.unireg.xml.party.taxresidence.v4.WithholdingTaxationPeriodType.MIXED;
		case SOURCE:
			return ch.vd.unireg.xml.party.taxresidence.v4.WithholdingTaxationPeriodType.PURE;
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

	public static ch.vd.unireg.xml.party.othercomm.v2.LegalForm coreToXMLv2(FormeJuridique formeJuridique) {
		if (formeJuridique == null) {
			return null;
		}
		switch (formeJuridique) {
		case ASS:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.ASSOCIATION;
		case COOP:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.COOPERATIVE_SOCIETY;
		case EDP:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.STATUTORY_CORPORATION;
		case EI:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.SOLE_PROPRIETORSHIP;
		case FOND:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.FOUNDATION;
		case IND:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.JOINT_POSSESSION;
		case PRO:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.CORPORATION_WITHOUT_COMPULSORY_REGISTRATION;
		case SA:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.LIMITED_COMPANY;
		case SAEDP:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.PUBLIC_LIMITED_COMPANY;
		case SARL:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.LIMITED_LIABILITY_COMPANY;
		case SC:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.LIMITED_PARTNERSHIP;
		case SCA:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP;
		case SCPC:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS;
		case SEE:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.BRANCH_OF_FOREIGN_BASED_COMPANY;
		case SES:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.BRANCH_OF_SWISS_COMPANY;
		case SICAF:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.CLOSED_END_INVESTMENT_TRUST;
		case SICAV:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.OPEN_ENDED_INVESTMENT_TRUST;
		case SNC:
			return ch.vd.unireg.xml.party.othercomm.v2.LegalForm.GENERAL_PARTNERSHIP;
		default:
			throw new IllegalArgumentException("unknown FormeJuridique = [" + formeJuridique + ']');
		}
	}

	@Nullable
	public static ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm coreToXMLv4(FormeJuridiqueEntreprise fj) {
		if (fj == null) {
			return null;
		}

		switch (fj) {

		case ASSOCIATION:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.ASSOCIATION;
		case SCOOP:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.COOPERATIVE_SOCIETY;
		case ADM_CH:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FEDERAL_ADMINISTRATION;
		case ADM_CT:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.CANTONAL_ADMINISTRATION;
		case ADM_DI:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.DISTRICT_ADMINISTRATION;
		case ADM_CO:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.MUNICIPALITY_ADMINISTRATION;
		case CORP_DP_ADM:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.STATUTORY_ADMINISTRATION;
		case ENT_CH:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FEDERAL_CORPORATION;
		case ENT_CT:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.CANTONAL_CORPORATION;
		case ENT_DI:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.DISTRICT_CORPORATION;
		case ENT_CO:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.MUNICIPALITY_CORPORATION;
		case CORP_DP_ENT:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.STATUTORY_CORPORATION;
		case FONDATION:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FOUNDATION;
		case SA:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.LIMITED_COMPANY;
		case SARL:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.LIMITED_LIABILITY_COMPANY;
		case SC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.LIMITED_PARTNERSHIP;
		case SCA:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP;
		case SNC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.GENERAL_PARTNERSHIP;
		case SCPC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS;
		case SICAV:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.OPEN_ENDED_INVESTMENT_TRUST;
		case SICAF:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.CLOSED_END_INVESTMENT_TRUST;
		case EI:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.SOLE_PROPRIETORSHIP;
		case ADM_PUBLIQUE_HS:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FOREIGN_STATUTORY_ADMINISTRATION;
		case ENT_HS:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FOREIGN_CORPORATION;
		case ENT_PUBLIQUE_HS:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FOREIGN_STATUTORY_CORPORATION;
		case FILIALE_HS_NIRC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.UNREGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY;
		case FILIALE_HS_RC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.REGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY;
		case IDP:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.STATUTORY_INSTITUTE;
		case INDIVISION:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.JOINT_POSSESSION;
		case FILIALE_CH_RC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.BRANCH_OF_SWISS_COMPANY;
		case ORG_INTERNAT:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.INTERNATIONAL_ORGANIZATION;
		case PARTICULIER:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.OTHER;
		case PNC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.NON_COMMERCIAL_PROXY;
		case SS:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.SIMPLE_COMPANY;
		default:
			throw new IllegalArgumentException("Forme juridique inconnue : " + fj);
		}
	}

	@Nullable
	public static ch.vd.unireg.xml.party.taxpayer.v5.LegalForm coreToXMLv5(FormeJuridiqueEntreprise fj) {
		if (fj == null) {
			return null;
		}

		switch (fj) {

		case ASSOCIATION:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.ASSOCIATION;
		case SCOOP:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.COOPERATIVE_SOCIETY;
		case ADM_CH:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FEDERAL_ADMINISTRATION;
		case ADM_CT:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.CANTONAL_ADMINISTRATION;
		case ADM_DI:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.DISTRICT_ADMINISTRATION;
		case ADM_CO:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.MUNICIPALITY_ADMINISTRATION;
		case CORP_DP_ADM:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.STATUTORY_ADMINISTRATION;
		case ENT_CH:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FEDERAL_CORPORATION;
		case ENT_CT:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.CANTONAL_CORPORATION;
		case ENT_DI:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.DISTRICT_CORPORATION;
		case ENT_CO:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.MUNICIPALITY_CORPORATION;
		case CORP_DP_ENT:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.STATUTORY_CORPORATION;
		case FONDATION:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FOUNDATION;
		case SA:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_COMPANY;
		case SARL:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_LIABILITY_COMPANY;
		case SC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_PARTNERSHIP;
		case SCA:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP;
		case SNC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.GENERAL_PARTNERSHIP;
		case SCPC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS;
		case SICAV:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.OPEN_ENDED_INVESTMENT_TRUST;
		case SICAF:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.CLOSED_END_INVESTMENT_TRUST;
		case EI:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.SOLE_PROPRIETORSHIP;
		case ADM_PUBLIQUE_HS:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FOREIGN_STATUTORY_ADMINISTRATION;
		case ENT_HS:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FOREIGN_CORPORATION;
		case ENT_PUBLIQUE_HS:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FOREIGN_STATUTORY_CORPORATION;
		case FILIALE_HS_NIRC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.UNREGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY;
		case FILIALE_HS_RC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.REGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY;
		case IDP:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.STATUTORY_INSTITUTE;
		case INDIVISION:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.JOINT_POSSESSION;
		case FILIALE_CH_RC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.BRANCH_OF_SWISS_COMPANY;
		case ORG_INTERNAT:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.INTERNATIONAL_ORGANIZATION;
		case PARTICULIER:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.OTHER;
		case PNC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.NON_COMMERCIAL_PROXY;
		case SS:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.SIMPLE_COMPANY;
		default:
			throw new IllegalArgumentException("Forme juridique inconnue : " + fj);
		}
	}

	public static ch.vd.unireg.xml.party.corporation.v4.CorporationFlagType coreToXMLv4(TypeFlagEntreprise type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case UTILITE_PUBLIQUE:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationFlagType.PUBLIC_INTEREST;
		case APM_SOC_IMM_SUBVENTIONNEE:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationFlagType.ASSOCIATION_FOUNDATION_REAL_ESTATE_COMPANY;
		case SOC_IMM_ACTIONNAIRES_LOCATAIRES:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationFlagType.TENANT_SHAREHOLDERS_REAL_ESTATE_COMPANY;
		case SOC_IMM_CARACTERE_SOCIAL:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationFlagType.SOCIAL_REAL_ESTATE_COMPANY;
		case SOC_IMM_ORDINAIRE:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationFlagType.REAL_ESTATE_COMPANY;
		case SOC_IMM_SUBVENTIONNEE:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationFlagType.SUBSIDIZED_REAL_ESTATE_COMPANY;
		case SOC_SERVICE:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationFlagType.SERVICE_COMPANY;
		default:
			throw new IllegalArgumentException("Type de flag inconnu : " + type);
		}
	}

	public static ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType coreToXMLv5(TypeFlagEntreprise type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case UTILITE_PUBLIQUE:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType.PUBLIC_INTEREST;
		case APM_SOC_IMM_SUBVENTIONNEE:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType.ASSOCIATION_FOUNDATION_REAL_ESTATE_COMPANY;
		case SOC_IMM_ACTIONNAIRES_LOCATAIRES:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType.TENANT_SHAREHOLDERS_REAL_ESTATE_COMPANY;
		case SOC_IMM_CARACTERE_SOCIAL:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType.SOCIAL_REAL_ESTATE_COMPANY;
		case SOC_IMM_ORDINAIRE:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType.REAL_ESTATE_COMPANY;
		case SOC_IMM_SUBVENTIONNEE:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType.SUBSIDIZED_REAL_ESTATE_COMPANY;
		case SOC_SERVICE:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType.SERVICE_COMPANY;
		case AUDIT:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType.AUDIT;
		case EXPERTISE:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType.EXPERTISE;
		case IMIN:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType.MINIMAL_TAX;
		default:
			throw new IllegalArgumentException("Type de flag inconnu : " + type);
		}
	}

	@Nullable
	public static String coreToXMLv1v2v3(FormeJuridiqueEntreprise fj) {
		if (fj == null) {
			return null;
		}

		switch (fj) {

		case ASSOCIATION:
			return "ASS";

		case SCOOP:
			return "S. COOP.";

		case ADM_CH:
		case ADM_CT:
		case ADM_DI:
		case ADM_CO:
		case CORP_DP_ADM:
			return "DP";

		case ENT_CH:
		case ENT_CT:
		case ENT_DI:
		case ENT_CO:
		case CORP_DP_ENT:
			return "DP/PM";

		case FONDATION:
			return "FONDATION";

		case SA:
			return "S.A.";

		case SARL:
			return "S.A.R.L.";

		case SC:
			return "S. COMM.";

		case SCA:
			return "S.COMM.ACT";

		case SNC:
			return "S.N.C.";

		case SCPC:
			return "FDS. PLAC.";

		case SICAV:
		case SICAF:
		case EI:
		case ADM_PUBLIQUE_HS:
		case ENT_HS:
		case ENT_PUBLIQUE_HS:
		case FILIALE_HS_NIRC:
		case FILIALE_HS_RC:
		case IDP:
		case INDIVISION:
		case ORG_INTERNAT:
		case PARTICULIER:
		case PNC:
		case SS:
		case FILIALE_CH_RC:
			return null;

		default:
			throw new IllegalArgumentException("Forme juridique inconnue : " + fj);
		}
	}

	@Nullable
	public static String coreToXMLv1v2v3(FormeLegale fj) {
		if (fj == null) {
			return null;
		}

		switch (fj) {

		case N_0109_ASSOCIATION:
			return "ASS";

		case N_0108_SOCIETE_COOPERATIVE:
			return "S. COOP.";

		case N_0220_ADMINISTRATION_CONFEDERATION:
		case N_0221_ADMINISTRATION_CANTON:
		case N_0222_ADMINISTRATION_DISTRICT:
		case N_0223_ADMINISTRATION_COMMUNE:
		case N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION:
			return "DP";

		case N_0230_ENTREPRISE_CONFEDERATION:
		case N_0231_ENTREPRISE_CANTON:
		case N_0232_ENTREPRISE_DISTRICT:
		case N_0233_ENTREPRISE_COMMUNE:
		case N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE:
			return "DP/PM";

		case N_0110_FONDATION:
			return "FONDATION";

		case N_0106_SOCIETE_ANONYME:
			return "S.A.";

		case N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE:
			return "S.A.R.L.";

		case N_0104_SOCIETE_EN_COMMANDITE:
			return "S. COMM.";

		case N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS:
			return "S.COMM.ACT";

		case N_0103_SOCIETE_NOM_COLLECTIF:
			return "S.N.C.";

		case N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX:
			return "FDS. PLAC.";

		case N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE:
		case N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE:
		case N_0101_ENTREPRISE_INDIVIDUELLE:
		case N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE:
		case N_0441_ENTREPRISE_ETRANGERE:
		case N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE:
		case N_0312_FILIALE_ETRANGERE_NON_AU_RC:
		case N_0111_FILIALE_ETRANGERE_AU_RC:
		case N_0117_INSTITUT_DE_DROIT_PUBLIC:
		case N_0119_CHEF_INDIVISION:
		case N_0329_ORGANISATION_INTERNATIONALE:
		case N_0113_FORME_JURIDIQUE_PARTICULIERE:
		case N_0118_PROCURATIONS_NON_COMMERCIALES:
		case N_0302_SOCIETE_SIMPLE:
		case N_0151_SUCCURSALE_SUISSE_AU_RC:
			return null;

		default:
			throw new IllegalArgumentException("Forme juridique inconnue : " + fj);
		}
	}

	@Nullable
	public static ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm coreToXMLv4Full(FormeLegale fl) {
		if (fl == null) {
			return null;
		}

		switch (fl) {

		case N_0109_ASSOCIATION:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.ASSOCIATION;
		case N_0108_SOCIETE_COOPERATIVE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.COOPERATIVE_SOCIETY;
		case N_0220_ADMINISTRATION_CONFEDERATION:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FEDERAL_ADMINISTRATION;
		case N_0221_ADMINISTRATION_CANTON:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.CANTONAL_ADMINISTRATION;
		case N_0222_ADMINISTRATION_DISTRICT:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.DISTRICT_ADMINISTRATION;
		case N_0223_ADMINISTRATION_COMMUNE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.MUNICIPALITY_ADMINISTRATION;
		case N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.STATUTORY_ADMINISTRATION;
		case N_0230_ENTREPRISE_CONFEDERATION:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FEDERAL_CORPORATION;
		case N_0231_ENTREPRISE_CANTON:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.CANTONAL_CORPORATION;
		case N_0232_ENTREPRISE_DISTRICT:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.DISTRICT_CORPORATION;
		case N_0233_ENTREPRISE_COMMUNE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.MUNICIPALITY_CORPORATION;
		case N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.STATUTORY_CORPORATION;
		case N_0110_FONDATION:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FOUNDATION;
		case N_0106_SOCIETE_ANONYME:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.LIMITED_COMPANY;
		case N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.LIMITED_LIABILITY_COMPANY;
		case N_0104_SOCIETE_EN_COMMANDITE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.LIMITED_PARTNERSHIP;
		case N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP;
		case N_0103_SOCIETE_NOM_COLLECTIF:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.GENERAL_PARTNERSHIP;
		case N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS;
		case N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.OPEN_ENDED_INVESTMENT_TRUST;
		case N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.CLOSED_END_INVESTMENT_TRUST;
		case N_0101_ENTREPRISE_INDIVIDUELLE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.SOLE_PROPRIETORSHIP;
		case N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FOREIGN_STATUTORY_ADMINISTRATION;
		case N_0441_ENTREPRISE_ETRANGERE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FOREIGN_CORPORATION;
		case N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.FOREIGN_STATUTORY_CORPORATION;
		case N_0312_FILIALE_ETRANGERE_NON_AU_RC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.UNREGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY;
		case N_0111_FILIALE_ETRANGERE_AU_RC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.REGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY;
		case N_0117_INSTITUT_DE_DROIT_PUBLIC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.STATUTORY_INSTITUTE;
		case N_0119_CHEF_INDIVISION:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.JOINT_POSSESSION;
		case N_0151_SUCCURSALE_SUISSE_AU_RC:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.BRANCH_OF_SWISS_COMPANY;
		case N_0329_ORGANISATION_INTERNATIONALE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.INTERNATIONAL_ORGANIZATION;
		case N_0113_FORME_JURIDIQUE_PARTICULIERE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.OTHER;
		case N_0118_PROCURATIONS_NON_COMMERCIALES:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.NON_COMMERCIAL_PROXY;
		case N_0302_SOCIETE_SIMPLE:
			return ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm.SIMPLE_COMPANY;
		default:
			throw new IllegalArgumentException("Forme juridique inconnue : " + fl);
		}
	}

	@Nullable
	public static ch.vd.unireg.xml.party.taxpayer.v5.LegalForm coreToXMLv5(FormeLegale fl) {
		if (fl == null) {
			return null;
		}

		switch (fl) {

		case N_0109_ASSOCIATION:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.ASSOCIATION;
		case N_0108_SOCIETE_COOPERATIVE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.COOPERATIVE_SOCIETY;
		case N_0220_ADMINISTRATION_CONFEDERATION:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FEDERAL_ADMINISTRATION;
		case N_0221_ADMINISTRATION_CANTON:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.CANTONAL_ADMINISTRATION;
		case N_0222_ADMINISTRATION_DISTRICT:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.DISTRICT_ADMINISTRATION;
		case N_0223_ADMINISTRATION_COMMUNE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.MUNICIPALITY_ADMINISTRATION;
		case N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.STATUTORY_ADMINISTRATION;
		case N_0230_ENTREPRISE_CONFEDERATION:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FEDERAL_CORPORATION;
		case N_0231_ENTREPRISE_CANTON:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.CANTONAL_CORPORATION;
		case N_0232_ENTREPRISE_DISTRICT:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.DISTRICT_CORPORATION;
		case N_0233_ENTREPRISE_COMMUNE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.MUNICIPALITY_CORPORATION;
		case N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.STATUTORY_CORPORATION;
		case N_0110_FONDATION:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FOUNDATION;
		case N_0106_SOCIETE_ANONYME:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_COMPANY;
		case N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_LIABILITY_COMPANY;
		case N_0104_SOCIETE_EN_COMMANDITE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_PARTNERSHIP;
		case N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP;
		case N_0103_SOCIETE_NOM_COLLECTIF:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.GENERAL_PARTNERSHIP;
		case N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS;
		case N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.OPEN_ENDED_INVESTMENT_TRUST;
		case N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.CLOSED_END_INVESTMENT_TRUST;
		case N_0101_ENTREPRISE_INDIVIDUELLE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.SOLE_PROPRIETORSHIP;
		case N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FOREIGN_STATUTORY_ADMINISTRATION;
		case N_0441_ENTREPRISE_ETRANGERE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FOREIGN_CORPORATION;
		case N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FOREIGN_STATUTORY_CORPORATION;
		case N_0312_FILIALE_ETRANGERE_NON_AU_RC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.UNREGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY;
		case N_0111_FILIALE_ETRANGERE_AU_RC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.REGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY;
		case N_0117_INSTITUT_DE_DROIT_PUBLIC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.STATUTORY_INSTITUTE;
		case N_0119_CHEF_INDIVISION:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.JOINT_POSSESSION;
		case N_0151_SUCCURSALE_SUISSE_AU_RC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.BRANCH_OF_SWISS_COMPANY;
		case N_0329_ORGANISATION_INTERNATIONALE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.INTERNATIONAL_ORGANIZATION;
		case N_0113_FORME_JURIDIQUE_PARTICULIERE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.OTHER;
		case N_0118_PROCURATIONS_NON_COMMERCIALES:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.NON_COMMERCIAL_PROXY;
		case N_0302_SOCIETE_SIMPLE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.SIMPLE_COMPANY;
		default:
			throw new IllegalArgumentException("Forme juridique inconnue : " + fl);
		}
	}

	public static ch.vd.unireg.xml.party.taxpayer.v5.LegalForm coreToXMLv5(FormeJuridique formeJuridique) {
		if (formeJuridique == null) {
			return null;
		}
		switch (formeJuridique) {
		case ASS:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.ASSOCIATION;
		case COOP:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.COOPERATIVE_SOCIETY;
		case EDP:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.STATUTORY_CORPORATION;
		case EI:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.SOLE_PROPRIETORSHIP;
		case FOND:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.FOUNDATION;
		case IND:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.JOINT_POSSESSION;
		case PRO:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.OTHER;
		case SA:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_COMPANY;
		case SAEDP:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.STATUTORY_CORPORATION;      // SA et SAEDP ne sont plus différenciées...
		case SARL:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_LIABILITY_COMPANY;
		case SC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_PARTNERSHIP;
		case SCA:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP;
		case SCPC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS;
		case SEE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.REGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY;
		case SES:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.BRANCH_OF_SWISS_COMPANY;
		case SICAF:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.CLOSED_END_INVESTMENT_TRUST;
		case SICAV:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.OPEN_ENDED_INVESTMENT_TRUST;
		case SNC:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.GENERAL_PARTNERSHIP;
		default:
			throw new IllegalArgumentException("unknown FormeJuridique = [" + formeJuridique + ']');
		}
	}

	@Nullable
	public static ch.vd.unireg.xml.party.corporation.v4.TaxSystemScope coreToXMLv4(RegimeFiscal.Portee portee) {
		if (portee == null) {
			return null;
		}

		switch (portee) {
		case CH:
			return ch.vd.unireg.xml.party.corporation.v4.TaxSystemScope.CH;
		case VD:
			return ch.vd.unireg.xml.party.corporation.v4.TaxSystemScope.VD;
		default:
			throw new IllegalArgumentException("Portée de régime fiscal inconnue : " + portee);
		}
	}

	public static ch.vd.unireg.xml.party.corporation.v5.TaxSystemScope coreToXMLv5(RegimeFiscal.Portee portee) {
		if (portee == null) {
			return null;
		}

		switch (portee) {
		case CH:
			return ch.vd.unireg.xml.party.corporation.v5.TaxSystemScope.CH;
		case VD:
			return ch.vd.unireg.xml.party.corporation.v5.TaxSystemScope.VD;
		default:
			throw new IllegalArgumentException("Portée de régime fiscal inconnue : " + portee);
		}
	}

	@Nullable
	public static ch.vd.unireg.xml.party.corporation.v1.LegalSeatType coreToXMLLegalSeatv1(TypeAutoriteFiscale taf) {
		if (taf == null) {
			return null;
		}

		switch(taf) {
		case COMMUNE_OU_FRACTION_VD:
		case COMMUNE_HC:
			return ch.vd.unireg.xml.party.corporation.v1.LegalSeatType.SWISS_MUNICIPALITY;
		case PAYS_HS:
			return ch.vd.unireg.xml.party.corporation.v1.LegalSeatType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnue : " + taf);
		}
	}

	@Nullable
	public static ch.vd.unireg.xml.party.corporation.v2.LegalSeatType coreToXMLLegalSeatv2(TypeAutoriteFiscale taf) {
		if (taf == null) {
			return null;
		}

		switch(taf) {
		case COMMUNE_OU_FRACTION_VD:
		case COMMUNE_HC:
			return ch.vd.unireg.xml.party.corporation.v2.LegalSeatType.SWISS_MUNICIPALITY;
		case PAYS_HS:
			return ch.vd.unireg.xml.party.corporation.v2.LegalSeatType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnue : " + taf);
		}
	}

	@Nullable
	public static ch.vd.unireg.xml.party.corporation.v3.LegalSeatType coreToXMLLegalSeatv3(TypeAutoriteFiscale taf) {
		if (taf == null) {
			return null;
		}

		switch(taf) {
		case COMMUNE_OU_FRACTION_VD:
		case COMMUNE_HC:
			return ch.vd.unireg.xml.party.corporation.v3.LegalSeatType.SWISS_MUNICIPALITY;
		case PAYS_HS:
			return ch.vd.unireg.xml.party.corporation.v3.LegalSeatType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnue : " + taf);
		}
	}

	@Nullable
	public static ch.vd.unireg.xml.party.corporation.v4.LegalSeatType coreToXMLLegalSeatv4(TypeAutoriteFiscale taf) {
		if (taf == null) {
			return null;
		}

		switch(taf) {
		case COMMUNE_OU_FRACTION_VD:
		case COMMUNE_HC:
			return ch.vd.unireg.xml.party.corporation.v4.LegalSeatType.SWISS_MUNICIPALITY;
		case PAYS_HS:
			return ch.vd.unireg.xml.party.corporation.v4.LegalSeatType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnue : " + taf);
		}
	}

	@Nullable
	public static ch.vd.unireg.xml.party.corporation.v5.LegalSeatType coreToXMLLegalSeatv5(TypeAutoriteFiscale taf) {
		if (taf == null) {
			return null;
		}

		switch(taf) {
		case COMMUNE_OU_FRACTION_VD:
		case COMMUNE_HC:
			return ch.vd.unireg.xml.party.corporation.v5.LegalSeatType.SWISS_MUNICIPALITY;
		case PAYS_HS:
			return ch.vd.unireg.xml.party.corporation.v5.LegalSeatType.FOREIGN_COUNTRY;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnue : " + taf);
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v2.IndividualTaxLiabilityType coreToXMLIndividualv2(TypeAssujettissement t) {
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

	public static ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType coreToXMLIndividualv3(TypeAssujettissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case DIPLOMATE_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType.SWISS_DIPLOMAT;
		case HORS_CANTON:
			return ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType.OTHER_CANTON;
		case HORS_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType.FOREIGN_COUNTRY;
		case INDIGENT:
			return ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType.INDIGENT;
		case MIXTE_137_1:
			return ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_1;
		case MIXTE_137_2:
			return ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_2;
		case NON_ASSUJETTI:
			return ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType.NONE;
		case SOURCE_PURE:
			return ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType.PURE_WITHHOLDING;
		case VAUDOIS_DEPENSE:
			return ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType.EXPENDITURE_BASED;
		case VAUDOIS_ORDINAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v3.IndividualTaxLiabilityType.ORDINARY_RESIDENT;
		default:
			throw new IllegalArgumentException("Type d'assujettissement inconnu = [" + t + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType coreToXMLIndividualv4(TypeAssujettissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case DIPLOMATE_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType.SWISS_DIPLOMAT;
		case HORS_CANTON:
			return ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType.OTHER_CANTON;
		case HORS_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType.FOREIGN_COUNTRY;
		case INDIGENT:
			return ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType.INDIGENT;
		case MIXTE_137_1:
			return ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_1;
		case MIXTE_137_2:
			return ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType.MIXED_WITHHOLDING_137_2;
		case NON_ASSUJETTI:
			return ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType.NONE;
		case SOURCE_PURE:
			return ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType.PURE_WITHHOLDING;
		case VAUDOIS_DEPENSE:
			return ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType.EXPENDITURE_BASED;
		case VAUDOIS_ORDINAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType.ORDINARY_RESIDENT;
		default:
			throw new IllegalArgumentException("Type d'assujettissement inconnu = [" + t + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v2.CorporationTaxLiabilityType coreToXMLCorporationv2(TypeAssujettissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case HORS_CANTON:
			return ch.vd.unireg.xml.party.taxresidence.v2.CorporationTaxLiabilityType.OTHER_CANTON;
		case HORS_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v2.CorporationTaxLiabilityType.FOREIGN_COUNTRY;
		case VAUDOIS_ORDINAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v2.CorporationTaxLiabilityType.ORDINARY_RESIDENT;
		case NON_ASSUJETTI:
			return ch.vd.unireg.xml.party.taxresidence.v2.CorporationTaxLiabilityType.NONE;
		default:
			throw new IllegalArgumentException("Type d'assujettissement inconnu = [" + t + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v3.CorporationTaxLiabilityType coreToXMLCorporationv3(TypeAssujettissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case HORS_CANTON:
			return ch.vd.unireg.xml.party.taxresidence.v3.CorporationTaxLiabilityType.OTHER_CANTON;
		case HORS_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v3.CorporationTaxLiabilityType.FOREIGN_COUNTRY;
		case VAUDOIS_ORDINAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v3.CorporationTaxLiabilityType.ORDINARY_RESIDENT;
		case NON_ASSUJETTI:
			return ch.vd.unireg.xml.party.taxresidence.v3.CorporationTaxLiabilityType.NONE;
		default:
			throw new IllegalArgumentException("Type d'assujettissement inconnu = [" + t + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxresidence.v4.CorporationTaxLiabilityType coreToXMLCorporationv4(TypeAssujettissement t) {
		if (t == null) {
			return null;
		}

		switch (t) {
		case HORS_CANTON:
			return ch.vd.unireg.xml.party.taxresidence.v4.CorporationTaxLiabilityType.OTHER_CANTON;
		case HORS_SUISSE:
			return ch.vd.unireg.xml.party.taxresidence.v4.CorporationTaxLiabilityType.FOREIGN_COUNTRY;
		case VAUDOIS_ORDINAIRE:
			return ch.vd.unireg.xml.party.taxresidence.v4.CorporationTaxLiabilityType.ORDINARY_RESIDENT;
		case NON_ASSUJETTI:
			return ch.vd.unireg.xml.party.taxresidence.v4.CorporationTaxLiabilityType.NONE;
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

	public static ch.vd.unireg.xml.party.corporation.v4.LighteningTarget coreToXMLv4(AllegementFiscal.TypeCollectivite type, Integer noOfsCommune) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case CONFEDERATION:
			return new ch.vd.unireg.xml.party.corporation.v4.LighteningTarget(new ch.vd.unireg.xml.party.corporation.v4.LighteningTarget.SwissConfederation(), null, null);
		case CANTON:
			return new ch.vd.unireg.xml.party.corporation.v4.LighteningTarget(null, new ch.vd.unireg.xml.party.corporation.v4.LighteningTarget.Canton(), null);
		case COMMUNE:
			return new ch.vd.unireg.xml.party.corporation.v4.LighteningTarget(null, null, new ch.vd.unireg.xml.party.corporation.v4.MunicipalityLighteningTarget(noOfsCommune));
		default:
			throw new IllegalArgumentException("Type de collectivité concernée par un allègement fiscal inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.corporation.v5.LighteningTarget coreToXMLv5(AllegementFiscal.TypeCollectivite type, Integer noOfsCommune) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case CONFEDERATION:
			return new ch.vd.unireg.xml.party.corporation.v5.LighteningTarget(new ch.vd.unireg.xml.party.corporation.v5.LighteningTarget.SwissConfederation(), null, null);
		case CANTON:
			return new ch.vd.unireg.xml.party.corporation.v5.LighteningTarget(null, new ch.vd.unireg.xml.party.corporation.v5.LighteningTarget.Canton(), null);
		case COMMUNE:
			return new ch.vd.unireg.xml.party.corporation.v5.LighteningTarget(null, null, new ch.vd.unireg.xml.party.corporation.v5.MunicipalityLighteningTarget(noOfsCommune));
		default:
			throw new IllegalArgumentException("Type de collectivité concernée par un allègement fiscal inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.corporation.v4.TaxType coreToXMLv4(AllegementFiscal.TypeImpot type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case BENEFICE:
			return ch.vd.unireg.xml.party.corporation.v4.TaxType.PROFIT;
		case CAPITAL:
			return ch.vd.unireg.xml.party.corporation.v4.TaxType.CAPITAL;
		default:
			throw new IllegalArgumentException("Type d'impôt concernée par un allègement fiscal inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.corporation.v5.TaxType coreToXMLv5(AllegementFiscal.TypeImpot type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case BENEFICE:
			return ch.vd.unireg.xml.party.corporation.v5.TaxType.PROFIT;
		case CAPITAL:
			return ch.vd.unireg.xml.party.corporation.v5.TaxType.CAPITAL;
		default:
			throw new IllegalArgumentException("Type d'impôt concernée par un allègement fiscal inconnu = [" + type + ']');
		}
	}

	public static String coreToXMLv1v2v3(TypeEtatEntreprise type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case ABSORBEE:
			return "05";
		case DISSOUTE:
			return "08";
		case EN_FAILLITE:
			return "04";
		case EN_LIQUIDATION:
			return "02";
		case FONDEE:
			return "07";
		case INSCRITE_RC:
			return "01";
		case RADIEE_RC:
			return "06";
		default:
			throw new IllegalArgumentException("Type d'état entreprise inconnu : [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.corporation.v4.CorporationStatusType coreToXMLv4(TypeEtatEntreprise type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case ABSORBEE:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationStatusType.ABSORBED;
		case DISSOUTE:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationStatusType.DISSOLVED;
		case EN_FAILLITE:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationStatusType.BANKRUPT;
		case EN_LIQUIDATION:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationStatusType.IN_LIQUIDATION;
		case FONDEE:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationStatusType.FOUNDED;
		case INSCRITE_RC:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationStatusType.RECORDED_IN_COMMERCIAL_REGISTER;
		case RADIEE_RC:
			return ch.vd.unireg.xml.party.corporation.v4.CorporationStatusType.REMOVED_FROM_COMMERCIAL_REGISTER;
		default:
			throw new IllegalArgumentException("Type d'état entreprise inconnu : [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.corporation.v5.CorporationStatusType coreToXMLv5(TypeEtatEntreprise type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case ABSORBEE:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationStatusType.ABSORBED;
		case DISSOUTE:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationStatusType.DISSOLVED;
		case EN_FAILLITE:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationStatusType.BANKRUPT;
		case EN_LIQUIDATION:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationStatusType.IN_LIQUIDATION;
		case FONDEE:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationStatusType.FOUNDED;
		case INSCRITE_RC:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationStatusType.RECORDED_IN_COMMERCIAL_REGISTER;
		case RADIEE_RC:
			return ch.vd.unireg.xml.party.corporation.v5.CorporationStatusType.REMOVED_FROM_COMMERCIAL_REGISTER;
		default:
			throw new IllegalArgumentException("Type d'état entreprise inconnu : [" + type + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxpayer.v4.LegalFormCategory coreToXMLv4(CategorieEntreprise categorie) {
		if (categorie == null) {
			return null;
		}

		switch (categorie) {
		case APM:
			return ch.vd.unireg.xml.party.taxpayer.v4.LegalFormCategory.ASSOCIATION_FOUNDATION;
		case AUTRE:
			return ch.vd.unireg.xml.party.taxpayer.v4.LegalFormCategory.OTHER;
		case DPAPM:
			return ch.vd.unireg.xml.party.taxpayer.v4.LegalFormCategory.PUBLIC_ADMINISTRATION;
		case DPPM:
			return ch.vd.unireg.xml.party.taxpayer.v4.LegalFormCategory.STATUTORY_CORPORATION;
		case FP:
			return ch.vd.unireg.xml.party.taxpayer.v4.LegalFormCategory.INVESTMENT_FUND;
		case PM:
			return ch.vd.unireg.xml.party.taxpayer.v4.LegalFormCategory.CAPITAL_COMPANY;
		case PP:
			return ch.vd.unireg.xml.party.taxpayer.v4.LegalFormCategory.SINGLE_PERSON_BUSINESS;
		case SP:
			return ch.vd.unireg.xml.party.taxpayer.v4.LegalFormCategory.SOLE_OWNERSHIP_COMPANY;
		default:
			throw new IllegalArgumentException("Type de catégorie d'entreprise inconnu : [" + categorie + ']');
		}
	}

	public static ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory coreToXMLv5(CategorieEntreprise categorie) {
		if (categorie == null) {
			return null;
		}

		switch (categorie) {
		case APM:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory.ASSOCIATION_FOUNDATION;
		case AUTRE:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory.OTHER;
		case DPAPM:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory.PUBLIC_ADMINISTRATION;
		case DPPM:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory.STATUTORY_CORPORATION;
		case FP:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory.INVESTMENT_FUND;
		case PM:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory.CAPITAL_COMPANY;
		case PP:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory.SINGLE_PERSON_BUSINESS;
		case SP:
			return ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory.SOLE_OWNERSHIP_COMPANY;
		default:
			throw new IllegalArgumentException("Type de catégorie d'entreprise inconnu : [" + categorie + ']');
		}
	}

	@NotNull
	public static CommunityOfOwnersType coreToXMLv5(@NotNull TypeCommunaute type) {
		switch (type) {
		case COMMUNAUTE_DE_BIENS:
			return CommunityOfOwnersType.COMMUNITY_OF_PROPERTY;
		case COMMUNAUTE_HEREDITAIRE:
			return CommunityOfOwnersType.COMMUNITY_OF_HEIRS;
		case INDIVISION:
			return CommunityOfOwnersType.JOINT_OWNERSHIP;
		case SOCIETE_SIMPLE:
			return CommunityOfOwnersType.SIMPLE_PARTNERSHIP;
		case INCONNU:
			return CommunityOfOwnersType.UNKNOWN;
		default:
			throw new IllegalArgumentException("Type de communauté inconnu = [" + type + "]");
		}
	}

	@NotNull
	public static OwnershipType coreToXMLv5(@NotNull GenrePropriete regime) {
		switch (regime) {
		case COMMUNE:
			return OwnershipType.COLLECTIVE_OWNERSHIP;
		case COPROPRIETE:
			return OwnershipType.SIMPLE_CO_OWNERSHIP;
		case INDIVIDUELLE:
			return OwnershipType.SOLE_OWNERSHIP;
		case PPE:
			return OwnershipType.CONDOMINIUM_OWNERSHIP;
		case FONDS_DOMINANT:
			return OwnershipType.DOMINANT_OWNERSHIP;
		default:
			throw new IllegalArgumentException("Genre de proriété inconnu = [" + regime + "]");
		}
	}
}
