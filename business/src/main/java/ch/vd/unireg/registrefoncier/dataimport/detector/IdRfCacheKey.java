package ch.vd.unireg.registrefoncier.dataimport.detector;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.cache.ObjectKey;

/**
 * Classe qui permet d'adapter un ID du registre foncier (= une String) à une clé utilisable avec un cache persistent Unireg.
 */
public class IdRfCacheKey implements ObjectKey {

	private static final long serialVersionUID = 261902516311503497L;

	@NotNull
	private final String idRF;

	public IdRfCacheKey(@NotNull String idRF) {
		this.idRF = idRF;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final IdRfCacheKey idRfCacheKey = (IdRfCacheKey) o;
		return Objects.equals(idRF, idRfCacheKey.idRF);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idRF);
	}

	@NotNull
	public String getIdRF() {
		return idRF;
	}

	@Override
	public long getId() {
		return 0;
	}

	@Override
	public String getComplement() {
		return idRF;
	}
}
