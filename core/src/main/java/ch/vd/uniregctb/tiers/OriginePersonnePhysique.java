package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.LengthConstants;

@Embeddable
public class OriginePersonnePhysique {

	public OriginePersonnePhysique() {
	}

	public OriginePersonnePhysique(@NotNull String libelle, @NotNull String sigleCanton) {
		this.libelle = libelle;
		this.sigleCanton = sigleCanton;
	}

	/**
	 * Libellé du lieu d'origine de la personne physique (<i>a priori</i> une commune...)
	 */
	private String libelle;

	/**
	 * Sigle du canton dans lequel se trouve le lieu d'origine
	 */
	private String sigleCanton;

	@Column(name = "LIBELLE", length = LengthConstants.TIERS_LIBELLE_ORIGINE, nullable = false)
	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}

	@Column(name = "CANTON", length = LengthConstants.TIERS_CANTON_ORIGINE, nullable = false)
	public String getSigleCanton() {
		return sigleCanton;
	}

	public void setSigleCanton(String sigleCanton) {
		this.sigleCanton = sigleCanton;
	}

	@Transient
	public String getLibelleAvecCanton() {
		final String cantonalPart = String.format("(%s)", sigleCanton);
		if (libelle.endsWith(cantonalPart)) {
			return libelle;
		}
		else {
			return String.format("%s %s", libelle, cantonalPart);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final OriginePersonnePhysique that = (OriginePersonnePhysique) o;
		return libelle.equals(that.libelle) && sigleCanton.equals(that.sigleCanton);
	}

	@Override
	public int hashCode() {
		int result = libelle.hashCode();
		result = 31 * result + sigleCanton.hashCode();
		return result;
	}
}
