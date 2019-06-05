package ch.vd.unireg.registrefoncier.key;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.registrefoncier.CommuneRF;

/**
 * Les informations d'identification unique d'une commune.
 */
public class CommuneRFKey {

	@Nullable
	private final Integer noRF;
	@Nullable
	private final Integer noOfs;

	public CommuneRFKey(int no) {
		if (no <= 0) {
			throw new IllegalArgumentException("Le numéro doit être positif");
		}
		final CommuneNoType type = CommuneNoType.detect(no);
		this.noRF = (type == CommuneNoType.RF ? no : null);
		this.noOfs = (type == CommuneNoType.OFS ? no : null);
	}

	public CommuneRFKey(@NotNull CommuneRF commune) {
		this.noRF = (commune.getNoRf() <= 0 ? null : commune.getNoRf());
		this.noOfs = (commune.getNoOfs() <= 0 ? null : commune.getNoOfs());
		if (this.noRF == null && this.noOfs == null) {
			throw new IllegalArgumentException("Les deux numéros sont nuls");
		}
	}

	/**
	 * @return le numéro RF de la commune
	 */
	@Nullable
	public Integer getNoRF() {
		return noRF;
	}

	/**
	 * @return le numéro RF de la commune
	 */
	@Nullable
	public Integer getNoOfs() {
		return noOfs;
	}

	@Override
	public String toString() {
		return "CommuneRFKey{" +
				"noRF=" + noRF +
				", noOfs=" + noOfs +
				'}';
	}
}
