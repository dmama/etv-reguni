package ch.vd.uniregctb.registrefoncier.key;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.registrefoncier.AyantDroitRF;

/**
 * Clé d'identification au registre foncier d'un ayant-droit sur un immeuble (propriétaire, usufruitier, ...).
 */
public class AyantDroitRFKey {

	@NotNull
	private final String idRF;

	public AyantDroitRFKey(@NotNull AyantDroitRF ayantDroit) {
		this.idRF = ayantDroit.getIdRF();
	}

	public AyantDroitRFKey(@NotNull String idRF) {
		this.idRF = idRF;
	}

	@NotNull
	public String getIdRF() {
		return idRF;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final AyantDroitRFKey that = (AyantDroitRFKey) o;
		return Objects.equals(idRF, that.idRF);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idRF);
	}
}
