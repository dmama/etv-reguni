package ch.vd.unireg.interfaces.entreprise.rcent.adapter.model;

import ch.vd.evd0022.v3.Authorisation;

/**
 * @author Raphaël Marmier, 2015-08-25
 */
public class OrganisationFunction {

	private final Integer cantonalId;
	private final String name;
	private final String firstName;

	private final String placeOfResidenceName;

	private final String functionText;
	private final Authorisation authorisation;
	private final String authorisationRestriction;

	public OrganisationFunction(ch.vd.evd0022.v3.Function function) {
		this.cantonalId = function.getParty().getPerson().getCantonalId();
		this.name = function.getParty().getPerson().getName();
		this.firstName = function.getParty().getPerson().getFirstName();
		this.placeOfResidenceName = function.getParty().getPlaceOfResidence().getPlaceOfResidenceName();
		this.functionText = function.getFunctionText();
		this.authorisation = function.getAuthorisation();
		this.authorisationRestriction = function.getAuthorisationRestriction();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final OrganisationFunction that = (OrganisationFunction) o;

		if (cantonalId != null ? !cantonalId.equals(that.cantonalId) : that.cantonalId != null) return false;
		if (!name.equals(that.name)) return false;
		if (firstName != null ? !firstName.equals(that.firstName) : that.firstName != null) return false;
		if (placeOfResidenceName != null ? !placeOfResidenceName.equals(that.placeOfResidenceName) : that.placeOfResidenceName != null) return false;
		if (functionText != null ? !functionText.equals(that.functionText) : that.functionText != null) return false;
		if (authorisation != that.authorisation) return false;
		return !(authorisationRestriction != null ? !authorisationRestriction.equals(that.authorisationRestriction) : that.authorisationRestriction != null);

	}

	@Override
	public int hashCode() {
		int result = cantonalId != null ? cantonalId.hashCode() : 0;
		result = 31 * result + name.hashCode();
		result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
		result = 31 * result + (placeOfResidenceName != null ? placeOfResidenceName.hashCode() : 0);
		result = 31 * result + (functionText != null ? functionText.hashCode() : 0);
		result = 31 * result + (authorisation != null ? authorisation.hashCode() : 0);
		result = 31 * result + (authorisationRestriction != null ? authorisationRestriction.hashCode() : 0);
		return result;
	}

	public Integer getCantonalId() {
		return cantonalId;
	}

	public String getName() {
		return name;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getPlaceOfResidenceName() {
		return placeOfResidenceName;
	}

	public String getFunctionText() {
		return functionText;
	}

	public Authorisation getAuthorisation() {
		return authorisation;
	}

	public String getAuthorisationRestriction() {
		return authorisationRestriction;
	}
}
