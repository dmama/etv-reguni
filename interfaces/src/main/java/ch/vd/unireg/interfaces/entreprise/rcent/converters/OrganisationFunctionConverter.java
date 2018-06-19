package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.entreprise.data.Autorisation;
import ch.vd.unireg.interfaces.entreprise.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.OrganisationFunction;

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
