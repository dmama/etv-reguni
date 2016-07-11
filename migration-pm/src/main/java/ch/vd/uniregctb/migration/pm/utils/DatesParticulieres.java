package ch.vd.uniregctb.migration.pm.utils;

import ch.vd.registre.base.date.RegDate;

/**
 * Bean Spring qui rassemble les dates particulières utilisées pendant la migration
 */
public class DatesParticulieres {

	private final RegDate seuilActivite;
	private final RegDate seuilDateNormale;
	private final RegDate seuilRepriseMandats;

	public DatesParticulieres(RegDate seuilActivite, RegDate seuilDateNormale, RegDate seuilRepriseMandats) {
		this.seuilActivite = seuilActivite;
		this.seuilDateNormale = seuilDateNormale;
		this.seuilRepriseMandats = seuilRepriseMandats;
	}

	public RegDate getSeuilActivite() {
		return seuilActivite;
	}

	public RegDate getSeuilDateNormale() {
		return seuilDateNormale;
	}

	public RegDate getSeuilRepriseMandats() {
		return seuilRepriseMandats;
	}
}
