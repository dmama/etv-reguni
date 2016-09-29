package ch.vd.uniregctb.adapter.rcent.historizer;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ech.ech0097.v2.NamedOrganisationId;

import ch.vd.evd0022.v3.Address;
import ch.vd.evd0022.v3.BusinessPublication;
import ch.vd.evd0022.v3.Capital;
import ch.vd.evd0022.v3.CommercialRegisterDiaryEntry;
import ch.vd.evd0022.v3.KindOfUidEntity;
import ch.vd.evd0022.v3.LegalForm;
import ch.vd.evd0022.v3.Organisation;
import ch.vd.evd0022.v3.OrganisationLocation;
import ch.vd.evd0022.v3.OrganisationSnapshot;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0022.v3.UidDeregistrationReason;
import ch.vd.evd0022.v3.UidRegisterStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adapter.rcent.historizer.builders.OrganisationBuilder;
import ch.vd.uniregctb.adapter.rcent.historizer.builders.OrganisationLocationBuilder;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.IndexedDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.ListDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.MultiValueDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.MultiValueIndexedDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.SingleValueIndexedDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.AdresseEqualator;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.BURRegistrationDataEqualator;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.BusinessPublicationEqualator;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.CapitalEqualator;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.Equalator;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.NamedOrganisationIdEqualator;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.RCRegistrationDataEqualator;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.AdressesCasePostaleIdeExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.AdressesEffectivesIdeExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.AdressesLegalesExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.BURRegistrationDataExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationAdditionalNameExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationBurTransferFromExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationBurTransferToExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationBusinessPublicationExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationIdentifiersExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationLegalFormExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationMunicipalityExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationNamesExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationRcByLawsDateExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationRcPurposeExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationTypeExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationUidInReplacementOfExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationUidReplacedByExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationsExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.RCCapitalExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.RCRegistrationDataExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.UidDeregistrationReasonExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.UidKindOfUidEntityExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.UidRegistrationStatusExtractor;
import ch.vd.uniregctb.adapter.rcent.model.BurRegistrationData;
import ch.vd.uniregctb.adapter.rcent.model.RCRegistrationData;

public class OrganisationHistorizer {

	private static final Equalator<Address> ADDRESS_EQUALATOR = new AdresseEqualator();

