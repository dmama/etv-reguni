package ch.vd.uniregctb.adapter.rcent.historizer.builders;

import java.math.BigInteger;
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
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adapter.rcent.historizer.convertor.DateRangedConvertor;
import ch.vd.uniregctb.adapter.rcent.historizer.convertor.MultivalueListConverter;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationFunction;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class OrganisationLocationBuilder {

	private final Map<BigInteger, List<DateRangeHelper.Ranged<Identifier>>> identifiers;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<String>>> names;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<String>>> otherNames;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<KindOfLocation>>> kindOfLocations;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Integer>>> seats;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Function>>> function;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> replacedBy;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> inReplacementOf;

	private final Map<BigInteger, List<DateRangeHelper.Ranged<CommercialRegisterStatus>>> rcStatus;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<String>>> rcName;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<CommercialRegisterEntryStatus>>> entryStatus;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> entryDate;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Capital>>> capital;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> rcLegalAddresses;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<String>>> purpose;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> byLawsDate;


	private final Map<BigInteger, List<DateRangeHelper.Ranged<UidRegisterStatus>>> uidStatus;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<UidRegisterTypeOfOrganisation>>> uidTypeOfOrganisation;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidEffectiveAddesses;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidPostalBoxAddresses;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<UidRegisterLiquidationReason>>> uidLiquidationReason;

	public OrganisationLocationBuilder(Map<BigInteger, List<DateRangeHelper.Ranged<Identifier>>> identifiers,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<String>>> names,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<String>>> otherNames,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<KindOfLocation>>> kindOfLocations,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Integer>>> seats,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Function>>> function,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> replacedBy,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> inReplacementOf,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<String>>> rcName,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> rcLegalAddresses,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<CommercialRegisterStatus>>> rcStatus,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<CommercialRegisterEntryStatus>>> entryStatus,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> entryDate,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Capital>>> capital,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<String>>> purpose, Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> byLawsDate,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<UidRegisterStatus>>> uidStatus,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<UidRegisterTypeOfOrganisation>>> uidTypeOfOrganisation,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidEffectiveAddesses,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidPostalBoxAddresses,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<UidRegisterLiquidationReason>>> uidLiquidationReason) {
		this.replacedBy = replacedBy;
		this.inReplacementOf = inReplacementOf;
		this.purpose = purpose;
		this.byLawsDate = byLawsDate;
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
				                                                                        entryDate.get(e.getKey()),
				                                                                        purpose.get(e.getKey()),
				                                                                        byLawsDate.get(e.getKey())),
				                                   new OrganisationLocation.RCEntUIDData(uidEffectiveAddesses.get(e.getKey()),
				                                                                         uidStatus.get(e.getKey()),
				                                                                         uidTypeOfOrganisation.get(e.getKey()),
				                                                                         uidPostalBoxAddresses.get(e.getKey()),
				                                                                         uidLiquidationReason.get(e.getKey())
				                                   ),
				                                   MultivalueListConverter.toMapOfListsOfDateRangedValues(identifiers.get(e.getKey()), Identifier::getIdentifierCategory,
				                                                                                          Identifier::getIdentifierValue),
				                                   otherNames.get(e.getKey()),
				                                   kindOfLocations.get(e.getKey()),
				                                   seats.get(e.getKey()),
				                                   function.get(e.getKey()) == null ? null : MultivalueListConverter
						                                   .toMapOfListsOfDateRangedValues(function.get(e.getKey()), f -> f.getParty().getPerson().getName(), OrganisationFunction::new),
				                                   replacedBy.get(e.getKey()) == null ? null : DateRangedConvertor.convert(replacedBy.get(e.getKey()), BigInteger::longValue),
				                                   inReplacementOf.get(e.getKey()) == null ? null :
						                                   MultivalueListConverter.toMapOfListsOfDateRangedValues(inReplacementOf.get(e.getKey()), BigInteger::longValue,
						                                                                                          BigInteger::longValue)
				     )
				)
				.collect(Collectors.toList());
	}
}
