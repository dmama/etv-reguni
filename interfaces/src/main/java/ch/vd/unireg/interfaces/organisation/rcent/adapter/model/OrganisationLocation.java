package ch.vd.unireg.interfaces.organisation.rcent.adapter.model;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.Address;
import ch.vd.evd0022.v3.BusinessPublication;
import ch.vd.evd0022.v3.Capital;
import ch.vd.evd0022.v3.CommercialRegisterDiaryEntry;
import ch.vd.evd0022.v3.KindOfUidEntity;
import ch.vd.evd0022.v3.LegalForm;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0022.v3.UidDeregistrationReason;
import ch.vd.evd0022.v3.UidRegisterStatus;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

public class OrganisationLocation {

    /**
     * Identifiant cantonal - "Clé primaire"
     */
    private final long cantonalId;
    @NotNull
    private final List<DateRangeHelper.Ranged<String>> name;

	public final RCEntRCData rc;
	public final RCEntUIDData uid;
	public final RCEntBURData bur;

	public final List<BusinessPublication> businessPublication;

	private final Map<String,List<DateRangeHelper.Ranged<String>>> identifiers;
	private final List<DateRangeHelper.Ranged<String>> additionalName;
	private final List<DateRangeHelper.Ranged<TypeOfLocation>> typeOfLocation;
	private final List<DateRangeHelper.Ranged<LegalForm>> legalForm;
	private final List<DateRangeHelper.Ranged<Integer>> seat;
	private final Map<String, List<DateRangeHelper.Ranged<OrganisationFunction>>> function;
	private final List<DateRangeHelper.Ranged<Long>> burTransferTo;
	private final List<DateRangeHelper.Ranged<Long>> burTransferFrom;
	private final List<DateRangeHelper.Ranged<Long>> uidReplacedBy;
	private final List<DateRangeHelper.Ranged<Long>> uidInReplacementOf;

	public OrganisationLocation(long cantonalId, @NotNull List<DateRangeHelper.Ranged<String>> name, RCEntRCData rc, RCEntUIDData uid, RCEntBURData bur,
	                            Map<String, List<DateRangeHelper.Ranged<String>>> identifiers, List<DateRangeHelper.Ranged<String>> additionalName,
	                            List<DateRangeHelper.Ranged<TypeOfLocation>> typeOfLocation, List<DateRangeHelper.Ranged<LegalForm>> legalForm,
	                            List<DateRangeHelper.Ranged<Integer>> seat, List<BusinessPublication> businessPublication,
	                            Map<String, List<DateRangeHelper.Ranged<OrganisationFunction>>> function, List<DateRangeHelper.Ranged<Long>> burTransferTo,
	                            List<DateRangeHelper.Ranged<Long>> burTransferFrom, List<DateRangeHelper.Ranged<Long>> uidReplacedBy,
	                            List<DateRangeHelper.Ranged<Long>> UidInReplacementOf) {
		this.cantonalId = cantonalId;
		this.name = name;
		this.rc = rc;
		this.uid = uid;
		this.bur = bur;
		this.businessPublication = businessPublication;
		this.identifiers = identifiers;
		this.additionalName = additionalName;
		this.typeOfLocation = typeOfLocation;
		this.legalForm = legalForm;
		this.seat = seat;
		this.function = function;
		this.burTransferTo = burTransferTo;
		this.burTransferFrom = burTransferFrom;
		this.uidReplacedBy = uidReplacedBy;
		this.uidInReplacementOf = UidInReplacementOf;
	}

	public long getCantonalId() {
		return cantonalId;
	}

	/**
	 * Historique des personnes associées à l'organisation dans le cadre d'une fonction. L'historique
	 * de chaque personne est séparé et indexé par son nom.
	 *
	 * Pourquoi son nom? Parce que c'est le seul champ obligatoire dont on dispose. L'historique d'une personne sera donc
	 * discontinu en cas de changement de nom suite à changement d'état civil ou correction. Mais c'est mieux que d'avoir des trous.
	 *
	 * Ce champ est donc essentiellement un champ de consultation.
	 *
	 * @return La Map des historiques, ou null si aucune fonction.
	 */
	public Map<String, List<DateRangeHelper.Ranged<OrganisationFunction>>> getFunction() {
		return function;
	}

	public Map<String,List<DateRangeHelper.Ranged<String>>> getIdentifiers() {
		return identifiers;
	}

	public List<DateRangeHelper.Ranged<TypeOfLocation>> getTypeOfLocation() {
		return typeOfLocation;
	}

	@NotNull
	public List<DateRangeHelper.Ranged<String>> getName() {
		return name;
	}

	/**
	 * @return L'historique du noms additionnel, ou null si aucun historique.
	 */
	public List<DateRangeHelper.Ranged<String>> getAdditionalName() {
		return additionalName;
	}

	public RCEntRCData getRc() {
		return rc;
	}

	public List<DateRangeHelper.Ranged<LegalForm>> getLegalForm() {
		return legalForm;
	}

	public List<DateRangeHelper.Ranged<Integer>> getSeat() {
		return seat;
	}

	public RCEntUIDData getUid() {
		return uid;
	}

	public RCEntBURData getBur() {
		return bur;
	}

	/**
	 * @return Les historiques de publication FOSC, indexés par date de publication.
	 */
	public List<BusinessPublication> getBusinessPublication() {
		return businessPublication;
	}

