/**
 *
 */
package ch.vd.uniregctb.type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 *
 * @author xsifnr
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uw0AAM28Ed2O8ZP7tr6cjA"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uw0AAM28Ed2O8ZP7tr6cjA"
 */
public enum PeriodeDecompte {
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_1L6bMM28Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3u5VUM28Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_47-McM28Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_5_fe8M28Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7lE8sM28Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_8qr54M28Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_98_2UM28Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#__Je3kM28Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_AiOMIM29Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_CIZfwM29Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_D2fZQM29Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_E66DoM29Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_GBROwM29Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_G-qrIM29Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_H4xi8M29Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_JA0JEM29Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_KQHNwM29Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LNbxoM29Ed2O8ZP7tr6cjA"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_MOOgQM29Ed2O8ZP7tr6cjA"
	 */
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