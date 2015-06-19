package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0021.v1.Country;
import ch.vd.evd0022.v1.PlaceOfResidence;
import ch.vd.unireg.interfaces.organisation.data.LieuDeResidence;

public class PlaceOfResidenceConverter extends BaseConverter<PlaceOfResidence, LieuDeResidence> {

	@Override
	@NotNull
	protected LieuDeResidence convert(@NotNull PlaceOfResidence placeOfResidence) {
		final Country country = placeOfResidence.getCountry();
		return new LieuDeResidence(placeOfResidence.getPlaceOfResidenceName(),
		                           placeOfResidence.getSwissMunicipality() != null ?
				                           placeOfResidence.getSwissMunicipality().getMunicipalityId() : null,
		                           country != null ? country.getCountryId() : null
		);
	}
}
