package ch.vd.unireg.interfaces.organisation.data;

public class Adresse {

    private String addressLine1;
    private String addressLine2;
    private String street;
    private String houseNumber;
    private String dwellingNumber;
    private String postOfficeBoxText;
    private Long postOfficeBoxNumber;
    private String locality;
    private String town;
    private Long swissZipCode;
    private String swissZipCodeAddOn;
    private Integer swissZipCodeId;
    private String foreignZipCode;
    private Pays pays;
    private Long federalBuildingId;
    private Long xCoordinate;
    private Long yCoordinate;

    public Adresse(final String addressLine1, final String addressLine2, final String street, final String houseNumber, final String dwellingNumber, final String postOfficeBoxText,
                   final Long postOfficeBoxNumber, final String locality, final String town, final Long swissZipCode, final String swissZipCodeAddOn, final Integer swissZipCodeId,
                   final String foreignZipCode, final Pays pays, final Long federalBuildingId, final Long xCoordinate, final Long yCoordinate) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.street = street;
        this.houseNumber = houseNumber;
        this.dwellingNumber = dwellingNumber;
        this.postOfficeBoxText = postOfficeBoxText;
        this.postOfficeBoxNumber = postOfficeBoxNumber;
        this.locality = locality;
        this.town = town;
        this.swissZipCode = swissZipCode;
        this.swissZipCodeAddOn = swissZipCodeAddOn;
        this.swissZipCodeId = swissZipCodeId;
        this.foreignZipCode = foreignZipCode;
        this.pays = pays;
        this.federalBuildingId = federalBuildingId;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public Pays getPays() {
        return pays;
    }

    public String getDwellingNumber() {
        return dwellingNumber;
    }

    public Long getFederalBuildingId() {
        return federalBuildingId;
    }

    public String getForeignZipCode() {
        return foreignZipCode;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public String getLocality() {
        return locality;
    }

    public Long getPostOfficeBoxNumber() {
        return postOfficeBoxNumber;
    }

    public String getPostOfficeBoxText() {
        return postOfficeBoxText;
    }

    public String getStreet() {
        return street;
    }

    public Long getSwissZipCode() {
        return swissZipCode;
    }

    public String getSwissZipCodeAddOn() {
        return swissZipCodeAddOn;
    }

    public Integer getSwissZipCodeId() {
        return swissZipCodeId;
    }

    public String getTown() {
        return town;
    }

    public Long getxCoordinate() {
        return xCoordinate;
    }

    public Long getyCoordinate() {
        return yCoordinate;
    }
}
