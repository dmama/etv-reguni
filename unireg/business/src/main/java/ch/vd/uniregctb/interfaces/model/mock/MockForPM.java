package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.ForPM;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockForPM implements ForPM {

	private RegDate dateDebut;
	private RegDate dateFin;
	private int noOfsAutoriteFiscale;
	private TypeNoOfs typeAutoriteFiscale;

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

	public int getNoOfsAutoriteFiscale() {
		return noOfsAutoriteFiscale;
	}

	public void setNoOfsAutoriteFiscale(int noOfsAutoriteFiscale) {
		this.noOfsAutoriteFiscale = noOfsAutoriteFiscale;
	}

	public TypeNoOfs getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeNoOfs typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}
}
