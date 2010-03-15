package ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.EtatPM;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EtatPMWrapper implements EtatPM {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final String code;

	public static EtatPMWrapper get(ch.vd.registre.pm.model.EtatPM target) {
		if (target == null) {
			return null;
		}
		return new EtatPMWrapper(target);
	}

	public EtatPMWrapper(ch.vd.registre.pm.model.EtatPM target) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.code = target.getCode();
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public String getCode() {
		return code;
	}
}
