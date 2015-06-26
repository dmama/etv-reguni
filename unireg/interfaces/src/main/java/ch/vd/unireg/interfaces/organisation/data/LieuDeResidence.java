package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

public class LieuDeResidence {

	@NotNull
	private String nomDuLieuResidence;

	/**
	 * municipalityId, si disponible
	 */
	private Integer commune;

	/**
	 * countryId, si disponible
	 */
	private Integer pays;


	public LieuDeResidence(@NotNull String nomDuLieuResidence, Integer commune, Integer pays) {
		this.nomDuLieuResidence = nomDuLieuResidence;
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
	public String getNomDuLieuResidence() {
		return nomDuLieuResidence;
	}
}
