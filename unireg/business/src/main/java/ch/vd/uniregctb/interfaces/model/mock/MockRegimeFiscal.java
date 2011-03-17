package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockRegimeFiscal {

	private RegDate dateDebut;
	private RegDate dateFin;
	private String code;

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
