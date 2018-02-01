package ch.vd.unireg.registrefoncier.key;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.registrefoncier.ImmeubleRF;

/**
 * Clé d'identification au registre foncier d'un immeuble.
 */
public class ImmeubleRFKey {

	@NotNull
	private final String idRF;

	public ImmeubleRFKey(@NotNull ImmeubleRF immeuble) {
		this.idRF = immeuble.getIdRF();
	}

	public ImmeubleRFKey(@NotNull String idRF) {
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
		final ImmeubleRFKey that = (ImmeubleRFKey) o;
		return Objects.equals(idRF, that.idRF);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idRF);
	}
}
