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
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged;

public class OrganisationLocation {

    /**
     * Identifiant cantonal - "Clé primaire"
     */
    private final long cantonalId;
    @NotNull
    private final List<DateRanged<String>> name;

	public final RCEntRCData rc;
	public final RCEntUIDData uid;

	private final Map<String,List<DateRanged<String>>> identifiers;
	private final List<DateRanged<String>> otherNames;
	private final List<DateRanged<KindOfLocation>> kindOfLocation;
	private final List<DateRanged<Integer>> seat;
	private final List<DateRanged<Function>> function;

	public OrganisationLocation(long cantonalId, @NotNull List<DateRanged<String>> name, RCEntRCData rc, RCEntUIDData uid,
	                            Map<String,List<DateRanged<String>>> identifiers, List<DateRanged<String>> otherNames,
	                            List<DateRanged<KindOfLocation>> kindOfLocation, List<DateRanged<Integer>> seat,
	                            List<DateRanged<Function>> function) {
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

	public List<DateRanged<Function>> getFunction() {
		return function;
	}

	public Map<String,List<DateRanged<String>>> getIdentifiers() {
		return identifiers;
	}

	public List<DateRanged<KindOfLocation>> getKindOfLocation() {
		return kindOfLocation;
	}

	@NotNull
	public List<DateRanged<String>> getName() {
		return name;
	}

	public List<DateRanged<String>> getOtherNames() {
		return otherNames;
	}

	public RCEntRCData getRc() {
		return rc;
	}

	public List<DateRanged<Integer>> getSeat() {
		return seat;
	}

	public RCEntUIDData getUid() {
		return uid;
	}

	public static class RCEntRCData {
		private final List<DateRanged<CommercialRegisterStatus>> status;
		private final List<DateRanged<String>> name;
		private final List<DateRanged<CommercialRegisterEntryStatus>> entryStatus;
		private final List<DateRanged<Capital>> capital;
		private final List<DateRanged<Address>> legalAddress;
		private final List<DateRanged<RegDate>> entryDate;

		public RCEntRCData(List<DateRanged<CommercialRegisterStatus>> status,
		                   List<DateRanged<String>> name, List<DateRanged<CommercialRegisterEntryStatus>> entryStatus,
		                   List<DateRanged<Capital>> capital, List<DateRanged<Address>> legalAddress, List<DateRanged<RegDate>> entryDate) {
			this.status = status;
			this.name = name;
			this.entryStatus = entryStatus;
			this.capital = capital;
			this.legalAddress = legalAddress;
			this.entryDate = entryDate;
		}

		public List<DateRanged<Capital>> getCapital() {
			return capital;
		}

		public List<DateRanged<CommercialRegisterEntryStatus>> getEntryStatus() {
			return entryStatus;
		}

		public List<DateRanged<Address>> getLegalAddress() {
			return legalAddress;
		}

		public List<DateRanged<String>> getName() {
			return name;
		}

		public List<DateRanged<CommercialRegisterStatus>> getStatus() {
			return status;
		}

		public List<DateRanged<RegDate>> getEntryDate() {
			return entryDate;
		}
	}

	public static class RCEntUIDData {
		private final List<DateRanged<UidRegisterStatus>> status;
		private final List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation;
		private final List<DateRanged<Address>> effectiveAddress;
		private final List<DateRanged<Address>> postOfficeBoxAddress;
		private final List<DateRanged<UidRegisterLiquidationReason>> liquidationReason;

		public RCEntUIDData(List<DateRanged<Address>> effectiveAddress, List<DateRanged<UidRegisterStatus>> status,
		                    List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation,
		                    List<DateRanged<Address>> postOfficeBoxAddress,
		                    List<DateRanged<UidRegisterLiquidationReason>> liquidationReason) {
			this.effectiveAddress = effectiveAddress;
			this.status = status;
			this.typeOfOrganisation = typeOfOrganisation;
			this.postOfficeBoxAddress = postOfficeBoxAddress;
			this.liquidationReason = liquidationReason;
		}

		public List<DateRanged<Address>> getEffectiveAddress() {
			return effectiveAddress;
		}

		public List<DateRanged<UidRegisterLiquidationReason>> getLiquidationReason() {
			return liquidationReason;
		}

		public List<DateRanged<Address>> getPostOfficeBoxAddress() {
			return postOfficeBoxAddress;
		}

		public List<DateRanged<UidRegisterStatus>> getStatus() {
			return status;
		}

		public List<DateRanged<UidRegisterTypeOfOrganisation>> getTypeOfOrganisation() {
			return typeOfOrganisation;
		}
	}
}
