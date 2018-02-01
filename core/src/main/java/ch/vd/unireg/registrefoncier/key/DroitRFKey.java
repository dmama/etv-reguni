package ch.vd.unireg.registrefoncier.key;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.registrefoncier.DroitRF;

/**
 * La clé d'identification unique d'un droit RF.
 */
public class DroitRFKey {

	@NotNull
	private final String masterIdRF;
	@NotNull
	private final String versionIdRF;

	public DroitRFKey(@NotNull DroitRF droit) {
		this.masterIdRF = droit.getMasterIdRF();
		this.versionIdRF = droit.getVersionIdRF();
	}

	public DroitRFKey(@NotNull String masterIdRF, @NotNull String versionIdRF) {
		this.masterIdRF = masterIdRF;
		this.versionIdRF = versionIdRF;
	}

	@NotNull
	public String getMasterIdRF() {
		return masterIdRF;
	}

	@NotNull
	public String getVersionIdRF() {
		return versionIdRF;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final DroitRFKey that = (DroitRFKey) o;
		return Objects.equals(masterIdRF, that.masterIdRF) &&
				Objects.equals(versionIdRF, that.versionIdRF);
	}

	@Override
	public int hashCode() {
		return Objects.hash(masterIdRF, versionIdRF);
	}
}
