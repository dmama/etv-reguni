package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Autorisation;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.model.OrganisationFunction;

public class OrganisationFunctionConverter extends BaseConverter<OrganisationFunction, FonctionOrganisation> {

	private static final AutorisationConverter AUTORISATION_CONVERTER = new AutorisationConverter();

	@Override
	@NotNull
	protected FonctionOrganisation convert(@NotNull OrganisationFunction function) {

		Autorisation autorisation= null;
		if (function.getAuthorisation() != null) {
			autorisation = AUTORISATION_CONVERTER.apply(function.getAuthorisation());
		}
		String restrictionAutorisation = function.getAuthorisationRestriction();

		return new FonctionOrganisation(function.getCantonalId(),
		                    function.getName(),
		                    function.getFirstName(),
		                    function.getPlaceOfResidenceName(),
		                    function.getFunctionText(),
		                    autorisation,
		                    restrictionAutorisation);
	}

}
