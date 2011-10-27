package ch.vd.uniregctb.webservices.party3.impl;

import java.util.Comparator;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;

/**
 * Comparateur qui permet de trier des fors fiscaux web dans l'ordre croissant.
 *
 * @see ch.vd.registre.base.date.DateRangeComparator
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public final class ForFiscalComparator implements Comparator<TaxResidence> {

	private final class ForFiscalWrapper implements DateRange {

		private final RegDate dateDebut;
		private final RegDate dateFin;

		public ForFiscalWrapper(TaxResidence f) {
			this.dateDebut = DataHelper.webToCore(f.getDateFrom());
			this.dateFin = DataHelper.webToCore(f.getDateTo());
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
	public int compare(TaxResidence o1, TaxResidence o2) {
		return DateRangeComparator.compareRanges(new ForFiscalWrapper(o1), new ForFiscalWrapper(o2));
	}
}
