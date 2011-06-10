package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.EvenementPM;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockEvenementPM implements EvenementPM {

	private RegDate date;
	private Long numeroPM;
	private String code;

	@Override
	public RegDate getDate() {
		return date;
	}

	public void setDate(RegDate date) {
		this.date = date;
	}

	@Override
	public Long getNumeroPM() {
		return numeroPM;
	}

	public void setNumeroPM(Long numeroPM) {
		this.numeroPM = numeroPM;
	}

	@Override
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
