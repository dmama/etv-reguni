package ch.vd.uniregctb.adapter.rcent.historizer;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.CommercialRegisterStatus;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationSnapshot;
import ch.vd.evd0022.v1.UidRegisterStatus;
import ch.vd.evd0022.v1.UidRegisterTypeOfOrganisation;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adapter.rcent.historizer.builders.OrganisationBuilder;
import ch.vd.uniregctb.adapter.rcent.historizer.builders.OrganisationLocationBuilder;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.IndexedDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.ListDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.MultiValueDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.MultiValueIndexedDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.SingleValueDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.collector.SingleValueIndexedDataCollector;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.AdresseEqualator;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.Equalator;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.IdentifierEqualator;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.AdressesCasePostaleIdeExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.AdressesEffectivesIdeExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.AdressesLegalesExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.KindOfLocationExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationIdentifiersExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationNamesExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationOtherNamesExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.LocationsExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.OrganisationInReplacementOfExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.OrganisationReplacedByExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.OrganisationTransferFromExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.OrganisationTransferToExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.RcStatusExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.SeatExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.UidStatusExtractor;
import ch.vd.uniregctb.adapter.rcent.historizer.extractor.UidTypeOfOrganisationExtractor;

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
		final ListDataCollector<Organisation, Identifier> organisationIdentifiersCollector = new MultiValueDataCollector<>(o -> o.getOrganisationIdentifier().stream(),
		                                                                                                     Equalator.DEFAULT,
		                                                                                                     locationId -> locationId
		);
		final ListDataCollector<Organisation, String> organisationNameCollector = new SingleValueDataCollector<>(Organisation::getOrganisationName,
		                                                                                                       Equalator.DEFAULT
		);
		final ListDataCollector<Organisation, String> organisationAdditionalNameCollector = new SingleValueDataCollector<>(Organisation::getOrganisationAdditionalName,
		                                                                                                                   Equalator.DEFAULT
		);
		final ListDataCollector<Organisation, LegalForm> legalFormsCollector = new SingleValueDataCollector<>(Organisation::getLegalForm,
		                                                                                                         Equalator.DEFAULT
		);
		final ListDataCollector<Organisation, BigInteger> locationsCollector = new MultiValueDataCollector<>(new LocationsExtractor(),
		                                                                                                          Equalator.DEFAULT,
		                                                                                                          locationId -> locationId
		);
		final ListDataCollector<Organisation, BigInteger> transferToCollector = new MultiValueDataCollector<>(new OrganisationTransferToExtractor(),
		                                                                                                      Equalator.DEFAULT,
		                                                                                                      locationId -> locationId
		);
		final ListDataCollector<Organisation, BigInteger> transferFromCollector = new MultiValueDataCollector<>(new OrganisationTransferFromExtractor(),
		                                                                                                       Equalator.DEFAULT,
		                                                                                                       locationId -> locationId
		);
		final ListDataCollector<Organisation, BigInteger> replacedByCollector = new SingleValueDataCollector<>(new OrganisationReplacedByExtractor(),
		                                                                                                       Equalator.DEFAULT
		);
		final ListDataCollector<Organisation, BigInteger> inReplacementOfCollector = new MultiValueDataCollector<>(new OrganisationInReplacementOfExtractor(),
		                                                                                                            Equalator.DEFAULT,
		                                                                                                            locationId -> locationId
	);


		// Etablissements

		final IndexedDataCollector<Organisation, Identifier, BigInteger> locationIdentifiersCollector = new MultiValueIndexedDataCollector<>(new LocationIdentifiersExtractor(),
		                                                                                                                                     new IdentifierEqualator(),
		                                                                                                                                     keyed -> keyed.getValue().getIdentifierCategory()
		);
		final IndexedDataCollector<Organisation, String, BigInteger> locationNamesCollector = new SingleValueIndexedDataCollector<>(new LocationNamesExtractor(),
		                                                                                                                            Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, String, BigInteger> locationOtherNamesCollector = new MultiValueIndexedDataCollector<>(new LocationOtherNamesExtractor(),
		                                                                                                                                Equalator.DEFAULT,
		                                                                                                                                Keyed::getValue
		);
		final IndexedDataCollector<Organisation, KindOfLocation, BigInteger> kindsOfLocationCollector = new SingleValueIndexedDataCollector<>(new KindOfLocationExtractor(),
		                                                                                                                                      Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, Integer, BigInteger> seatsCollector = new SingleValueIndexedDataCollector<>(new SeatExtractor(),
		                                                                                                                      Equalator.DEFAULT
		);

		// RC

		final IndexedDataCollector<Organisation, CommercialRegisterStatus, BigInteger> locationRcstatusCollector = new SingleValueIndexedDataCollector<>(new RcStatusExtractor(),
		                                                                                                                                                 Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, Address, BigInteger> locationRcLegalAddressCollector = new SingleValueIndexedDataCollector<>(new AdressesLegalesExtractor(),
		                                                                                                                                      ADDRESS_EQUALATOR
		);

		// IDE

		final IndexedDataCollector<Organisation, UidRegisterStatus, BigInteger> locationUidStatus = new SingleValueIndexedDataCollector<>(new UidStatusExtractor(),
		                                                                                                                                  Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, UidRegisterTypeOfOrganisation, BigInteger> locationUidTypeOfOrganisation = new SingleValueIndexedDataCollector<>(new UidTypeOfOrganisationExtractor(),
		                                                                                                                                                          Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, Address, BigInteger> locationUidEffectiveAddressCollector = new SingleValueIndexedDataCollector<>(new AdressesEffectivesIdeExtractor(),
		                                                                                                                                           ADDRESS_EQUALATOR
		);
		final IndexedDataCollector<Organisation, Address, BigInteger> locationPostalBoxUidAddressCollector = new SingleValueIndexedDataCollector<>(new AdressesCasePostaleIdeExtractor(),
		                                                                                                                                           ADDRESS_EQUALATOR
		);


		// on collecte les plages de dates dans les collectors
		Historizer.historize(organisationMap, Arrays.asList(organisationIdentifiersCollector,
		                                                    organisationNameCollector,
		                                                    organisationAdditionalNameCollector,
		                                                    legalFormsCollector,
		                                                    locationsCollector,
		                                                    transferToCollector,
		                                                    transferFromCollector,
		                                                    replacedByCollector,
		                                                    inReplacementOfCollector,
		                                                    locationIdentifiersCollector,
		                                                    locationNamesCollector,
		                                                    locationOtherNamesCollector,
		                                                    kindsOfLocationCollector,
		                                                    seatsCollector,
		                                                    locationRcstatusCollector,
		                                                    locationRcLegalAddressCollector,
		                                                    locationUidStatus,
		                                                    locationUidTypeOfOrganisation,
		                                                    locationUidEffectiveAddressCollector,
		                                                    locationPostalBoxUidAddressCollector

		));


		// Composition des

		// Etablissement

		OrganisationLocationBuilder locationBuilder = new OrganisationLocationBuilder(
				locationIdentifiersCollector.getCollectedData(),
				locationNamesCollector.getCollectedData(),
				locationOtherNamesCollector.getCollectedData(),
				kindsOfLocationCollector.getCollectedData(),
				seatsCollector.getCollectedData(),
//		private final List<DateRanged<Function>> function;
//		private final List<DateRanged<Long>> replacedBy;
//		private final List<DateRanged<Long>> inReplacementOf;

				locationRcstatusCollector.getCollectedData(),
//		private final List<DateRanged<String>> name;
//		private final List<DateRanged<CommercialRegisterEntryStatus>> entryStatus;
//		private final List<DateRanged<Capital>> capital;
				locationRcLegalAddressCollector.getCollectedData(),

				locationUidStatus.getCollectedData(),
				locationUidTypeOfOrganisation.getCollectedData(),
				locationUidEffectiveAddressCollector.getCollectedData(),
				locationPostalBoxUidAddressCollector.getCollectedData()
//		private final List<DateRanged<UidRegisterPublicStatus>> publicStatus;
//		private final List<DateRanged<UidRegisterLiquidationReason>> liquidationReason;

		);

		// Entreprise / Organisation
		OrganisationBuilder orgaBuilder = new OrganisationBuilder(
				organisationMap.entrySet().stream().findFirst().get().getValue().getCantonalId(),
				organisationIdentifiersCollector.getCollectedData(),
				organisationNameCollector.getCollectedData(),
				organisationAdditionalNameCollector.getCollectedData(),
				legalFormsCollector.getCollectedData(),
				locationsCollector.getCollectedData(),
				transferToCollector.getCollectedData(),
				transferFromCollector.getCollectedData(),
				replacedByCollector.getCollectedData(),
				inReplacementOfCollector.getCollectedData(),
				locationBuilder.build()
		);

		return orgaBuilder.build();
	}
}