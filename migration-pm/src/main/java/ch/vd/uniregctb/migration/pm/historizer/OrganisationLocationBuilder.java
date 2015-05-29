package ch.vd.uniregctb.migration.pm.historizer;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.rcent.model.OrganisationLocation;

public class OrganisationLocationBuilder {

	//  	private final List<DateRanged<Identifier>> identifier;
	final Map<BigInteger, List<DateRanged<Identifier>>> identifiers;
	//		private final List<DateRanged<String>> name;
	final Map<BigInteger, List<DateRanged<String>>> nomsEtablissements;
	//		private final List<DateRanged<String>> otherNames;
	final Map<BigInteger, List<DateRanged<String>>> autresNomsEtablissements;
	//		private final List<DateRanged<KindOfLocation>> kindOfLocation;
	final Map<BigInteger, List<DateRanged<KindOfLocation>>> genreEtablissements;
	//		private final List<DateRanged<Integer>> seat;
	final Map<BigInteger, List<DateRanged<Integer>>> sieges;
//		private final List<DateRanged<Function>> function;
//		private final List<DateRanged<Long>> replacedBy;
//		private final List<DateRanged<Long>> inReplacementOf;

	//		private final List<DateRanged<CommercialRegisterStatus>> status;
//		private final List<DateRanged<String>> name;
//		private final List<DateRanged<CommercialRegisterEntryStatus>> entryStatus;
//		private final List<DateRanged<Capital>> capital;
//		private final List<DateRanged<Address>> legalAddress;
	final Map<BigInteger, List<DateRanged<Address>>> adressesRc;

	//		private final List<DateRanged<UidRegisterStatus>> status;
//		private final List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation;
//		private final List<DateRanged<Address>> effectiveAddress;
	final Map<BigInteger, List<DateRanged<Address>>> adressesIdeEffectives;
	//		private final List<DateRanged<Address>> postOfficeBoxAddress;
	final Map<BigInteger, List<DateRanged<Address>>> adressesIdeCasePostale;
//		private final List<DateRanged<UidRegisterPublicStatus>> publicStatus;
//		private final List<DateRanged<UidRegisterLiquidationReason>> liquidationReason;

//		private final List<DateRanged<UidRegisterStatus>> status;
//		private final List<DateRanged<UidRegisterTypeOfOrganisation>> typeOfOrganisation;
//		private final List<DateRanged<Address>> effectiveAddress;
//		private final List<DateRanged<Address>> postOfficeBoxAddress;
//		private final List<DateRanged<UidRegisterPublicStatus>> publicStatus;
//		private final List<DateRanged<UidRegisterLiquidationReason>> liquidationReason;


	public OrganisationLocationBuilder(Map<BigInteger, List<DateRanged<Identifier>>> identifiers,
	                                   Map<BigInteger, List<DateRanged<String>>> nomsEtablissements,
	                                   Map<BigInteger, List<DateRanged<String>>> autresNomsEtablissements,
	                                   Map<BigInteger, List<DateRanged<KindOfLocation>>> genreEtablissements,
	                                   Map<BigInteger, List<DateRanged<Integer>>> sieges,
	                                   Map<BigInteger, List<DateRanged<Address>>> adressesRc,
	                                   Map<BigInteger, List<DateRanged<Address>>> adressesIdeEffectives,
	                                   Map<BigInteger, List<DateRanged<Address>>> adressesIdeCasePostale) {
		this.adressesIdeCasePostale = adressesIdeCasePostale;
		this.identifiers = identifiers;
		this.nomsEtablissements = nomsEtablissements;
		this.autresNomsEtablissements = autresNomsEtablissements;
		this.genreEtablissements = genreEtablissements;
		this.sieges = sieges;
		this.adressesRc = adressesRc;
		this.adressesIdeEffectives = adressesIdeEffectives;
	}

	public List<OrganisationLocation> build() {
		// TODO: le boulot
		return null;
	}
}
