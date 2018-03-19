package ch.vd.unireg.webservices.party4.impl;

import java.util.Set;

import ch.vd.unireg.interfaces.infra.data.TypeAffranchissement;
import ch.vd.unireg.webservices.party4.SearchMode;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.party.debtor.v2.CommunicationMode;
import ch.vd.unireg.xml.party.debtor.v2.DebtorCategory;
import ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod;
import ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriodicity;
import ch.vd.unireg.xml.party.immovableproperty.v1.OwnershipType;
import ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v2.Sex;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.unireg.xml.party.taxdeclaration.v2.DocumentType;
import ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus;
import ch.vd.unireg.xml.party.taxpayer.v2.WithholdingTaxTariff;
import ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod;
import ch.vd.unireg.xml.party.v2.AccountNumberFormat;
import ch.vd.unireg.xml.party.v2.PartyType;
import ch.vd.unireg.avatar.TypeAvatar;
import ch.vd.unireg.interfaces.model.CompteBancaire;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypePermis;

public abstract class EnumHelper {

	public static Set<CategorieImpotSource> getCategoriesImpotSourceAutorisees() {
		return ch.vd.unireg.xml.EnumHelper.CIS_SUPPORTEES_V2;
	}

	public static Set<TypeAvatar> getTypesAvatarsIgnores() {
		return ch.vd.unireg.xml.EnumHelper.TA_IGNORES_V2;
	}

	public static MaritalStatus coreToWeb(ch.vd.unireg.type.EtatCivil etatCivil) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(etatCivil);
	}

	public static DebtorCategory coreToWeb(ch.vd.unireg.type.CategorieImpotSource categorieImpotSource) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(categorieImpotSource);
	}

	public static TaxDeclarationStatusType coreToWeb(TypeEtatDocumentFiscal type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(type);
	}

	public static TaxType coreToWeb(ch.vd.unireg.type.GenreImpot genreImpot) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv1(genreImpot);
	}

	public static TaxLiabilityReason coreToWeb(ch.vd.unireg.type.MotifRattachement rattachement) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv1(rattachement);
	}

	public static TaxationAuthorityType coreToWeb(ch.vd.unireg.type.TypeAutoriteFiscale typeForFiscal) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv1(typeForFiscal);
	}

	public static TaxationMethod coreToWeb(ch.vd.unireg.type.ModeImposition mode) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv1(mode);
	}

	public static CommunicationMode coreToWeb(ch.vd.unireg.type.ModeCommunication mode) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(mode);
	}

	public static WithholdingTaxDeclarationPeriod coreToWeb(ch.vd.unireg.type.PeriodeDecompte periodeDecompte) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(periodeDecompte);
	}

	public static WithholdingTaxDeclarationPeriodicity coreToWeb(ch.vd.unireg.type.PeriodiciteDecompte periodiciteDecompte) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(periodiciteDecompte);
	}

	public static NaturalPersonCategory coreToWeb(ch.vd.unireg.type.CategorieEtranger categorie) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(categorie);
	}

	public static NaturalPersonCategory coreToWeb(TypePermis permis) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(permis);
	}

	public static RelationBetweenPartiesType coreToWeb(ch.vd.unireg.type.TypeRapportEntreTiers type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv1(type);
	}

	public static WithholdingTaxTariff coreToWeb(ch.vd.unireg.type.TarifImpotSource tarif) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(tarif);
	}

	public static DocumentType coreToWeb(ch.vd.unireg.type.TypeDocument type) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(type);
	}

	public static LiabilityChangeReason coreToWeb(ch.vd.unireg.type.MotifFor ouverture) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv1(ouverture);
	}

	public static SearchMode coreToWeb(ch.vd.unireg.tiers.TiersCriteria.TypeRecherche type) {
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
			throw new IllegalArgumentException("Type de recherche inconnu = [" + type + ']');
		}
	}

	public static ch.vd.unireg.tiers.TiersCriteria.TypeRecherche webToCore(SearchMode type) {
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
			throw new IllegalArgumentException("Type de recherche inconnu = [" + type + ']');
		}
	}

	public static Sex coreToWeb(ch.vd.unireg.type.Sexe sexe) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(sexe);
	}

	public static TariffZone coreToWeb(TypeAffranchissement t) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv1(t);
	}

	public static AccountNumberFormat coreToWeb(CompteBancaire.Format format) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv2(format);
	}

	public static OwnershipType coreToWeb(GenrePropriete genre) {
		return ch.vd.unireg.xml.EnumHelper.coreToXMLv1(genre);
	}

	public static CategorieImpotSource webToCore(DebtorCategory category) {
		return ch.vd.unireg.xml.EnumHelper.xmlToCore(category);
	}

	public static TiersCriteria.TypeTiers webToCore(PartyType type) {
		return ch.vd.unireg.xml.EnumHelper.xmlToCore(type);
	}
}