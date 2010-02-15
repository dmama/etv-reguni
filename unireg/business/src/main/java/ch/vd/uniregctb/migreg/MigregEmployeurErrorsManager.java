package ch.vd.uniregctb.migreg;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;

public class MigregEmployeurErrorsManager extends MigregErrorsManager {

	public MigregEmployeurErrorsManager(HostMigratorHelper helper, int ctbStart, int ctbEnd) throws Exception {
		super(helper, ctbStart, ctbEnd);
	}

	@Override
	protected List<Long> getHostCtbsInRange(int debut, int fin) {
		List<Long> list = new ArrayList<Long>();

		JdbcTemplate template = new JdbcTemplate(this.helper.db2DataSource);
		SqlRowSet rows = template.queryForRowSet("SELECT NO_EMPLOYEUR FROM " + helper.getTableIs("employeur") + " WHERE                                                             "
				+ "       (                                                           "
				+ "         ACTIF='O'                                                 "
				+ "      OR                                                           "
				+ "         DESIGN_ABREGEE LIKE '%@%'                                 "
				+ "    ) AND NO_EMPLOYEUR >="+ debut
				+ "      AND NO_EMPLOYEUR <= " + fin);
		while (rows.next()) {
			int nb = rows.getInt(1)+ DebiteurPrestationImposable.FIRST_MIGRATION_ID;
			list.add(new Long(nb));
		}

		SqlRowSet rowsInactif = template.queryForRowSet("SELECT emp.NO_EMPLOYEUR,                                               "
				+ "       emp.DESIGNATION_1,                                                                "
				+ "       emp.DESIGNATION_2,                                                                "
				+ "       emp.DESIGNATION_3,                                                                "
				+ "       emp.DESIGNATION_4,                                                                "
				+ "       emp.CORRESPONDANT,                                                                "
				+ "       emp.NO_TELEPHONE,                                                                 "
				+ "       emp.CO_PERIODICITE,                                                               "
				+ "       emp.CO_RAPPEL,                                                                    "
				+ "       emp.FK_ENTPRNO,                                                                   "
				+ "       emp.FK_CAT_NOEMPL,                                                                "
				+ "       emp.DESIGN_ABREGEE                                                                "
				+ " FROM                                                                                    "
				+ helper.getTableIs("employeur")+" emp,"+ helper.getTableIs("facture_employeur")+" fact     "
				+ " WHERE                                                                                   "
				+ "        (                                                                                "
				+ "           emp.ACTIF='N'                                                                 "
				+ "        AND                                                                              "
				+ "            emp. NO_EMPLOYEUR = fact.FK_CAE_EMPNO                                        "
				+ "        AND                                                                              "
				+ "           fact.FK_TFINOTECH = 1                                                         "
				+ "        AND                                                                              "
				+ "           fact.CO_ETAT = 'B'                                                            "
				+ "        AND                                                                              "
				+ "           YEAR(fact.DAD_PER_EFFECTIVE)= 2009                                            "
				+ "       )                                                                                 "
				+ "        AND NO_EMPLOYEUR >="+ debut
				+ "        AND NO_EMPLOYEUR <= " + fin);
		while (rowsInactif.next()) {
			int nb = rowsInactif.getInt(1)+ DebiteurPrestationImposable.FIRST_MIGRATION_ID;
			list.add(new Long(nb));
		}
		return list;
	}

}
