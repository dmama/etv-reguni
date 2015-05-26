package ch.vd.uniregctb.migration.pm.rcent.model.wrappers;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0021.v1.Country;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedWrapper;

public class RCEntAddress extends RCEntRangedWrapper<Address> {

	public RCEntAddress(RegDate beginDate, RegDate endDateDate, Address element) {
		super(beginDate, endDateDate, element);
	}

	public Long getYCoordinate() {
		return getElement().getYCoordinate();
	}

	public String getAddressLine1() {
		return getElement().getAddressLine1();
	}

	public String getAddressLine2() {
		return getElement().getAddressLine2();
	}

	public Country getCountry() {
		return getElement().getCountry();
	}

	public String getDwellingNumber() {
		return getElement().getDwellingNumber();
	}

	public Long getFederalBuildingId() {
		return getElement().getFederalBuildingId();
	}

	public String getForeignZipCode() {
		return getElement().getForeignZipCode();
	}

	public String getHouseNumber() {
		return getElement().getHouseNumber();
	}

	public String getLocality() {
		return getElement().getLocality();
	}

	public Long getPostOfficeBoxNumber() {
		return getElement().getPostOfficeBoxNumber();
	}

	public String getPostOfficeBoxText() {
		return getElement().getPostOfficeBoxText();
	}

	public String getStreet() {
		return getElement().getStreet();
	}

	public Long getSwissZipCode() {
		return getElement().getSwissZipCode();
	}

	public String getSwissZipCodeAddOn() {
		return getElement().getSwissZipCodeAddOn();
	}

	public Integer getSwissZipCodeId() {
		return getElement().getSwissZipCodeId();
	}

	public String getTown() {
		return getElement().getTown();
	}

	public Long getXCoordinate() {
		return getElement().getXCoordinate();
	}
}
