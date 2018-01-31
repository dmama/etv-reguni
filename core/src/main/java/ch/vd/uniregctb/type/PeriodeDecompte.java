package ch.vd.uniregctb.type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

public enum PeriodeDecompte {
	M01 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precAnneeFin = current.year();
			RegDate nouvDateDebut = RegDate.get(precAnneeFin + 1, 1, 1);
			RegDate nouvDateFin = RegDate.get(precAnneeFin + 1, 1, 31);
			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}
		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 1, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 1, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);

		}},
	M02 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;
			if (precMoisFin >= 2) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 2, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 3, 1);
				nouvDateFin = nouvDateFin.addDays(-1);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 2, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 3, 1);
				nouvDateFin = nouvDateFin.addDays(-1);
			}
			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);
		}
		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 2, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 3, 1);
			dateFin = dateFin.addDays(-1);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}
	},
	M03 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 3) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 3, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 3, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 3, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 3, 31);
			}
			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);
		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 3, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 3, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}
	},
	M04 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 4) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 4, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 4, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 4, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 4, 30);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 4, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 4, 30);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	M05 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 5) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 5, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 5, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 5, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 5, 31);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}
		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 5, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 5, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	M06 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 6) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 6, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 6, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 6, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 6, 30);
			}
			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 6, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 6, 30);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	M07 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 7) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 7, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 7, 31);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 7, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 7, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	M08 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 8) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 8, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 8, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 8, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 8, 31);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 8, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 8, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	M09 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {

			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 9) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 9, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 9, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 9, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 9, 30);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 9, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 9, 30);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	M10 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;


			if (precMoisFin >= 10) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 10, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 10, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 10, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 10, 31);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 10, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 10, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	M11 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {

			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 11) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 11, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 11, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 11, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 11, 30);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 11, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 11, 30);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	M12 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin == 12) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 12, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 12, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 12, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 12, 31);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 12, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 12, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	T1 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {

			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			nouvDateDebut = RegDate.get(precAnneeFin + 1, 1, 1);
			nouvDateFin = RegDate.get(precAnneeFin + 1, 3, 31);

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 1, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 3, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	T2 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 3) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 4, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 6, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 4, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 6, 30);
			}
			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 4, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 6, 30);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	T3 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {

			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 6) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 9, 30);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 9, 30);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 7, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 9, 30);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	T4 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {

			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 9) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 10, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 12, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 10, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 12, 31);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 10, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 12, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	S1 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {

			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			nouvDateDebut = RegDate.get(precAnneeFin + 1, 1, 1);
			nouvDateFin = RegDate.get(precAnneeFin + 1, 6, 30);

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 1, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 6, 30);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	S2 {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {
			int precMoisFin = current.month();
			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			if (precMoisFin >= 6) {
				nouvDateDebut = RegDate.get(precAnneeFin + 1, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin + 1, 12, 31);
			}
			else {
				nouvDateDebut = RegDate.get(precAnneeFin, 7, 1);
				nouvDateFin = RegDate.get(precAnneeFin, 12, 31);
			}

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);

		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 7, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 12, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}},
	A {
		@Override
		public DateRange getPeriodeSuivante(RegDate current) {

			int precAnneeFin = current.year();

			RegDate nouvDateDebut = null;
			RegDate nouvDateFin = null;

			nouvDateDebut = RegDate.get(precAnneeFin + 1, 1, 1);
			nouvDateFin = RegDate.get(precAnneeFin + 1, 12, 31);

			return new DateRangeHelper.Range(nouvDateDebut, nouvDateFin);
		}

		@Override
		public DateRange getPeriodeCourante(RegDate current) {
			int anneeCourante = current.year();
			RegDate dateDebut = RegDate.get(anneeCourante, 1, 1);
			RegDate dateFin = RegDate.get(anneeCourante, 12, 31);
			return new DateRangeHelper.Range(dateDebut, dateFin);
		}};

	public abstract DateRange getPeriodeSuivante(RegDate current);

	public abstract DateRange getPeriodeCourante(RegDate current);
}