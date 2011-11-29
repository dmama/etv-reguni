package ch.vd.uniregctb.webservices.tiers2.impl;

import java.util.Comparator;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal;

/**
 * Comparateur qui permet de trier des fors fiscaux web dans l'ordre croissant.
 *
 * @see DateRangeComparator
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public final class ForFiscalComparator implements Comparator<ForFiscal> {

	private static final class ForFiscalWrapper implements DateRange {

		private final RegDate dateDebut;
		private final RegDate dateFin;

		public ForFiscalWrapper(ForFiscal f) {
			this.dateDebut = RegDate.get(Date.asJavaDate(f.dateOuverture));
			this.dateFin = RegDate.get(Date.asJavaDate(f.dateFermeture));
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
		public boolean isValidAt(RegDate date) {
			return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
		}
	}

	@Override
	public int compare(ForFiscal o1, ForFiscal o2) {
		return DateRangeComparator.compareRanges(new ForFiscalWrapper(o1), new ForFiscalWrapper(o2));
	}
}
