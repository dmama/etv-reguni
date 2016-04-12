package ch.vd.uniregctb.adapter.rcent.historizer.builders;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.ech.ech0097.v2.NamedOrganisationId;

import ch.vd.evd0022.v3.Address;
import ch.vd.evd0022.v3.BusinessPublication;
import ch.vd.evd0022.v3.Capital;
import ch.vd.evd0022.v3.CommercialRegisterStatus;
import ch.vd.evd0022.v3.DissolutionReason;
import ch.vd.evd0022.v3.Function;
import ch.vd.evd0022.v3.KindOfUidEntity;
import ch.vd.evd0022.v3.LegalForm;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0022.v3.UidDeregistrationReason;
import ch.vd.evd0022.v3.UidRegisterStatus;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adapter.rcent.historizer.convertor.DateRangedConvertor;
import ch.vd.uniregctb.adapter.rcent.historizer.convertor.MultivalueListConverter;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationFunction;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class OrganisationLocationBuilder {

	private final Map<BigInteger, List<DateRangeHelper.Ranged<NamedOrganisationId>>> identifiers;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<String>>> names;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<String>>> additionalName;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<TypeOfLocation>>> kindOfLocations;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<LegalForm>>> legalForm;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Integer>>> seats;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BusinessPublication>>> businessPublication;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Function>>> function;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> burTransferTo;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> burTransferFrom;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> uidReplacedBy;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> uidInReplacementOf;

	private final Map<BigInteger, List<DateRangeHelper.Ranged<CommercialRegisterStatus>>> rcStatus;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<DissolutionReason>>> rcVdDissolutionReason;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> entryDate;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> VdEntryDate;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Capital>>> capital;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> rcLegalAddresses;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<String>>> purpose;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> byLawsDate;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> cancellationDate;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> vDCancellationDate;


	private final Map<BigInteger, List<DateRangeHelper.Ranged<UidRegisterStatus>>> uidStatus;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<KindOfUidEntity>>> uidTypeOfOrganisation;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidEffectiveAddesses;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidPostalBoxAddresses;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<UidDeregistrationReason>>> uidLiquidationReason;

	public OrganisationLocationBuilder(Map<BigInteger, List<DateRangeHelper.Ranged<NamedOrganisationId>>> identifiers,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<String>>> names,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<String>>> additionalName,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<TypeOfLocation>>> kindOfLocations,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<LegalForm>>> legalForm,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BusinessPublication>>> businessPublication,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Integer>>> seats,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Function>>> function,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> burTransferTo,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> burTransferFrom,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> uidReplacedBy,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> UidInReplacementOf,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> rcLegalAddresses,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<CommercialRegisterStatus>>> rcStatus,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<DissolutionReason>>> rcVdDissolutionReason,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> entryDate,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> VdEntryDate,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Capital>>> capital,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<String>>> purpose,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> byLawsDate,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> cancellationDate,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> vDCancellationDate,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<UidRegisterStatus>>> uidStatus,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<KindOfUidEntity>>> uidTypeOfOrganisation,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidEffectiveAddesses,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidPostalBoxAddresses,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<UidDeregistrationReason>>> uidLiquidationReason) {
		this.legalForm = legalForm;
		this.burTransferTo = burTransferTo;
		this.burTransferFrom = burTransferFrom;
		this.uidReplacedBy = uidReplacedBy;
		this.uidInReplacementOf = UidInReplacementOf;
		this.rcVdDissolutionReason = rcVdDissolutionReason;
		this.purpose = purpose;
		this.byLawsDate = byLawsDate;
		this.uidTypeOfOrganisation = uidTypeOfOrganisation;
		this.uidPostalBoxAddresses = uidPostalBoxAddresses;
		this.identifiers = identifiers;
		this.names = names;
		this.additionalName = additionalName;
		this.kindOfLocations = kindOfLocations;
		this.seats = seats;
		this.function = function;
		this.rcStatus = rcStatus;
		this.entryDate = entryDate;
		this.VdEntryDate = VdEntryDate;
		this.capital = capital;
		this.rcLegalAddresses = rcLegalAddresses;
		this.uidStatus = uidStatus;
		this.uidEffectiveAddesses = uidEffectiveAddesses;
		this.uidLiquidationReason = uidLiquidationReason;
		this.cancellationDate = cancellationDate;
		this.vDCancellationDate = vDCancellationDate;
		this.businessPublication = businessPublication;
	}

	public List<OrganisationLocation> build() {
		return names.entrySet().stream() // On peut itérer sur names car le nom est obligatoire. Chaque orga est donc présente dans cette map.
				.map(e -> new OrganisationLocation(e.getKey().longValue(),
				                                   e.getValue(),
				                                   new OrganisationLocation.RCEntRCData(rcStatus.get(e.getKey()),
				                                                                        rcVdDissolutionReason.get(e.getKey()),
				                                                                        capital.get(e.getKey()),
				                                                                        rcLegalAddresses.get(e.getKey()),
				                                                                        entryDate.get(e.getKey()),
				                                                                        VdEntryDate.get(e.getKey()),
				                                                                        purpose.get(e.getKey()),
				                                                                        byLawsDate.get(e.getKey()),
				                                                                        cancellationDate.get(e.getKey()),
				                                                                        vDCancellationDate.get(e.getKey())),
				                                   new OrganisationLocation.RCEntUIDData(uidEffectiveAddesses.get(e.getKey()),
				                                                                         uidStatus.get(e.getKey()),
				                                                                         uidTypeOfOrganisation.get(e.getKey()),
				                                                                         uidPostalBoxAddresses.get(e.getKey()),
				                                                                         uidLiquidationReason.get(e.getKey())
				                                   ),
				                                   MultivalueListConverter.toMapOfListsOfDateRangedValues(identifiers.get(e.getKey()), NamedOrganisationId::getOrganisationIdCategory,
				                                                                                                NamedOrganisationId::getOrganisationId),
				                                   additionalName.get(e.getKey()) == null ? null : additionalName.get(e.getKey()),
				                                   kindOfLocations.get(e.getKey()),
				                                   legalForm.get(e.getKey()),
				                                   seats.get(e.getKey()),
				                                   businessPublication.get(e.getKey()) == null ? null : MultivalueListConverter
						                                   .toMapOfListsOfDateRangedValues(businessPublication.get(e.getKey()), fosc -> fosc.getSwissGazetteOfCommercePublication().getPublicationDate(), java.util.function.Function.identity()),
				                                   function.get(e.getKey()) == null ? null : MultivalueListConverter
						                                   .toMapOfListsOfDateRangedValues(function.get(e.getKey()), f -> f.getParty().getPerson().getName(), OrganisationFunction::new),
				                                   burTransferTo.get(e.getKey()) == null ? null : DateRangedConvertor.convert(burTransferTo.get(e.getKey()), BigInteger::longValue),
				                                   burTransferFrom.get(e.getKey()) == null ? null : DateRangedConvertor.convert(burTransferFrom.get(e.getKey()), BigInteger::longValue),
				                                   uidReplacedBy.get(e.getKey()) == null ? null : DateRangedConvertor.convert(uidReplacedBy.get(e.getKey()), BigInteger::longValue),
				                                   uidInReplacementOf.get(e.getKey()) == null ? null : DateRangedConvertor.convert(uidInReplacementOf.get(e.getKey()), BigInteger::longValue)
				     )
				)
				.collect(Collectors.toList());
	}
}
