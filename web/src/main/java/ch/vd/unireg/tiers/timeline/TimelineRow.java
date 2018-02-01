package ch.vd.unireg.tiers.timeline;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRange;

/**
 * Représente une ligne dans la table
 */
public class TimelineRow {

	public final TimelineRange periode;
	public TimelineCell forPrincipal;
	public final List<TimelineCell> forsSecondaires = new ArrayList<>(1);
	public TimelineCell forGestion;
	public TimelineCell assujettissementSource;
	public TimelineCell assujettissementRole;
	public TimelineCell assujettissement;
	public TimelineCell periodeImposition;
	public TimelineCell periodeImpositionIS;

	public TimelineRow(TimelineRange periode) {
		this.periode = periode;
		this.forPrincipal = TimelineCell.FILLER;
		this.forsSecondaires.add(TimelineCell.FILLER);
		this.forGestion = TimelineCell.FILLER;
		this.assujettissementSource = TimelineCell.FILLER;
		this.assujettissementRole = TimelineCell.FILLER;
		this.assujettissement = TimelineCell.FILLER;
		this.periodeImposition = TimelineCell.FILLER;
		this.periodeImpositionIS = TimelineCell.FILLER;
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

	public TimelineCell getAssujettissementSource() {
		return assujettissementSource;
	}

	public TimelineCell getAssujettissementRole() {
		return assujettissementRole;
	}

	public TimelineCell getAssujettissement() {
		return assujettissement;
	}

	public TimelineCell getPeriodeImposition() {
		return periodeImposition;
	}

	public TimelineCell getPeriodeImpositionIS() {
		return periodeImpositionIS;
	}
}
