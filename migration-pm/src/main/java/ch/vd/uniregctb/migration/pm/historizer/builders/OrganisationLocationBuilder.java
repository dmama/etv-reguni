package ch.vd.uniregctb.migration.pm.historizer.builders;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.CommercialRegisterStatus;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.UidRegisterTypeOfOrganisation;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.rcent.model.OrganisationLocation;

public class OrganisationLocationBuilder {

	//  	private final List<DateRanged<Identifier>> identifier;
	private final Map<BigInteger, List<DateRanged<Identifier>>> identifiers;
	//		private final List<DateRanged<String>> name;
	private final Map<BigInteger, List<DateRanged<String>>> names;
	//		private final List<DateRanged<String>> otherNames;
	private final Map<BigInteger, List<DateRanged<String>>> otherNames;
	//		private final List<DateRanged<KindOfLocation>> kindOfLocation;
	private final Map<BigInteger, List<DateRanged<KindOfLocation>>> kindOfLocations;
	//		private final List<DateRanged<Integer>> seat;
	private final Map<BigInteger, List<DateRanged<Integer>>> sieges;
//		private final List<DateRanged<Function>> function;
//		private final List<DateRanged<Long>> replacedBy;
//		private final List<DateRanged<Long>> inReplacementOf;

	//		private final List<DateRanged<CommercialRegisterStatus>> status;
	private final Map<BigInteger, List<DateRanged<CommercialRegisterStatus>>> status;
//		private final List<DateRanged<String>> name;
//		private final List<DateRanged<CommercialRegisterEntryStatus>> entryStatus;
//		private final List<DateRanged<Capital>> capital;
//		private final List<DateRanged<Address>> legalAddress;
    private final Map<BigInteger, List<DateRanged<Address>>> rcAddresses;

	//		private final List<DateRanged<UidRegisterStatus>> status;
//		private final List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation;
	private final Map<BigInteger, List<DateRanged<UidRegisterTypeOfOrganisation>>> uidTypeOfOrganisation;
//		private final List<DateRanged<Address>> effectiveAddress;
	private final Map<BigInteger, List<DateRanged<Address>>> uidEffectiveAddesses;
	//		private final List<DateRanged<Address>> postOfficeBoxAddress;
	private final Map<BigInteger, List<DateRanged<Address>>> uidPostalBoxAddresses;
//		private final List<DateRanged<UidRegisterPublicStatus>> publicStatus;
//		private final List<DateRanged<UidRegisterLiquidationReason>> liquidationReason;

//		private final List<DateRanged<UidRegisterStatus>> status;
//		private final List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation;
//		private final List<DateRanged<Address>> effectiveAddress;
//		private final List<DateRanged<Address>> postOfficeBoxAddress;
//		private final List<DateRanged<UidRegisterPublicStatus>> publicStatus;
//		private final List<DateRanged<UidRegisterLiquidationReason>> liquidationReason;


	public OrganisationLocationBuilder(Map<BigInteger, List<DateRanged<Identifier>>> identifiers,
	                                   Map<BigInteger, List<DateRanged<String>>> names,
	                                   Map<BigInteger, List<DateRanged<String>>> otherNames,
	                                   Map<BigInteger, List<DateRanged<KindOfLocation>>> kindOfLocations,
	                                   Map<BigInteger, List<DateRanged<Integer>>> sieges,
	                                   Map<BigInteger, List<DateRanged<CommercialRegisterStatus>>> status,
	                                   Map<BigInteger, List<DateRanged<Address>>> rcAddresses,
	                                   Map<BigInteger, List<DateRanged<UidRegisterTypeOfOrganisation>>> uidTypeOfOrganisation,
	                                   Map<BigInteger, List<DateRanged<Address>>> uidEffectiveAddesses,
	                                   Map<BigInteger, List<DateRanged<Address>>> uidPostalBoxAddresses) {
		this.uidTypeOfOrganisation = uidTypeOfOrganisation;
		this.uidPostalBoxAddresses = uidPostalBoxAddresses;
		this.identifiers = identifiers;
		this.names = names;
		this.otherNames = otherNames;
		this.kindOfLocations = kindOfLocations;
		this.sieges = sieges;
		this.status = status;
		this.rcAddresses = rcAddresses;
		this.uidEffectiveAddesses = uidEffectiveAddesses;
	}

	public List<OrganisationLocation> build() {
		// TODO: le boulot
		return null;
	}
}
