package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.Source;
import ch.vd.uniregctb.tiers.Sourced;

public class FormeJuridiqueView implements Sourced<Source>, CollatableDateRange {

	private final Long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final FormeLegale type;
	private final Source source;

	public FormeJuridiqueView(FormeLegaleHisto forme) {
		this(forme.getId(), forme.getDateDebut(), forme.getDateFin(), forme.getFormeLegale(), forme.getSource());
	}

	public FormeJuridiqueView(Long id, RegDate dateDebut, RegDate dateFin, FormeLegale type, Source source) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.type = type;
		this.source = source;
	}

	public Long getId() {
		return id;
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
		if (next instanceof FormeJuridiqueView) {
			final FormeJuridiqueView nextFormeJuridique = (FormeJuridiqueView) next;
			return DateRangeHelper.isCollatable(this, next) && nextFormeJuridique.type == type
					&& nextFormeJuridique.source == source && nextFormeJuridique.id.equals(id);
		}
		return false;
	}

	@Override
	public FormeJuridiqueView collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Ranges non collatables!");
		}
		return new FormeJuridiqueView(id, dateDebut, next.getDateFin(), type, source);
	}

	@Override
	public Source getSource() {
		return source;
	}
}
