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
	private final List<DateRangeHelper.Ranged<OrganisationFunction>> function;

	public OrganisationLocation(long cantonalId, @NotNull List<DateRangeHelper.Ranged<String>> name, RCEntRCData rc, RCEntUIDData uid,
	                            Map<String,List<DateRangeHelper.Ranged<String>>> identifiers, List<DateRangeHelper.Ranged<String>> otherNames,
	                            List<DateRangeHelper.Ranged<KindOfLocation>> kindOfLocation, List<DateRangeHelper.Ranged<Integer>> seat,
	                            List<DateRangeHelper.Ranged<OrganisationFunction>> function) {
		this.cantonalId = cantonalId;
		this.name = name;
		this.rc = rc;
		this.uid = uid;
		this.identifiers = identifiers;
		this.otherNames = otherNames;
		this.kindOfLocation = kindOfLocation;
		this.seat = seat;
		this.function = function;
	}

	public long getCantonalId() {
		return cantonalId;
	}

	public List<DateRangeHelper.Ranged<OrganisationFunction>> getFunction() {
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

	public static class RCEntRCData {
		private final List<DateRangeHelper.Ranged<CommercialRegisterStatus>> status;
		private final List<DateRangeHelper.Ranged<String>> name;
		private final List<DateRangeHelper.Ranged<CommercialRegisterEntryStatus>> entryStatus;
		private final List<DateRangeHelper.Ranged<Capital>> capital;
		private final List<DateRangeHelper.Ranged<Address>> legalAddress;
		private final List<DateRangeHelper.Ranged<RegDate>> entryDate;

		public RCEntRCData(List<DateRangeHelper.Ranged<CommercialRegisterStatus>> status,
		                   List<DateRangeHelper.Ranged<String>> name, List<DateRangeHelper.Ranged<CommercialRegisterEntryStatus>> entryStatus,
		                   List<DateRangeHelper.Ranged<Capital>> capital, List<DateRangeHelper.Ranged<Address>> legalAddress, List<DateRangeHelper.Ranged<RegDate>> entryDate) {
			this.status = status;
			this.name = name;
			this.entryStatus = entryStatus;
			this.capital = capital;
			this.legalAddress = legalAddress;
			this.entryDate = entryDate;
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
