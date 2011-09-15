package ch.vd.uniregctb.rf;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Numéro d'identification d'un immeuble décomposé en ses sous-constituants.
 */
@Embeddable
public class NumeroImmeuble {

	private int noOfsCommune;
	private int noParcelle;
	private int noLot;
	private int noSousLot;

	public NumeroImmeuble() {
	}

	public NumeroImmeuble(int noOfsCommune, int noParcelle, int noLot, int noSousLot) {
		this.noOfsCommune = noOfsCommune;
		this.noParcelle = noParcelle;
		this.noLot = noLot;
		this.noSousLot = noSousLot;
	}

	@Column(name = "NO_OFS_COMMUNE", nullable = false)
	public int getNoOfsCommune() {
		return noOfsCommune;
	}

	public void setNoOfsCommune(int noOfsCommune) {
		this.noOfsCommune = noOfsCommune;
	}

	@Column(name = "NO_PARCELLE", nullable = false)
	public int getNoParcelle() {
		return noParcelle;
	}

	public void setNoParcelle(int noParcelle) {
		this.noParcelle = noParcelle;
	}

	@Column(name = "NO_LOT", nullable = false)
	public int getNoLot() {
		return noLot;
	}

	public void setNoLot(int noLot) {
		this.noLot = noLot;
	}

	@Column(name = "NO_SOUSLOT", nullable = false)
	public int getNoSousLot() {
		return noSousLot;
	}

	public void setNoSousLot(int noSousLot) {
		this.noSousLot = noSousLot;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final NumeroImmeuble that = (NumeroImmeuble) o;

		if (noLot != that.noLot) return false;
		if (noOfsCommune != that.noOfsCommune) return false;
		if (noParcelle != that.noParcelle) return false;
		if (noSousLot != that.noSousLot) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = noOfsCommune;
		result = 31 * result + noParcelle;
		result = 31 * result + noLot;
		result = 31 * result + noSousLot;
		return result;
	}

	@Override
	public String toString() {
		return String.format("%d/%d/%d/%d", noOfsCommune, noParcelle, noLot, noSousLot);
	}
}
