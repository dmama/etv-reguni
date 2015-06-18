package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0021.v1.Address;
import ch.vd.unireg.interfaces.organisation.data.Adresse;

public class AddressConverter extends BaseConverter<Address, Adresse> {

	@Override
	protected Adresse convert(@NotNull Address address) {
		return new Adresse(address.getAddressLine1(),
		                   address.getAddressLine2(),
		                   address.getStreet(),
		                   address.getHouseNumber(),
		                   address.getDwellingNumber(),
		                   address.getPostOfficeBoxText(),
		                   address.getPostOfficeBoxNumber(),
		                   address.getLocality(),
		                   address.getTown(),
		                   address.getSwissZipCode(),
		                   address.getSwissZipCodeAddOn(),
		                   address.getSwissZipCodeId(),
		                   address.getForeignZipCode(),
		                   address.getCountry().getCountryId(),
		                   address.getFederalBuildingId(),
		                   address.getXCoordinate(),
		                   address.getYCoordinate());
	}
}
