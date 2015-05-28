package ch.vd.uniregctb.migration.pm.historizer;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.evd0022.v1.OrganisationSnapshot;
import ch.vd.evd0022.v1.SwissMunicipality;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.historizer.collector.FlattenDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.FlattenIndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.IndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.LinearDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.SimpleDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;
import ch.vd.uniregctb.migration.pm.historizer.equalator.AdresseEqualator;
import ch.vd.uniregctb.migration.pm.historizer.equalator.Equalator;
import ch.vd.uniregctb.migration.pm.historizer.equalator.OrganisationLocationEqualator;
import ch.vd.uniregctb.migration.pm.historizer.equalator.SeatEqualator;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.AdressesCasePostaleIdeExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.AdressesEffectivesIdeExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.AdressesLegalesExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.EtablissementPrincipalExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.EtablissementsSecondairesExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.KindOfLocationExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.OrganisationLocationExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.SeatExtractor;

public class OrganisationHistorizer {

	private static final Equalator<Address> ADDRESS_EQUALATOR = new AdresseEqualator();

	public Object mapOrganisation(List<OrganisationSnapshot> snapshots) {

		// d'abord, on transforme cette liste en map de snapshots indexés par date
		final Map<RegDate, Organisation> organisationMap = snapshots.stream().collect(Collectors.toMap(OrganisationSnapshot::getBeginValidityDate,
		                                                                                               OrganisationSnapshot::getOrganisation)
		);

		// on enregistre les data collectors au niveau de l'organisation faîtière (= l'entreprise)
		final LinearDataCollector<Organisation, LegalForm> legalFormCollector = new SimpleDataCollector<>(Organisation::getLegalForm,
		                                                                                                  Equalator.DEFAULT
		);
		final LinearDataCollector<Organisation, BigInteger> etablissementPrincipalCollector = new SimpleDataCollector<>(new EtablissementPrincipalExtractor(),
		                                                                                                                Equalator.DEFAULT
		);
		final LinearDataCollector<Organisation, BigInteger> etablissementsSecondairesCollector = new FlattenDataCollector<>(new EtablissementsSecondairesExtractor(),
		                                                                                                                    Equalator.DEFAULT,
		                                                                                                                    s -> s
		);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesRcEtablissementsCollector = new FlattenIndexedDataCollector<>(new AdressesLegalesExtractor(),
		                                                                                                                                    ADDRESS_EQUALATOR,
		                                                                                                                                    Keyed::getKey
		);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesIdeEtablissementsCollector = new FlattenIndexedDataCollector<>(new AdressesEffectivesIdeExtractor(),
		                                                                                                                                     ADDRESS_EQUALATOR,
		                                                                                                                                     Keyed::getKey
		);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesIdeCasePostaleEtablissementsCollector = new FlattenIndexedDataCollector<>(new AdressesCasePostaleIdeExtractor(),
		                                                                                                                                                ADDRESS_EQUALATOR,
		                                                                                                                                                Keyed::getKey
		);
		final IndexedDataCollector<Organisation, SwissMunicipality, BigInteger> seatCollector = new FlattenIndexedDataCollector<>(new SeatExtractor(),
		                                                                                                                          new SeatEqualator(),
		                                                                                                                          Keyed::getKey
		);
		final IndexedDataCollector<Organisation, KindOfLocation, BigInteger> kindOfLocationCollector = new FlattenIndexedDataCollector<>(new KindOfLocationExtractor(),
		                                                                                                                                    Equalator.DEFAULT,
		                                                                                                                                    Keyed::getKey
		);

		// on collecte les plages de dates dans les collectors
		Historizer.historize(organisationMap, Arrays.asList(legalFormCollector,
		                                                    etablissementPrincipalCollector,
		                                                    etablissementsSecondairesCollector,
		                                                    adressesRcEtablissementsCollector,
		                                                    adressesIdeEtablissementsCollector,
		                                                    adressesIdeCasePostaleEtablissementsCollector,
		                                                    seatCollector,
		                                                    kindOfLocationCollector
		));

		// récupération des plages de valeurs
		final List<DateRanged<LegalForm>> formesJuridiques = legalFormCollector.getCollectedData();
		final List<DateRanged<BigInteger>> prnEtablissements = etablissementPrincipalCollector.getCollectedData();
		final List<DateRanged<BigInteger>> secEtablissements = etablissementsSecondairesCollector.getCollectedData();
		final Map<BigInteger, List<DateRanged<Address>>> adressesRc = adressesRcEtablissementsCollector.getCollectedData();
		final Map<BigInteger, List<DateRanged<Address>>> adressesIdeEffectives = adressesIdeEtablissementsCollector.getCollectedData();
		final Map<BigInteger, List<DateRanged<Address>>> adressesIdeCasePostale = adressesIdeCasePostaleEtablissementsCollector.getCollectedData();
		final Map<BigInteger, List<DateRanged<SwissMunicipality>>> seatMunicipalities = seatCollector.getCollectedData();
		final Map<BigInteger, List<DateRanged<KindOfLocation>>> genreDEtablissements = kindOfLocationCollector.getCollectedData();

		// et finalement on construit un objet à renvoyer à l'appelant

		List<ch.vd.uniregctb.migration.pm.rcent.model.OrganisationLocation> etablissements = EtablissementsBuilder.build();

		//ch.vd.uniregctb.migration.pm.rcent.model.OrganisationLocation.RCEntRCData rc = new ch.vd.uniregctb.migration.pm.rcent.model.OrganisationLocation.RCEntRCData()
		//ch.vd.uniregctb.migration.pm.rcent.model.Organisation organisation = new ch.vd.uniregctb.migration.pm.rcent.model.Organisation();

		return null;
	}
}
