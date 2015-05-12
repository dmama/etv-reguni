package ch.vd.uniregctb.migration.pm.historizer;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0021.v1.Country;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.evd0022.v1.OrganisationSnapshot;
import ch.vd.evd0022.v1.SwissMunicipality;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.migration.pm.historizer.collector.FlattenDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.FlattenIndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.IndexedDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.equalator.Equalator;
import ch.vd.uniregctb.migration.pm.rcent.component.DateRanged;
import ch.vd.uniregctb.migration.pm.rcent.component.Keyed;
import ch.vd.uniregctb.migration.pm.historizer.collector.LinearDataCollector;
import ch.vd.uniregctb.migration.pm.historizer.collector.SimpleDataCollector;

public class OrganisationHistorizer {

	public static class Etablissement {

		private final long id;
		private final String noIde;
		private final String nom;
		private final Integer noOfsCommune;
		private final String noga;

		public Etablissement(long id, String noIde, String nom, Integer noOfsCommune, String noga) {
			this.id = id;
			this.noIde = noIde;
			this.nom = nom;
			this.noOfsCommune = noOfsCommune;
			this.noga = noga;
		}

		public Etablissement(OrganisationLocation location) {
			this(location.getCantonalId().longValue(),
			     NO_IDE_EXTRACTOR.apply(location.getIdentifier()),
			     location.getName(),
			     Optional.ofNullable(location.getSeat()).map(SwissMunicipality::getMunicipalityId).orElse(null),
			     location.getNogaCode());
		}

		public long getId() {
			return id;
		}

		public String getNoIde() {
			return noIde;
		}

		public String getNom() {
			return nom;
		}

		public Integer getNoOfsCommune() {
			return noOfsCommune;
		}

		public String getNoga() {
			return noga;
		}
	}

	public static class EtablissementPrincipal extends Etablissement {
		public EtablissementPrincipal(OrganisationLocation location) {
			super(location);
		}
	}

	public static class EtablissementSecondaire extends Etablissement {
		public EtablissementSecondaire(OrganisationLocation location) {
			super(location);
		}
	}

	private static final Equalator<Etablissement> ETABLISSEMENT_EQUALATOR = OrganisationHistorizer::etablissementsEqual;
	private static final Equalator<Address> ADDRESS_EQUALATOR = OrganisationHistorizer::addressesEqual;

	private static final Function<List<Identifier>, String> NO_IDE_EXTRACTOR = OrganisationHistorizer::extractNumeroIDE;

	public Object mapOrganisation(List<OrganisationSnapshot> snapshots) {

		// d'abord, on transforme cette liste en map de snapshots indexés par date
		final Map<RegDate, Organisation> organisationMap = snapshots.stream()
				.collect(Collectors.toMap(os -> XmlUtils.xmlcal2regdate(os.getBeginValidityDate()),
				                          OrganisationSnapshot::getOrganisation));

		// on enregistre les data collectors au niveau de l'organisation faîtière (= l'entreprise)
		final LinearDataCollector<Organisation, LegalForm> legalFromCollector = new SimpleDataCollector<>(Organisation::getLegalForm, Equalator.DEFAULT);
		final LinearDataCollector<Organisation, EtablissementPrincipal> etablissementPrincipalCollector = new SimpleDataCollector<>(OrganisationHistorizer::extractEtablissementPrincipal, ETABLISSEMENT_EQUALATOR);
		final LinearDataCollector<Organisation, EtablissementSecondaire> etablissementsSecondairesCollector = new FlattenDataCollector<>(OrganisationHistorizer::extractEtablissementsSecondaires, ETABLISSEMENT_EQUALATOR, EtablissementSecondaire::getId);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesRcEtablissementsCollector = new FlattenIndexedDataCollector<>(OrganisationHistorizer::extractAdressesLegalesRC, ADDRESS_EQUALATOR, Keyed::getKey);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesIdeEtablissementsCollector = new FlattenIndexedDataCollector<>(OrganisationHistorizer::extractAdressesEffectivesIDE, ADDRESS_EQUALATOR, Keyed::getKey);
		final IndexedDataCollector<Organisation, Address, BigInteger> adressesIdeCasePostaleEtablissementsCollector = new FlattenIndexedDataCollector<>(OrganisationHistorizer::extractAdressesCasePostaleIDE, ADDRESS_EQUALATOR, Keyed::getKey);

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

	@Nullable
	private static String extractNumeroIDE(List<Identifier> identifiers) {
		return identifiers.stream()
				.filter(i -> "CH.IDE".equals(i.getIdentifierCategory()))
				.findAny()
				.map(Identifier::getIdentifierValue)
				.orElse(null);
	}

	@Nullable
	private static EtablissementPrincipal extractEtablissementPrincipal(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> KindOfLocation.ETABLISSEMENT_PRINCIPAL == ol.getKindOfLocation())
				.findAny()
				.map(EtablissementPrincipal::new)
				.orElse(null);
	}

