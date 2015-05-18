package ch.vd.uniregctb.migration.pm.historizer;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationSnapshot;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.migration.pm.historizer.collector.FlattenDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.FlattenIndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.IndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.LinearDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.SimpleDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;
import ch.vd.uniregctb.migration.pm.historizer.equalator.AdresseEqualator;
import ch.vd.uniregctb.migration.pm.historizer.equalator.Equalator;
import ch.vd.uniregctb.migration.pm.historizer.equalator.EtablissementEqualator;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.AdressesCasePostaleIdeExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.AdressesEffectivesIdeExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.AdressesLegalesExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.Etablissement;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.EtablissementPrincipal;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.EtablissementPrincipalExtractor;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.EtablissementSecondaire;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.EtablissementsSecondairesExtractor;

public class OrganisationHistorizer {

	private static final Equalator<Etablissement> ETABLISSEMENT_EQUALATOR = new EtablissementEqualator();
	private static final Equalator<Address> ADDRESS_EQUALATOR = new AdresseEqualator();

	private static final Function<Organisation, EtablissementPrincipal> ETABLISSEMENT_PRINCIPAL_EXTRACTOR = new EtablissementPrincipalExtractor();
	private static final Function<Organisation, Stream<? extends EtablissementSecondaire>> ETABLISSEMENTS_SECONDAIRES_EXTRACTOR = new EtablissementsSecondairesExtractor();
	private static final Function<Organisation, Stream<Keyed<BigInteger, Address>>> ADRESSES_LEGALES_EXTRACTOR = new AdressesLegalesExtractor();
	private static final Function<Organisation, Stream<Keyed<BigInteger, Address>>> ADRESSES_EFFECTIVES_EXTRACTOR = new AdressesEffectivesIdeExtractor();
	private static final Function<Organisation, Stream<Keyed<BigInteger, Address>>> ADRESSES_CASE_POSTALE_IDE_EXTRACTOR = new AdressesCasePostaleIdeExtractor();

	public Object mapOrganisation(List<OrganisationSnapshot> snapshots) {

		// d'abord, on transforme cette liste en map de snapshots indexés par date
		final Map<RegDate, Organisation> organisationMap = snapshots.stream()
				.collect(Collectors.toMap(os -> XmlUtils.xmlcal2regdate(os.getBeginValidityDate()),
				                          OrganisationSnapshot::getOrganisation));

		// on enregistre les data collectors au niveau de l'organisation faîtière (= l'entreprise)
		final LinearDataCollector<Organisation, LegalForm> legalFromCollector = new SimpleDataCollector<>(Organisation::getLegalForm, Equalator.DEFAULT);
		final LinearDataCollector<Organisation, EtablissementPrincipal> etablissementPrincipalCollector = new SimpleDataCollector<>(ETABLISSEMENT_PRINCIPAL_EXTRACTOR, ETABLISSEMENT_EQUALATOR);
		final LinearDataCollector<Organisation, EtablissementSecondaire> etablissementsSecondairesCollector = new FlattenDataCollector<>(ETABLISSEMENTS_SECONDAIRES_EXTRACTOR, ETABLISSEMENT_EQUALATOR, EtablissementSecondaire::getId);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesRcEtablissementsCollector = new FlattenIndexedDataCollector<>(ADRESSES_LEGALES_EXTRACTOR, ADDRESS_EQUALATOR, Keyed::getKey);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesIdeEtablissementsCollector = new FlattenIndexedDataCollector<>(ADRESSES_EFFECTIVES_EXTRACTOR, ADDRESS_EQUALATOR, Keyed::getKey);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesIdeCasePostaleEtablissementsCollector = new FlattenIndexedDataCollector<>(ADRESSES_CASE_POSTALE_IDE_EXTRACTOR, ADDRESS_EQUALATOR, Keyed::getKey);

		// on collecte les plages de dates dans les collectors
		Historizer.historize(organisationMap, Arrays.asList(legalFromCollector,
		                                                    etablissementPrincipalCollector,
		                                                    etablissementsSecondairesCollector,
		                                                    adressesRcEtablissementsCollector,
		                                                    adressesIdeEtablissementsCollector,
		                                                    adressesIdeCasePostaleEtablissementsCollector));

		// récupération des plages de valeurs
		final List<DateRanged<LegalForm>> formesJuridiques = legalFromCollector.getCollectedData();
		final List<DateRanged<EtablissementPrincipal>> prnEtablissements = etablissementPrincipalCollector.getCollectedData();
		final List<DateRanged<EtablissementSecondaire>> secEtablissements = etablissementsSecondairesCollector.getCollectedData();
		final Map<BigInteger, List<DateRanged<Address>>> adressesRc = adressesRcEtablissementsCollector.getCollectedData();
		final Map<BigInteger, List<DateRanged<Address>>> adressesIdeEffectives = adressesIdeEtablissementsCollector.getCollectedData();
		final Map<BigInteger, List<DateRanged<Address>>> adressesIdeCasePostale = adressesIdeCasePostaleEtablissementsCollector.getCollectedData();

		// et finalement on construit un objet à renvoyer à l'appelant
		// TODO construire l'objet à renvoyer
		return null;
	}
}
