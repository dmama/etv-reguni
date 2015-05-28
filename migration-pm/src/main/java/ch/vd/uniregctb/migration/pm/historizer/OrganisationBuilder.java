package ch.vd.uniregctb.migration.pm.historizer;

import java.math.BigInteger;
import java.util.List;

import ch.vd.evd0022.v1.LegalForm;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.rcent.model.Organisation;
import ch.vd.uniregctb.migration.pm.rcent.model.OrganisationLocation;

public class OrganisationBuilder {
//		private private final List<Identifier> organisationIdentifiers;

	//		private private final List<DateRanged<String>> organisationName;
	private final List<DateRanged<String>> nomsEntreprise;
	//		private private final List<DateRanged<String>> organisationAdditionalName;
	private final List<DateRanged<String>> nomsAdditionnelsEntreprise;
	//		private private final List<DateRanged<LegalForm>> legalForm;
	private final List<DateRanged<LegalForm>> formesJuridiques;
	//
//		private private final List<DateRanged<OrganisationLocation>> locations;
	private final List<DateRanged<BigInteger>> prnEtablissements;
	private final List<DateRanged<BigInteger>> secEtablissements;
	//
//		private private final List<DateRanged<Long>> transferTo;
	private final List<DateRanged<BigInteger>> transfereA;
	//		private private final List<DateRanged<Long>> transferFrom;
	private final List<DateRanged<BigInteger>> transfereDe;
	//		private private final List<DateRanged<Long>> replacedBy;
	private final List<DateRanged<BigInteger>> remplacePar;
	//		private private final List<DateRanged<Long>> inPreplacementOf;
	private final List<DateRanged<BigInteger>> enRemplacementDe;

	private final List<OrganisationLocation> locations;

	public OrganisationBuilder(List<DateRanged<String>> nomsEntreprise,
	                           List<DateRanged<String>> nomsAdditionnelsEntreprise,
	                           List<DateRanged<LegalForm>> formesJuridiques, List<DateRanged<BigInteger>> prnEtablissements,
	                           List<DateRanged<BigInteger>> secEtablissements, List<DateRanged<BigInteger>> transfereA,
	                           List<DateRanged<BigInteger>> transfereDe, List<DateRanged<BigInteger>> remplacePar,
	                           List<DateRanged<BigInteger>> enRemplacementDe,
	                           List<OrganisationLocation> locations) {
		this.enRemplacementDe = enRemplacementDe;
		this.nomsEntreprise = nomsEntreprise;
		this.nomsAdditionnelsEntreprise = nomsAdditionnelsEntreprise;
		this.formesJuridiques = formesJuridiques;
		this.prnEtablissements = prnEtablissements;
		this.secEtablissements = secEtablissements;
		this.transfereA = transfereA;
		this.transfereDe = transfereDe;
		this.remplacePar = remplacePar;
		this.locations = locations;
	}

	public Organisation build() {
		// TODO: le boulot
		return null;
	}
}
