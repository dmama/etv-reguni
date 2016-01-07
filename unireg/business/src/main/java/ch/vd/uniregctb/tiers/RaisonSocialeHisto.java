package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.Rerangeable;

/**
 * @author Raphaël Marmier, 2016-01-07, <raphael.marmier@vd.ch>
 */
public class RaisonSocialeHisto implements Sourced<Source>, CollatableDateRange, Duplicable<RaisonSocialeHisto>, Annulable, Rerangeable<RaisonSocialeHisto> {

	private final Long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final String raisonSociale;
	private final Source source;

	public RaisonSocialeHisto(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, String raisonSociale, Source source) {
		this.id = id;
		this.annule = annule;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.raisonSociale = raisonSociale;
		this.source = source;
	}

	public Long getId() {
		return id;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public boolean isCollatable(DateRange next) {
		boolean collatable = DateRangeHelper.isCollatable(this, next) && next instanceof RaisonSocialeHisto;
		if (collatable) {
			final RaisonSocialeHisto nextRaisonSocialeHisto = (RaisonSocialeHisto) next;
			collatable = nextRaisonSocialeHisto.raisonSociale.equals(raisonSociale)
					&& nextRaisonSocialeHisto.source == source
					&& nextRaisonSocialeHisto.annule == annule
					&& ((nextRaisonSocialeHisto.id == null && id == null) || (nextRaisonSocialeHisto.id != null && id != null && nextRaisonSocialeHisto.id.equals(id)));
		}
		return collatable;
	}

	@Override
	public DateRange collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Les ranges ne sont pas collatables...");
		}
		return new RaisonSocialeHisto(id, annule, dateDebut, next.getDateFin(), raisonSociale, source);
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return false;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public RaisonSocialeHisto duplicate() {
		return new RaisonSocialeHisto(id, annule, dateDebut, dateFin, raisonSociale, source);
	}

	@Override
	public RaisonSocialeHisto rerange(DateRange range) {
		return new RaisonSocialeHisto(id, annule, range.getDateDebut(), range.getDateFin(), raisonSociale, source);
	}

	@Override
	public Source getSource() {
		return source;
	}
}
