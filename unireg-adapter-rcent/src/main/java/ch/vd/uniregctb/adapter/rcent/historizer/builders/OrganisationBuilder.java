package ch.vd.uniregctb.adapter.rcent.historizer.builders;

import java.math.BigInteger;
import java.util.List;

import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.adapter.rcent.historizer.convertor.DateRangedConvertor;
import ch.vd.uniregctb.adapter.rcent.historizer.convertor.IdentifierListConverter;
import ch.vd.uniregctb.adapter.rcent.model.Organisation;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class OrganisationBuilder {
	private final BigInteger cantonalId;
	private final List<DateRangeHelper.Ranged<Identifier>> organisationIdentifiers;
	private final List<DateRangeHelper.Ranged<String>> nomsEntreprise;
	private final List<DateRangeHelper.Ranged<String>> nomsAdditionnelsEntreprise;
	private final List<DateRangeHelper.Ranged<LegalForm>> formesJuridiques;
	private final List<DateRangeHelper.Ranged<BigInteger>> locations;
	private final List<DateRangeHelper.Ranged<BigInteger>> transfereA;
	private final List<DateRangeHelper.Ranged<BigInteger>> transfereDe;
	private final List<DateRangeHelper.Ranged<BigInteger>> remplacePar;
	private final List<DateRangeHelper.Ranged<BigInteger>> enRemplacementDe;

private final List<OrganisationLocation> locationsData;

	public OrganisationBuilder(BigInteger cantonalId,
	                           List<DateRangeHelper.Ranged<Identifier>> organisationIdentifiers,
	                           List<DateRangeHelper.Ranged<String>> nomsEntreprise,
	                           List<DateRangeHelper.Ranged<String>> nomsAdditionnelsEntreprise,
	                           List<DateRangeHelper.Ranged<LegalForm>> formesJuridiques,
	                           List<DateRangeHelper.Ranged<BigInteger>> locations,
	                           List<DateRangeHelper.Ranged<BigInteger>> transfereA,
	                           List<DateRangeHelper.Ranged<BigInteger>> transfereDe,
	                           List<DateRangeHelper.Ranged<BigInteger>> remplacePar,
	                           List<DateRangeHelper.Ranged<BigInteger>> enRemplacementDe,
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
