package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.ech.ech0011.v5.PlaceOfOrigin;

public class OrigineRCPers implements Origine, Serializable {

	private static final long serialVersionUID = 6092653856557033522L;

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
}
