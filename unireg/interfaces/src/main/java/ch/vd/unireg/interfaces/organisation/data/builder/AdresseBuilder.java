package ch.vd.unireg.interfaces.organisation.data.builder;

import ch.vd.unireg.interfaces.organisation.data.Adresse;

public class AdresseBuilder {
	private String addressLine1;
	private String addressLine2;
	private String street;
	private String houseNumber;
	private String dwellingNumber;
	private String postOfficeBoxText;
	private Long postOfficeBoxNumber;
	private String locality;
	private String town;
	private Integer swissZipCode;
	private String swissZipCodeAddOn;
	private Integer swissZipCodeId;
	private String foreignZipCode;
	private Integer pays;
	private Long federalBuildingId;
	private Long xCoordinate;
	private Long yCoordinate;

	public AdresseBuilder() {
	}

	public AdresseBuilder withAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
		return this;
	}

	public AdresseBuilder withAddressLine2(String addressLine2) {
		this.addressLine2 = addressLine2;
		return this;
	}

	public AdresseBuilder withStreet(String street) {
		this.street = street;
		return this;
	}

	public AdresseBuilder withHouseNumber(String houseNumber) {
		this.houseNumber = houseNumber;
		return this;
	}

	public AdresseBuilder withDwellingNumber(String dwellingNumber) {
		this.dwellingNumber = dwellingNumber;
		return this;
	}

	public AdresseBuilder withPostOfficeBoxText(String postOfficeBoxText) {
		this.postOfficeBoxText = postOfficeBoxText;
		return this;
	}

	public AdresseBuilder withPostOfficeBoxNumber(Long postOfficeBoxNumber) {
		this.postOfficeBoxNumber = postOfficeBoxNumber;
		return this;
	}

	public AdresseBuilder withLocality(String locality) {
		this.locality = locality;
		return this;
	}

	public AdresseBuilder withTown(String town) {
		this.town = town;
		return this;
	}

	public AdresseBuilder withSwissZipCode(Integer swissZipCode) {
		this.swissZipCode = swissZipCode;
		return this;
	}

	public AdresseBuilder withSwissZipCodeAddOn(String swissZipCodeAddOn) {
		this.swissZipCodeAddOn = swissZipCodeAddOn;
		return this;
	}

	public AdresseBuilder withSwissZipCodeId(Integer swissZipCodeId) {
		this.swissZipCodeId = swissZipCodeId;
		return this;
	}

	public AdresseBuilder withForeignZipCode(String foreignZipCode) {
		this.foreignZipCode = foreignZipCode;
		return this;
	}

	public AdresseBuilder withPays(Integer pays) {
		this.pays = pays;
		return this;
	}

	public AdresseBuilder withFederalBuildingId(Long federalBuildingId) {
		this.federalBuildingId = federalBuildingId;
		return this;
	}

	public AdresseBuilder withXCoordinate(Long xCoordinate) {
		this.xCoordinate = xCoordinate;
		return this;
	}

	public AdresseBuilder withYCoordinate(Long yCoordinate) {
		this.yCoordinate = yCoordinate;
		return this;
	}

	public Adresse build() {
		return new Adresse(addressLine1, addressLine2, street, houseNumber, dwellingNumber, postOfficeBoxText, postOfficeBoxNumber, locality, town, swissZipCode, swissZipCodeAddOn, swissZipCodeId,
		            foreignZipCode, pays, federalBuildingId, xCoordinate, yCoordinate);
	}
}
