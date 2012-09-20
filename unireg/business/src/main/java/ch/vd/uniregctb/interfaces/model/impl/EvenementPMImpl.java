package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.EvenementPM;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementPMImpl implements EvenementPM {

	private final RegDate date;
	private final Long numeroPM;
	private final String code;

	public static EvenementPMImpl get(ch.vd.registre.pm.model.EvenementPM target) {
		if (target == null) {
			return null;
		}
		return new EvenementPMImpl(target);
	}

	private EvenementPMImpl(ch.vd.registre.pm.model.EvenementPM target) {
		this.date = RegDate.get(target.getDateEvenement());
		this.numeroPM = target.getNumeroPM();
		this.code = target.getCodeEvenement();
	}

	@Override
	public RegDate getDate() {
		return date;
	}

	@Override
	public Long getNumeroPM() {
		return numeroPM;
	}

	@Override
	public String getCode() {
		return code;
	}
}
