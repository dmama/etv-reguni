package ch.vd.uniregctb.migration.pm.rcent.model;

import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Capital;
import ch.vd.evd0022.v1.CommercialRegisterEntryStatus;
import ch.vd.evd0022.v1.CommercialRegisterStatus;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.UidRegisterLiquidationReason;
import ch.vd.evd0022.v1.UidRegisterPublicStatus;
import ch.vd.evd0022.v1.UidRegisterStatus;
import ch.vd.evd0022.v1.UidRegisterTypeOfOrganisation;
import ch.vd.evd0022.v1.VatRegisterEntryStatus;
import ch.vd.evd0022.v1.VatRegisterStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;

public class OrganisationLocation {

    /**
     * Identifiant cantonal - "Clé primaire"
     */
    final private long cantonalId;
    @NotNull
    final private List<DateRanged<String>> name;

	public final RCEntRCData rc;
	public final RCEntUIDData uid;
	public final RCEntVATData vat;

	public final List<DateRanged<Identifier>> identifier;
	public final List<DateRanged<String>> otherNames;
	public final List<DateRanged<KindOfLocation>> kindOfLocation;
	public final List<DateRanged<Integer>> seat;
	public final List<DateRanged<Function>> function;
	public final List<DateRanged<String>> nogaCode;
	public final List<DateRanged<Long>> replacedBy;
	public final List<DateRanged<Long>> inReplacementOf;

	public OrganisationLocation(long cantonalId, @NotNull List<DateRanged<String>> name, RCEntRCData rc, RCEntUIDData uid, RCEntVATData vat,
	                            List<DateRanged<Identifier>> identifier, List<DateRanged<String>> otherNames,
	                            List<DateRanged<KindOfLocation>> kindOfLocation, List<DateRanged<Integer>> seat,
	                            List<DateRanged<Function>> function, List<DateRanged<String>> nogaCode,
	                            List<DateRanged<Long>> replacedBy, List<DateRanged<Long>> inReplacementOf) {
		this.cantonalId = cantonalId;
		this.name = name;
		this.rc = rc;
		this.uid = uid;
		this.vat = vat;
		this.identifier = identifier;
		this.otherNames = otherNames;
		this.kindOfLocation = kindOfLocation;
		this.seat = seat;
		this.function = function;
		this.nogaCode = nogaCode;
		this.replacedBy = replacedBy;
		this.inReplacementOf = inReplacementOf;
	}

	public long getCantonalId() {
		return cantonalId;
	}

	public List<DateRanged<Function>> getFunction() {
		return function;
	}

	public List<DateRanged<Identifier>> getIdentifier() {
		return identifier;
	}

	public List<DateRanged<Long>> getInReplacementOf() {
		return inReplacementOf;
	}

	public List<DateRanged<KindOfLocation>> getKindOfLocation() {
		return kindOfLocation;
	}

	@NotNull
	public List<DateRanged<String>> getName() {
		return name;
	}

	public List<DateRanged<String>> getNogaCode() {
		return nogaCode;
	}

	public List<DateRanged<String>> getOtherNames() {
		return otherNames;
	}

	public RCEntRCData getRc() {
		return rc;
	}

	public List<DateRanged<Long>> getReplacedBy() {
		return replacedBy;
	}

	public List<DateRanged<Integer>> getSeat() {
		return seat;
	}

	public RCEntUIDData getUid() {
		return uid;
	}

	public RCEntVATData getVat() {
		return vat;
	}

	public static class RCEntRCData {
		private final List<DateRanged<CommercialRegisterStatus>> status;
		private final List<DateRanged<String>> name;
		private final List<DateRanged<CommercialRegisterEntryStatus>> entryStatus;
		private final List<DateRanged<Capital>> capital;
		private final List<DateRanged<Address>> legalAddress;
		private final List<DateRanged> entryDate;
		private final List<DateRanged> cancellationDate;

		public RCEntRCData(List<DateRanged> cancellationDate, List<DateRanged<CommercialRegisterStatus>> status,
		                   List<DateRanged<String>> name, List<DateRanged<CommercialRegisterEntryStatus>> entryStatus,
		                   List<DateRanged<Capital>> capital, List<DateRanged<Address>> legalAddress, List<DateRanged> entryDate) {
			this.cancellationDate = cancellationDate;
			this.status = status;
			this.name = name;
			this.entryStatus = entryStatus;
			this.capital = capital;
			this.legalAddress = legalAddress;
			this.entryDate = entryDate;
		}

		public List<DateRanged> getCancellationDate() {
			return cancellationDate;
		}

		public List<DateRanged<Capital>> getCapital() {
			return capital;
		}

		public List<DateRanged> getEntryDate() {
			return entryDate;
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
	}

	public static class RCEntUIDData {
		private final List<DateRanged<UidRegisterStatus>> status;
		private final List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation;
		private final List<Address> effectiveAddress;
		private final List<Address> postOfficeBoxAddress;
		private final List<DateRanged<UidRegisterPublicStatus>> publicStatus;
		private final List<DateRanged<UidRegisterLiquidationReason>> liquidationReason;

		public RCEntUIDData(List<Address> effectiveAddress, List<DateRanged<UidRegisterStatus>> status,
		                    List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation, List<Address> postOfficeBoxAddress,
		                    List<DateRanged<UidRegisterPublicStatus>> publicStatus,
		                    List<DateRanged<UidRegisterLiquidationReason>> liquidationReason) {
			this.effectiveAddress = effectiveAddress;
			this.status = status;
			this.typeOfOrganisation = typeOfOrganisation;
			this.postOfficeBoxAddress = postOfficeBoxAddress;
			this.publicStatus = publicStatus;
			this.liquidationReason = liquidationReason;
		}

		public List<Address> getEffectiveAddress() {
			return effectiveAddress;
		}

		public List<DateRanged<UidRegisterLiquidationReason>> getLiquidationReason() {
			return liquidationReason;
		}

		public List<Address> getPostOfficeBoxAddress() {
			return postOfficeBoxAddress;
		}

		public List<DateRanged<UidRegisterPublicStatus>> getPublicStatus() {
			return publicStatus;
		}

		public List<DateRanged<UidRegisterStatus>> getStatus() {
			return status;
		}

		public List<DateRanged<UidRegisterTypeOfOrganisation>> getTypeOfOrganisation() {
			return typeOfOrganisation;
		}
	}

	public static class RCEntVATData {

		private final List<DateRanged<VatRegisterStatus>> VATStatus;
		private final List<DateRanged<VatRegisterEntryStatus>> VATEntryStatus;
		private final List<DateRanged<RegDate>> VATEntryDate;
		private final List<DateRanged<RegDate>> VATCancellationDate;

		public RCEntVATData(List<DateRanged<RegDate>> VATCancellationDate, List<DateRanged<VatRegisterStatus>> VATStatus,
		                    List<DateRanged<VatRegisterEntryStatus>> VATEntryStatus,
		                    List<DateRanged<RegDate>> VATEntryDate) {
			this.VATCancellationDate = VATCancellationDate;
			this.VATStatus = VATStatus;
			this.VATEntryStatus = VATEntryStatus;
			this.VATEntryDate = VATEntryDate;
		}

		public List<DateRanged<RegDate>> getVATCancellationDate() {
			return VATCancellationDate;
		}

		public List<DateRanged<RegDate>> getVATEntryDate() {
			return VATEntryDate;
		}

		public List<DateRanged<VatRegisterEntryStatus>> getVATEntryStatus() {
			return VATEntryStatus;
		}

		public List<DateRanged<VatRegisterStatus>> getVATStatus() {
			return VATStatus;
		}
	}
}