	/**
	 * Historique de l'établissement remplacé à 'IDE.
	 * @return L'historique de l'établissement remplacé, ou null si aucun historique.
	 */
	public List<DateRangeHelper.Ranged<Long>> getUidInReplacementOf() {
		return uidInReplacementOf;
	}

	/**
	 * Historique de établissement remplacant à l'IDE
	 * @return L'historique de l'établissement remplacant, ou null si aucun historique.
	 */
	public List<DateRangeHelper.Ranged<Long>> getUidReplacedBy() {
		return uidReplacedBy;
	}

	/**
	 * Historique de l'établissement repris, selon le BUR.
	 * @return L'historique des établissements repris, ou null si aucun historique.
	 */
	public List<DateRangeHelper.Ranged<Long>> getBurTransferFrom() {
		return burTransferFrom;
	}

	/**
	 * Historique de l'établissement reprenant, selon le BUR.
	 * @return L'historique de l'établissement reprenant, ou null si aucun historique.
	 */
	public List<DateRangeHelper.Ranged<Long>> getBurTransferTo() {
		return burTransferTo;
	}

	public static class RCEntRCData {

		private final List<DateRangeHelper.Ranged<RCRegistrationData>> registrationData;
		private final List<DateRangeHelper.Ranged<Capital>> capital;
		private final List<DateRangeHelper.Ranged<Address>> legalAddress;
		private final List<DateRangeHelper.Ranged<String>> purpose;
		private final List<DateRangeHelper.Ranged<RegDate>> byLawsDate;
		private final List<CommercialRegisterDiaryEntry> diaryEntries;

		public RCEntRCData(List<DateRangeHelper.Ranged<RCRegistrationData>> registrationData,
		                   List<DateRangeHelper.Ranged<Capital>> capital, List<DateRangeHelper.Ranged<Address>> legalAddress,
		                   List<DateRangeHelper.Ranged<String>> purpose, List<DateRangeHelper.Ranged<RegDate>> byLawsDate,
		                   List<CommercialRegisterDiaryEntry> diaryEntries) {
			this.registrationData = registrationData;
			this.capital = capital;
			this.legalAddress = legalAddress;
			this.purpose = purpose;
			this.byLawsDate = byLawsDate;
			this.diaryEntries = diaryEntries;
		}

		public List<DateRangeHelper.Ranged<RCRegistrationData>> getRegistrationData() {
			return registrationData;
		}

		public List<DateRangeHelper.Ranged<Capital>> getCapital() {
			return capital;
		}

		public List<DateRangeHelper.Ranged<Address>> getLegalAddress() {
			return legalAddress;
		}

		public List<DateRangeHelper.Ranged<String>> getPurpose() {
			return purpose;
		}

		public List<DateRangeHelper.Ranged<RegDate>> getByLawsDate() {
			return byLawsDate;
		}

		public List<CommercialRegisterDiaryEntry> getDiaryEntries() {
			return diaryEntries;
		}
	}

	public static class RCEntUIDData {
		private final List<DateRangeHelper.Ranged<UidRegisterStatus>> status;
		private final List<DateRangeHelper.Ranged<KindOfUidEntity>> typeOfOrganisation;
		private final List<DateRangeHelper.Ranged<Address>> effectiveAddress;
		private final List<DateRangeHelper.Ranged<Address>> postOfficeBoxAddress;
		private final List<DateRangeHelper.Ranged<UidDeregistrationReason>> liquidationReason;

		public RCEntUIDData(List<DateRangeHelper.Ranged<Address>> effectiveAddress, List<DateRangeHelper.Ranged<UidRegisterStatus>> status,
		                    List<DateRangeHelper.Ranged<KindOfUidEntity>> typeOfOrganisation,
		                    List<DateRangeHelper.Ranged<Address>> postOfficeBoxAddress,
		                    List<DateRangeHelper.Ranged<UidDeregistrationReason>> liquidationReason) {
			this.effectiveAddress = effectiveAddress;
			this.status = status;
			this.typeOfOrganisation = typeOfOrganisation;
			this.postOfficeBoxAddress = postOfficeBoxAddress;
			this.liquidationReason = liquidationReason;
		}

		public List<DateRangeHelper.Ranged<Address>> getEffectiveAddress() {
			return effectiveAddress;
		}

		public List<DateRangeHelper.Ranged<UidDeregistrationReason>> getLiquidationReason() {
			return liquidationReason;
		}

		public List<DateRangeHelper.Ranged<Address>> getPostOfficeBoxAddress() {
			return postOfficeBoxAddress;
		}

		public List<DateRangeHelper.Ranged<UidRegisterStatus>> getStatus() {
			return status;
		}

		public List<DateRangeHelper.Ranged<KindOfUidEntity>> getTypeOfOrganisation() {
			return typeOfOrganisation;
		}
	}

	public static class RCEntBURData {
		private final List<DateRangeHelper.Ranged<BurRegistrationData>> registrationData;

		public RCEntBURData(List<DateRangeHelper.Ranged<BurRegistrationData>> registrationData) {
			this.registrationData = registrationData;
		}

		public List<DateRangeHelper.Ranged<BurRegistrationData>> getRegistrationData() {
			return registrationData;
		}
	}
}
