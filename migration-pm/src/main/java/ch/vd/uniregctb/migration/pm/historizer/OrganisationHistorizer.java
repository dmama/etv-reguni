package ch.vd.uniregctb.migration.pm.historizer;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationSnapshot;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.historizer.collector.IndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.ListDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.MultiValueDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.MultiValueIndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.SingleValueDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.SingleValueIndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;
import ch.vd.uniregctb.migration.pm.historizer.equalator.AdresseEqualator;
import ch.vd.uniregctb.migration.pm.historizer.equalator.Equalator;
import ch.vd.uniregctb.migration.pm.historizer.equalator.IdentifierEqualator;
import ch.vd.uniregctb.migration.pm.historizer.extractor.AdressesCasePostaleIdeExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.AdressesEffectivesIdeExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.AdressesLegalesExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.EtablissementsExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.KindOfLocationExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.OrganisationLocationIdentifiersExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.OrganisationLocationNamesExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.OrganisationLocationOtherNamesExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.SeatExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.TransferFromExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.TransferToExtractor;

public class OrganisationHistorizer {

	private static final Equalator<Address> ADDRESS_EQUALATOR = new AdresseEqualator();

	public ch.vd.uniregctb.migration.pm.rcent.model.Organisation mapOrganisation(List<OrganisationSnapshot> snapshots) {

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
		final ListDataCollector<Organisation, BigInteger> locationsCollector = new MultiValueDataCollector<>(new EtablissementsExtractor(),
		                                                                                                          Equalator.DEFAULT,
		                                                                                                          locationId -> locationId
		);
		final ListDataCollector<Organisation, BigInteger> transferToCollector = new MultiValueDataCollector<>(new TransferToExtractor(),
		                                                                                                      Equalator.DEFAULT,
		                                                                                                      locationId -> locationId
		);
		final ListDataCollector<Organisation, BigInteger> transferFromCollector = new MultiValueDataCollector<>(new TransferFromExtractor(),
		                                                                                                       Equalator.DEFAULT,
		                                                                                                       locationId -> locationId
		);
		final ListDataCollector<Organisation, BigInteger> replacedByCollector = new SingleValueDataCollector<>(o -> o.getReplacedBy().getCantonalId(),
		                                                                                                        Equalator.DEFAULT
		);
		final ListDataCollector<Organisation, BigInteger> inReplacementOfCollector = new MultiValueDataCollector<>(new TransferFromExtractor(),
		                                                                                                            Equalator.DEFAULT,
		                                                                                                            locationId -> locationId
		);


		// Etablissements

		final IndexedDataCollector<Organisation, Identifier, BigInteger> locationIdentifiersCollector = new MultiValueIndexedDataCollector<>(new OrganisationLocationIdentifiersExtractor(),
		                                                                                                                                            new IdentifierEqualator(),
		                                                                                                                                            keyed -> keyed.getValue().getIdentifierCategory()
		);
		final IndexedDataCollector<Organisation, String, BigInteger> locationNamesCollector = new SingleValueIndexedDataCollector<>(new OrganisationLocationNamesExtractor(),
		                                                                                                                                 Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, String, BigInteger> locationOtherNamesCollector = new MultiValueIndexedDataCollector<>(new OrganisationLocationOtherNamesExtractor(),
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

		final IndexedDataCollector<Organisation, Address, BigInteger> locationRcAddressCollector = new SingleValueIndexedDataCollector<>(new AdressesLegalesExtractor(),
		                                                                                                                                        ADDRESS_EQUALATOR
		);

		// IDE

		final IndexedDataCollector<Organisation, Address, BigInteger> locationUidAddressCollector = new SingleValueIndexedDataCollector<>(new AdressesEffectivesIdeExtractor(),
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
		                                                    locationRcAddressCollector,
		                                                    locationUidAddressCollector,
		                                                    locationPostalBoxUidAddressCollector

		));


		// Composition des

		// Etablissement

		OrganisationLocationBuilder locationBuilder = new OrganisationLocationBuilder(

//  	private final List<DateRanged<Identifier>> identifier;
				locationIdentifiersCollector.getCollectedData(),
//		private final List<DateRanged<String>> name;
				locationNamesCollector.getCollectedData(),
//		private final List<DateRanged<String>> otherNames;
				locationOtherNamesCollector.getCollectedData(),
//		private final List<DateRanged<KindOfLocation>> kindOfLocation;
				kindsOfLocationCollector.getCollectedData(),
//		private final List<DateRanged<Integer>> seat;
				seatsCollector.getCollectedData(),
//		private final List<DateRanged<Function>> function;
//		private final List<DateRanged<Long>> replacedBy;
//		private final List<DateRanged<Long>> inReplacementOf;

//		private final List<DateRanged<CommercialRegisterStatus>> status;
//		private final List<DateRanged<String>> name;
//		private final List<DateRanged<CommercialRegisterEntryStatus>> entryStatus;
//		private final List<DateRanged<Capital>> capital;
//		private final List<DateRanged<Address>> legalAddress;
				locationRcAddressCollector.getCollectedData(),

//		private final List<DateRanged<UidRegisterStatus>> status;
//		private final List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation;
//		private final List<DateRanged<Address>> effectiveAddress;
				locationUidAddressCollector.getCollectedData(),
//		private final List<DateRanged<Address>> postOfficeBoxAddress;
				locationPostalBoxUidAddressCollector.getCollectedData()
//		private final List<DateRanged<UidRegisterPublicStatus>> publicStatus;
//		private final List<DateRanged<UidRegisterLiquidationReason>> liquidationReason;

//		private final List<DateRanged<UidRegisterStatus>> status;
//		private final List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation;
//		private final List<DateRanged<Address>> effectiveAddress;
//		private final List<DateRanged<Address>> postOfficeBoxAddress;
//		private final List<DateRanged<UidRegisterPublicStatus>> publicStatus;
//		private final List<DateRanged<UidRegisterLiquidationReason>> liquidationReason;

		);

		// Entreprise / Organisation
		OrganisationBuilder orgaBuilder = new OrganisationBuilder(
//		private final List<Identifier> organisationIdentifiers;

				organisationMap.get(0).getCantonalId(),
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