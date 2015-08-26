package ch.vd.uniregctb.adapter.rcent.historizer.builders;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Capital;
import ch.vd.evd0022.v1.CommercialRegisterEntryStatus;
import ch.vd.evd0022.v1.CommercialRegisterStatus;
import ch.vd.evd0022.v1.Function;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.UidRegisterLiquidationReason;
import ch.vd.evd0022.v1.UidRegisterStatus;
import ch.vd.evd0022.v1.UidRegisterTypeOfOrganisation;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged;
import ch.vd.uniregctb.adapter.rcent.historizer.convertor.IdentifierListConverter;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationFunction;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class OrganisationLocationBuilder {

	private final Map<BigInteger, List<DateRanged<Identifier>>> identifiers;
	private final Map<BigInteger, List<DateRanged<String>>> names;
	private final Map<BigInteger, List<DateRanged<String>>> otherNames;
	private final Map<BigInteger, List<DateRanged<KindOfLocation>>> kindOfLocations;
	private final Map<BigInteger, List<DateRanged<Integer>>> seats;
	private final Map<BigInteger, List<DateRanged<Function>>> function;

	private final Map<BigInteger, List<DateRanged<CommercialRegisterStatus>>> rcStatus;
	private final Map<BigInteger, List<DateRanged<String>>> rcName;
	private final Map<BigInteger, List<DateRanged<CommercialRegisterEntryStatus>>> entryStatus;
	private final Map<BigInteger, List<DateRanged<RegDate>>> entryDate;
	private final Map<BigInteger, List<DateRanged<Capital>>> capital;
    private final Map<BigInteger, List<DateRanged<Address>>> rcLegalAddresses;

	private final Map<BigInteger, List<DateRanged<UidRegisterStatus>>> uidStatus;
	private final Map<BigInteger, List<DateRanged<UidRegisterTypeOfOrganisation>>> uidTypeOfOrganisation;
	private final Map<BigInteger, List<DateRanged<Address>>> uidEffectiveAddesses;
	private final Map<BigInteger, List<DateRanged<Address>>> uidPostalBoxAddresses;
	private final Map<BigInteger, List<DateRanged<UidRegisterLiquidationReason>>> uidLiquidationReason;

	public OrganisationLocationBuilder(Map<BigInteger, List<DateRanged<Identifier>>> identifiers,
	                                   Map<BigInteger, List<DateRanged<String>>> names,
	                                   Map<BigInteger, List<DateRanged<String>>> otherNames,
	                                   Map<BigInteger, List<DateRanged<KindOfLocation>>> kindOfLocations,
	                                   Map<BigInteger, List<DateRanged<Integer>>> seats,
	                                   Map<BigInteger, List<DateRanged<Function>>> function,
	                                   Map<BigInteger, List<DateRanged<String>>> rcName,
	                                   Map<BigInteger, List<DateRanged<Address>>> rcLegalAddresses,
	                                   Map<BigInteger, List<DateRanged<CommercialRegisterStatus>>> rcStatus,
	                                   Map<BigInteger, List<DateRanged<CommercialRegisterEntryStatus>>> entryStatus,
	                                   Map<BigInteger, List<DateRanged<RegDate>>> entryDate,
	                                   Map<BigInteger, List<DateRanged<Capital>>> capital,
	                                   Map<BigInteger, List<DateRanged<UidRegisterStatus>>> uidStatus,
	                                   Map<BigInteger, List<DateRanged<UidRegisterTypeOfOrganisation>>> uidTypeOfOrganisation,
	                                   Map<BigInteger, List<DateRanged<Address>>> uidEffectiveAddesses,
	                                   Map<BigInteger, List<DateRanged<Address>>> uidPostalBoxAddresses,
	                                   Map<BigInteger, List<DateRanged<UidRegisterLiquidationReason>>> uidLiquidationReason) {
		this.uidTypeOfOrganisation = uidTypeOfOrganisation;
		this.uidPostalBoxAddresses = uidPostalBoxAddresses;
		this.identifiers = identifiers;
		this.names = names;
		this.otherNames = otherNames;
		this.kindOfLocations = kindOfLocations;
		this.seats = seats;
		this.function = function;
		this.rcName = rcName;
		this.rcStatus = rcStatus;
		this.entryDate = entryDate;
		this.entryStatus = entryStatus;
		this.capital = capital;
		this.rcLegalAddresses = rcLegalAddresses;
		this.uidStatus = uidStatus;
		this.uidEffectiveAddesses = uidEffectiveAddesses;
		this.uidLiquidationReason = uidLiquidationReason;
	}

	public List<OrganisationLocation> build() {
		return names.entrySet().stream() // On peut itérer sur names car le nom est obligatoire. Chaque orga est donc présente dans cette map.
				.map(e -> new OrganisationLocation(e.getKey().longValue(),
				                                   e.getValue(),
				                                   new OrganisationLocation.RCEntRCData(rcStatus.get(e.getKey()),
				                                                                        rcName.get(e.getKey()),
				                                                                        entryStatus.get(e.getKey()),
				                                                                        capital.get(e.getKey()),
				                                                                        rcLegalAddresses.get(e.getKey()),
				                                                                        entryDate.get(e.getKey())
				                                   ),
				                                   new OrganisationLocation.RCEntUIDData(uidEffectiveAddesses.get(e.getKey()),
				                                                                         uidStatus.get(e.getKey()),
				                                                                         uidTypeOfOrganisation.get(e.getKey()),
				                                                                         uidPostalBoxAddresses.get(e.getKey()),
				                                                                         uidLiquidationReason.get(e.getKey())
				                                   ),
				                                   IdentifierListConverter.toMapOfListsOfDateRangedValues(identifiers.get(e.getKey())),
				                                   otherNames.get(e.getKey()),
				                                   kindOfLocations.get(e.getKey()),
				                                   seats.get(e.getKey()),
				                                   convertOrganisationFunction(function.get(e.getKey()))
				     )
				)
				.collect(Collectors.toList());
	}

	private List<DateRanged<OrganisationFunction>> convertOrganisationFunction(List<DateRanged<Function>> dateRangeds) {
		if (dateRangeds != null) {
			List<DateRanged<OrganisationFunction>> functions = new ArrayList<>(dateRangeds.size());
			for (DateRanged<Function> rf : dateRangeds) {
				functions.add(new DateRanged<>(rf.getDateDebut(), rf.getDateFin(), new OrganisationFunction(rf.getPayload())));
			}
			return functions;
		}
		return Collections.emptyList();
	}


}
