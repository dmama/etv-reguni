package ch.vd.unireg.tiers.timeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Représente la table contenant la "timeline"
 */
@SuppressWarnings("UnusedDeclaration")
public class TimelineTable {

	public final boolean invertedTime;
	public final RegDate bigBang;
	public final List<TimelineRow> rows = new ArrayList<>();

	/**
	 * @param invertedTime <code>true</code> si les {@link #rows} doivent être affichées de la plus récente à la plus vieille (anti-chronologique), et <code>false</code> pour l'ordre "naturel" (chronologique)
	 * @param bigBang première date de visualisation des données (1.1.2003 en principe, date de passage à la taxation annuelle pré-numerando)
	 */
	public TimelineTable(boolean invertedTime, RegDate bigBang) {
		this.invertedTime = invertedTime;
		this.bigBang = bigBang;
	}

	public List<TimelineRow> getRows() {
		return rows;
	}

	public int getForsSecondairesSize() {
		if (rows.isEmpty()) {
			return 1;
		}
		else {
			return rows.get(0).forsSecondaires.size();
		}
	}

	/**
	 * Renseigne l'axe du temps (= les périodes minimales)
	 */
	public void setPeriodes(List<DateRange> periodes) {
		rows.clear();
		for (DateRange p : periodes) {
			rows.add(new TimelineRow(new TimelineRange(p)));
		}

		// tri des lignes selon l'ordre qui va bien
		rows.sort(new Comparator<TimelineRow>() {
			@Override
			public int compare(TimelineRow o1, TimelineRow o2) {
				return DateRangeComparator.compareRanges(o1.getPeriode(), o2.getPeriode()) * (invertedTime ? -1 : 1);
			}
		});

		// on calcule les yearspans
		int year = -1;
		TimelineRange head = null;
		for (TimelineRow row : rows) {
			TimelineRange current = row.periode;
			if (current.isMultiYears()) { // on ne spanne pas les ranges qui s'étalent sur plusieurs années
				year = -1;
				head = null;
			}
			else {
				if (head == null || current.getDateDebut().year() != year) {
					year = current.getDateDebut().year();
					head = current;
				}
				else {
					head.incYearspan();
					current.setYearSpan(0);
				}
			}
		}
	}

	private static boolean isSameEndDate(TimelineRow row, DateRange range) {
		return range.getDateFin() == row.periode.getDateFin();
	}

	private static boolean isSameBeginDate(TimelineRow row, DateRange range, RegDate bigBang) {
		return range.getDateDebut() == row.periode.getDateDebut() || (row.periode.getDateDebut() == bigBang && RegDateHelper.isBeforeOrEqual(range.getDateDebut(), bigBang, NullDateBehavior.EARLIEST));
	}

	private boolean isFirstRowForRange(TimelineRow row, DateRange range, RegDate bigBang) {
		return invertedTime ? isSameEndDate(row, range) : isSameBeginDate(row, range, bigBang);
	}

	private boolean isLastRowForRange(TimelineRow row, DateRange range, RegDate bigBang) {
		return invertedTime ? isSameBeginDate(row, range, bigBang) : isSameEndDate(row, range);
	}

	/**
	 * Ajoute un range dans la colonne "fors principaux"
	 */
	public void addForPrincipal(DateRange range) {
		TimelineCell c = new TimelineCell(range);
		int longueur = 0;
		for (TimelineRow r : rows) {
			if (isFirstRowForRange(r, range, bigBang)) {
				Assert.isTrue(r.forPrincipal == TimelineCell.FILLER);
				r.forPrincipal = c;
				longueur++;
			}
			else if (DateRangeHelper.within(r.periode, range)) {
				Assert.isTrue(r.forPrincipal == TimelineCell.FILLER);
				r.forPrincipal = TimelineCell.SPAN;
				longueur++;
			}
		}
		c.longueurAffichage = longueur;
	}

	/**
	 * Ajoute un range dans la colonne "fors gestion"
	 */
	public void addForGestion(DateRange range) {
		TimelineCell c = new TimelineCell(range);
		int longueur = 0;
		for (TimelineRow r : rows) {
			if (isFirstRowForRange(r, range, bigBang)) {
				Assert.isTrue(r.forGestion == TimelineCell.FILLER);
				r.forGestion = c;
				longueur++;
			}
			else if (DateRangeHelper.within(r.periode, range)) {
				Assert.isTrue(r.forGestion == TimelineCell.FILLER);
				r.forGestion = TimelineCell.SPAN;
				longueur++;
			}
		}
		c.longueurAffichage = longueur;
	}

	/**
	 * Ajoute un range dans la colonne "assujettissements source"
	 */
	public void addAssujettissementSource(DateRange range) {
		TimelineCell c = new TimelineCell(range);
		int longueur = 0;
		for (TimelineRow r : rows) {
			if (isFirstRowForRange(r, range, bigBang)) {
				Assert.isTrue(r.assujettissementSource == TimelineCell.FILLER);
				r.assujettissementSource = c;
				longueur++;
			}
			else if (DateRangeHelper.within(r.periode, range)) {
				Assert.isTrue(r.assujettissementSource == TimelineCell.FILLER);
				r.assujettissementSource = TimelineCell.SPAN;
				longueur++;
			}
		}
		c.longueurAffichage = longueur;
	}

