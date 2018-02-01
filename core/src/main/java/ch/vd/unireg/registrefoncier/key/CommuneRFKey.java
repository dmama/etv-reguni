package ch.vd.unireg.registrefoncier.key;

import java.util.Objects;

public class CommuneRFKey {

	private final int noRF;

	public CommuneRFKey(int noRF) {
		this.noRF = noRF;
	}

	public int getNoRF() {
		return noRF;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final CommuneRFKey that = (CommuneRFKey) o;
		return noRF == that.noRF;
	}

	@Override
	public int hashCode() {
		return Objects.hash(noRF);
	}
}
