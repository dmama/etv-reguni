package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.FormeJuridique;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class FormeJuridiqueImpl implements FormeJuridique {

	private RegDate dateDebut;
	private RegDate dateFin;
	private String code;

	public static FormeJuridiqueImpl get(ch.vd.registre.pm.model.FormeJuridique target) {
		if (target == null) {
			return null;
		}
		return new FormeJuridiqueImpl(target);
	}


	private FormeJuridiqueImpl(ch.vd.registre.pm.model.FormeJuridique target) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.code = target.getCode();
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
