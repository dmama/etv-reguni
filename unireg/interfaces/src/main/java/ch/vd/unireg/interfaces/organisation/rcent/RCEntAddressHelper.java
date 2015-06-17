package ch.vd.unireg.interfaces.organisation.rcent;

import ch.vd.unireg.interfaces.organisation.data.Adresse;

public class RCEntAddressHelper {
	public static Adresse fromRCEntAddress(ch.vd.evd0021.v1.Address address) {
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
