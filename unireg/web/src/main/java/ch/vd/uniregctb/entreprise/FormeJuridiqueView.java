package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;

public class FormeJuridiqueView implements CollatableDateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final FormeLegale type;

	public FormeJuridiqueView(DateRanged<FormeLegale> forme) {
		this(forme.getDateDebut(), forme.getDateFin(), forme.getPayload());
	}

	public FormeJuridiqueView(RegDate dateDebut, RegDate dateFin, FormeLegale type) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.type = type;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public FormeLegale getType() {
		return type;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public boolean isCollatable(DateRange next) {
		return DateRangeHelper.isCollatable(this, next) && next instanceof FormeJuridiqueView && ((FormeJuridiqueView) next).type == type;
	}

	@Override
	public FormeJuridiqueView collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Ranges non collatables!");
		}
		return new FormeJuridiqueView(dateDebut, next.getDateFin(), type);
	}
}
