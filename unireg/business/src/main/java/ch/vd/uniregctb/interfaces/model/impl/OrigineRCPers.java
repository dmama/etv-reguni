package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.ech.ech0011.v5.PlaceOfOrigin;

import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Pays;

public class OrigineRCPers implements Origine, Serializable {

	private static final long serialVersionUID = 6092653856557033522L;

	private String nomLieu;
	private String sigleCanton;

	public OrigineRCPers(PlaceOfOrigin placeOfOrigin) {
		this.nomLieu = placeOfOrigin.getOriginName();
		this.sigleCanton = (placeOfOrigin.getCanton() == null ? null :placeOfOrigin.getCanton().name());
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
	public Pays getPays() {
		return null;
	}
}
