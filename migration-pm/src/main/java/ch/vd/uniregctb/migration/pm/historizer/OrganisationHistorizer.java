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
		final ListDataCollector<Organisation, String> nomsEntrepriseCollector = new SingleValueDataCollector<>(Organisation::getOrganisationName,
		                                                                                                       Equalator.DEFAULT
		);
		final ListDataCollector<Organisation, String> nomsAdditionnelsEntrepriseCollector = new SingleValueDataCollector<>(Organisation::getOrganisationAdditionalName,
		                                                                                                                   Equalator.DEFAULT
		);
		final ListDataCollector<Organisation, LegalForm> formesLegalesCollector = new SingleValueDataCollector<>(Organisation::getLegalForm,
		                                                                                                         Equalator.DEFAULT
		);
		final ListDataCollector<Organisation, BigInteger> etablissementsCollector = new MultiValueDataCollector<>(new EtablissementsExtractor(),
		                                                                                                          Equalator.DEFAULT,
		                                                                                                          locationId -> locationId
		);
		final ListDataCollector<Organisation, BigInteger> transfereACollector = new MultiValueDataCollector<>(new TransferToExtractor(),
		                                                                                                      Equalator.DEFAULT,
		                                                                                                      locationId -> locationId
		);
		final ListDataCollector<Organisation, BigInteger> transfereDeCollector = new MultiValueDataCollector<>(new TransferFromExtractor(),
		                                                                                                       Equalator.DEFAULT,
		                                                                                                       locationId -> locationId
		);
		final ListDataCollector<Organisation, BigInteger> remplaceParCollector = new SingleValueDataCollector<>(o -> o.getReplacedBy().getCantonalId(),
		                                                                                                        Equalator.DEFAULT
		);
		final ListDataCollector<Organisation, BigInteger> enRemplacementDeCollector = new MultiValueDataCollector<>(new TransferFromExtractor(),
		                                                                                                            Equalator.DEFAULT,
		                                                                                                            locationId -> locationId
		);


		// Etablissements

		final IndexedDataCollector<Organisation, Identifier, BigInteger> identifiantsEtablissementsCollector = new MultiValueIndexedDataCollector<>(new OrganisationLocationIdentifiersExtractor(),
		                                                                                                                                            new IdentifierEqualator(),
		                                                                                                                                            keyed -> keyed.getValue().getIdentifierCategory()
		);
		final IndexedDataCollector<Organisation, String, BigInteger> nomsEtablissementsCollector = new SingleValueIndexedDataCollector<>(new OrganisationLocationNamesExtractor(),
		                                                                                                                                 Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, String, BigInteger> autresNomsEtablissementsCollector = new MultiValueIndexedDataCollector<>(new OrganisationLocationOtherNamesExtractor(),
		                                                                                                                                      Equalator.DEFAULT,
		                                                                                                                                      Keyed::getValue
		);
		final IndexedDataCollector<Organisation, KindOfLocation, BigInteger> genresEtablissementCollector = new SingleValueIndexedDataCollector<>(new KindOfLocationExtractor(),
		                                                                                                                                          Equalator.DEFAULT
		);
		final IndexedDataCollector<Organisation, Integer, BigInteger> siegesCollector = new SingleValueIndexedDataCollector<>(new SeatExtractor(),
		                                                                                                                      Equalator.DEFAULT
		);

		// RC

		final IndexedDataCollector<Organisation, Address, BigInteger> adressesRcEtablissementsCollector = new SingleValueIndexedDataCollector<>(new AdressesLegalesExtractor(),
		                                                                                                                                        ADDRESS_EQUALATOR
		);

		// IDE

		final IndexedDataCollector<Organisation, Address, BigInteger> adressesIdeEtablissementsCollector = new SingleValueIndexedDataCollector<>(new AdressesEffectivesIdeExtractor(),
		                                                                                                                                         ADDRESS_EQUALATOR
		);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesIdeCasePostaleEtablissementsCollector = new SingleValueIndexedDataCollector<>(new AdressesCasePostaleIdeExtractor(),
		                                                                                                                                                    ADDRESS_EQUALATOR
		);


		// on collecte les plages de dates dans les collectors
		Historizer.historize(organisationMap, Arrays.asList(nomsEntrepriseCollector,
		                                                    nomsAdditionnelsEntrepriseCollector,
		                                                    formesLegalesCollector,
		                                                    etablissementsCollector,
		                                                    transfereACollector,
		                                                    transfereDeCollector,
		                                                    remplaceParCollector,
		                                                    enRemplacementDeCollector,
		                                                    identifiantsEtablissementsCollector,
		                                                    nomsEtablissementsCollector,
		                                                    autresNomsEtablissementsCollector,
		                                                    genresEtablissementCollector,
		                                                    siegesCollector,
		                                                    adressesRcEtablissementsCollector,
		                                                    adressesIdeEtablissementsCollector,
		                                                    adressesIdeCasePostaleEtablissementsCollector

		));


		// Composition des

		// Etablissement

		OrganisationLocationBuilder locationBuilder = new OrganisationLocationBuilder(

//  	private final List<DateRanged<Identifier>> identifier;
				identifiantsEtablissementsCollector.getCollectedData(),
//		private final List<DateRanged<String>> name;
				nomsEtablissementsCollector.getCollectedData(),
//		private final List<DateRanged<String>> otherNames;
				autresNomsEtablissementsCollector.getCollectedData(),
//		private final List<DateRanged<KindOfLocation>> kindOfLocation;
				genresEtablissementCollector.getCollectedData(),
//		private final List<DateRanged<Integer>> seat;
				siegesCollector.getCollectedData(),
//		private final List<DateRanged<Function>> function;
//		private final List<DateRanged<Long>> replacedBy;
//		private final List<DateRanged<Long>> inReplacementOf;

//		private final List<DateRanged<CommercialRegisterStatus>> status;
//		private final List<DateRanged<String>> name;
//		private final List<DateRanged<CommercialRegisterEntryStatus>> entryStatus;
//		private final List<DateRanged<Capital>> capital;
//		private final List<DateRanged<Address>> legalAddress;
				adressesRcEtablissementsCollector.getCollectedData(),

//		private final List<DateRanged<UidRegisterStatus>> status;
//		private final List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation;
//		private final List<DateRanged<Address>> effectiveAddress;
				adressesIdeEtablissementsCollector.getCollectedData(),
//		private final List<DateRanged<Address>> postOfficeBoxAddress;
				adressesIdeCasePostaleEtablissementsCollector.getCollectedData()
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

//		private final List<DateRanged<String>> organisationName;
				nomsEntrepriseCollector.getCollectedData(),
//		private final List<DateRanged<String>> organisationAdditionalName;
				nomsAdditionnelsEntrepriseCollector.getCollectedData(),
//		private final List<DateRanged<LegalForm>> legalForm;
				formesLegalesCollector.getCollectedData(),
//
//		private final List<DateRanged<OrganisationLocation>> locations;
				etablissementsCollector.getCollectedData(),
//
//		private final List<DateRanged<Long>> transferTo;
				transfereACollector.getCollectedData(),
//		private final List<DateRanged<Long>> transferFrom;
				transfereDeCollector.getCollectedData(),
//		private final List<DateRanged<Long>> replacedBy;
				remplaceParCollector.getCollectedData(),
//		private final List<DateRanged<Long>> inPreplacementOf;
				enRemplacementDeCollector.getCollectedData(),
				locationBuilder.build()
		);

		return orgaBuilder.build();
	}
}