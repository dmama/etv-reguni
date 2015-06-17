package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

public class LieuDeResidence {

	@NotNull
	private String placeOfResidenceName;

	/**
	 * municipalityId, si disponible
	 */
	private Integer commune;

	/**
	 * countryId, si disponible
	 */
	private Integer pays;


	public LieuDeResidence(@NotNull String placeOfResidenceName, Integer commune, Integer pays) {
		this.placeOfResidenceName = placeOfResidenceName;
		this.commune = commune;
		this.pays = pays;
	}

	public Integer getCommune() {
		return commune;
	}

	public Integer getPays() {
		return pays;
	}

	@NotNull
	public String getPlaceOfResidenceName() {
		return placeOfResidenceName;
	}
}
