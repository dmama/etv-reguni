package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
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

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
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
	public int getNoOfsAutoriteFiscale() {
		return noOfsAutoriteFiscale;
	}

	public void setNoOfsAutoriteFiscale(int noOfsAutoriteFiscale) {
		this.noOfsAutoriteFiscale = noOfsAutoriteFiscale;
	}

	@Override
	public TypeNoOfs getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeNoOfs typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}
}
