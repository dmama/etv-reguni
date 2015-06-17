package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

public class Pays {

    private Integer countryId;
    private String countryIdISO2;
	@NotNull
    private String countryName;

	public Pays(Integer countryId, String countryIdISO2, @NotNull String countryName) {
		this.countryId = countryId;
		this.countryIdISO2 = countryIdISO2;
		this.countryName = countryName;
	}

	public Integer getCountryId() {
		return countryId;
	}

	public String getCountryIdISO2() {
		return countryIdISO2;
	}

	public String getCountryName() {
		return countryName;
	}
}
