package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.uniregctb.interfaces.model.TuteurGeneral;

public class TuteurGeneralWrapper implements TuteurGeneral {

	private final ch.vd.registre.civil.model.TuteurGeneral target;

	public static TuteurGeneralWrapper get(ch.vd.registre.civil.model.TuteurGeneral target) {
		if (target == null) {
			return null;
		}
		return new TuteurGeneralWrapper(target);
	}

	private TuteurGeneralWrapper(ch.vd.registre.civil.model.TuteurGeneral target) {
		this.target = target;
	}

	public String getNoTelephoneContact() {
		return target.getNoTelephoneContact();
	}

	public String getNomContact() {
		return target.getNomContact();
	}

	public String getNomOffice() {
		return target.getNomOffice();
	}

	public String getPrenomContact() {
		return target.getPrenomContact();
	}

}
