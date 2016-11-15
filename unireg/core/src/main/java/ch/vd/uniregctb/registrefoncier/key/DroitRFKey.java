package ch.vd.uniregctb.registrefoncier.key;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class DroitRFKey {

	@NotNull
	private final String masterIdRF;

	public DroitRFKey(@NotNull String masterIdRF) {
		this.masterIdRF = masterIdRF;
	}

	@NotNull
	public String getMasterIdRF() {
		return masterIdRF;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final DroitRFKey that = (DroitRFKey) o;
		return Objects.equals(masterIdRF, that.masterIdRF);
	}

	@Override
	public int hashCode() {
		return Objects.hash(masterIdRF);
	}
}
