package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;

public class SiegeView implements DateRange {

	private RegDate dateDebut;
	private RegDate dateFin;
	private int noOfsSiege;
	private TypeNoOfs type;

	public SiegeView() {
	}

	public SiegeView(DateRanged<Integer> siege, TypeNoOfs type) {
		this.dateDebut = siege.getDateDebut();
		this.dateFin = siege.getDateFin();
		this.noOfsSiege = siege.getPayload();
		this.type = type;
	}

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

	public int getNoOfsSiege() {
		return noOfsSiege;
	}

	public void setNoOfsSiege(int noOfsSiege) {
		this.noOfsSiege = noOfsSiege;
	}

	public TypeNoOfs getType() {
		return type;
	}

	public void setType(TypeNoOfs type) {
		this.type = type;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
