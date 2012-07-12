package ch.vd.uniregctb.tiers.timeline;

import ch.vd.registre.base.date.DateRange;

/**
 * Représente une cellule dans la table
 */
public class TimelineCell {

	public static final TimelineCell FILLER = new TimelineCell(true, false);
	public static final TimelineCell SPAN = new TimelineCell(false, true);

	public final boolean filler; // si vrai, la cellule est vide
	public final boolean span; // si vrai, la cellule est "remplie" par le span d'un range

	public int longueurAffichage;
	public final DateRange range;

	TimelineCell(boolean filler, boolean span) {
		this.filler = filler;
		this.span = span;
		this.range = null;
		this.longueurAffichage = -1;
	}

	public TimelineCell(DateRange range) {
		this.filler = false;
		this.span = false;
		this.range = range;
		this.longueurAffichage = 0;
	}

	public boolean isFiller() {
		return filler;
	}

	public boolean isSpan() {
		return span;
	}

	public int getLongueurAffichage() {
		return longueurAffichage;
	}

	public DateRange getRange() {
		return range;
	}
}
