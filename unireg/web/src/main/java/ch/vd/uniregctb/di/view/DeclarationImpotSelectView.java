package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;

/**
 * Form backing object pour l'écran de sélection de la période dans le cas de la création d'une nouvelle déclaration.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DeclarationImpotSelectView {

	public static class ViewRange {
		private final DateRange range;
		/**
		 * <b>vrai</b> s'il s'agit d'une déclaration qui peut être émise sur demande, mais n'est pas obligatoire (cas du contribuable
		 * hors-Suisse et possédant un immeuble dans le canton -> seule la première DI est obligatoire, les suivantes peuvent être émises à la
		 * demande).
		 */
		private final boolean optionnelle;

		public ViewRange(DateRange range, boolean optionnelle) {
			this.range = range;
			this.optionnelle = optionnelle;
		}

		public String getId() {
			return String.format("%d-%d", range.getDateDebut().index(), range.getDateFin().index());
		}

		public String getDescription() {
			final RegDate debut = range.getDateDebut();
			final RegDate fin = range.getDateFin();
			if (debut.month() == 1 && debut.day() == 1 && fin.month() == 12 && fin.day() == 31) {
				return "Année " + debut.year() + " complète";
			}
			else {
				return "Période du " + RegDateHelper.dateToDisplayString(debut) + " au " + RegDateHelper.dateToDisplayString(fin);
			}
		}

		public boolean isOptionnelle() {
			return optionnelle;
		}
	}

	private TiersGeneralView contribuable;

	private List<ViewRange> ranges;

	public TiersGeneralView getContribuable() {
		return contribuable;
	}

	public void setContribuable(TiersGeneralView contribuable) {
		this.contribuable = contribuable;
	}

	public void setRangesFromDateRanges(List<PeriodeImposition> ranges) {
		this.ranges = new ArrayList<ViewRange>(ranges.size());
		for (PeriodeImposition r : ranges) {
			// [UNIREG-1742] dans certain cas, les déclarations sont remplacées par une note à l'administration fiscale de l'autre canton
			// [UNIREG-1742] les diplomates suisses ne reçoivent pas de déclaration 
			if (!r.isRemplaceeParNote() && !r.isDiplomateSuisse()) {
				this.ranges.add(new ViewRange(r, r.isOptionnelle()));
			}
		}
	}

	public List<ViewRange> getRanges() {
		return ranges;
	}

	public void setRanges(List<ViewRange> ranges) {
		this.ranges = ranges;
	}
}
