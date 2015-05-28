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
import ch.vd.uniregctb.migration.pm.historizer.collector.FlattenDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.FlattenIndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.IndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.LinearDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.SimpleDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.container.DualKey;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;
import ch.vd.uniregctb.migration.pm.historizer.equalator.AdresseEqualator;
import ch.vd.uniregctb.migration.pm.historizer.equalator.Equalator;
import ch.vd.uniregctb.migration.pm.historizer.equalator.IdentifierEqualator;
import ch.vd.uniregctb.migration.pm.historizer.extractor.AdressesCasePostaleIdeExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.AdressesEffectivesIdeExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.AdressesLegalesExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.EtablissementPrincipalExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.EtablissementsSecondairesExtractor;
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
		final LinearDataCollector<Organisation, String> nomsEntrepriseCollector = new SimpleDataCollector<>(Organisation::getOrganisationName,
		                                                                                                    Equalator.DEFAULT
		);
		final LinearDataCollector<Organisation, String> nomsAdditionnelsEntrepriseCollector = new SimpleDataCollector<>(Organisation::getOrganisationAdditionalName,
		                                                                                                               Equalator.DEFAULT
		);
		final LinearDataCollector<Organisation, LegalForm> formesLegalesCollector = new SimpleDataCollector<>(Organisation::getLegalForm,
		                                                                                                      Equalator.DEFAULT
		);
		final LinearDataCollector<Organisation, BigInteger> etablissementPrincipalCollector = new SimpleDataCollector<>(new EtablissementPrincipalExtractor(),
		                                                                                                                Equalator.DEFAULT
		);
		final LinearDataCollector<Organisation, BigInteger> etablissementsSecondairesCollector = new FlattenDataCollector<>(new EtablissementsSecondairesExtractor(),
		                                                                                                                    Equalator.DEFAULT,
		                                                                                                                    s -> s
		);
		final LinearDataCollector<Organisation, BigInteger> transfereACollector = new FlattenDataCollector<>(new TransferToExtractor(),
		                                                                                                     Equalator.DEFAULT,
		                                                                                                     s -> s
		);
		final LinearDataCollector<Organisation, BigInteger> transfereDeCollector = new FlattenDataCollector<>(new TransferFromExtractor(),
		                                                                                                      Equalator.DEFAULT,
		                                                                                                      s -> s
		);
		final LinearDataCollector<Organisation, BigInteger> remplaceParCollector = new SimpleDataCollector<>(o -> o.getReplacedBy().getCantonalId(),
		                                                                                                     Equalator.DEFAULT
		);
		final LinearDataCollector<Organisation, BigInteger> enRemplacementDeCollector = new FlattenDataCollector<>(new TransferFromExtractor(),
		                                                                                                           Equalator.DEFAULT,
		                                                                                                           s -> s
		);


		// Etablissements

		final IndexedDataCollector<Organisation, Identifier, DualKey<BigInteger, String>> identifiantsEtablissementsCollector = new FlattenIndexedDataCollector<>(new OrganisationLocationIdentifiersExtractor(),
		                                                                                                                                                          new IdentifierEqualator(),
		                                                                                                                                                          Keyed::getKey
		);
		final IndexedDataCollector<Organisation, String, BigInteger> nomsEtablissementsCollector = new FlattenIndexedDataCollector<>(new OrganisationLocationNamesExtractor(),
		                                                                                                                             Equalator.DEFAULT,
		                                                                                                                             Keyed::getKey
		);
		final IndexedDataCollector<Organisation, String, DualKey<BigInteger, String>> autresNomsEtablissementsCollector = new FlattenIndexedDataCollector<>(new OrganisationLocationOtherNamesExtractor(),
		                                                                                                                                                    Equalator.DEFAULT,
		                                                                                                                                                    Keyed::getKey
		);
		final IndexedDataCollector<Organisation, KindOfLocation, BigInteger> genresEtablissementCollector = new FlattenIndexedDataCollector<>(new KindOfLocationExtractor(),
		                                                                                                                                      Equalator.DEFAULT,
		                                                                                                                                      Keyed::getKey
		);
		final IndexedDataCollector<Organisation, Integer, BigInteger> siegesCollector = new FlattenIndexedDataCollector<>(new SeatExtractor(),
		                                                                                                                  Equalator.DEFAULT,
		                                                                                                                  Keyed::getKey
		);

		// RC

		final IndexedDataCollector<Organisation, Address, BigInteger> adressesRcEtablissementsCollector = new FlattenIndexedDataCollector<>(new AdressesLegalesExtractor(),
		                                                                                                                                    ADDRESS_EQUALATOR,
		                                                                                                                                    Keyed::getKey
		);

		// IDE

		final IndexedDataCollector<Organisation, Address, BigInteger> adressesIdeEtablissementsCollector = new FlattenIndexedDataCollector<>(new AdressesEffectivesIdeExtractor(),
		                                                                                                                                     ADDRESS_EQUALATOR,
		                                                                                                                                     Keyed::getKey
		);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesIdeCasePostaleEtablissementsCollector = new FlattenIndexedDataCollector<>(new AdressesCasePostaleIdeExtractor(),
		                                                                                                                                                ADDRESS_EQUALATOR,
		                                                                                                                                                Keyed::getKey
		);


		// on collecte les plages de dates dans les collectors
		Historizer.historize(organisationMap, Arrays.asList(nomsEntrepriseCollector,
		                                                    nomsAdditionnelsEntrepriseCollector,
		                                                    formesLegalesCollector,
		                                                    etablissementPrincipalCollector,
		                                                    etablissementsSecondairesCollector,
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

		// récupération des plages de valeurs

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
			etablissementPrincipalCollector.getCollectedData(),
			etablissementsSecondairesCollector.getCollectedData(),
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
