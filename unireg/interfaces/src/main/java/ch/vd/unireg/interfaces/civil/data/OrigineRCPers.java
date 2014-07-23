package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.ech.ech0011.v5.PlaceOfOrigin;

public class OrigineRCPers implements Origine, Serializable {

	private static final long serialVersionUID = 8693728941213245492L;

	private String nomLieu;

	public OrigineRCPers(PlaceOfOrigin placeOfOrigin) {
		this.nomLieu = placeOfOrigin.getOriginName();
	}

	public static Origine get(PlaceOfOrigin placeOfOrigin) {
		if (placeOfOrigin == null) {
			return null;
		}
		return new OrigineRCPers(placeOfOrigin);
	}

	@Override
	public String getNomLieu() {
		return nomLieu;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final OrigineRCPers that = (OrigineRCPers) o;

		if (nomLieu != null ? !nomLieu.equals(that.nomLieu) : that.nomLieu != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return nomLieu != null ? nomLieu.hashCode() : 0;
	}
}
