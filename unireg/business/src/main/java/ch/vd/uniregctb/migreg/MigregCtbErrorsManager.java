package ch.vd.uniregctb.migreg;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class MigregCtbErrorsManager extends MigregErrorsManager {

	public MigregCtbErrorsManager(HostMigratorHelper helper, int ctbStart, int ctbEnd) throws Exception {
		super(helper, ctbStart, ctbEnd);
	}

	@Override
	protected List<Long> getHostCtbsInRange(int debut, int fin) {
		List<Long> list = new ArrayList<Long>();

		JdbcTemplate template = new JdbcTemplate(this.helper.db2DataSource);
		SqlRowSet rows = template.queryForRowSet("SELECT NO_CONTRIBUABLE FROM " + helper.db2Schema + ".CONTRIBUABLE where NO_CONTRIBUABLE >= "
				+ debut + " AND NO_CONTRIBUABLE <= " + fin);
		while (rows.next()) {
			int nb = rows.getInt(1);
			list.add(new Long(nb));
		}
		return list;
	}

}
