package ch.vd.uniregctb.adapter.rcent.historizer.builders;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.ech.ech0097.v2.NamedOrganisationId;

import ch.vd.evd0022.v3.Address;
import ch.vd.evd0022.v3.BusinessPublication;
import ch.vd.evd0022.v3.Capital;
import ch.vd.evd0022.v3.CommercialRegisterDiaryEntry;
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
import ch.vd.uniregctb.adapter.rcent.model.BurRegistrationData;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationFunction;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;
import ch.vd.uniregctb.adapter.rcent.model.RCRegistrationData;

public class OrganisationLocationBuilder {

	private final Map<BigInteger, List<DateRangeHelper.Ranged<NamedOrganisationId>>> identifiers;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<String>>> names;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<String>>> additionalName;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<TypeOfLocation>>> kindOfLocations;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<LegalForm>>> legalForm;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Integer>>> seats;
	private final Map<BigInteger, List<BusinessPublication>> businessPublication;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Function>>> function;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> burTransferTo;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> burTransferFrom;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> uidReplacedBy;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> uidInReplacementOf;

	private final Map<BigInteger, List<DateRangeHelper.Ranged<RCRegistrationData>>> rcRegistrationData;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Capital>>> capital;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> rcLegalAddresses;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<String>>> purpose;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> byLawsDate;
	private final Map<BigInteger, List<CommercialRegisterDiaryEntry>> diaryEntries;

	private final Map<BigInteger, List<DateRangeHelper.Ranged<UidRegisterStatus>>> uidStatus;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<KindOfUidEntity>>> uidTypeOfOrganisation;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidEffectiveAddesses;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidPostalBoxAddresses;
	private final Map<BigInteger, List<DateRangeHelper.Ranged<UidDeregistrationReason>>> uidLiquidationReason;

	private final Map<BigInteger, List<DateRangeHelper.Ranged<BurRegistrationData>>> burRegistrationData;

	public OrganisationLocationBuilder(Map<BigInteger, List<DateRangeHelper.Ranged<NamedOrganisationId>>> identifiers,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<String>>> names,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<String>>> additionalName,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<TypeOfLocation>>> kindOfLocations,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<LegalForm>>> legalForm,
	                                   Map<BigInteger, List<BusinessPublication>> businessPublication,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Integer>>> seats,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Function>>> function,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> burTransferTo,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> burTransferFrom,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> uidReplacedBy,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BigInteger>>> UidInReplacementOf,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> rcLegalAddresses,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<RCRegistrationData>>> rcRegistrationData,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Capital>>> capital,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<String>>> purpose,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<RegDate>>> byLawsDate,
	                                   Map<BigInteger, List<CommercialRegisterDiaryEntry>> diaryEntries,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<UidRegisterStatus>>> uidStatus,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<KindOfUidEntity>>> uidTypeOfOrganisation,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidEffectiveAddesses,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<Address>>> uidPostalBoxAddresses,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<UidDeregistrationReason>>> uidLiquidationReason,
	                                   Map<BigInteger, List<DateRangeHelper.Ranged<BurRegistrationData>>> burRegistrationData) {
		this.legalForm = legalForm;
		this.burTransferTo = burTransferTo;
		this.burTransferFrom = burTransferFrom;
		this.uidReplacedBy = uidReplacedBy;
		this.uidInReplacementOf = UidInReplacementOf;
		this.rcRegistrationData = rcRegistrationData;
		this.purpose = purpose;
		this.byLawsDate = byLawsDate;
		this.diaryEntries = diaryEntries;
		this.uidTypeOfOrganisation = uidTypeOfOrganisation;
		this.uidPostalBoxAddresses = uidPostalBoxAddresses;
		this.identifiers = identifiers;
		this.names = names;
		this.additionalName = additionalName;
		this.kindOfLocations = kindOfLocations;
		this.seats = seats;
		this.function = function;
		this.capital = capital;
		this.rcLegalAddresses = rcLegalAddresses;
		this.uidStatus = uidStatus;
		this.uidEffectiveAddesses = uidEffectiveAddesses;
		this.uidLiquidationReason = uidLiquidationReason;
		this.businessPublication = businessPublication;
		this.burRegistrationData = burRegistrationData;
	}

	public List<OrganisationLocation> build() {
		return names.entrySet().stream() // On peut itérer sur names car le nom est obligatoire. Chaque orga est donc présente dans cette map.
				.map(e -> new OrganisationLocation(e.getKey().longValue(),
				                                   e.getValue(),
				                                   new OrganisationLocation.RCEntRCData(rcRegistrationData.get(e.getKey()),
				                                                                        capital.get(e.getKey()),
				                                                                        rcLegalAddresses.get(e.getKey()),
				                                                                        purpose.get(e.getKey()),
				                                                                        byLawsDate.get(e.getKey()),
				                                                                        diaryEntries.get(e.getKey())),
				                                   new OrganisationLocation.RCEntUIDData(uidEffectiveAddesses.get(e.getKey()),
				                                                                         uidStatus.get(e.getKey()),
				                                                                         uidTypeOfOrganisation.get(e.getKey()),
				                                                                         uidPostalBoxAddresses.get(e.getKey()),
				                                                                         uidLiquidationReason.get(e.getKey())
				                                   ),
				                                   new OrganisationLocation.RCEntBURData(burRegistrationData.get(e.getKey())),
				                                   identifiers.get(e.getKey()) == null
						                                   ? null
						                                   : MultivalueListConverter.toMapOfListsOfDateRangedValues(identifiers.get(e.getKey()), NamedOrganisationId::getOrganisationIdCategory, NamedOrganisationId::getOrganisationId),
				                                   additionalName.get(e.getKey()),
				                                   kindOfLocations.get(e.getKey()),
				                                   legalForm.get(e.getKey()),
				                                   seats.get(e.getKey()),
				                                   businessPublication.get(e.getKey()) == null
						                                   ? null
						                                   : businessPublication.get(e.getKey()),
				                                   function.get(e.getKey()) == null
						                                   ? null
						                                   : MultivalueListConverter.toMapOfListsOfDateRangedValues(function.get(e.getKey()), f -> f.getParty().getPerson().getName(), OrganisationFunction::new),
				                                   burTransferTo.get(e.getKey()) == null ? null : DateRangedConvertor.convert(burTransferTo.get(e.getKey()), BigInteger::longValue),
				                                   burTransferFrom.get(e.getKey()) == null ? null : DateRangedConvertor.convert(burTransferFrom.get(e.getKey()), BigInteger::longValue),
				                                   uidReplacedBy.get(e.getKey()) == null ? null : DateRangedConvertor.convert(uidReplacedBy.get(e.getKey()), BigInteger::longValue),
				                                   uidInReplacementOf.get(e.getKey()) == null ? null : DateRangedConvertor.convert(uidInReplacementOf.get(e.getKey()), BigInteger::longValue)

				     )
				)
				.collect(Collectors.toList());
	}
}
