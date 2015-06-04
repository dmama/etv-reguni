package ch.vd.uniregctb.migration.pm.historizer;

import java.math.BigInteger;
import java.util.List;

import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.rcent.model.Organisation;
import ch.vd.uniregctb.migration.pm.rcent.model.OrganisationLocation;

public class OrganisationBuilder {
	private final BigInteger cantonalId;
	private final List<DateRanged<Identifier>> organisationIdentifiers;
	private final List<DateRanged<String>> nomsEntreprise;
	private final List<DateRanged<String>> nomsAdditionnelsEntreprise;
	private final List<DateRanged<LegalForm>> formesJuridiques;
	private final List<DateRanged<BigInteger>> etablissements;
	private final List<DateRanged<BigInteger>> transfereA;
	private final List<DateRanged<BigInteger>> transfereDe;
	private final List<DateRanged<BigInteger>> remplacePar;
	private final List<DateRanged<BigInteger>> enRemplacementDe;

	private final List<OrganisationLocation> locations;

	public OrganisationBuilder(BigInteger cantonalId,
	                           List<DateRanged<Identifier>> organisationIdentifiers,
	                           List<DateRanged<String>> nomsEntreprise,
	                           List<DateRanged<String>> nomsAdditionnelsEntreprise,
	                           List<DateRanged<LegalForm>> formesJuridiques,
	                           List<DateRanged<BigInteger>> etablissements, List<DateRanged<BigInteger>> transfereA,
	                           List<DateRanged<BigInteger>> transfereDe, List<DateRanged<BigInteger>> remplacePar,
	                           List<DateRanged<BigInteger>> enRemplacementDe,
	                           List<OrganisationLocation> locations) {
		this.cantonalId = cantonalId;
		this.organisationIdentifiers = organisationIdentifiers;
		this.enRemplacementDe = enRemplacementDe;
		this.nomsEntreprise = nomsEntreprise;
		this.nomsAdditionnelsEntreprise = nomsAdditionnelsEntreprise;
		this.formesJuridiques = formesJuridiques;
		this.etablissements = etablissements;
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
