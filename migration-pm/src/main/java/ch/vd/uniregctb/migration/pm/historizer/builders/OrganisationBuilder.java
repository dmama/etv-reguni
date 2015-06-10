package ch.vd.uniregctb.migration.pm.historizer.builders;

import java.math.BigInteger;
import java.util.List;

import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.historizer.convertor.DateRangedConvertor;
import ch.vd.uniregctb.migration.pm.historizer.convertor.IdentifierListConverter;
import ch.vd.uniregctb.migration.pm.rcent.model.Organisation;
import ch.vd.uniregctb.migration.pm.rcent.model.OrganisationLocation;

public class OrganisationBuilder {
	private final BigInteger cantonalId;
	private final List<DateRanged<Identifier>> organisationIdentifiers;
	private final List<DateRanged<String>> nomsEntreprise;
	private final List<DateRanged<String>> nomsAdditionnelsEntreprise;
	private final List<DateRanged<LegalForm>> formesJuridiques;
	private final List<DateRanged<BigInteger>> locations;
	private final List<DateRanged<BigInteger>> transfereA;
	private final List<DateRanged<BigInteger>> transfereDe;
	private final List<DateRanged<BigInteger>> remplacePar;
	private final List<DateRanged<BigInteger>> enRemplacementDe;

private final List<OrganisationLocation> locationsData;

	public OrganisationBuilder(BigInteger cantonalId,
	                           List<DateRanged<Identifier>> organisationIdentifiers,
	                           List<DateRanged<String>> nomsEntreprise,
	                           List<DateRanged<String>> nomsAdditionnelsEntreprise,
	                           List<DateRanged<LegalForm>> formesJuridiques,
	                           List<DateRanged<BigInteger>> locations,
	                           List<DateRanged<BigInteger>> transfereA,
	                           List<DateRanged<BigInteger>> transfereDe,
	                           List<DateRanged<BigInteger>> remplacePar,
	                           List<DateRanged<BigInteger>> enRemplacementDe,
	                           List<OrganisationLocation> locationsData) {
		this.cantonalId = cantonalId;
		this.organisationIdentifiers = organisationIdentifiers;
		this.enRemplacementDe = enRemplacementDe;
		this.nomsEntreprise = nomsEntreprise;
		this.nomsAdditionnelsEntreprise = nomsAdditionnelsEntreprise;
		this.formesJuridiques = formesJuridiques;
		this.locations = locations;
		this.transfereA = transfereA;
		this.transfereDe = transfereDe;
		this.remplacePar = remplacePar;
		this.locationsData = locationsData;
	}

	public Organisation build() {
		return new Organisation(cantonalId.longValue(),
		                        IdentifierListConverter.toMapOfListsOfDateRangedValues(organisationIdentifiers),
		                        nomsEntreprise,
		                        nomsAdditionnelsEntreprise,
		                        formesJuridiques,
		                        DateRangedConvertor.convert(locations, BigInteger::longValue),
		                        locationsData,
		                        DateRangedConvertor.convert(transfereA, BigInteger::longValue),
		                        DateRangedConvertor.convert(transfereDe, BigInteger::longValue),
		                        DateRangedConvertor.convert(remplacePar, BigInteger::longValue),
		                        DateRangedConvertor.convert(enRemplacementDe, BigInteger::longValue));
	}
}
