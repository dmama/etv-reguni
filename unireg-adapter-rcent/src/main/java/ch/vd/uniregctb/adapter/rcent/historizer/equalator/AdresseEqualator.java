package ch.vd.uniregctb.adapter.rcent.historizer.equalator;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0021.v1.Country;

public class AdresseEqualator implements Equalator<Address> {

	@Override
	 public boolean test(Address a1, Address a2) {
		if (a1 == a2) {
			return true;
		}
		if (a1 == null || a2 == null || a1.getClass() != a2.getClass()) {
			return false;
		}

		if (a1.getAddressLine1() != null ? !a1.getAddressLine1().equals(a2.getAddressLine1()) : a2.getAddressLine1() != null) return false;
		if (a1.getAddressLine2() != null ? !a1.getAddressLine2().equals(a2.getAddressLine2()) : a2.getAddressLine2() != null) return false;
		if (a1.getDwellingNumber() != null ? !a1.getDwellingNumber().equals(a2.getDwellingNumber()) : a2.getDwellingNumber() != null) return false;
		if (a1.getFederalBuildingId() != null ? !a1.getFederalBuildingId().equals(a2.getFederalBuildingId()) : a2.getFederalBuildingId() != null) return false;
		if (a1.getForeignZipCode() != null ? !a1.getForeignZipCode().equals(a2.getForeignZipCode()) : a2.getForeignZipCode() != null) return false;
		if (a1.getHouseNumber() != null ? !a1.getHouseNumber().equals(a2.getHouseNumber()) : a2.getHouseNumber() != null) return false;
		if (a1.getLocality() != null ? !a1.getLocality().equals(a2.getLocality()) : a2.getLocality() != null) return false;
		if (a1.getPostOfficeBoxNumber() != null ? !a1.getPostOfficeBoxNumber().equals(a2.getPostOfficeBoxNumber()) : a2.getPostOfficeBoxNumber() != null) return false;
		if (a1.getPostOfficeBoxText() != null ? !a1.getPostOfficeBoxText().equals(a2.getPostOfficeBoxText()) : a2.getPostOfficeBoxText() != null) return false;
		if (a1.getStreet() != null ? !a1.getStreet().equals(a2.getStreet()) : a2.getStreet() != null) return false;
		if (a1.getSwissZipCode() != null ? !a1.getSwissZipCode().equals(a2.getSwissZipCode()) : a2.getSwissZipCode() != null) return false;
		if (a1.getSwissZipCodeAddOn() != null ? !a1.getSwissZipCodeAddOn().equals(a2.getSwissZipCodeAddOn()) : a2.getSwissZipCodeAddOn() != null) return false;
		if (a1.getSwissZipCodeId() != null ? !a1.getSwissZipCodeId().equals(a2.getSwissZipCodeId()) : a2.getSwissZipCodeId() != null) return false;
		if (a1.getTown() != null ? !a1.getTown().equals(a2.getTown()) : a2.getTown() != null) return false;
		if (a1.getXCoordinate() != null ? !a1.getXCoordinate().equals(a2.getXCoordinate()) : a2.getXCoordinate() != null) return false;
		if (a1.getYCoordinate() != null ? !a1.getYCoordinate().equals(a2.getYCoordinate()) : a2.getYCoordinate() != null) return false;
		return countriesEqual(a1.getCountry(), a2.getCountry());
	}

	private static boolean countriesEqual(Country c1, Country c2) {
		if (c1 == c2) {
			return true;
		}
		if (c1 == null || c2 == null || c1.getClass() != c2.getClass()) {
			return false;
		}
		return Equalator.DEFAULT.test(c1.getCountryId(), c2.getCountryId());
	}
}
