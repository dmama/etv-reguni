package ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.EvenementPM;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementPMWrapper implements EvenementPM {

	private final RegDate date;
	private final Long numeroPM;
	private final String code;

	public static EvenementPMWrapper get(ch.vd.registre.pm.model.EvenementPM target) {
		if (target == null) {
			return null;
		}
		return new EvenementPMWrapper(target);
	}

	private EvenementPMWrapper(ch.vd.registre.pm.model.EvenementPM target) {
		this.date = RegDate.get(target.getDateEvenement());
		this.numeroPM = target.getNumeroPM();
		this.code = target.getCodeEvenement();
	}

	public RegDate getDate() {
		return date;
	}

	public Long getNumeroPM() {
		return numeroPM;
	}

	public String getCode() {
		return code;
	}
}
