package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.EtatPM;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EtatPMImpl implements EtatPM {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final String code;

	public static EtatPMImpl get(ch.vd.registre.pm.model.EtatPM target) {
		if (target == null) {
			return null;
		}
		return new EtatPMImpl(target);
	}

	public EtatPMImpl(ch.vd.registre.pm.model.EtatPM target) {
		this.dateDebut = RegDateHelper.get(target.getDateDebut());
		this.dateFin = RegDateHelper.get(target.getDateFin());
		this.code = target.getCode();
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public String getCode() {
		return code;
	}
}
