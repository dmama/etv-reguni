package ch.vd.uniregctb.adapter.rcent.model;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Capital;
import ch.vd.evd0022.v1.CommercialRegisterEntryStatus;
import ch.vd.evd0022.v1.CommercialRegisterStatus;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.UidRegisterLiquidationReason;
import ch.vd.evd0022.v1.UidRegisterStatus;
import ch.vd.evd0022.v1.UidRegisterTypeOfOrganisation;
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

	private final Map<String,List<DateRangeHelper.Ranged<String>>> identifiers;
	private final List<DateRangeHelper.Ranged<String>> otherNames;
	private final List<DateRangeHelper.Ranged<KindOfLocation>> kindOfLocation;
	private final List<DateRangeHelper.Ranged<Integer>> seat;
	private final Map<String, List<DateRangeHelper.Ranged<OrganisationFunction>>> function;
	private final List<DateRangeHelper.Ranged<Long>> replacedBy;
	private final Map<Long, List<DateRangeHelper.Ranged<Long>>> inReplacementOf;

	public OrganisationLocation(long cantonalId, @NotNull List<DateRangeHelper.Ranged<String>> name, RCEntRCData rc, RCEntUIDData uid,
	                            Map<String,List<DateRangeHelper.Ranged<String>>> identifiers, List<DateRangeHelper.Ranged<String>> otherNames,
	                            List<DateRangeHelper.Ranged<KindOfLocation>> kindOfLocation, List<DateRangeHelper.Ranged<Integer>> seat,
	                            Map<String, List<DateRangeHelper.Ranged<OrganisationFunction>>> function, List<DateRangeHelper.Ranged<Long>> replacedBy,
	                            Map<Long, List<DateRangeHelper.Ranged<Long>>> inReplacementOf) {
		this.cantonalId = cantonalId;
		this.name = name;
		this.rc = rc;
		this.uid = uid;
		this.identifiers = identifiers;
		this.otherNames = otherNames;
		this.kindOfLocation = kindOfLocation;
		this.seat = seat;
		this.function = function;
		this.replacedBy = replacedBy;
		this.inReplacementOf = inReplacementOf;
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

	public List<DateRangeHelper.Ranged<KindOfLocation>> getKindOfLocation() {
		return kindOfLocation;
	}

	@NotNull
	public List<DateRangeHelper.Ranged<String>> getName() {
		return name;
	}

	public List<DateRangeHelper.Ranged<String>> getOtherNames() {
		return otherNames;
	}

	public RCEntRCData getRc() {
		return rc;
	}

	public List<DateRangeHelper.Ranged<Integer>> getSeat() {
		return seat;
	}

	public RCEntUIDData getUid() {
		return uid;
	}

	public List<DateRangeHelper.Ranged<Long>> getReplacedBy() {
		return replacedBy;
	}

	/**
	 * Historique multivaleur des établissements remplacés, indexés par leur identifiant cantonal.
	 * @return La Map des établissements remplacés, ou null si aucun historique.
	 */
	public Map<Long, List<DateRangeHelper.Ranged<Long>>> getInReplacementOf() {
		return inReplacementOf;
	}

	public static class RCEntRCData {
		private final List<DateRangeHelper.Ranged<CommercialRegisterStatus>> status;
		private final List<DateRangeHelper.Ranged<String>> name;
		private final List<DateRangeHelper.Ranged<CommercialRegisterEntryStatus>> entryStatus;
		private final List<DateRangeHelper.Ranged<Capital>> capital;
		private final List<DateRangeHelper.Ranged<Address>> legalAddress;
		private final List<DateRangeHelper.Ranged<RegDate>> entryDate;
		private final List<DateRangeHelper.Ranged<String>> purpose;
		private final List<DateRangeHelper.Ranged<RegDate>> byLawsDate;

		public RCEntRCData(List<DateRangeHelper.Ranged<CommercialRegisterStatus>> status,
		                   List<DateRangeHelper.Ranged<String>> name, List<DateRangeHelper.Ranged<CommercialRegisterEntryStatus>> entryStatus,
		                   List<DateRangeHelper.Ranged<Capital>> capital, List<DateRangeHelper.Ranged<Address>> legalAddress, List<DateRangeHelper.Ranged<RegDate>> entryDate,
		                   List<DateRangeHelper.Ranged<String>> purpose, List<DateRangeHelper.Ranged<RegDate>> byLawsDate) {
			this.status = status;
			this.name = name;
			this.entryStatus = entryStatus;
			this.capital = capital;
			this.legalAddress = legalAddress;
			this.entryDate = entryDate;
			this.purpose = purpose;
			this.byLawsDate = byLawsDate;
		}

		public List<DateRangeHelper.Ranged<Capital>> getCapital() {
			return capital;
		}

		public List<DateRangeHelper.Ranged<CommercialRegisterEntryStatus>> getEntryStatus() {
			return entryStatus;
		}

		public List<DateRangeHelper.Ranged<Address>> getLegalAddress() {
			return legalAddress;
		}

		public List<DateRangeHelper.Ranged<String>> getName() {
			return name;
		}

		public List<DateRangeHelper.Ranged<CommercialRegisterStatus>> getStatus() {
			return status;
		}

		public List<DateRangeHelper.Ranged<RegDate>> getEntryDate() {
			return entryDate;
		}

		public List<DateRangeHelper.Ranged<String>> getPurpose() {
			return purpose;
		}

		public List<DateRangeHelper.Ranged<RegDate>> getByLawsDate() {
			return byLawsDate;
		}
	}

	public static class RCEntUIDData {
		private final List<DateRangeHelper.Ranged<UidRegisterStatus>> status;
		private final List<DateRangeHelper.Ranged<UidRegisterTypeOfOrganisation>> typeOfOrganisation;
		private final List<DateRangeHelper.Ranged<Address>> effectiveAddress;
		private final List<DateRangeHelper.Ranged<Address>> postOfficeBoxAddress;
		private final List<DateRangeHelper.Ranged<UidRegisterLiquidationReason>> liquidationReason;

		public RCEntUIDData(List<DateRangeHelper.Ranged<Address>> effectiveAddress, List<DateRangeHelper.Ranged<UidRegisterStatus>> status,
		                    List<DateRangeHelper.Ranged<UidRegisterTypeOfOrganisation>> typeOfOrganisation,
		                    List<DateRangeHelper.Ranged<Address>> postOfficeBoxAddress,
		                    List<DateRangeHelper.Ranged<UidRegisterLiquidationReason>> liquidationReason) {
			this.effectiveAddress = effectiveAddress;
			this.status = status;
			this.typeOfOrganisation = typeOfOrganisation;
			this.postOfficeBoxAddress = postOfficeBoxAddress;
			this.liquidationReason = liquidationReason;
		}

		public List<DateRangeHelper.Ranged<Address>> getEffectiveAddress() {
			return effectiveAddress;
		}

		public List<DateRangeHelper.Ranged<UidRegisterLiquidationReason>> getLiquidationReason() {
			return liquidationReason;
		}

		public List<DateRangeHelper.Ranged<Address>> getPostOfficeBoxAddress() {
			return postOfficeBoxAddress;
		}

		public List<DateRangeHelper.Ranged<UidRegisterStatus>> getStatus() {
			return status;
		}

		public List<DateRangeHelper.Ranged<UidRegisterTypeOfOrganisation>> getTypeOfOrganisation() {
			return typeOfOrganisation;
		}
	}
}
