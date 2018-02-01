package ch.vd.unireg.webservices.v7;

import java.util.EnumSet;
import java.util.Set;

import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.ws.security.v7.AllowedAccess;
import ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType;
import ch.vd.unireg.xml.party.corporation.v5.TaxSystemScope;
import ch.vd.unireg.xml.party.ebilling.v1.EbillingStatusType;
import ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType;
import ch.vd.unireg.xml.party.person.v5.Sex;
import ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType;
import ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclarationType;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.taxpayer.v5.LegalForm;
import ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus;
import ch.vd.unireg.xml.party.taxpayer.v5.WithholdingTaxTariff;
import ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType;
import ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationMethod;
import ch.vd.unireg.xml.party.withholding.v1.CommunicationMode;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod;
import ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity;
import ch.vd.unireg.avatar.TypeAvatar;
import ch.vd.unireg.metier.assujettissement.MotifAssujettissement;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.FormeJuridique;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TarifImpotSource;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeFlagEntreprise;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public abstract class EnumHelper {

	public static AllowedAccess toXml(Niveau niveau) {
		if (niveau == null) {
			return AllowedAccess.NONE;
		}
		switch (niveau) {
			case ECRITURE:
				return AllowedAccess.READ_WRITE;
			case LECTURE:
				return AllowedAccess.READ_ONLY;
			default:
				throw new IllegalArgumentException("Unsupported value: " + niveau);
		}
	}

	public static TiersCriteria.TypeRecherche toCore(SearchMode searchMode) {
		if (searchMode == null) {
			return null;
		}
		switch (searchMode) {
			case IS_EXACTLY:
				return TiersCriteria.TypeRecherche.EST_EXACTEMENT;
			case CONTAINS:
				return TiersCriteria.TypeRecherche.CONTIENT;
			case PHONETIC:
				return TiersCriteria.TypeRecherche.PHONETIQUE;
			default:
				throw new IllegalArgumentException("Unsupported value: " + searchMode);
		}
	}

	public static Set<TiersCriteria.TypeTiers> toCore(Set<PartySearchType> types) {
		if (types == null || types.isEmpty()) {
			return null;
		}
		final Set<TiersCriteria.TypeTiers> res = EnumSet.noneOf(TiersCriteria.TypeTiers.class);
		for (PartySearchType type : types) {
			res.add(toCore(type));
		}
		return res;
	}

	public static TiersCriteria.TypeTiers toCore(PartySearchType type) {
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
		case ESTABLISHMENT:
			return TiersCriteria.TypeTiers.ETABLISSEMENT;
		case NATURAL_PERSON:
			return TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE;
		case NON_RESIDENT_NATURAL_PERSON:
			return TiersCriteria.TypeTiers.NON_HABITANT;
		case RESIDENT_NATURAL_PERSON:
			return TiersCriteria.TypeTiers.HABITANT;
		case ADMINISTRATIVE_AUTHORITY:
			return TiersCriteria.TypeTiers.COLLECTIVITE_ADMINISTRATIVE;
		case OTHER_COMMUNITY:
			return TiersCriteria.TypeTiers.AUTRE_COMMUNAUTE;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + ']');
		}
	}

	public static Set<CategorieImpotSource> getCategoriesImpotSourceAutorisees() {
		return ch.vd.unireg.xml.EnumHelper.CIS_SUPPORTEES_V5;
	}

	public static Set<TypeAvatar> getTypesAvatarsIgnores() {
		return ch.vd.unireg.xml.EnumHelper.TA_IGNORES_V5;
	}

	public static CommunicationMode coreToWeb(ModeCommunication mode) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv3(mode);
	}

	public static TaxSystemScope coreToWeb(RegimeFiscal.Portee portee) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(portee);
	}

	public static Sex coreToWeb(Sexe sexe) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(sexe);
	}

	public static DebtorCategory coreToWeb(CategorieImpotSource cat) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv3(cat);
	}

	public static MaritalStatus coreToWeb(EtatCivil etatCivil) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(etatCivil);
	}

	public static LiabilityChangeReason coreToWeb(MotifFor motif) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv4(motif);
	}

	public static LiabilityChangeReason coreToWeb(MotifAssujettissement motif) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv4(motif);
	}

	public static NaturalPersonCategoryType coreToWeb(CategorieEtranger cat) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(cat);
	}

	public static NaturalPersonCategoryType coreToWeb(TypePermis type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(type);
	}

	public static RelationBetweenPartiesType coreToWeb(TypeRapportEntreTiers type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv3(type);
	}

	public static TaxDeclarationStatusType coreToWeb(TypeEtatDocumentFiscal type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(type);
	}

	public static TaxType coreToWeb(GenreImpot genre) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv4(genre);
	}

	public static TaxLiabilityReason coreToWeb(MotifRattachement motif) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv4(motif);
	}

	public static TaxationAuthorityType coreToWeb(TypeAutoriteFiscale type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv4(type);
	}

	public static TaxationMethod coreToWeb(ModeImposition mode) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv4(mode);
	}

	public static OrdinaryTaxDeclarationType coreToWeb(TypeDocument type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(type);
	}

	public static WithholdingTaxDeclarationPeriodicity coreToWeb(PeriodiciteDecompte periodicite) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv3(periodicite);
	}

	public static WithholdingTaxDeclarationPeriod coreToWeb(PeriodeDecompte periode) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv3(periode);
	}

	public static WithholdingTaxTariff coreToWeb(TarifImpotSource tarif) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(tarif);
	}

	public static LegalForm coreToWeb(FormeJuridique forme) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(forme);
	}

	public static LegalForm coreToWeb(FormeJuridiqueEntreprise forme) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(forme);
	}

	public static IndividualTaxLiabilityType coreToWeb(TypeAssujettissement type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLIndividualv4(type);
	}

	public static EbillingStatusType coreToWeb(TypeEtatDestinataire type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv1(type);
	}

	public static CorporationFlagType coreToWeb(TypeFlagEntreprise type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv5(type);
	}
}
