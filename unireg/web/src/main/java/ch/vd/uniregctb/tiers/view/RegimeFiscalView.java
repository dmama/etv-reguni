package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

public class RegimeFiscalView implements DateRange {

	private final Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeRegimeFiscal type;

	public RegimeFiscalView(Long id, RegDate dateDebut, RegDate dateFin, TypeRegimeFiscal type) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.type = type;
	}

	public Long getId() {
		return id;
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

	public TypeRegimeFiscal getType() {
		return type;
	}

	public void setType(TypeRegimeFiscal type) {
		this.type = type;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
