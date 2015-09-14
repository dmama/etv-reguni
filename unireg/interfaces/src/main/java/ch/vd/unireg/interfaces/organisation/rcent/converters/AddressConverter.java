package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0021.v1.Address;
import ch.vd.unireg.interfaces.organisation.data.Adresse;

public class AddressConverter extends BaseConverter<Address, Adresse> {

	/**
	 * Convertisseur d'adresse
	 *
	 * NOTE: Réduction du champs SwissZipCode Long vers Integer
	 *
	 * @param address L'adresse à convertir
	 * @return L'adresse convertie
	 */
	@Override
	@NotNull
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
		                   address.getSwissZipCode().intValue(),
		                   address.getSwissZipCodeAddOn(),
		                   address.getSwissZipCodeId(),
		                   address.getForeignZipCode(),
		                   address.getCountry().getCountryId(),
		                   address.getFederalBuildingId(),
		                   address.getXCoordinate(),
		                   address.getYCoordinate());
	}
}
