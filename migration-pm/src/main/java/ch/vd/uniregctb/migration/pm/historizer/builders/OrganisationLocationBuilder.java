package ch.vd.uniregctb.migration.pm.historizer.builders;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.CommercialRegisterStatus;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.UidRegisterTypeOfOrganisation;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.rcent.model.OrganisationLocation;

public class OrganisationLocationBuilder {

	private final Map<BigInteger, List<DateRanged<Identifier>>> identifiers;
	private final Map<BigInteger, List<DateRanged<String>>> names;
	private final Map<BigInteger, List<DateRanged<String>>> otherNames;
	private final Map<BigInteger, List<DateRanged<KindOfLocation>>> kindOfLocations;
	private final Map<BigInteger, List<DateRanged<Integer>>> seats;
//		private final List<DateRanged<Function>> function;
//		private final List<DateRanged<Long>> replacedBy;
//		private final List<DateRanged<Long>> inReplacementOf;

	private final Map<BigInteger, List<DateRanged<CommercialRegisterStatus>>> status;
//		private final List<DateRanged<String>> name;
//		private final List<DateRanged<CommercialRegisterEntryStatus>> entryStatus;
//		private final List<DateRanged<Capital>> capital;
    private final Map<BigInteger, List<DateRanged<Address>>> rcLegalAddresses;

	//		private final List<DateRanged<UidRegisterStatus>> status;
	private final Map<BigInteger, List<DateRanged<UidRegisterTypeOfOrganisation>>> uidTypeOfOrganisation;
	private final Map<BigInteger, List<DateRanged<Address>>> uidEffectiveAddesses;
	private final Map<BigInteger, List<DateRanged<Address>>> uidPostalBoxAddresses;
//		private final List<DateRanged<UidRegisterPublicStatus>> publicStatus;
//		private final List<DateRanged<UidRegisterLiquidationReason>> liquidationReason;

	public OrganisationLocationBuilder(Map<BigInteger, List<DateRanged<Identifier>>> identifiers,
	                                   Map<BigInteger, List<DateRanged<String>>> names,
	                                   Map<BigInteger, List<DateRanged<String>>> otherNames,
	                                   Map<BigInteger, List<DateRanged<KindOfLocation>>> kindOfLocations,
	                                   Map<BigInteger, List<DateRanged<Integer>>> seats,
	                                   Map<BigInteger, List<DateRanged<CommercialRegisterStatus>>> status,
	                                   Map<BigInteger, List<DateRanged<Address>>> rcLegalAddresses,
	                                   Map<BigInteger, List<DateRanged<UidRegisterTypeOfOrganisation>>> uidTypeOfOrganisation,
	                                   Map<BigInteger, List<DateRanged<Address>>> uidEffectiveAddesses,
	                                   Map<BigInteger, List<DateRanged<Address>>> uidPostalBoxAddresses) {
		this.uidTypeOfOrganisation = uidTypeOfOrganisation;
		this.uidPostalBoxAddresses = uidPostalBoxAddresses;
		this.identifiers = identifiers;
		this.names = names;
		this.otherNames = otherNames;
		this.kindOfLocations = kindOfLocations;
		this.seats = seats;
		this.status = status;
		this.rcLegalAddresses = rcLegalAddresses;
		this.uidEffectiveAddesses = uidEffectiveAddesses;
	}

	public List<OrganisationLocation> build() {
		return names.entrySet().stream() // On peut itérer sur names car le nom est obligatoire. Chaque orga est donc présente dans cette map.
				.map(e -> new OrganisationLocation(e.getKey().longValue(),
				                                   e.getValue(),
				                                   new OrganisationLocation.RCEntRCData(status.get(e.getKey()),
				                                                                        null,
				                                                                        null,
				                                                                        null,
				                                                                        rcLegalAddresses.get(e.getKey()),
				                                                                        null
				                                   ),
				                                   new OrganisationLocation.RCEntUIDData(uidEffectiveAddesses.get(e.getKey()),
				                                                                         null,
				                                                                         uidTypeOfOrganisation.get(e.getKey()),
				                                                                         uidPostalBoxAddresses.get(e.getKey()),
				                                                                         null,
				                                                                         null
				                                   ),
				                                   identifiers.get(e.getKey()),
				                                   otherNames.get(e.getKey()),
				                                   kindOfLocations.get(e.getKey()),
				                                   seats.get(e.getKey()),
				                                   null,
				                                   null,
				                                   null
				     )
				)
				.collect(Collectors.toList());
	}
}
