package ch.vd.uniregctb.tiers.timeline;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRange;

/**
 * Représente une ligne dans la table
 */
public class TimelineRow {
	public final TimelineRange periode;
	public TimelineCell forPrincipal;
	public final List<TimelineCell> forsSecondaires = new ArrayList<TimelineCell>(1);
	public TimelineCell forGestion;
	public TimelineCell assujettissement;
	public TimelineCell periodeImposition;

	public TimelineRow(TimelineRange periode) {
		this.periode = periode;
		this.forPrincipal = TimelineCell.FILLER;
		this.forsSecondaires.add(TimelineCell.FILLER);
		this.forGestion = TimelineCell.FILLER;
		this.assujettissement = TimelineCell.FILLER;
		this.periodeImposition = TimelineCell.FILLER;
	}

	public DateRange getPeriode() {
		return periode;
	}

	public TimelineCell getForPrincipal() {
		return forPrincipal;
	}

	public List<TimelineCell> getForsSecondaires() {
		return forsSecondaires;
	}

	public TimelineCell getForGestion() {
		return forGestion;
	}

	public TimelineCell getAssujettissement() {
		return assujettissement;
	}

	public TimelineCell getPeriodeImposition() {
		return periodeImposition;
	}
}
