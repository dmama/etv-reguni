package ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.FormeJuridique;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class FormeJuridiqueWrapper implements FormeJuridique {

	private RegDate dateDebut;
	private RegDate dateFin;
	private String code;

	public static FormeJuridiqueWrapper get(ch.vd.registre.pm.model.FormeJuridique target) {
		if (target == null) {
			return null;
		}
		return new FormeJuridiqueWrapper(target);
	}


	private FormeJuridiqueWrapper(ch.vd.registre.pm.model.FormeJuridique target) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.code = target.getCode();
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