	/**
	 * Ajoute un range dans la colonne "assujettissements rôle"
	 */
	public void addAssujettissementRole(DateRange range) {
		TimelineCell c = new TimelineCell(range);
		int longueur = 0;
		for (TimelineRow r : rows) {
			if (isFirstRowForRange(r, range, bigBang)) {
				Assert.isTrue(r.assujettissementRole == TimelineCell.FILLER);
				r.assujettissementRole = c;
				longueur++;
			}
			else if (DateRangeHelper.within(r.periode, range)) {
				Assert.isTrue(r.assujettissementRole == TimelineCell.FILLER);
				r.assujettissementRole = TimelineCell.SPAN;
				longueur++;
			}
		}
		c.longueurAffichage = longueur;
	}

	/**
	 * Ajoute un range dans la colonne "assujettissements"
	 */
	public void addAssujettissement(DateRange range) {
		TimelineCell c = new TimelineCell(range);
		int longueur = 0;
		for (TimelineRow r : rows) {
			if (isFirstRowForRange(r, range, bigBang)) {
				Assert.isTrue(r.assujettissement == TimelineCell.FILLER);
				r.assujettissement = c;
				longueur++;
			}
			else if (DateRangeHelper.within(r.periode, range)) {
				Assert.isTrue(r.assujettissement == TimelineCell.FILLER);
				r.assujettissement = TimelineCell.SPAN;
				longueur++;
			}
		}
		c.longueurAffichage = longueur;
	}

	/**
	 * Ajoute un range dans la colonne "périodes d'imposition"
	 */
	public void addPeriodeImposition(DateRange range) {
		TimelineCell c = new TimelineCell(range);
		int longueur = 0;
		for (TimelineRow r : rows) {
			if (isFirstRowForRange(r, range, bigBang)) {
				Assert.isTrue(r.periodeImposition == TimelineCell.FILLER);
				r.periodeImposition = c;
				longueur++;
			}
			else if (DateRangeHelper.within(r.periode, range)) {
				Assert.isTrue(r.periodeImposition == TimelineCell.FILLER);
				r.periodeImposition = TimelineCell.SPAN;
				longueur++;
			}
		}
		c.longueurAffichage = longueur;
	}

	public void addPeriodeImpositionIS(DateRange range) {
		TimelineCell c = new TimelineCell(range);
		int longueur = 0;
		for (TimelineRow r : rows) {
			if (isFirstRowForRange(r, range, bigBang)) {
				Assert.isTrue(r.periodeImpositionIS == TimelineCell.FILLER);
				r.periodeImpositionIS = c;
				longueur++;
			}
			else if (DateRangeHelper.within(r.periode, range)) {
				Assert.isTrue(r.periodeImpositionIS == TimelineCell.FILLER);
				r.periodeImpositionIS = TimelineCell.SPAN;
				longueur++;
			}
		}
		c.longueurAffichage = longueur;
	}

	/**
	 * Ajoute un range dans la colonne "fors secondaires". Note: il est possible d'avoir plusieurs fors secondaires valides pour une
	 * période donnée.
	 */
	public void addForSecondaire(DateRange range) {

		// recherche de l'emplacement
		int debut = -1;
		int fin = -1;
		int levels = 0;
		for (int i = 0; i < rows.size(); ++i) {
			TimelineRow r = rows.get(i);
			levels = r.forsSecondaires.size();
			if (isFirstRowForRange(r, range, bigBang)) {
				debut = i;
				fin = i;
			}
			else if (isLastRowForRange(r, range, bigBang)) {
				fin = i;
				break;
			}
		}

		// recherche du niveau où placer le for
		int level = 0;
		boolean collision = false;
		for (; level < levels; ++level) {
			collision = false;
			for (int i = debut; i <= fin; ++i) {
				TimelineRow r = rows.get(i);
				if (r.forsSecondaires.get(level) != TimelineCell.FILLER) {
					collision = true;
					break;
				}
			}
			if (!collision) {
				break;
			}
		}

		// ajout d'un niveau supplémentaire si nécessaire
		if (collision) {
			addForSecondaireLevel();
		}

		// ajout du for
		TimelineCell c = new TimelineCell(range);

		for (int i = debut; i <= fin; ++i) {
			TimelineRow r = rows.get(i);
			Assert.isTrue(r.forsSecondaires.get(level) == TimelineCell.FILLER);
			if (i == debut) {
				r.forsSecondaires.set(level, c);
			}
			else {
				r.forsSecondaires.set(level, TimelineCell.SPAN);
			}
		}

		c.longueurAffichage = fin - debut + 1;
	}

	/**
	 * Ajoute un niveau à tous les collections contenant les fors secondaires
	 */
	private void addForSecondaireLevel() {
		for (TimelineRow r : rows) {
			r.forsSecondaires.add(TimelineCell.FILLER);
		}
	}
}