	/**
	 * Convert a serie of snapshot into an historized representation.
	 * @param snapshots At least one snapshot of the organisation to historize.
	 * @return The historized data
	 */
	public ch.vd.uniregctb.adapter.rcent.model.Organisation mapOrganisation(List<OrganisationSnapshot> snapshots) {

		// Entreprise

		// d'abord, on transforme cette liste en map de snapshots indexés par date
		final Map<RegDate, Organisation> organisationMap = snapshots.stream().collect(Collectors.toMap(OrganisationSnapshot::getBeginValidityDate,
		                                                                                               OrganisationSnapshot::getOrganisation)
		);

		// on enregistre les data collectors au niveau de l'organisation faîtière (= l'entreprise)
		final ListDataCollector<Organisation, NamedOrganisationId> organisationIdentifiersCollector = new MultiValueDataCollector<>(o -> o.getIdentifier().stream(),
		                                                                                                                            new NamedOrganisationIdEqualator(),
		                                                                                                                            Function.identity()
		);
		final ListDataCollector<Organisation, BigInteger> locationsCollector = new MultiValueDataCollector<>(new LocationsExtractor(),
		                                                                                                     Equalator.DEFAULT,
		                                                                                                     Function.identity()
		);


		// Etablissements

		final IndexedDataCollector<Organisation, NamedOrganisationId, BigInteger> locationIdentifiersCollector = new MultiValueIndexedDataCollector<>(new LocationIdentifiersExtractor(),
		                                                                                                                                              new NamedOrganisationIdEqualator(),
		                                                                                                                                              keyed -> keyed.getValue().getOrganisationIdCategory()
		);
		final IndexedDataCollector<Organisation, String, BigInteger> locationNamesCollector = new SingleValueIndexedDataCollector<>(new LocationNamesExtractor(),
		                                                                                                                            Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, String, BigInteger> locationAdditionalNameCollector = new SingleValueIndexedDataCollector<>(new LocationAdditionalNameExtractor(),
		                                                                                                                                     Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, TypeOfLocation, BigInteger> locationKindsOfLocationCollector = new SingleValueIndexedDataCollector<>(new LocationTypeExtractor(),
		                                                                                                                                              Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, LegalForm, BigInteger> locationLegalFormsCollector = new SingleValueIndexedDataCollector<>(new LocationLegalFormExtractor(),
		                                                                                                                                    Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, Integer, BigInteger> locationMunicipalityCollector = new SingleValueIndexedDataCollector<>(new LocationMunicipalityExtractor(),
		                                                                                                                                    Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, BusinessPublication, BigInteger> locationBusinessPublicationCollector = new MultiValueIndexedDataCollector<>(new LocationBusinessPublicationExtractor(),
		                                                                                                                                                      new BusinessPublicationEqualator(),
		                                                                                                                                                      Function.identity()
		);
		final IndexedDataCollector<Organisation, BigInteger, BigInteger> locationUidReplacedByCollector = new SingleValueIndexedDataCollector<>(new LocationUidReplacedByExtractor(),
		                                                                                                                                        Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, BigInteger, BigInteger> locationUidInReplacementOfCollector = new SingleValueIndexedDataCollector<>(new LocationUidInReplacementOfExtractor(),
		                                                                                                                                             Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, BigInteger, BigInteger> locationBurTransferToCollector = new SingleValueIndexedDataCollector<>(new LocationBurTransferToExtractor(),
		                                                                                                                                        Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, BigInteger, BigInteger> locationBurTransferFromCollector = new SingleValueIndexedDataCollector<>(new LocationBurTransferFromExtractor(),
		                                                                                                                                         Equalator.DEFAULT
		);

		// RC

		final IndexedDataCollector<Organisation, Address, BigInteger> locationRcLegalAddressCollector = new SingleValueIndexedDataCollector<>(new AdressesLegalesExtractor(),
		                                                                                                                                      ADDRESS_EQUALATOR
		);
		final IndexedDataCollector<Organisation, Capital, BigInteger> locationRcCapitalCollector = new SingleValueIndexedDataCollector<>(new RCCapitalExtractor(),
		                                                                                                                                 new CapitalEqualator()
		);
		final IndexedDataCollector<Organisation, RCRegistrationData, BigInteger> locationRcRegistrationDataCollector = new SingleValueIndexedDataCollector<>(new RCRegistrationDataExtractor(),
		                                                                                                                                                     new RCRegistrationDataEqualator()
		);
		final IndexedDataCollector<Organisation, String, BigInteger> locationRcPurposeCollector = new SingleValueIndexedDataCollector<>(new LocationRcPurposeExtractor(),
		                                                                                                                                Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, RegDate, BigInteger> locationRcByLawsDateCollector = new SingleValueIndexedDataCollector<>(new LocationRcByLawsDateExtractor(),
		                                                                                                                                    Equalator.DEFAULT
		);

		// IDE

		final IndexedDataCollector<Organisation, UidRegisterStatus, BigInteger> locationUidStatus = new SingleValueIndexedDataCollector<>(new UidRegistrationStatusExtractor(),
		                                                                                                                                  Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, KindOfUidEntity, BigInteger> locationUidTypeOfOrganisation = new SingleValueIndexedDataCollector<>(new UidKindOfUidEntityExtractor(),
		                                                                                                                                            Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, Address, BigInteger> locationUidEffectiveAddressCollector = new SingleValueIndexedDataCollector<>(new AdressesEffectivesIdeExtractor(),
		                                                                                                                                           ADDRESS_EQUALATOR
		);
		final IndexedDataCollector<Organisation, Address, BigInteger> locationPostalBoxUidAddressCollector = new SingleValueIndexedDataCollector<>(new AdressesCasePostaleIdeExtractor(),
		                                                                                                                                           ADDRESS_EQUALATOR
		);
		final IndexedDataCollector<Organisation, UidDeregistrationReason, BigInteger> locationUidRegisterLiquidationReason = new SingleValueIndexedDataCollector<>(new UidDeregistrationReasonExtractor(),
		                                                                                                                                                           Equalator.DEFAULT
		);

		// REE
		final IndexedDataCollector<Organisation, BurRegistrationData, BigInteger> locationBurRegistrationDataCollector = new SingleValueIndexedDataCollector<>(new BURRegistrationDataExtractor(),
		                                                                                                                                                       new BURRegistrationDataEqualator()
		);


		// on collecte les plages de dates dans les collectors
		Historizer.historize(organisationMap, Arrays.asList(organisationIdentifiersCollector,
		                                                    locationsCollector,

		                                                    locationIdentifiersCollector,
		                                                    locationNamesCollector,
		                                                    locationAdditionalNameCollector,
		                                                    locationKindsOfLocationCollector,
		                                                    locationLegalFormsCollector,
		                                                    locationMunicipalityCollector,
		                                                    locationBusinessPublicationCollector,
		                                                    locationUidInReplacementOfCollector,
		                                                    locationUidReplacedByCollector,
		                                                    locationBurTransferToCollector,
		                                                    locationBurTransferFromCollector,

		                                                    locationRcRegistrationDataCollector,
		                                                    locationRcLegalAddressCollector,
		                                                    locationRcByLawsDateCollector,
		                                                    locationRcPurposeCollector,
		                                                    locationRcCapitalCollector,

		                                                    locationUidStatus,
		                                                    locationUidTypeOfOrganisation,
		                                                    locationUidEffectiveAddressCollector,
		                                                    locationPostalBoxUidAddressCollector,
		                                                    locationUidRegisterLiquidationReason,

		                                                    locationBurRegistrationDataCollector

		));

		// Données non historisées
		//Map<BigInteger, List<CommercialRegisterDiaryEntry>> diaryEntries = new HashMap<>();
		final Map.Entry<RegDate, Organisation> lastSnapshot = organisationMap.entrySet().stream()
				.max(Comparator.comparing(Map.Entry::getKey))
				.orElse(null);
		final Map<BigInteger, List<CommercialRegisterDiaryEntry>> diaryEntries = lastSnapshot.getValue().getOrganisationLocation().stream()
				.filter(l -> l.getCommercialRegisterData() != null)
				.filter(l -> l.getCommercialRegisterData().getDiaryEntry() != null && ! l.getCommercialRegisterData().getDiaryEntry().isEmpty())
				.collect(Collectors.toMap(OrganisationLocation::getCantonalId, l -> l.getCommercialRegisterData().getDiaryEntry()));

		// Composition des
		// Etablissement

		final OrganisationLocationBuilder locationBuilder = new OrganisationLocationBuilder(
				locationIdentifiersCollector.getCollectedData(),
				locationNamesCollector.getCollectedData(),
				locationAdditionalNameCollector.getCollectedData(),
				locationKindsOfLocationCollector.getCollectedData(),
				locationLegalFormsCollector.getCollectedData(),
				locationBusinessPublicationCollector.getCollectedData(),
				locationMunicipalityCollector.getCollectedData(),
				Collections.emptyMap(),
				locationBurTransferToCollector.getCollectedData(),
				locationBurTransferFromCollector.getCollectedData(),
				locationUidReplacedByCollector.getCollectedData(),
				locationUidInReplacementOfCollector.getCollectedData(),

				locationRcLegalAddressCollector.getCollectedData(),
				locationRcRegistrationDataCollector.getCollectedData(),
				locationRcCapitalCollector.getCollectedData(),
				locationRcPurposeCollector.getCollectedData(),
				locationRcByLawsDateCollector.getCollectedData(),

				diaryEntries,
				locationUidStatus.getCollectedData(),
				locationUidTypeOfOrganisation.getCollectedData(),
				locationUidEffectiveAddressCollector.getCollectedData(),
				locationPostalBoxUidAddressCollector.getCollectedData(),
				locationUidRegisterLiquidationReason.getCollectedData(),

				locationBurRegistrationDataCollector.getCollectedData()
		);

		// Entreprise / Organisation
		final OrganisationBuilder orgaBuilder = new OrganisationBuilder(
				organisationMap.entrySet().stream().findFirst().get().getValue().getCantonalId(),
				organisationIdentifiersCollector.getCollectedData(),
				locationsCollector.getCollectedData(),

				locationBuilder.build()
		);

		return orgaBuilder.build();
	}
}