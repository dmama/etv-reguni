package ch.vd.uniregctb.registrefoncier.key;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;

public class SurfaceAuSolRFKey {

	@NotNull
	private final String idRF;
	@NotNull
	private final String type;
	private final int surface;

	public SurfaceAuSolRFKey(@NotNull String idRF, @NotNull String type, int surface) {
		this.idRF = idRF;
		this.type = type;
		this.surface = surface;
	}

	public SurfaceAuSolRFKey(@NotNull SurfaceAuSolRF s) {
		this.idRF = s.getImmeuble().getIdRF();
		this.type = s.getType();
		this.surface = s.getSurface();
	}

	@NotNull
	public String getIdRF() {
		return idRF;
	}

	@NotNull
	public String getType() {
		return type;
	}

	public int getSurface() {
		return surface;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final SurfaceAuSolRFKey that = (SurfaceAuSolRFKey) o;
		return surface == that.surface &&
				Objects.equals(idRF, that.idRF) &&
				Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idRF, type, surface);
	}
}
