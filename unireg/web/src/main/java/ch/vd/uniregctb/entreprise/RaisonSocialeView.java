package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.RaisonSocialeHisto;
import ch.vd.uniregctb.tiers.Source;
import ch.vd.uniregctb.tiers.Sourced;

public class RaisonSocialeView implements Sourced<Source>, CollatableDateRange {

	private final Long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final String raisonSociale;
	private final Source source;

	public RaisonSocialeView(RaisonSocialeHisto forme) {
		this(forme.getId(), forme.getDateDebut(), forme.getDateFin(), forme.getRaisonSociale(), forme.getSource());
	}

	public RaisonSocialeView(Long id, RegDate dateDebut, RegDate dateFin, String raisonSociale, Source source) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.raisonSociale = raisonSociale;
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

	public String getRaisonSociale() {
		return raisonSociale;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public boolean isCollatable(DateRange next) {
		if (next instanceof RaisonSocialeView) {
			final RaisonSocialeView nextFormeJuridique = (RaisonSocialeView) next;
			return DateRangeHelper.isCollatable(this, next) && nextFormeJuridique.raisonSociale.equals(raisonSociale)
					&& nextFormeJuridique.source == source && nextFormeJuridique.id.equals(id);
		}
		return false;
	}

	@Override
	public RaisonSocialeView collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Ranges non collatables!");
		}
		return new RaisonSocialeView(id, dateDebut, next.getDateFin(), raisonSociale, source);
	}

	@Override
	public Source getSource() {
		return source;
	}
}
