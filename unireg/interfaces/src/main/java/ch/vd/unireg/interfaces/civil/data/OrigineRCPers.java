package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.ech.ech0011.v5.PlaceOfOrigin;

public class OrigineRCPers implements Origine, Serializable {

	private static final long serialVersionUID = 9205836493560510209L;

	private final String nomLieu;
	private final String sigleCanton;

	public OrigineRCPers(PlaceOfOrigin placeOfOrigin) {
		this.nomLieu = placeOfOrigin.getOriginName();
		this.sigleCanton = placeOfOrigin.getCanton().value();
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
	public String getSigleCanton() {
		return sigleCanton;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final OrigineRCPers that = (OrigineRCPers) o;

		if (nomLieu != null ? !nomLieu.equals(that.nomLieu) : that.nomLieu != null) return false;
		if (sigleCanton != null ? !sigleCanton.equals(that.sigleCanton) : that.sigleCanton != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = nomLieu != null ? nomLieu.hashCode() : 0;
		result = 31 * result + (sigleCanton != null ? sigleCanton.hashCode() : 0);
		return result;
	}
}
