package ch.vd.uniregctb.migration.pm.engine.data;

import ch.vd.registre.base.date.RegDate;

public class RaisonSocialeData {

	private final String raisonSociale;
	private final RegDate dateValidite;

	public RaisonSocialeData(String raisonSociale, RegDate dateValidite) {
		this.raisonSociale = raisonSociale;
		this.dateValidite = dateValidite;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public RegDate getDateValidite() {
		return dateValidite;
	}
}
