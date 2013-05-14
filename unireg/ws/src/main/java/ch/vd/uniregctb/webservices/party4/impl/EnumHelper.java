package ch.vd.uniregctb.webservices.party4.impl;

import ch.vd.unireg.webservices.party4.SearchMode;
import ch.vd.unireg.xml.party.address.v1.TariffZone;
import ch.vd.unireg.xml.party.debtor.v2.CommunicationMode;
import ch.vd.unireg.xml.party.debtor.v2.DebtorCategory;
import ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriod;
import ch.vd.unireg.xml.party.debtor.v2.WithholdingTaxDeclarationPeriodicity;
import ch.vd.unireg.xml.party.immovableproperty.v1.ImmovablePropertyType;
import ch.vd.unireg.xml.party.immovableproperty.v1.MutationType;
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
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.rf.TypeImmeuble;
import ch.vd.uniregctb.rf.TypeMutation;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.TypePermis;

public abstract class EnumHelper {

	public static MaritalStatus coreToWeb(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(etatCivil);
	}

	public static DebtorCategory coreToWeb(ch.vd.uniregctb.type.CategorieImpotSource categorieImpotSource) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(categorieImpotSource);
	}

	public static TaxDeclarationStatusType coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration type) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(type);
	}

	public static TaxType coreToWeb(ch.vd.uniregctb.type.GenreImpot genreImpot) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXML(genreImpot);
	}

	public static TaxLiabilityReason coreToWeb(ch.vd.uniregctb.type.MotifRattachement rattachement) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXML(rattachement);
	}

	public static TaxationAuthorityType coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale typeForFiscal) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXML(typeForFiscal);
	}

	public static TaxationMethod coreToWeb(ch.vd.uniregctb.type.ModeImposition mode) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXML(mode);
	}

	public static CommunicationMode coreToWeb(ch.vd.uniregctb.type.ModeCommunication mode) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(mode);
	}

	public static WithholdingTaxDeclarationPeriod coreToWeb(ch.vd.uniregctb.type.PeriodeDecompte periodeDecompte) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(periodeDecompte);
	}

	public static WithholdingTaxDeclarationPeriodicity coreToWeb(ch.vd.uniregctb.type.PeriodiciteDecompte periodiciteDecompte) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(periodiciteDecompte);
	}

	public static NaturalPersonCategory coreToWeb(ch.vd.uniregctb.type.CategorieEtranger categorie) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(categorie);
	}

	public static NaturalPersonCategory coreToWeb(TypePermis permis) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(permis);
	}

	public static RelationBetweenPartiesType coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers type) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXML(type);
	}

	public static WithholdingTaxTariff coreToWeb(ch.vd.uniregctb.type.TarifImpotSource tarif) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(tarif);
	}

	public static DocumentType coreToWeb(ch.vd.uniregctb.type.TypeDocument type) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(type);
	}

	public static LiabilityChangeReason coreToWeb(ch.vd.uniregctb.type.MotifFor ouverture) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXML(ouverture);
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
			throw new IllegalArgumentException("Type de recherche inconnu = [" + type + ']');
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
			throw new IllegalArgumentException("Type de recherche inconnu = [" + type + ']');
		}
	}

	public static Sex coreToWeb(ch.vd.uniregctb.type.Sexe sexe) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(sexe);
	}

	public static TariffZone coreToWeb(ch.vd.uniregctb.interfaces.model.TypeAffranchissement t) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXML(t);
	}

	public static AccountNumberFormat coreToWeb(CompteBancaire.Format format) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXMLv2(format);
	}

	public static OwnershipType coreToWeb(GenrePropriete genre) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXML(genre);
	}

	public static ImmovablePropertyType coreToWeb(TypeImmeuble type) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXML(type);
	}

	public static MutationType coreToWeb(TypeMutation type) {
		return ch.vd.uniregctb.xml.EnumHelper.coreToXML(type);
	}

	public static CategorieImpotSource webToCore(DebtorCategory category) {
		return ch.vd.uniregctb.xml.EnumHelper.xmlToCore(category);
	}

	public static TiersCriteria.TypeTiers webToCore(PartyType type) {
		return ch.vd.uniregctb.xml.EnumHelper.xmlToCore(type);
	}
}