	@Nullable
	private static Stream<EtablissementSecondaire> extractEtablissementsSecondaires(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> KindOfLocation.ETABLISSEMENT_SECONDAIRE == ol.getKindOfLocation())
				.map(EtablissementSecondaire::new);
	}

	private static Stream<Keyed<BigInteger, Address>> extractAdressesLegalesRC(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getCommercialRegisterData() != null && ol.getCommercialRegisterData().getLegalAddress() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getCommercialRegisterData().getLegalAddress()));
	}

	private static Stream<Keyed<BigInteger, Address>> extractAdressesEffectivesIDE(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getUidRegisterData() != null && ol.getUidRegisterData().getEffectiveAddress() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getUidRegisterData().getEffectiveAddress()));
	}

	private static Stream<Keyed<BigInteger, Address>> extractAdressesCasePostaleIDE(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getUidRegisterData() != null && ol.getUidRegisterData().getPostOfficeBoxAddress() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getUidRegisterData().getPostOfficeBoxAddress()));
	}

	private static boolean etablissementsEqual(Etablissement e1, Etablissement e2) {
		if (e1 == e2) {
			return true;
		}
		if (e1 == null || e2 == null || e1.getClass() != e2.getClass()) {
			return false;
		}

		if (e1.id != e2.id) return false;
		if (e1.noIde != null ? !e1.noIde.equals(e2.noIde) : e2.noIde != null) return false;
		if (e1.nom != null ? !e1.nom.equals(e2.nom) : e2.nom != null) return false;
		if (e1.noOfsCommune != null ? !e1.noOfsCommune.equals(e2.noOfsCommune) : e2.noOfsCommune != null) return false;
		return !(e1.noga != null ? !e1.noga.equals(e2.noga) : e2.noga != null);
	}

	private static boolean countriesEqual(Country c1, Country c2) {
		if (c1 == c2) {
			return true;
		}
		if (c1 == null || c2 == null || c1.getClass() != c2.getClass()) {
			return false;
		}
		return Equalator.DEFAULT.test(c1.getCountryId(), c2.getCountryId());
	}

	private static boolean addressesEqual(Address a1, Address a2) {
		if (a1 == a2) {
			return true;
		}
		if (a1 == null || a2 == null || a1.getClass() != a2.getClass()) {
			return false;
		}

		if (a1.getAddressLine1() != null ? !a1.getAddressLine1().equals(a2.getAddressLine1()) : a2.getAddressLine1() != null) return false;
		if (a1.getAddressLine2() != null ? !a1.getAddressLine2().equals(a2.getAddressLine2()) : a2.getAddressLine2() != null) return false;
		if (a1.getDwellingNumber() != null ? !a1.getDwellingNumber().equals(a2.getDwellingNumber()) : a2.getDwellingNumber() != null) return false;
		if (a1.getFederalBuildingId() != null ? !a1.getFederalBuildingId().equals(a2.getFederalBuildingId()) : a2.getFederalBuildingId() != null) return false;
		if (a1.getForeignZipCode() != null ? !a1.getForeignZipCode().equals(a2.getForeignZipCode()) : a2.getForeignZipCode() != null) return false;
		if (a1.getHouseNumber() != null ? !a1.getHouseNumber().equals(a2.getHouseNumber()) : a2.getHouseNumber() != null) return false;
		if (a1.getLocality() != null ? !a1.getLocality().equals(a2.getLocality()) : a2.getLocality() != null) return false;
		if (a1.getPostOfficeBoxNumber() != null ? !a1.getPostOfficeBoxNumber().equals(a2.getPostOfficeBoxNumber()) : a2.getPostOfficeBoxNumber() != null) return false;
		if (a1.getPostOfficeBoxText() != null ? !a1.getPostOfficeBoxText().equals(a2.getPostOfficeBoxText()) : a2.getPostOfficeBoxText() != null) return false;
		if (a1.getStreet() != null ? !a1.getStreet().equals(a2.getStreet()) : a2.getStreet() != null) return false;
		if (a1.getSwissZipCode() != null ? !a1.getSwissZipCode().equals(a2.getSwissZipCode()) : a2.getSwissZipCode() != null) return false;
		if (a1.getSwissZipCodeAddOn() != null ? !a1.getSwissZipCodeAddOn().equals(a2.getSwissZipCodeAddOn()) : a2.getSwissZipCodeAddOn() != null) return false;
		if (a1.getSwissZipCodeId() != null ? !a1.getSwissZipCodeId().equals(a2.getSwissZipCodeId()) : a2.getSwissZipCodeId() != null) return false;
		if (a1.getTown() != null ? !a1.getTown().equals(a2.getTown()) : a2.getTown() != null) return false;
		if (a1.getXCoordinate() != null ? !a1.getXCoordinate().equals(a2.getXCoordinate()) : a2.getXCoordinate() != null) return false;
		if (a1.getYCoordinate() != null ? !a1.getYCoordinate().equals(a2.getYCoordinate()) : a2.getYCoordinate() != null) return false;
		return countriesEqual(a1.getCountry(), a2.getCountry());
	}
}
