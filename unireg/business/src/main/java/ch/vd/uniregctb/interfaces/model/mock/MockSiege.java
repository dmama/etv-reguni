package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Siege;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockSiege implements Siege {

	private RegDate dateDebut;
	private RegDate dateFin;
	private int noOfsSiege;
	private TypeNoOfs type;

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
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
	public int getNoOfsSiege() {
		return noOfsSiege;
	}

	public void setNoOfsSiege(int noOfsSiege) {
		this.noOfsSiege = noOfsSiege;
	}

	@Override
	public TypeNoOfs getType() {
		return type;
	}

	public void setType(TypeNoOfs type) {
		this.type = type;
	}
}
